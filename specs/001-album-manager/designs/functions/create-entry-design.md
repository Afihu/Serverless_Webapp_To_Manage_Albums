# Purpose of This Lambda 
- Handles the creation of new albums and image entries in the system, especially in DynamoDB.

# Intended Format of incoming event
- Expects an API Gateway event with a JSON body containing a `type` discriminator to distinguish between album and image operations.

## Album Creation Event

For empty album creation:
- `type`: "album" (discriminator)
- `albumName`: Name of the album (required, 1-255 chars)
- `description`: (Optional) Description of the album (0-500 chars)

```json
// In this event, user is trying to create a new empty album.
{
    "albumName": "My Vacation Images",
    "description": "Images from my trip to Hawaii",
    "type": "album"
}
// Note: This request does NOT handle images' entries creation during album creation. 
// Image entries are created separately via the batch image upload event.
```

## Image Metadata Creation Event

For creating image metadata entries after browser upload to S3:
- `type`: "image" (discriminator)
- `albumId`: Identifier of the album to which images are being added (required, must exist)
- `images`: An array of image objects, each containing:
  - `fileName`: Original filename of the image file (may have duplicate suffix appended by client, e.g., `sunset[1].png`)
  - `fileSize`: Size in bytes (1-52,428,800 for 50 MB max)
  - `fileFormat`: Image format (JPEG, PNG, WEBP, GIF, HEIC)
  - `mimeType`: MIME type (e.g., image/jpeg)
  - `width`: Image width in pixels
  - `height`: Image height in pixels
  - `checksumMd5`: MD5 hash of the uploaded file
- `description`: (Optional) Description for the batch upload (applies to all images in batch)

```json
// In this event, user is trying to create metadata for images already uploaded to S3.
// POST /albums/{albumId}/images -> albumId is provided in path parameter.
{
    "albumId": "550e8400-e29b-41d4-a716-446655440000",
    "images": [
        {
            "fileName": "beach.png",
            "fileSize": 2048576,
            "fileFormat": "PNG",
            "mimeType": "image/png",
            "width": 3840,
            "height": 2160,
            "checksumMd5": "5d41402abc4b2a76b9719d911017c592"
        },
        {
            "fileName": "sunset[1].png",
            "fileSize": 3145728,
            "fileFormat": "PNG",
            "mimeType": "image/png",
            "width": 4000,
            "height": 3000,
            "checksumMd5": "6e52503bcd5c3a87d0830d911027d6a3"
        }
    ],
    "description": "Images from my trip to Hawaii",
    "type": "image"
}
// Note: This request can only be sent AFTER the album has been created with matching albumId
// Backend constructs s3Key from: {userId}/IMAGE#{albumId}#{imageId}#OG
// For simplicity, all images in batch share the same description (if provided).
// User can update individual image metadata later via UpdateEntryFunction if needed.
```

# Expected Logic

## Shared-layer
- Most of the validation and DynamoDB interaction logic is abstracted into a shared layer for reuse across different Lambda functions.
- The shared layer provides utility functions for:
  - Input validation through domain models
  - DynamoDB services
  - Error handling
  - Response formatting
  - Logging wrappers

## Album Creation Flow

1. Parse and validate the incoming event via the `type` field
2. For `type: "album"`:
  - **Validation**:
    - Verify `albumName` is not empty/whitespace and 1-255 chars
    - Verify album name is unique per `userId` (strong consistency read to `albums` table), DynamoDB querying logics implemented in shared layer.
      - If duplicate found, return HTTP 409 error
  - **Create Album Entry**:
    - `userId`: Extract from Cognito claims (production) or use "dev-user" (development)
    - `sk`: `ALBUM#{albumId}` where `albumId` is generated UUID v4 by the Lambda function
    - `albumId`: Store the generated UUID v4
    - `albumName`: Use value from request
    - `description`: Use value from request if provided, otherwise omit (optional attribute)
    - `createdAt`: ISO 8601 timestamp (server-generated, current time)
    - `updatedAt`: ISO 8601 timestamp (server-generated, matches createdAt on creation)
    - `imageCount`: Set to 0 (no images initially)
    - `entityType`: Set to "ALBUM" (discriminator for filtering)
  - **Result**: Return HTTP 201 with created album object:
  ```json
  {
    "success": true,
    "albumId": "550e8400-e29b-41d4-a716-446655440000",
  }
  ```

## Image Metadata Creation Flow

1. Parse and validate the incoming event via the `type` field
2. For `type: "image"`:
  - **Validation**:
    - Verify `albumId` exists in DynamoDB with `Query(PK=userId, SK="ALBUM#{albumId}")` (strong consistency), logics implemented in shared layer.
      - If album not found, return HTTP 404 error
    - For each image in `images` array:
      - Verify `fileName` is 1-255 chars
      - Verify `fileSize` is 1-52,428,800 bytes (1 to 50 MB)
      - Verify `fileFormat` is one of: JPEG, PNG, WEBP, GIF, HEIC
      - Verify `mimeType` corresponds to `fileFormat`
      - Verify `width` and `height` are > 0
      - Verify `checksumMd5` is 32-char hex string
  - **Create Image Entries**: For each image in batch:
    - `userId`: Extract from Cognito claims (production) or use "dev-user" (development)
    - `sk`: `IMAGE#{albumId}#{imageId}` (use provided `albumId` and generated `imageId`)
    - `imageId`: Generate the UUID v4 for each image and store it
    - `albumId`: Store the album reference provided in request
    - `fileName`: Use value from request
    - `fileSize`: Use value from request
    - `fileFormat`: Use value from request
    - `uploadedAt`: ISO 8601 timestamp (server-generated, current time)
    - `s3Key`: Construct as `{userId}/IMAGE#{albumId}#{imageId}#OG` (backend-generated from inputs)
    - `thumbnailS3Key`: Set to `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp` (will be populated by ProcessImageFunction)
    - `width`: Use value from request
    - `height`: Use value from request
    - `mimeType`: Use value from request
    - `uploadStatus`: Set to "COMPLETED" (browser uploaded to S3 before this Lambda was invoked)
    - `resizeStatus`: Set to "PENDING" (ProcessImageFunction will handle resize asynchronously)
    - `resizeCompletedAt`: Omit (will be populated by ProcessImageFunction when complete)
    - `checksumMd5`: Use value from request
    - `entityType`: Set to "IMAGE" (discriminator for filtering)
  - **After all metadata is written**: the number of images created in this batch is passed to `UpdateEntry` Lambda and have it update asynchronously:
    - Invoke `UpdateEntryFunction` with:
      ```json
        {
          "type": "album",
          "userId": "dev-user",
          "albumId": "550e8400-e29b-41d4-a716-446655440000",
          "operation": "increment_image_count",
          "imageCount": 2
        }
      ```
  - **Result**: Return HTTP 201 with array of created image objects:
    ```json
    {
      "success": true,
      "images": [
        {
          "imageId": "660f9511-f30c-52e5-b817-557766551111",
          "fileName": "beach.png",  // Echo back for confirmation in case the file name is as-is
          "s3Key": "dev-user/IMAGE#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111#OG",
          "thumbnailS3Key": "dev-user/IMAGE#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111/thumb-256.webp"
        },
        {
          "imageId": "770g0612-g41d-63f6-c918-668877662222",
          "fileName": "sunset[1].png", // Echo back for confirmation in case the file name has duplicate suffix
          "s3Key": "dev-user/IMAGE#550e8400-e29b-41d4-a716-446655440000#770g0612-g41d-63f6-c918-668877662222#OG",
          "thumbnailS3Key": "dev-user/IMAGE#550e8400-e29b-41d4-a716-446655440000#770g0612-g41d-63f6-c918-668877662222/thumb-256.webp"
        }
      ],
      "albumId": "550e8400-e29b-41d4-a716-446655440000"
    }
    ```
    - Response includes `fileName` for confirmation (helps client verify correct request was processed)
    - Client already maintains local file → fileName mapping from request phase
    - `s3Key` and `thumbnailS3Key` can be used directly for GetUploadUrl calls

## Error Handling

- **Album Name Already Exists**: Return HTTP 409 (Conflict) with error message
- **Album Not Found**: Return HTTP 404 (Not Found) with error message
- **Invalid Input**: Return HTTP 400 (Bad Request) with detailed validation error
- **DynamoDB Error**: Return HTTP 500 (Internal Server Error) with error message
- **Concurrent Album Deletion**: Handle gracefully with retry logic or clear error message

---

# Validation Rules

## Album Validation

**Input Validation**:

1. albumName is not null/blank
2. albumName length: 1-255 characters
3. albumName must not be whitespace-only
4. albumName must be unique per userId (strong consistency read before write)


**Java Implementation**:
// CreateAlbumFunction validation
if (albumName == null || albumName.isBlank()) {
  throw new ValidationException("Album name is required");
}
if (albumName.length() < 1 || albumName.length() > 255) {
  throw new ValidationException("Album name must be between 1 and 255 characters");
}
if (albumName.trim().isEmpty()) {
  throw new ValidationException("Album name cannot be whitespace only");
}

// Check for duplicate album names per user (strong consistency)
QueryRequest queryRequest = new QueryRequest()
  .withTableName("albums")
  .withKeyConditionExpression("userId = :userId AND begins_with(sk, :albumPrefix)")
  .addExpressionAttributeValuesEntry(":userId", userId)
  .addExpressionAttributeValuesEntry(":albumPrefix", "ALBUM#")
  .withConsistentRead(true);

QueryResult result = dynamoDbClient.query(queryRequest);
for (Map<String, AttributeValue> item : result.getItems()) {
  String existingName = item.get("albumName").getS();
  if (albumName.equals(existingName)) {
    throw new ValidationException("Album name already exists for this user");
  }
}


**Constraints from data-model.md**:
- `albumName` cannot be empty or whitespace-only
- `albumName` must be unique per `userId` (application-level enforcement in Lambda)
- `createdAt` must not be in future (server generates current time)

---

## Image Metadata Validation

**Input Validation per Image**:
```
1. imageId is a valid UUID v4 format
2. fileName is 1-255 characters
3. fileSize is 1 to 52,428,800 bytes (50 MB)
4. fileFormat is one of: JPEG, PNG, WEBP, GIF, HEIC
5. mimeType corresponds to fileFormat
6. width and height are > 0
7. checksumMd5 is a 32-character hexadecimal string
8. albumId exists in DynamoDB with matching userId
```

**Java Implementation**:
```java
// Validation for each image in batch
for (ImageMetadata imageMetadata : images) {
  // 1. Validate imageId is UUID v4
  try {
    UUID.fromString(imageMetadata.getImageId());
  } catch (IllegalArgumentException e) {
    throw new ValidationException("imageId must be a valid UUID v4 format");
  }

  // 2. Validate fileName (1-255 chars)
  if (imageMetadata.getFileName() == null || imageMetadata.getFileName().isBlank()) {
    throw new ValidationException("fileName cannot be null or blank");
  }
  if (imageMetadata.getFileName().length() < 1 || imageMetadata.getFileName().length() > 255) {
    throw new ValidationException("fileName must be between 1 and 255 characters");
  }

  // 3. Validate fileSize (1 to 50 MB)
  long fileSize = imageMetadata.getFileSize();
  if (fileSize <= 0 || fileSize > 52_428_800L) {
    throw new ValidationException("fileSize must be between 1 and 52,428,800 bytes (50 MB)");
  }

  // 4. Validate fileFormat
  ImageFormat format = ImageFormat.valueOf(imageMetadata.getFileFormat());
  if (format == null) {
    throw new ValidationException("fileFormat must be one of: JPEG, PNG, WEBP, GIF, HEIC");
  }

  // 5. Validate mimeType matches fileFormat
  if (!format.getMimeType().equalsIgnoreCase(imageMetadata.getMimeType())) {
    throw new ValidationException(
      String.format("mimeType '%s' does not match fileFormat '%s'",
        imageMetadata.getMimeType(), format)
    );
  }

  // 6. Validate width and height > 0
  if (imageMetadata.getWidth() <= 0 || imageMetadata.getHeight() <= 0) {
    throw new ValidationException("width and height must be greater than 0");
  }

  // 7. Validate checksumMd5 (32-char hex string)
  if (!imageMetadata.getChecksumMd5().matches("^[0-9a-fA-F]{32}$")) {
    throw new ValidationException("checksumMd5 must be a 32-character hexadecimal string");
  }
}

// 8. Verify albumId exists
QueryRequest queryRequest = new QueryRequest()
  .withTableName("albums")
  .withKeyConditionExpression("userId = :userId AND sk = :albumSk")
  .addExpressionAttributeValuesEntry(":userId", userId)
  .addExpressionAttributeValuesEntry(":albumSk", "ALBUM#" + albumId)
  .withConsistentRead(true);

QueryResult result = dynamoDbClient.query(queryRequest);
if (result.getCount() == 0) {
  throw new ValidationException("Album not found");
}
```

**Constraints from data-model.md**:
- `albumId` must exist in table with matching `userId`
- `fileSize` must be 1 to 52,428,800 bytes (50 MB)
- `fileFormat` must be one of: JPEG, PNG, WEBP, GIF, HEIC
- `uploadedAt` must not be in future (server generates current time)
- `width` and `height` must be > 0
- `mimeType` must correspond to `fileFormat`

---

# S3 Key Construction

When creating image metadata, the Lambda constructs S3 keys using the following format:

**Original Image Key** (albums-prod bucket):
```
{userId}/IMAGE#{albumId}#{imageId}#OG
```

**Thumbnail Key** (image-thumbnails bucket, generated later by ProcessImageFunction):
```
{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp
```

**Java Implementation**:
```java
// Construct S3 keys from input parameters
String s3KeyOriginal = userId + "/IMAGE#" + albumId + "#" + imageId + "#OG";
String s3KeyThumbnail = userId + "/IMAGE#" + albumId + "#" + imageId + "/thumb-256.webp";

// Store in DynamoDB image record
imageRecord.setS3Key(s3KeyOriginal);
imageRecord.setThumbnailS3Key(s3KeyThumbnail);  // Will be populated by ProcessImageFunction
```

**Benefits**:
- Direct path construction from DynamoDB records (no lookup needed)
- Alignment with DynamoDB sort key structure for consistency
- Enables type-prefixed range queries on S3 keys if needed      