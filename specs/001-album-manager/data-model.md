# Data Model: Photo Album Manager

**Created**: 2025-11-08  
**Input**: Feature specification + System design diagram + Research decisions  
**Output**: Detailed entity definitions, DynamoDB schemas, and state models

---

## Entity Definitions

### Unified Data Model: Single Table Design

**Purpose**: Store both albums and photos in a single DynamoDB table using composite sort keys to distinguish entity types.

**DynamoDB Table**: `albums` (single table for both albums and photos)  
**Partition Key (PK)**: `userId` (String)  
**Sort Key (SK)**: `ALBUM#{albumId}` or `PHOTO#{albumId}#{photoId}` (String)  
**TTL**: None (permanent unless user deletes)

#### Sort Key Format Rules

- **Album Records**: `ALBUM#{albumId}` where `{albumId}` is UUID v4
- **Photo Records**: `PHOTO#{albumId}#{photoId}` where both are UUID v4

This allows efficient querying:
- Get all albums for user: `Query(PK=userId, SK begins_with "ALBUM#")`
- Get all photos in album: `Query(PK=userId, SK begins_with "PHOTO#{albumId}#")`
- The files can be identified and allow for duplicate names across different users as they are tied to its UUID, not the its name.
- During development, only one userId exists, "dev-user".
- In production, userId is the Cognito sub claim.
---

### Album Record Structure

| Attribute | Type | Required | Constraints | Description |
|-----------|------|----------|-------------|-------------|
| `userId` | String | ✅ Yes | 1-128 chars, UUID format | AWS Cognito sub claim or user identifier |
| `sk` | String | ✅ Yes | Format: `ALBUM#{albumId}` | Composite sort key with type prefix |
| `albumId` | String | ✅ Yes | UUID v4 format | Generated server-side, globally unique |
| `albumName` | String | ✅ Yes | 1-255 chars, no leading/trailing spaces | User-provided album name |
| `createdAt` | String (ISO 8601) | ✅ Yes | Must be valid ISO 8601 format | Timestamp of album creation (server-generated) |
| `updatedAt` | String (ISO 8601) | ✅ Yes | Must be >= createdAt | Timestamp of last modification |
| `photoCount` | Number | ✅ Yes | >= 0, integer | Denormalized count for fast display (updated on photo add/delete) |
| `description` | String | ❌ No | 0-500 chars | Optional album description |
| `entityType` | String | ✅ Yes | Literal: `ALBUM` | Discriminator for filtering queries (optional, for clarity) |

**Validation Rules**:
- `albumName` must be unique per `userId` (application-level enforcement in Lambda)
- `albumName` cannot be empty or whitespace-only
- `photoCount` must be kept in sync with actual photos via Lambda updates
- `createdAt` must not be in future
- No album can be modified or deleted if upload is in progress for its photos

**Example Album Record**:
```json
{
  "userId": "auth0|user-123abc",
  "sk": "ALBUM#550e8400-e29b-41d4-a716-446655440000",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "albumName": "Family Trip 2025",
  "description": "Summer vacation to Switzerland",
  "createdAt": "2025-11-08T10:30:00Z",
  "updatedAt": "2025-11-08T14:45:00Z",
  "photoCount": 47,
  "entityType": "ALBUM"
}
```

---

### Photo Record Structure

| Attribute | Type | Required | Constraints | Description |
|-----------|------|----------|-------------|-------------|
| `userId` | String | ✅ Yes | 1-128 chars, must match album's userId | Owner of the photo (for access control) |
| `sk` | String | ✅ Yes | Format: `PHOTO#{albumId}#{photoId}` | Composite sort key with type prefix |
| `photoId` | String | ✅ Yes | UUID v4 format | Generated server-side, globally unique |
| `albumId` | String | ✅ Yes | UUID v4 format | Reference to parent album (queryable via SK prefix) |
| `fileName` | String | ✅ Yes | 1-255 chars, original filename | Original filename provided by uploader |
| `fileSize` | Number | ✅ Yes | 1-52428800 (50 MB max) | Size in bytes |
| `fileFormat` | String | ✅ Yes | Enum: JPEG, PNG, WEBP, GIF, HEIC | Image format detected at upload |
| `uploadedAt` | String (ISO 8601) | ✅ Yes | Valid ISO 8601, must be server-generated | Timestamp when upload completed |
| `s3Key` | String | ✅ Yes | Format: `{userId}/ALBUM#{albumId}/original/{fileName}` | S3 object key for full-resolution photo |
| `thumbnailS3Key` | String | ✅ Yes | Format: `{userId}/PHOTO#{albumId}#{photoId}/thumb-256.webp` | S3 object key for 256x256 WebP thumbnail |
| `width` | Number | ✅ Yes | > 0, integer | Image width in pixels |
| `height` | Number | ✅ Yes | > 0, integer | Image height in pixels |
| `mimeType` | String | ✅ Yes | Valid MIME type (e.g., `image/jpeg`) | MIME type of the photo |
| `uploadStatus` | String | ✅ Yes | Enum: `PENDING`, `COMPLETED`, `FAILED` | Current upload status |
| `checksumMd5` | String | ✅ Yes | 32-char hex string | MD5 hash of file for integrity verification |
| `resizeStatus` | String | ✅ Yes | Enum: `PENDING`, `COMPLETED`, `FAILED` | Status of resize processing |
| `resizeCompletedAt` | String (ISO 8601) | ❌ No | Valid ISO 8601 format | Timestamp when resize finished |
| `entityType` | String | ✅ Yes | Literal: `PHOTO` | Discriminator for filtering queries (optional, for clarity) |

**Validation Rules**:
- `albumId` must exist in table with matching `userId` and `sk` begins_with `ALBUM#`
- `fileSize` must be 1 to 52,428,800 bytes (50 MB)
- `fileFormat` must be one of: JPEG, PNG, WEBP, GIF, HEIC
- `uploadedAt` must not be in future
- `width` and `height` must be > 0
- `mimeType` must correspond to `fileFormat`
- `uploadStatus` must be COMPLETED before photo appears in album list

**Example Photo Record**:
```json
{
  "userId": "auth0|user-123abc",
  "sk": "PHOTO#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111",
  "photoId": "660f9511-f30c-52e5-b817-557766551111",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "sunset_beach.jpg",
  "fileSize": 2048576,
  "fileFormat": "JPEG",
  "uploadedAt": "2025-11-08T14:30:00Z",
  "s3Key": "auth0|user-123abc/ALBUM#550e8400-e29b-41d4-a716-446655440000/original/sunset_beach.jpg",
  "thumbnailS3Key": "auth0|user-123abc/PHOTO#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111/thumb-256.webp",
  "width": 3840,
  "height": 2160,
  "mimeType": "image/jpeg",
  "uploadStatus": "COMPLETED",
  "resizeStatus": "COMPLETED",
  "resizeCompletedAt": "2025-11-08T14:31:15Z",
  "checksumMd5": "5d41402abc4b2a76b9719d911017c592",
  "entityType": "PHOTO"
}
```

---

### Query Patterns

**Query All Albums for User**:
```
Query(PK=userId, SK begins_with "ALBUM#")
Result: All album records for that user, sorted by albumId
```

**Query All Photos in Album**:
```
Query(PK=userId, SK begins_with "PHOTO#{albumId}#")
Result: All photos in album, sorted by photoId
```

**Get Specific Album**:
```
GetItem(PK=userId, SK="ALBUM#{albumId}")
Result: Single album record
```

**Get Specific Photo**:
```
GetItem(PK=userId, SK="PHOTO#{albumId}#{photoId}")
Result: Single photo record
```

---

## S3 Storage Design

### Bucket Strategy

The application uses a multi-bucket approach for organized storage and lifecycle management:

#### 1. **Primary Photos Bucket** (`albums-prod`)
Stores all original photos with type-prefixed hierarchical structure.

**Storage Structure**:
```
albums-prod/
├── {userId}/
│   ├── ALBUM#{albumId}/
│   │   └── original/{fileName}           # Original uploaded photo
│   │
│   ├── ALBUM#{albumId-2}/
│   │   └── original/{fileName}
```

**Key Naming Convention**: Uses `ALBUM#{albumId}` prefix to align with DynamoDB sort key structure for consistency.

**Bucket Configuration**:
- **Versioning**: Enabled (allow recovery of deleted photos)
- **Access Control**: Private (all access via presigned URLs)
- **Lifecycle Rules**:
  - Delete incomplete multipart uploads after 1 day
  - Transition to Glacier after 90 days (optional archival)
- **Encryption**: SSE-S3 or SSE-KMS
- **CORS**: Configured for browser uploads (for presigned URLs)
- **Events**: S3 Put Object → triggers ResizeImage Lambda

#### 2. **Image Thumbnails Bucket** (`image-thumbnails`)
Stores resized WebP thumbnails (256x256) using type-prefixed paths aligned with DynamoDB structure.

**Storage Structure**:
```
image-thumbnails/
├── {userId}/
│   ├── PHOTO#{albumId}#{photoId}/
│   │   └── thumb-256.webp               # 256x256 WebP thumbnail
```

**Key Naming Convention**: Uses `PHOTO#{albumId}#{photoId}` prefix to match DynamoDB sort key structure for direct path construction.

**Bucket Configuration**:
- **Versioning**: Disabled (thumbnails are regenerable)
- **Access Control**: Private (all access via presigned URLs)
- **Lifecycle Rules**:
  - Delete incomplete multipart uploads after 1 day
- **Encryption**: SSE-S3
- **CORS**: Configured for browser access via presigned URLs
- **Events**: None (thumbnails are generated by ResizeImage Lambda and stored here)

#### 3. **Static Assets Bucket** (`albums-static`)
Hosts frontend (HTML, CSS, JS) and serves directly from S3.

**Structure**:
```
albums-static/
├── index.html
├── app.js
├── styles.css
└── assets/
    ├── images/
    └── icons/
```



### Resizing Process Workflow

```
1. User uploads photo via presigned URL to albums-prod bucket
   ↓
   S3 event fired (s3:ObjectCreated:Put)
   ↓
2. ResizeImage Lambda triggered (async)
   ↓
3. Lambda downloads original from albums-prod
   ↓
4. Generates 1 resized version:
   - thumb-256.webp (256x256, quality 90)
   ↓
5. Uploads resized thumbnail to image-thumbnails bucket in same photoId folder
   ↓
6. Updates DynamoDB photo record:
   - resizeStatus: COMPLETED
   - resizeCompletedAt: ISO 8601 timestamp
   ↓
7. Thumbnail immediately available for display
   ✓ UI fetches thumbnail via presigned URL from GetDownloadURLFunction
```

**Optimization Notes**:
- WebP format provides excellent compression (~25-30% smaller than JPEG)
- Single 256x256 size optimized for display across devices
- Processing time: <3 seconds for typical 3-5 MB photo
- Lambda memory: 256 MB sufficient (single-size processing)

### Naming Conventions

All naming conventions follow a unified pattern across DynamoDB and S3 to ensure consistency and enable direct path construction.

**DynamoDB Sort Key Format**:
```
ALBUM#{albumId}              # Album records
PHOTO#{albumId}#{photoId}    # Photo records
```

**S3 Object Key Format** (aligned with DynamoDB):

**Original Photo** (albums-prod bucket):
```
{userId}/ALBUM#{albumId}/original/{fileName}
```

**Resize Variant** (image-thumbnails bucket):
```
{userId}/PHOTO#{albumId}#{photoId}/thumb-256.webp
```

**Benefits**:
- **Consistency**: Same type-prefixed structure across DynamoDB and S3
- **Direct URL Construction**: No database lookup needed; construct S3 keys from DynamoDB records
- **Query Efficiency**: Sort key prefixes enable fast range queries (e.g., "PHOTO#album-123#*" for all photos in album)
- **Easy Debugging**: Clear entity type in both database and object keys
- **Scalable**: Support for future entity types or resume information without schema changes

---

## State Machines

### Photo Upload & Resize State Machine

The photo lifecycle involves two independent state tracks: upload status and resize status. Photos appear in album list immediately after **COMPLETED upload** (with temporary thumbnail), and are upgraded to final thumbnail when **COMPLETED resize**.

```
UPLOAD STATUS TRACK:                          RESIZE STATUS TRACK:
    ┌─────────────┐                               ┌─────────────┐
    │   PENDING   │ (GetUploadURLFunction)        │   PENDING   │ (Photo uploaded, waiting for resize)
    └──────┬──────┘                               └──────┬──────┘
           │                                             │
           │ (Browser uploads to S3)                     │ (S3 event fires ProcessImageFunction)
           ▼                                             ▼
    ┌──────────────┐                           ┌──────────────────┐
    │  COMPLETED   │◄──────────────────────────│    PROCESSING    │
    │ (Photo visible│   Thumbnail replaced      │ (Resizing,       │
    │  in album)   │   when resize completes   │  optimizing)     │
    └──────┬───────┘                           └────────┬─────────┘
           │                                           │
    ┌─────────────────────────────────────────┐  ┌────────────┐
    │ Upload fails before S3 storage          │  │ COMPLETED  │ (Final thumbnail ready)
    ▼                                         ▼  └────────────┘
 ┌────────┐                                ┌────────────┐
 │ FAILED │ (Error logged, S3 cleanup)     │  FAILED    │ (Resize error, log, keep temp)
 └────────┘                                └────────────┘
```

**Upload Status Transitions** (photo stored in `uploadStatus` field):
1. **PENDING** → **COMPLETED**: Photo successfully uploaded to S3, metadata record created in DynamoDB with temporary thumbnail URL (original image). **Photo appears in album list immediately.**
2. **PENDING** → **FAILED**: Upload timeout (>5min), file validation error, S3 error. S3 cleanup occurs, user notified.

**Resize Status Transitions** (photo stored in `resizeStatus` field):
1. **PENDING** → **COMPLETED**: ProcessImageFunction generates 256x256 WebP thumbnail, uploads to S3, updates DynamoDB with final thumbnail URL. **Album list updates with optimized thumbnail.**
2. **PENDING** → **FAILED**: Resize processing error (invalid image, memory limit, timeout). Log error, retain temporary thumbnail for user, notify user of partial state.

**Album List Visibility** (clarity from user request):
- Photos appear when: `uploadStatus == COMPLETED` (regardless of `resizeStatus`)
- Photos displayed with: Original image (temporary) if `resizeStatus == PENDING`, OR final 256x256 WebP if `resizeStatus == COMPLETED`
- Transition is transparent to user: temporary thumbnail automatically replaces with optimized version

---

### Album Deletion State Machine

```
    ┌─────────────┐
    │   ACTIVE    │
    └──────┬──────┘
           │
           │ (DeleteEntryFunction called with type=album)
           ▼
    ┌─────────────────────────┐
    │   DELETING_PHOTOS       │  (Query all photos with SK begins_with PHOTO#{albumId}#)
    └──────┬────────┬─────────┘
           │        │
      Success   (Error)
           │        │
           ▼        ▼
    ┌──────────┐  ┌────────────────┐
    │ DELETED  │  │ DELETE_FAILED  │  (Retry logic, log error)
    └──────────┘  └────────────────┘
```

**State Transitions**:
1. **ACTIVE** → **DELETING_PHOTOS**: Query DynamoDB for all photos in album; delete each photo's original + thumbnail S3 objects
2. **DELETING_PHOTOS** → **DELETED**: All photos and S3 objects deleted; album record deleted from DynamoDB
3. **DELETING_PHOTOS** → **DELETE_FAILED**: Partial deletion; retry with exponential backoff; album may be left in partial state

---

## Client Polling Strategy for Thumbnail Completion

### Overview
Browser polls the `ReadEntryFunction` endpoint to detect when the resize process completes and the final thumbnail becomes available. This enables real-time UI updates without server push infrastructure.

### Polling Request Format

**Endpoint**: `POST /read-entry` (ReadEntryFunction)

**Request Body**:
```json
{
  "type": "image",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "photoId": "660f9511-f30c-52e5-b817-557766551111"
}
```

**Response Format**:
```json
{
  "photoId": "660f9511-f30c-52e5-b817-557766551111",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "uploadStatus": "COMPLETED",
  "resizeStatus": "PENDING|COMPLETED|FAILED",
  "temporaryThumbnailUrl": "https://albums-prod.s3.amazonaws.com/{userId}/ALBUM#{albumId}/original/{fileName}?X-Amz-Expires=3600",
  "finalThumbnailUrl": "https://image-thumbnails.s3.amazonaws.com/{userId}/PHOTO#{albumId}#{photoId}/thumb-256.webp?X-Amz-Expires=3600",
  "uploadedAt": "2025-11-08T14:30:00Z",
  "resizeCompletedAt": "2025-11-08T14:31:15Z"
}
```

### Polling Algorithm

**Initial State**:
- `delay = 200` (milliseconds)
- `maxDelay = 5000` (milliseconds, 5 seconds cap)
- `maxAttempts = 60` (30 second timeout)
- `attemptCount = 0`

**Polling Loop**:
```
While attemptCount < maxAttempts:
  1. Wait delay milliseconds
  2. Send ReadEntry request for (type=image, albumId, photoId)
  3. If response.resizeStatus == "COMPLETED":
       - Replace temporary thumbnail with finalThumbnailUrl
       - Stop polling (success)
       - Log: "Thumbnail ready after N attempts"
  4. Else if response.resizeStatus == "FAILED":
       - Keep temporary thumbnail visible
       - Stop polling (failure state)
       - Log: "Thumbnail processing failed"
  5. Else (PENDING):
       - Schedule next poll
       - Increase delay: delay = min(delay × 1.1, maxDelay) (10% backoff)
       - Increment attemptCount
       - Log: "Polling attempt N, next delay in X ms"

After maxAttempts:
  - Polling timeout: keep temporary thumbnail
  - User can manually refresh to retry
```

**Backoff Progression** (10% increase per attempt):
- Attempt 1: 200ms delay
- Attempt 2: 220ms delay (200 × 1.1)
- Attempt 3: 242ms delay (220 × 1.1)
- Attempt 4: 266ms delay (242 × 1.1)
- Attempt 5: 293ms delay (266 × 1.1)
- Continues with 10% increase until capped at maxDelay (5000ms)

### Error Handling

| Scenario | Action |
|----------|--------|
| Network error on poll | Retry immediately; count toward maxAttempts |
| HTTP 404 (photo not found) | Stop polling; log error; show error to user |
| HTTP 500 (server error) | Retry with exponential backoff |
| Polling timeout (60 attempts) | Stop polling; keep temporary thumbnail; allow manual refresh |
| resizeStatus == FAILED | Stop polling immediately; show warning to user |

### Frontend Implementation Notes

- Start polling **immediately after upload completes**
- Use browser `fetch()` with 5-second timeout per request
- Debounce UI updates: only update DOM if thumbnail URL changes
- Store photo metadata locally to preserve state across navigation
- Resume polling if user returns to album (fresh attempt counter)

## DynamoDB Design Decisions

### Single Table vs. Multi-Table

**Decision**: Single table (`albums`) for both album and photo records using type-prefixed composite sort keys

**Rationale**:
- **Simplified Operations**: One table = one billing target, simplified backup/restore, easier monitoring
- **Query Efficiency**: Range queries on sort key enable efficient album and photo lookups without secondary indexes
- **Reduced Latency**: No cross-table queries or joins needed
- **Cost Optimization**: Single on-demand table scales for both entity types
- **Maintainability**: Single source of truth for user data

**Schema**:
```
PK: userId (String)          # All records partitioned by user
SK: Type#{id}                # ALBUM#{albumId} or PHOTO#{albumId}#{photoId}
```

---

## Validation Rules by Entity

### Album Validation

```java
// CreateAlbumFunction validation
if (albumName.isEmpty() || albumName.isBlank()) {
  throw new ValidationException("Album name is required");
}
if (albumName.length() > 255) {
  throw new ValidationException("Album name must be <= 255 characters");
}
if (albumName.trim().isEmpty()) {
  throw new ValidationException("Album name cannot be whitespace only");
}

// Check for duplicate album names per user
if (albumExists(userId, albumName)) {
  throw new ValidationException("Album name already exists for this user");
}
```

### Photo Validation

```java
// UpdateEntryFunction validation
long fileSizeBytes = metadata.getContentLength();
if (fileSizeBytes > 52_428_800) { // 50 MB
  throw new ValidationException("File size exceeds 50 MB limit");
}

String contentType = metadata.getContentType();
Set<String> allowedMimeTypes = Set.of(
  "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic"
);
if (!allowedMimeTypes.contains(contentType)) {
  throw new ValidationException("Invalid image format");
}

// Verify dimensions after processing
BufferedImage image = ImageIO.read(s3Object);
if (image.getWidth() <= 0 || image.getHeight() <= 0) {
  throw new ValidationException("Image has invalid dimensions");
}
```

---

## Data Consistency & Concurrency

### Eventual Consistency Scenarios

1. **Photo Count Mismatch**: 
   - Problem: `album.photoCount` may not match actual photos in photos table after concurrent deletes
   - Solution: Implement periodic reconciliation Lambda; accept eventual consistency for list view counts

2. **Stale Photo Lists**:
   - Problem: Photo list may not show newly uploaded photo immediately
   - Solution: Client-side refresh; DynamoDB streams (future) for real-time updates

### Strong Consistency Requirements

1. **Create Album**: Must verify uniqueness before write → Strong consistency read before write
2. **Delete Photo**: Must verify photo exists before delete → Strong consistency read
3. **Album Metadata**: Always use strong consistency for reads to ensure correct photoCount display

---

## Backup & Recovery Strategy

| Entity | Backup Method | Recovery RPO | Recovery RTO |
|--------|---------------|--------------|--------------|
| **DynamoDB** | AWS Backup automated daily | 24 hours | <4 hours |
| **S3 Photos** | S3 versioning enabled | Immediate | <1 minute |
| **S3 Static Assets** | Versioning enabled | Immediate | <5 minutes |

---

## Data Retention & Cleanup

| Data Type | Retention Policy | Cleanup Method |
|-----------|------------------|-----------------|
| **Albums** | Permanent until user deletes | On-demand deletion via DeleteAlbumFunction |
| **Photos** | Permanent until user deletes | On-demand deletion via DeleteImageFunction; S3 object deleted immediately |
| **Thumbnails** | Same as photos | Deleted with photo via S3 lifecycle rule |
| **S3 Upload Staging** | 24 hours (incomplete uploads) | S3 Lifecycle Policy: delete incomplete multipart uploads after 1 day |
| **CloudWatch Logs** | 30 days (retention policy) | AWS managed |
| **X-Ray Traces** | 30 days (retention policy) | AWS managed |

---

## Technical Implementation Notes

### Image Processing (WebP Thumbnail Generation)

**Format**: WebP (modern, efficient, 30-40% smaller than JPEG for photos)  
**Size**: 256x256 pixels (single size, not multiple variants)  
**Quality**: 90% for high-quality thumbnails with minimal file size

**Java Implementation Considerations**:
- ❌ Java's built-in `javax.imageio.ImageIO` does NOT natively support WebP format
- ✅ Use **Thumbnailator** (net.coobird:thumbnailator:0.4.19) - pure Java library with WebP support
- ✅ No external native bindings required (unlike Google's libwebp), works reliably in AWS Lambda cold starts
- ✅ FAT JAR deployment via Maven Shade Plugin (all dependencies bundled; ~50-60 MB per Lambda)

**S3 Storage Optimization**:
- Store original photos in `albums-prod` bucket (user pays for originals)
- Store 256x256 WebP thumbnails in `image-thumbnails` bucket (separate for cost tracking)
- Single thumbnail per photo (reduces complexity, fast generation)
- Presigned URLs for both upload (original) and download (thumbnail)

---

## Summary

✅ **1 Unified DynamoDB Table** (`albums`): Albums + Photos with type-prefixed composite sort keys  
✅ **Sort Key Structure**: `ALBUM#{albumId}` and `PHOTO#{albumId}#{photoId}` for efficient range queries  
✅ **S3 Naming Aligned**: Object keys mirror DynamoDB structure (`ALBUM#`, `PHOTO#` prefixes)  
✅ **On-Demand Capacity**: Auto-scaling for unpredictable MVP load  
✅ **Efficient Queries**: 
   - Get all albums: `Query(PK=userId, SK begins_with "ALBUM#")`
   - Get album photos: `Query(PK=userId, SK begins_with "PHOTO#{albumId}#")`  
✅ **Strong Consistency**: Used for critical operations (create, delete); eventual consistency for lists  
✅ **Validation Rules**: Enforced at application layer (Lambda) before DB writes  
✅ **State Management**: Photo upload and album deletion state machines defined  
✅ **Backup & Recovery**: DynamoDB snapshots + S3 versioning enabled  

**Next Phase**: Define API contracts (OpenAPI specs) for all 7 Lambda functions
