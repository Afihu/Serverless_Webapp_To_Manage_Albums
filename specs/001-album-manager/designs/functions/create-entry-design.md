# Purpose of This Lambda 
- Handles the creation of new albums' and images' entries in the system, especially in DynamoDB.

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
  - `imageId`: incremental ID (temporary ID to group batch uploads)
  - `fileName`: Original filename of the image file (backend constructs S3 path from this)
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
            "tempImageId": "1",
            "fileName": "beach.png",
            "fileSize": 2048576,
            "fileFormat": "PNG",
            "mimeType": "image/png",
            "width": 3840,
            "height": 2160,
            "checksumMd5": "5d41402abc4b2a76b9719d911017c592"
        },
        {
            "tempImageId": "2",
            "fileName": "sunset.png",
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
// Note: This request can only be sent AFTER:
// 1. Album has been created with matching albumId
// 2. Images have been uploaded to S3 via presigned URL from GetUploadURLFunction
// Backend constructs s3Key from: {userId}/ALBUM#{albumId}/original/{fileName}
// For simplicity, all images in batch share the same description (if provided).
// User can update individual image metadata later via UpdateEntryFunction if needed.
```

# Expected Logic

## Album Creation Flow

1. Parse and validate the incoming event via the `type` field
2. For `type: "album"`:
   - **Validation**:
     - Verify `albumName` is not empty/whitespace and 1-255 chars
     - Verify album name is unique per `userId` (strong consistency read to `albums` table)
   - **Create Album Entry**:
     - `userId`: Extract from Cognito claims (production) or use "dev-user" (development)
     - `sk`: `ALBUM#{albumId}` where `albumId` is generated UUID v4
     - `albumId`: Store the generated UUID v4
     - `albumName`: Use value from request
     - `description`: Use value from request if provided, otherwise omit (optional attribute)
     - `createdAt`: ISO 8601 timestamp (server-generated, current time)
     - `updatedAt`: ISO 8601 timestamp (server-generated, matches createdAt on creation)
     - `imageCount`: Set to 0 (no images initially)
     - `entityType`: Set to "ALBUM" (discriminator for filtering)
   - **Result**: Return HTTP 201 with created album object including generated `albumId`

## Image Metadata Creation Flow

1. Parse and validate the incoming event via the `type` field
2. For `type: "image"`:
   - **Validation**:
     - Verify `albumId` exists in DynamoDB with `Query(PK=userId, SK="ALBUM#{albumId}")` (strong consistency)
     - If album not found, return HTTP 404 error
     - For each image in `images` array:
       - Verify `imageId` is UUID v4 format
       - Verify `fileName` is 1-255 chars
       - Verify `fileSize` is 1-52,428,800 bytes (1 to 50 MB)
       - Verify `fileFormat` is one of: JPEG, PNG, WEBP, GIF, HEIC
       - Verify `mimeType` corresponds to `fileFormat`
       - Verify `width` and `height` are > 0
       - Verify `checksumMd5` is 32-char hex string
   - **Create Image Entries**: For each image in batch:
     - `userId`: Extract from Cognito claims (production) or use "dev-user" (development)
     - `sk`: `IMAGE#{albumId}#{imageId}` (use provided `albumId` and `imageId`)
     - `imageId`: Store the UUID v4 provided in request
     - `albumId`: Store the album reference
     - `fileName`: Use value from request
     - `fileSize`: Use value from request
     - `fileFormat`: Use value from request
     - `uploadedAt`: ISO 8601 timestamp (server-generated, current time)
     - `s3Key`: Construct as `{userId}/ALBUM#{albumId}/original/{fileName}` (backend-generated from inputs)
     - `thumbnailS3Key`: Set to `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp` (will be populated by ProcessImageFunction)
     - `width`: Use value from request
     - `height`: Use value from request
     - `mimeType`: Use value from request
     - `uploadStatus`: Set to "COMPLETED" (browser uploaded to S3 before this Lambda was invoked)
     - `resizeStatus`: Set to "PENDING" (ProcessImageFunction will handle resize asynchronously)
     - `resizeCompletedAt`: Omit (will be populated by ProcessImageFunction when complete)
     - `checksumMd5`: Use value from request
     - `entityType`: Set to "IMAGE" (discriminator for filtering)
   - **Update Album imageCount**:
     - Query all existing images for this album: `Query(PK=userId, SK begins_with "IMAGE#{albumId}#")`
     - Count the total images (existing + newly created in this batch)
     - Update album record: `UpdateItem(PK=userId, SK="ALBUM#{albumId}", imageCount=totalCount)`
     - Use conditional update to ensure album still exists
   - **Result**: Return HTTP 201 with array of created image objects

## Error Handling

- **Album Name Already Exists**: Return HTTP 409 (Conflict) with error message
- **Album Not Found**: Return HTTP 404 (Not Found) with error message
- **Invalid Input**: Return HTTP 400 (Bad Request) with detailed validation error
- **DynamoDB Error**: Return HTTP 500 (Internal Server Error) with error message
- **Concurrent Album Deletion**: Handle gracefully with retry logic or clear error message      