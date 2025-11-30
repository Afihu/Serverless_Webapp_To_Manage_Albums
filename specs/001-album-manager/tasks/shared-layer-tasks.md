# Shared Layer Implementation Tasks

**Project**: Image Album Manager  
**Component**: Shared Utilities Layer (Lambda Layer)  
**Purpose**: Common models, services, utilities, and exception handling for all 7 operation-based Lambdas  
**Created**: 2025-11-25  
**Dependency Status**: CRITICAL PATH - Must complete before any Lambda functions can be implemented

---

## Overview

The shared layer provides centralized, reusable code consumed by all Lambda functions (CreateEntry, ReadEntry, UpdateEntry, DeleteEntry, ListEntries, GetUploadURL, ProcessImage). This layer is packaged as a JAR and deployed as an AWS Lambda Layer, reducing code duplication and ensuring consistency.

### Package Structure

```
com.album/
├── models/              # Entity DTOs and domain objects
├── services/            # Business logic for DynamoDB and S3 operations
├── util/                # Helper utilities (logging, ID generation, type discrimination)
├── exception/           # Custom exceptions with AWS error context
├── handler/             # Base handler classes with common Lambda patterns
└── request/             # Request/Response DTOs for API contract validation
```

---

## Phase 1: Domain Models (Models)

These are fundamental DTO and entity classes representing the core domain. All are immutable or near-immutable, with validation baked in where appropriate.

### T1.1 - Album Entity Model [✅ COMPLETED]

**File**: `src/main/java/com/albummanager/models/Album.java`

**Status**: ✅ Implemented with Builder pattern, immutable fields, comprehensive validation

**Class Definition**:
```java
public class Album {
    private final String userId;           // e.g., "auth0|user-123abc"
    private final String albumId;          // UUID v4, generated server-side
    private final String albumName;        // 1-255 chars, unique per userId
    private final String description;      // Optional, 0-500 chars
    private final long createdAt;          // Unix timestamp (milliseconds)
    private final long updatedAt;          // Unix timestamp (milliseconds)
    private final int imageCount;          // Denormalized count, >= 0
}
```

**Responsibilities**:
- Immutable data carrier (final fields)
- Constructor with builder pattern or all-args constructor + validation
- Getters for all fields
- `equals()`, `hashCode()`, `toString()` implementations
- No business logic beyond accessors

**Validation**:
- `albumName`: not null, not blank, 1-255 chars, trim whitespace
- `description`: optional, 0-500 chars if present
- `userId`: not null, not blank, 1-128 chars
- `albumId`: not null, valid UUID v4 format
- `imageCount`: >= 0
- `createdAt` <= `updatedAt` (timestamp validation)

**Dependencies**: None (pure POJO)

**Test Cases**:
- ✅ Valid album creation with all fields
- ✅ Valid album creation with optional description null
- ✅ Album with maximum length name (255 chars)
- ✅ Album with minimum length name (1 char)
- ✅ Invalid: albumName blank or empty
- ✅ Invalid: albumName > 255 chars
- ✅ Invalid: description > 500 chars
- ✅ Invalid: userId null or blank
- ✅ Invalid: createdAt > updatedAt

---

### T1.2 - Image Entity Model [✅ COMPLETED]

**File**: `src/main/java/com/albummanager/models/Image.java`

**Status**: ✅ Implemented with all required fields, enum usage (ImageFormat, UploadStatus, ResizeStatus), comprehensive validation

**Class Definition**:
```java
public class Image {
    private final String userId;
    private final String imageId;          // UUID v4, generated server-side
    private final String albumId;          // Reference to parent album
    private final String fileName;         // Original filename, 1-255 chars
    private final long fileSize;           // Bytes, 1-52,428,800 (50 MB)
    private final String fileFormat;       // Enum: JPEG, PNG, WEBP, GIF, HEIC
    private final long uploadedAt;         // Unix timestamp (milliseconds)
    private final String s3Key;            // Path in albums-prod bucket
    private final String thumbnailS3Key;   // Path in image-thumbnails bucket
    private final int width;               // Image dimension in pixels, > 0
    private final int height;              // Image dimension in pixels, > 0
    private final String mimeType;         // e.g., "image/jpeg"
    private final String uploadStatus;     // Enum: PENDING, COMPLETED, FAILED
    private final String checksumMd5;      // 32-char hex string
    private final String resizeStatus;     // Enum: PENDING, COMPLETED, FAILED
    private final Long resizeCompletedAt;  // Unix timestamp, optional (null if not completed)
}
```

**Responsibilities**:
- Immutable data carrier for image metadata
- All getters; no setters
- `equals()`, `hashCode()`, `toString()` implementations

**Validation**:
- `imageId`: not null, valid UUID v4
- `albumId`: not null, valid UUID v4
- `fileName`: not null, not blank, 1-255 chars
- `fileSize`: > 0, <= 52,428,800 bytes
- `fileFormat`: one of {JPEG, PNG, WEBP, GIF, HEIC}
- `mimeType`: must correspond to fileFormat (e.g., "image/jpeg" for JPEG)
- `width`, `height`: > 0
- `uploadStatus`: one of {PENDING, COMPLETED, FAILED}
- `resizeStatus`: one of {PENDING, COMPLETED, FAILED}
- `checksumMd5`: 32-char hex string (alphanumeric 0-9a-f)
- `uploadedAt`: not null, Unix timestamp
- `resizeCompletedAt`: only present if resizeStatus == COMPLETED

**Dependencies**: None

**Test Cases**:
- ✅ Valid image creation with all required fields
- ✅ Valid image with null resizeCompletedAt (not yet processed)
- ✅ Valid image with resizeCompletedAt present (processed)
- ✅ Image with maximum fileSize (exactly 50 MB)
- ✅ Image with minimum fileSize (1 byte)
- ✅ Invalid: fileSize > 50 MB
- ✅ Invalid: fileFormat not in allowed enum
- ✅ Invalid: mimeType doesn't match fileFormat
- ✅ Invalid: width or height <= 0
- ✅ Invalid: uploadStatus not in enum
- ✅ Invalid: resizeStatus not in enum
- ✅ Invalid: checksumMd5 not 32-char hex

---

### T1.3 - Enums: ImageFormat, UploadStatus, ResizeStatus [✅ COMPLETED]

**Files**:
- `src/main/java/com/albummanager/models/ImageFormat.java` ✅
- `src/main/java/com/albummanager/models/UploadStatus.java` ✅
- `src/main/java/com/albummanager/models/ResizeStatus.java` ✅

**Status**: ✅ All enums implemented with helper methods (getMimeType, fromMimeType, parse)

**Definition**:
```java
public enum ImageFormat {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    GIF("image/gif", "gif"),
    HEIC("image/heic", "heic");
    
    private final String mimeType;
    private final String extension;
    
    // Methods:
    // - getMimeType()
    // - getExtension()
    // - fromMimeType(String mimeType) -> ImageFormat (throws IllegalArgumentException if not found)
    // - isSupported(String mimeType) -> boolean
}
```

**Files**:
- `src/main/java/com/albummanager/models/UploadStatus.java` → PENDING, COMPLETED, FAILED
- `src/main/java/com/albummanager/models/ResizeStatus.java` → PENDING, COMPLETED, FAILED

**Dependencies**: None

---

### T1.4 - Entry Superclass / Interface [✅ COMPLETED]

**File**: `src/main/java/com/albummanager/models/Entry.java` ✅

**Status**: ✅ Interface implemented; Album and Image both implement Entry

**Purpose**: Polymorphic base for Album and Image to enable unified handling in services

**Definition** (interface approach):
```java
public interface Entry {
    String getUserId();
    String getId();           // albumId or imageId
    long getCreatedAt();      // Unix timestamp
    String getEntityType();   // "album" or "image"
}
```

**Implementations**: Both Album and Image implement Entry

**Dependencies**: Album, Image

**Test Cases**:
- ✅ Album implements Entry correctly
- ✅ Image implements Entry correctly
- ✅ Can store Album in List<Entry>
- ✅ Can store Image in List<Entry>

---

### T1.5 - DynamoDB Sort Key Builder Utility [✅ COMPLETED]

**File**: `src/main/java/com/albummanager/util/SortKeyBuilder.java` ✅

**Status**: ✅ All methods implemented with comprehensive validation and error handling

**Purpose**: Centralize sort key formatting for type-prefixed composite keys

**Methods**:
```java
public class SortKeyBuilder {
    // Build sort keys with type prefixes
    public static String buildAlbumSortKey(String albumId) -> "ALBUM#{albumId}"
    
    public static String buildImageSortKey(String albumId, String imageId) -> "IMAGE#{albumId}#{imageId}"
    
    // Parse sort keys to extract components
    public static String extractAlbumId(String sortKey) -> "albumId" (throws exception if invalid)
    
    public static Map<String, String> extractImageIds(String sortKey) -> {albumId, imageId}
    
    // Validation helpers
    public static boolean isAlbumSortKey(String sortKey) -> boolean
    
    public static boolean isImageSortKey(String sortKey) -> boolean
    
    // Query prefix builders (for QuerySpec operations)
    public static String albumQueryPrefix() -> "ALBUM#"
    
    public static String imageQueryPrefix(String albumId) -> "IMAGE#{albumId}#"
}
```

**Dependencies**: None

**Test Cases**:
- ✅ Build album sort key correctly
- ✅ Build image sort key correctly
- ✅ Extract album ID from valid album sort key
- ✅ Extract image IDs from valid image sort key
- ✅ Invalid sort key throws exception
- ✅ Query prefix builder for albums
- ✅ Query prefix builder for images

---

## Phase 2: Exception Handling (Exception)

### T2.1 - Custom Exception Hierarchy [P]

**Files**:
- `src/main/java/com/albummanager/exception/AlbumManagerException.java` (base)
- `src/main/java/com/albummanager/exception/ValidationException.java`
- `src/main/java/com/albummanager/exception/ResourceNotFoundException.java`
- `src/main/java/com/albummanager/exception/DuplicateAlbumException.java`
- `src/main/java/com/albummanager/exception/S3OperationException.java`
- `src/main/java/com/albummanager/exception/DynamoDBOperationException.java`
- `src/main/java/com/albummanager/exception/ImageProcessingException.java`

**Base Exception**:
```java
public class AlbumManagerException extends Exception {
    private final String errorCode;      // Machine-readable code (e.g., "ALBUM_NOT_FOUND")
    private final int httpStatus;        // HTTP status code (400, 404, 500)
    private final Map<String, Object> context;  // Additional context (userId, albumId, etc.)
    
    // Getters for error code, status, context
    // toString() includes all context for logging
}
```

**Specific Exceptions**:
- `ValidationException` → 400 Bad Request (missing fields, invalid format)
- `ResourceNotFoundException` → 404 Not Found (album/image doesn't exist)
- `DuplicateAlbumException` → 409 Conflict (album name already exists)
- `S3OperationException` → 500/503 (S3 SDK errors, retryable)
- `DynamoDBOperationException` → 500/503 (DynamoDB SDK errors)
- `ImageProcessingException` → 500 (image resizing, validation failures)

**Dependencies**: None

**Test Cases**:
- ✅ Create exception with error code and HTTP status
- ✅ Exception with context data is properly captured
- ✅ Exception hierarchy (all extend AlbumManagerException)
- ✅ toString() includes context for debugging

---

## Phase 3: DynamoDB Service (Services)

### T3.1 - DynamoDB Configuration & Client Initialization [P]

**File**: `src/main/java/com/albummanager/services/DynamoDBConfig.java`

**Purpose**: Centralized DynamoDB client setup, table configuration, and connection pooling

**Responsibilities**:
```java
public class DynamoDBConfig {
    // Static singleton instance
    private static DynamoDBClient client;
    
    // Initialize client with region, credentials, retry policy
    public static DynamoDBClient getClient() -> DynamoDBClient
    
    // Configuration constants
    public static final String TABLE_NAME = "albums";
    public static final String PK_NAME = "userId";
    public static final String SK_NAME = "sk";
    
    // Close/cleanup (for graceful shutdown)
    public static void close()
}
```

**Configuration Details**:
- Region: Environment variable `AWS_REGION` (default: us-east-1)
- Retry policy: exponential backoff, max 3 retries for transient errors
- Timeout: 30 seconds per request
- Connection pooling: Default AWS SDK connection pool

**Dependencies**: AWS SDK v2 (DynamoDB)

---

### T3.2 - Album CRUD Service [P]

**File**: `src/main/java/com/albummanager/services/AlbumService.java`

**Core Methods**:

```java
public class AlbumService {
    // CREATE
    public Album createAlbum(String userId, String albumName, String description)
        throws ValidationException, DuplicateAlbumException, DynamoDBOperationException
    
    // READ (single)
    public Album getAlbum(String userId, String albumId)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // READ (list all user albums)
    public List<Album> listAlbums(String userId)
        throws DynamoDBOperationException
    
    // READ (list with pagination)
    public PaginatedResult<Album> listAlbumsWithPagination(String userId, String paginationToken, int pageSize)
        throws DynamoDBOperationException
    
    // UPDATE
    public Album updateAlbum(String userId, String albumId, UpdateAlbumRequest updateRequest)
        throws ResourceNotFoundException, ValidationException, DuplicateAlbumException, DynamoDBOperationException
    
    // DELETE
    public void deleteAlbum(String userId, String albumId)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // Increment image count (atomic DynamoDB update)
    public void incrementImageCount(String userId, String albumId, int increment)
        throws DynamoDBOperationException
    
    // Helper: Validate album name uniqueness
    private boolean isAlbumNameUnique(String userId, String albumName)
        throws DynamoDBOperationException
}
```

**Implementation Details**:

**createAlbum**:
- Validate `albumName`: not empty, 1-255 chars, no leading/trailing spaces
- Check for duplicate album name via query: `Query(userId, "ALBUM#" begins_with)` and scan for name match
- Generate UUID v4 for albumId
- Write to DynamoDB with `uploadStatus: COMPLETED`, `imageCount: 0`
- Catch `ConditionalCheckFailedException` → DuplicateAlbumException
- Catch other SDK exceptions → DynamoDBOperationException with context (userId, albumName)
- Log: "Album created: {albumId} for user {userId}"

**getAlbum**:
- Construct sort key: `ALBUM#{albumId}`
- GetItem from DynamoDB with strong consistency
- If not found → ResourceNotFoundException
- Return Album object
- Log: "Album retrieved: {albumId}"

**listAlbums**:
- Query DynamoDB: `Query(PK=userId, SK begins_with "ALBUM#")`
- Use eventually consistent read (cheaper, acceptable for list view per data-model.md)
- Parse all results into Album list
- Sort by createdAt descending (newest first)
- Handle pagination token if provided
- Return List<Album>

**listAlbumsWithPagination**:
- Same as listAlbums but honor pagination token and pageSize
- Return `PaginatedResult<Album>` with nextToken (base64-encoded lastEvaluatedKey)

**updateAlbum**:
- Validate album exists: GetItem first
- Validate new albumName (if provided): check uniqueness if changed
- Use UpdateExpression to atomically update: `albumName`, `description`, `updatedAt`
- Return updated Album object
- Log: "Album updated: {albumId}"

**deleteAlbum**:
- Validate album exists first
- Validate album is empty (imageCount == 0) via strong consistency read
  - If not empty, throw DynamoDBOperationException (cannot delete album with images)
- DeleteItem from DynamoDB
- Log: "Album deleted: {albumId}"

**incrementImageCount**:
- Use UpdateExpression to atomically increment: `SET imageCount = imageCount + :val`
- Handle if album doesn't exist (no-op or exception depends on use case)

**Dependencies**: DynamoDBConfig, Album, AlbumManagerException hierarchy, SortKeyBuilder, Logger

**Test Cases**:
- ✅ Create album with valid name
- ✅ Create album with description
- ✅ Invalid: albumName blank
- ✅ Invalid: albumName > 255 chars
- ✅ Duplicate album name throws DuplicateAlbumException
- ✅ Retrieve existing album
- ✅ Retrieve non-existent album throws ResourceNotFoundException
- ✅ List all albums for user returns sorted list
- ✅ List with pagination returns nextToken when > pageSize
- ✅ Update album name (check uniqueness)
- ✅ Update description
- ✅ Delete empty album succeeds
- ✅ Delete non-empty album throws exception
- ✅ Increment imageCount atomically

---

### T3.3 - Image CRUD Service [P]

**File**: `src/main/java/com/albummanager/services/ImageService.java`

**Core Methods**:

```java
public class ImageService {
    // CREATE (initial, with uploadStatus=PENDING)
    public Image createImage(String userId, String albumId, CreateImageRequest request)
        throws ValidationException, ResourceNotFoundException, DynamoDBOperationException
    
    // READ (single)
    public Image getImage(String userId, String imageId, String albumId)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // READ (all in album)
    public List<Image> listImagesInAlbum(String userId, String albumId)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // READ (with pagination)
    public PaginatedResult<Image> listImagesInAlbumWithPagination(
        String userId, String albumId, String paginationToken, int pageSize)
        throws DynamoDBOperationException
    
    // UPDATE (status fields)
    public Image updateImageStatus(String userId, String albumId, String imageId, String uploadStatus, String resizeStatus)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // UPDATE (resize completion)
    public Image markImageResizeComplete(String userId, String albumId, String imageId)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // DELETE
    public void deleteImage(String userId, String albumId, String imageId)
        throws ResourceNotFoundException, DynamoDBOperationException
    
    // Helper: Get all images in album for cascading delete
    public List<Image> getAllImagesInAlbumForDeletion(String userId, String albumId)
        throws DynamoDBOperationException
}
```

**Implementation Details**:

**createImage**:
- Validate album exists: call AlbumService.getAlbum()
- Validate request fields: fileName, fileSize, fileFormat, mimeType
  - Check fileSize <= 50 MB
  - Check fileFormat in {JPEG, PNG, WEBP, GIF, HEIC}
  - Validate mimeType matches fileFormat
- Generate UUID v4 for imageId
- Construct S3 keys: 
  - `s3Key`: `{userId}/ALBUM#{albumId}/original/{fileName}`
  - `thumbnailS3Key`: `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp`
- Write to DynamoDB with `uploadStatus: PENDING`, `resizeStatus: PENDING`
- Increment album's imageCount via AlbumService.incrementImageCount()
- Return Image object
- Log: "Image created: {imageId} in album {albumId}"

**getImage**:
- Construct sort key: `IMAGE#{albumId}#{imageId}`
- GetItem from DynamoDB
- If not found → ResourceNotFoundException
- Return Image object

**listImagesInAlbum**:
- Query DynamoDB: `Query(PK=userId, SK begins_with "IMAGE#{albumId}#")`
- Eventually consistent read
- Parse all results, filter by uploadStatus == COMPLETED (only show uploaded images)
- Sort by uploadedAt descending
- Return List<Image>

**listImagesInAlbumWithPagination**:
- Same as above but with pagination token support
- Return PaginatedResult<Image>

**updateImageStatus**:
- Validate image exists: GetItem
- UpdateExpression to set uploadStatus and/or resizeStatus
- If uploadStatus changes from PENDING → COMPLETED:
  - Decrement album imageCount by 1 initially, then re-add via increment
  - OR use conditional write to ensure count accuracy
- If resizeStatus changes to COMPLETED:
  - Set `resizeCompletedAt` to current Unix timestamp
- Return updated Image
- Log: "Image status updated: {imageId}, uploadStatus={new}, resizeStatus={new}"

**markImageResizeComplete**:
- Validate image exists
- UpdateExpression to set resizeStatus: COMPLETED, resizeCompletedAt: now()
- Return updated Image

**deleteImage**:
- Validate image exists (GetItem)
- DeleteItem from DynamoDB (sort key: IMAGE#{albumId}#{imageId})
- Decrement album imageCount via AlbumService
- Log: "Image deleted: {imageId} from album {albumId}"
- Note: Caller (DeleteEntryFunction) responsible for S3 cleanup

**getAllImagesInAlbumForDeletion**:
- Query all images in album
- Do NOT filter by uploadStatus (get all, even pending/failed)
- Return List<Image> for cascading delete

**Dependencies**: DynamoDBConfig, Image, AlbumService, AlbumManagerException hierarchy, SortKeyBuilder, Logger

**Test Cases**:
- ✅ Create image with valid metadata
- ✅ Invalid: fileSize > 50 MB throws exception
- ✅ Invalid: fileFormat not supported
- ✅ Invalid: mimeType doesn't match fileFormat
- ✅ Create image in non-existent album throws ResourceNotFoundException
- ✅ Create image increments album imageCount
- ✅ Retrieve existing image
- ✅ Retrieve non-existent image throws exception
- ✅ List images in album (only COMPLETED ones)
- ✅ List with pagination
- ✅ Update image status atomically
- ✅ Mark resize complete sets timestamp
- ✅ Delete image decrements album imageCount
- ✅ Get all images in album (including pending/failed)

---

## Phase 4: S3 Service (Services)

### T4.1 - S3 Configuration & Client Initialization [P]

**File**: `src/main/java/com/albummanager/services/S3Config.java`

**Responsibilities**:
```java
public class S3Config {
    private static S3Client client;
    
    public static S3Client getClient() -> S3Client
    
    public static final String IMAGES_BUCKET = "albums-prod";
    public static final String THUMBNAILS_BUCKET = "image-thumbnails";
    public static final String STATIC_ASSETS_BUCKET = "albums-static";
    
    public static void close()
}
```

**Configuration**:
- Region: Environment variable `AWS_REGION` (default: us-east-1)
- Retry policy: exponential backoff, max 3 retries

---

### T4.2 - S3 Utility Service [P]

**File**: `src/main/java/com/albummanager/services/S3Service.java`

**Core Methods**:

```java
public class S3Service {
    // Generate presigned URLs for download (GET)
    public String generatePresignedDownloadUrl(String bucket, String s3Key, long expirationSeconds)
        throws S3OperationException
    
    // Generate presigned URLs for upload (POST)
    public String generatePresignedUploadUrl(String bucket, String s3Key, long expirationSeconds)
        throws S3OperationException
    
    // Upload object to S3
    public void uploadObject(String bucket, String s3Key, byte[] data, String contentType)
        throws S3OperationException
    
    // Download object from S3
    public byte[] downloadObject(String bucket, String s3Key)
        throws S3OperationException, ResourceNotFoundException
    
    // Delete object from S3
    public void deleteObject(String bucket, String s3Key)
        throws S3OperationException
    
    // Delete multiple objects (for album deletion)
    public void deleteObjects(String bucket, List<String> s3Keys)
        throws S3OperationException
    
    // Check if object exists
    public boolean objectExists(String bucket, String s3Key)
        throws S3OperationException
    
    // Get object metadata (size, content-type, etc.)
    public ObjectMetadata getObjectMetadata(String bucket, String s3Key)
        throws S3OperationException, ResourceNotFoundException
}
```

**Implementation Details**:

**generatePresignedDownloadUrl**:
- Use S3Presigner: `GetObjectRequest` for specified bucket/key
- Set expiration: `GetUrlRequest` with `expirationSeconds`
- Default expiration if not provided: 3600 seconds (1 hour)
- Return presigned URL string
- Catch `SdkException` → S3OperationException with context

**generatePresignedUploadUrl**:
- Use S3Presigner: `PutObjectRequest` for specified bucket/key
- Set expiration: `GetUrlRequest` with `expirationSeconds`
- Default: 900 seconds (15 minutes for uploads)
- Return presigned URL string
- Catch `SdkException` → S3OperationException

**uploadObject**:
- Call `PutObjectRequest` with bucket, key, data, contentType
- Catch `SdkException` → S3OperationException with retry hint

**downloadObject**:
- Call `GetObjectRequest` for bucket/key
- Stream response to byte array
- If 404 (NoSuchKey) → ResourceNotFoundException
- Catch other SDK exceptions → S3OperationException

**deleteObject**:
- Call `DeleteObjectRequest` for bucket/key
- No-op if object doesn't exist (idempotent)
- Catch `SdkException` → S3OperationException

**deleteObjects**:
- Batch delete multiple keys in single request (more efficient)
- Use `DeleteObjectsRequest` with list of keys
- Catch `SdkException` → S3OperationException

**objectExists**:
- Try `HeadObjectRequest` for bucket/key
- Return true if successful, false if 404, throw exception for other errors

**getObjectMetadata**:
- `HeadObjectRequest` returns ContentLength, ContentType, ETag, etc.
- Return ObjectMetadata object (custom wrapper)
- If 404 → ResourceNotFoundException

**Dependencies**: S3Config, S3OperationException, AlbumManagerException, Logger

**Test Cases** (Integration with LocalStack/S3 mock):
- ✅ Generate presigned download URL (URL format validation)
- ✅ Generate presigned upload URL (valid for 15 min)
- ✅ Upload object with correct content-type
- ✅ Download object returns byte array
- ✅ Download non-existent object throws ResourceNotFoundException
- ✅ Delete existing object
- ✅ Delete non-existent object (no-op)
- ✅ Batch delete multiple objects
- ✅ Check object exists returns true/false
- ✅ Get metadata returns size and content-type

---

## Phase 5: Request/Response DTOs (Request)

### T5.1 - Common Request/Response Wrappers [P]

**File**: `src/main/java/com/albummanager/request/CreateAlbumRequest.java`
```java
public class CreateAlbumRequest {
    private String albumName;       // Required
    private String description;     // Optional
}
```

**File**: `src/main/java/com/albummanager/request/CreateImageRequest.java`
```java
public class CreateImageRequest {
    private String albumId;         // Required
    private String fileName;        // Required
    private long fileSize;          // Required (bytes)
    private String fileFormat;      // Required (JPEG, PNG, etc.)
    private String mimeType;        // Required (image/jpeg, etc.)
    private int width;              // Required
    private int height;             // Required
}
```

**File**: `src/main/java/com/albummanager/request/UpdateAlbumRequest.java`
```java
public class UpdateAlbumRequest {
    private String albumName;       // Optional (only update if provided)
    private String description;     // Optional
}
```

**File**: `src/main/java/com/albummanager/request/UpdateImageStatusRequest.java`
```java
public class UpdateImageStatusRequest {
    private String uploadStatus;    // Optional
    private String resizeStatus;    // Optional
}
```

**File**: `src/main/java/com/albummanager/request/ApiResponse.java`
```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String errorCode;       // For errors
    private Map<String, Object> context;  // For errors
}
```

**File**: `src/main/java/com/albummanager/request/PaginatedResult.java`
```java
public class PaginatedResult<T> {
    private List<T> items;
    private String nextToken;       // Base64-encoded, null if no more items
    private int totalCount;         // Optional (DynamoDB doesn't guarantee)
}
```

**All DTO classes**:
- Generated from JSON via Gson
- Constructor with all fields + builder pattern
- Getters/setters
- `toString()`, `equals()`, `hashCode()`

**Dependencies**: Gson

**Test Cases**:
- ✅ Deserialize JSON to DTO
- ✅ Serialize DTO to JSON
- ✅ Handle null optional fields
- ✅ Validation on builder (if using builder pattern)

---

## Phase 6: Logging & Observability (Util)

### T6.1 - Structured Logger Utility [P]

**File**: `src/main/java/com/albummanager/util/StructuredLogger.java`

**Purpose**: Centralized JSON logging with Lambda context (request ID, trace ID)

**Methods**:
```java
public class StructuredLogger {
    // Initialize with Lambda context (RequestId, X-Ray trace ID)
    public static StructuredLogger forContext(Context lambdaContext)
    
    // Logging methods
    public void info(String message, Map<String, Object> context)
    public void warn(String message, Map<String, Object> context)
    public void error(String message, Throwable exception, Map<String, Object> context)
    
    // Convenience overloads
    public void info(String message)
    public void warn(String message)
    public void error(String message, Throwable exception)
    
    // Log format (JSON, compatible with CloudWatch Insights)
    {
        "timestamp": "2025-11-08T14:30:00Z",
        "level": "INFO",
        "requestId": "aws-request-id",
        "xrayTraceId": "x-ray-trace-id",
        "service": "AlbumService",
        "message": "Album created",
        "context": {
            "userId": "user-123",
            "albumId": "album-xyz"
        },
        "durationMs": 145
    }
}
```

**Implementation**:
- Wrap SLF4J + Logback for JSON output
- Extract AWS Lambda context: `Context.getAwsRequestId()`, `Context.getTraceId()`
- Use Gson to serialize context as JSON
- Log to stdout (Lambda runtime captures to CloudWatch)

**Dependencies**: SLF4J, Logback, Gson

**Test Cases**:
- ✅ Log with Lambda context includes requestId
- ✅ JSON output is valid JSON
- ✅ Context map serialized correctly
- ✅ Exception stack traces included in error logs
- ✅ Timestamp in ISO 8601 format

---

### T6.2 - Type Discriminator Utility [P]

**File**: `src/main/java/com/albummanager/util/TypeDiscriminator.java`

**Purpose**: Centralized type checking and routing for album vs. image requests

**Methods**:
```java
public class TypeDiscriminator {
    // Enum for type safety
    public enum EntityType {
        ALBUM("album"),
        IMAGE("image");
        
        private final String value;
        public String getValue() -> value
    }
    
    // Parse string to enum
    public static EntityType parse(String type)
        throws ValidationException  // "Invalid type: xyz"
    
    // Get type from request map
    public static EntityType getType(Map<String, Object> request)
        throws ValidationException  // "type field is required"
    
    // Validate type is album
    public static void requireAlbum(EntityType type)
        throws ValidationException
    
    // Validate type is image
    public static void requireImage(EntityType type)
        throws ValidationException
}
```

**Dependencies**: ValidationException

**Test Cases**:
- ✅ Parse "album" → EntityType.ALBUM
- ✅ Parse "image" → EntityType.IMAGE
- ✅ Parse invalid type throws ValidationException
- ✅ Extract type from request map
- ✅ Missing type field throws ValidationException
- ✅ Require album throws for image type
- ✅ Require image throws for album type

---

### T6.3 - UUID Generator Utility [P]

**File**: `src/main/java/com/albummanager/util/IdGenerator.java`

**Purpose**: Centralized UUID v4 generation

**Methods**:
```java
public class IdGenerator {
    // Generate UUID v4
    public static String generateId() -> String (UUID v4 without hyphens, lowercase)
    
    // Validate UUID v4 format
    public static boolean isValidUUID(String id) -> boolean
    
    // Generate short ID (for testing, optional)
    public static String generateShortId() -> String (12-char alphanumeric)
}
```

**Implementation**:
- Use Java's `java.util.UUID.randomUUID()`
- Remove hyphens, convert to lowercase for S3/DynamoDB storage
- Return as string

**Dependencies**: None

**Test Cases**:
- ✅ Generate UUID is valid v4 format
- ✅ Generated UUIDs are unique
- ✅ Validate UUID accepts valid format
- ✅ Validate UUID rejects invalid format

---

### T6.4 - Constants Utility [P]

**File**: `src/main/java/com/albummanager/util/Constants.java`

**Purpose**: Centralized configuration constants

**Contents**:
```java
public class Constants {
    // DynamoDB
    public static final int MAX_ALBUM_NAME_LENGTH = 255;
    public static final int MIN_ALBUM_NAME_LENGTH = 1;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_IMAGE_SIZE_BYTES = 52_428_800;  // 50 MB
    public static final int MAX_FILENAME_LENGTH = 255;
    
    // S3
    public static final long PRESIGNED_URL_EXPIRATION_DOWNLOAD = 3600;  // 1 hour
    public static final long PRESIGNED_URL_EXPIRATION_UPLOAD = 900;     // 15 minutes
    public static final String IMAGES_BUCKET = "albums-prod";
    public static final String THUMBNAILS_BUCKET = "image-thumbnails";
    
    // Lambda
    public static final int LAMBDA_TIMEOUT_SECONDS = 900;  // 15 minutes
    public static final String DEFAULT_REGION = "us-east-1";
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}
```

**Dependencies**: None

---

## Phase 7: Testing Infrastructure (Test)

### T7.1 - Unit Test Base Class [P]

**File**: `src/test/java/com/albummanager/base/AlbumManagerTestBase.java`

**Purpose**: Common test setup, fixtures, and helpers

**Contents**:
```java
public abstract class AlbumManagerTestBase {
    // Fixtures
    protected static final String TEST_USER_ID = "test-user-123";
    protected static final String TEST_ALBUM_ID = "album-uuid-1234";
    protected static final String TEST_IMAGE_ID = "image-uuid-5678";
    
    // Helper methods
    protected Album createTestAlbum(String userId, String albumName)
    protected Image createTestImage(String userId, String albumId, String fileName)
    
    // Assertion helpers
    protected void assertAlbumEquals(Album expected, Album actual)
    protected void assertImageEquals(Image expected, Image actual)
}
```

**Dependencies**: JUnit 5, Album, Image

---

### T7.2 - Integration Tests for Services [P]

**Files**:
- `src/test/java/com/albummanager/services/AlbumServiceTest.java`
- `src/test/java/com/albummanager/services/ImageServiceTest.java`
- `src/test/java/com/albummanager/services/S3ServiceTest.java`

**Scope** (Unit tests only, no external integration):
- Mock DynamoDB client
- Mock S3 client
- Test business logic with mocked AWS SDK
- Verify exception handling

**Example AlbumServiceTest**:
```java
@Test
public void testCreateAlbumSuccess() {
    AlbumService service = new AlbumService(mockDynamoDBClient);
    Album album = service.createAlbum(TEST_USER_ID, "My Album", "Description");
    
    assertNotNull(album);
    assertEquals("My Album", album.getAlbumName());
    assertEquals(TEST_USER_ID, album.getUserId());
}

@Test
public void testCreateAlbumDuplicateNameThrows() {
    // Mock DynamoDB to return existing album with same name
    // Expect DuplicateAlbumException
}
```

**Dependencies**: JUnit 5, Mockito (for mocking)

---

## Phase 8: Logback Configuration

### T8.1 - Logback JSON Configuration [P]

**File**: `src/main/resources/logback.xml`

**Configuration**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- JSON encoder for structured logging -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

**Note**: May require additional dependency: `net.logstash.logback:logstash-logback-encoder`

---

## Implementation Order & Dependencies

### Critical Path (Complete in Order):

1. **Models** (Phase 1): Define domain objects
   - T1.1, T1.2, T1.3, T1.4, T1.5
   - No dependencies; all others depend on these

2. **Exceptions** (Phase 2): Define error hierarchy
   - T2.1
   - Dependency: models (for context)

3. **Utilities** (Phase 6 early): Constants, ID generation, loggers
   - T6.2, T6.3, T6.4 can run in parallel
   - T6.1 depends on Logback config (T8.1)

4. **DynamoDB Service** (Phase 3): Album & Image operations
   - T3.1, T3.2, T3.3
   - Dependencies: Models, Exceptions, Utilities

5. **S3 Service** (Phase 4): Presigned URLs & uploads
   - T4.1, T4.2
   - Dependencies: Exceptions

6. **Request/Response DTOs** (Phase 5)
   - T5.1
   - Dependencies: Models

7. **Testing** (Phase 7)
   - T7.1, T7.2
   - Dependencies: All of above

8. **Configuration** (Phase 8)
   - T8.1

### Parallelizable Tasks [P]:

- T1.1, T1.2 (Album & Image models) - run in parallel
- T1.3, T1.4, T1.5 (Enums, utilities) - run after models
- T6.2, T6.3, T6.4 (Utilities) - all parallel
- T3.2, T3.3 (AlbumService, ImageService) - after T3.1
- T7.1, T7.2 (Tests) - last phase

---

## Definition of Done per Task

Each task is complete when:

1. ✅ Code is written and compiles without errors
2. ✅ All public methods have Javadoc comments
3. ✅ Unit tests pass (for services: T7.2)
4. ✅ Exception handling is explicit (no uncaught SDK exceptions)
5. ✅ Logging statements are in place for debugging
6. ✅ No hardcoded strings (use Constants utility)
7. ✅ Null checks on all public method parameters
8. ✅ `mvn clean package` succeeds for shared-layer module
9. ✅ Code follows project conventions (naming, structure, style)
10. ✅ Commit with message: `[shared-layer] T{task-id}: {task-name}`

---

## Success Criteria for Shared Layer

- ✅ All models compile and are serializable to/from JSON
- ✅ All services have >80% test coverage
- ✅ DynamoDB operations properly handle eventual consistency
- ✅ S3 presigned URLs are valid and correctly formatted
- ✅ Exception hierarchy properly distinguishes error categories
- ✅ Logging outputs valid JSON to CloudWatch
- ✅ No external dependencies beyond AWS SDK v2, SLF4J, Gson
- ✅ JAR packages successfully: `mvn clean package`
- ✅ JAR can be deployed as Lambda Layer
- ✅ All 7 operation Lambdas can import and use shared-layer classes

---

## Notes

- **Pagination Token**: Use base64-encoded DynamoDB `LastEvaluatedKey` as pagination token
- **Timestamps**: Always use Unix milliseconds (System.currentTimeMillis()), never Date objects
- **UUID Format**: Generate without hyphens, lowercase (e.g., `abc123def456`)
- **S3 Keys**: Always include userId prefix for multi-tenancy future-proofing
- **Error Context**: Always include userId, albumId, imageId in exception context for debugging
- **DynamoDB Consistency**: Query operations use eventually consistent reads (cheaper, acceptable); GetItem uses strong consistency for critical operations

