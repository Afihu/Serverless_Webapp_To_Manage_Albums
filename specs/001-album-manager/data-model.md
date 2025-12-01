# Data Model: Image Album Manager

**Created**: 2025-11-08  
**Input**: Feature specification + System design diagram + Research decisions  
**Output**: Detailed entity definitions, DynamoDB schemas, and state models

---

## Entity Definitions

### Unified Data Model: Single Table Design

**Purpose**: Store both albums and images in a single DynamoDB table using composite sort keys to distinguish entity types.

**DynamoDB Table**: `albums` (single table for both albums and images)  
**Partition Key (PK)**: `userId` (String)  
**Sort Key (SK)**: `ALBUM#{albumId}` or `IMAGE#{albumId}#{imageId}` (String)  
**TTL**: None (permanent unless user deletes)

#### Sort Key Format Rules

- **Album Records**: `ALBUM#{albumId}` where `{albumId}` is UUID v4
- **Image Records**: `IMAGE#{albumId}#{imageId}` where both are UUID v4

This allows efficient querying:
- Get all albums for user: `Query(PK=userId, SK begins_with "ALBUM#")`
- Get all images in album: `Query(PK=userId, SK begins_with "IMAGE#{albumId}#")`
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
| `imageCount` | Number | ✅ Yes | >= 0, integer | Denormalized count for fast display (updated on image add/delete) |
| `description` | String | ❌ No | 0-500 chars | Optional album description |
| `entityType` | String | ✅ Yes | Literal: `ALBUM` | Discriminator for filtering queries (optional, for clarity) |

**Validation Rules**:
- `albumName` must be unique per `userId` (application-level enforcement in Lambda)
- `albumName` cannot be empty or whitespace-only
- `imageCount` must be kept in sync with actual images via Lambda updates
- `createdAt` must not be in future
- No album can be modified or deleted if upload is in progress for its images

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
  "imageCount": 47,
  "entityType": "ALBUM"
}
```

---

### Image Record Structure

| Attribute | Type | Required | Constraints | Description |
|-----------|------|----------|-------------|-------------|
| `userId` | String | ✅ Yes | 1-128 chars, must match album's userId | Owner of the image (for access control) |
| `sk` | String | ✅ Yes | Format: `IMAGE#{albumId}#{imageId}` | Composite sort key with type prefix |
| `imageId` | String | ✅ Yes | UUID v4 format | Generated server-side, globally unique |
| `albumId` | String | ✅ Yes | UUID v4 format | Reference to parent album (queryable via SK prefix) |
| `fileName` | String | ✅ Yes | 1-255 chars, original filename | Original filename provided by uploader |
| `fileSize` | Number | ✅ Yes | 1-52428800 (50 MB max) | Size in bytes |
| `fileFormat` | String | ✅ Yes | Enum: JPEG, PNG, WEBP, GIF, HEIC | Image format detected at upload |
| `uploadedAt` | String (ISO 8601) | ✅ Yes | Valid ISO 8601, must be server-generated | Timestamp when upload completed |
| `s3Key` | String | ✅ Yes | Format: `{userId}/IMAGE#{albumId}#{imageId}#OG` | S3 object key for full-resolution image |
| `thumbnailS3Key` | String | ✅ Yes | Format: `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp` | S3 object key for 256x256 WebP thumbnail |
| `width` | Number | ✅ Yes | > 0, integer | Image width in pixels |
| `height` | Number | ✅ Yes | > 0, integer | Image height in pixels |
| `mimeType` | String | ✅ Yes | Valid MIME type (e.g., `image/jpeg`) | MIME type of the image |
| `uploadStatus` | String | ✅ Yes | Enum: `PENDING`, `COMPLETED`, `FAILED` | Current upload status |
| `checksumMd5` | String | ✅ Yes | 32-char hex string | MD5 hash of file for integrity verification |
| `resizeStatus` | String | ✅ Yes | Enum: `PENDING`, `COMPLETED`, `FAILED` | Status of resize processing |
| `resizeCompletedAt` | String (ISO 8601) | ❌ No | Valid ISO 8601 format | Timestamp when resize finished |
| `entityType` | String | ✅ Yes | Literal: `IMAGE` | Discriminator for filtering queries (optional, for clarity) |

**Validation Rules**:
- `albumId` must exist in table with matching `userId` and `sk` begins_with `ALBUM#`
- `fileSize` must be 1 to 52,428,800 bytes (50 MB)
- `fileFormat` must be one of: JPEG, PNG, WEBP, GIF, HEIC
- `uploadedAt` must not be in future
- `width` and `height` must be > 0
- `mimeType` must correspond to `fileFormat`
- `uploadStatus` must be COMPLETED before image appears in album list

**Example Image Record**:
```json
{
  "userId": "auth0|user-123abc",
  "sk": "IMAGE#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111",
  "imageId": "660f9511-f30c-52e5-b817-557766551111",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "sunset_beach.jpg",
  "fileSize": 2048576,
  "fileFormat": "JPEG",
  "uploadedAt": "2025-11-08T14:30:00Z",
  "s3Key": "auth0|user-123abc/IMAGE#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111#OG",
  "thumbnailS3Key": "auth0|user-123abc/IMAGE#550e8400-e29b-41d4-a716-446655440000#660f9511-f30c-52e5-b817-557766551111/thumb-256.webp",
  "width": 3840,
  "height": 2160,
  "mimeType": "image/jpeg",
  "uploadStatus": "COMPLETED",
  "resizeStatus": "COMPLETED",
  "resizeCompletedAt": "2025-11-08T14:31:15Z",
  "checksumMd5": "5d41402abc4b2a76b9719d911017c592",
  "entityType": "IMAGE"
}
```

---

### Query Patterns

**Query All Albums for User**:
```
Query(PK=userId, SK begins_with "ALBUM#")
Result: All album records for that user, sorted by albumId
```

**Query All Images in Album**:
```
Query(PK=userId, SK begins_with "IMAGE#{albumId}#")
Result: All images in album, sorted by imageId
```

**Get Specific Album**:
```
GetItem(PK=userId, SK="ALBUM#{albumId}")
Result: Single album record
```

**Get Specific Image**:
```
GetItem(PK=userId, SK="IMAGE#{albumId}#{imageId}")
Result: Single image record
```

---

## S3 Storage Design

### Bucket Strategy

The application uses a multi-bucket approach for organized storage and lifecycle management:

#### 1. **Primary Images Bucket** (`albums-prod`)
Stores all original images uploaded by users.

**Key Naming Convention**: `{userId}/IMAGE#{albumId}#{imageId}#OG`  
**For detailed S3 key construction, see**: `designs/functions/create-entry-design.md` (Image Metadata Creation Flow)

**Bucket Configuration**:
- **Versioning**: Enabled (allow recovery of deleted images)
- **Access Control**: Private (all access via presigned URLs)
- **Lifecycle Rules**:
  - Delete incomplete multipart uploads after 1 day
  - Transition to Glacier after 90 days (optional archival)
- **Encryption**: SSE-S3 or SSE-KMS
- **CORS**: Configured for browser uploads (for presigned URLs)
- **Events**: S3 Put Object → triggers ResizeImage Lambda

#### 2. **Image Thumbnails Bucket** (`image-thumbnails`)
Stores resized WebP thumbnails (256x256).

**Key Naming Convention**: `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp`  
**For resizing workflow, see**: `designs/flows/create-entry-flow.md` (step 16)

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



**For resizing process workflow details, see**: `designs/flows/create-entry-flow.md` (step 16)

**Optimization Notes**:
- WebP format provides excellent compression (~25-30% smaller than JPEG)
- Single 256x256 size optimized for display across devices
- Processing time: <3 seconds for typical 3-5 MB image
- Lambda memory: 256 MB sufficient (single-size processing)

### Naming Conventions

All naming conventions follow a unified pattern across DynamoDB and S3 to ensure consistency.

**DynamoDB Sort Key Format**:
```
ALBUM#{albumId}              # Album records
IMAGE#{albumId}#{imageId}    # Image records
```

**S3 Object Key Format**:
```
{userId}/IMAGE#{albumId}#{imageId}#OG                      # Original image
{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp          # Thumbnail
```

**Benefits**:
- **Consistency**: Same type-prefixed structure across DynamoDB and S3
- **Direct URL Construction**: No database lookup needed; construct S3 keys from DynamoDB records
- **Query Efficiency**: Sort key prefixes enable fast range queries
- **Easy Debugging**: Clear entity type in both database and object keys

**For detailed S3 key construction logic, see**: `designs/functions/create-entry-design.md`

---

## State Models Reference

**For detailed state machine diagrams and flow descriptions, see:**
- **Image Upload & Resize States**: `designs/flows/create-entry-flow.md` (steps 16-18)
- **Album Deletion State Machine**: Future documentation in deletion flow
- **Client Polling Strategy**: `designs/flows/create-entry-flow.md` (step 17)

The core state fields in the data model (`uploadStatus`, `resizeStatus`, `resizeCompletedAt`) are updated by Lambda functions according to the workflows defined in the flow and design documents.

## DynamoDB Design Decisions

### Single Table vs. Multi-Table

**Decision**: Single table (`albums`) for both album and image records using type-prefixed composite sort keys

**Rationale**:
- **Simplified Operations**: One table = one billing target, simplified backup/restore, easier monitoring
- **Query Efficiency**: Range queries on sort key enable efficient album and image lookups without secondary indexes
- **Reduced Latency**: No cross-table queries or joins needed
- **Cost Optimization**: Single on-demand table scales for both entity types
- **Maintainability**: Single source of truth for user data

**Schema**:
```
PK: userId (String)          # All records partitioned by user
SK: Type#{id}                # ALBUM#{albumId} or IMAGE#{albumId}#{imageId}
```

---

## Validation Rules Reference

**For detailed validation logic and implementation examples, see:**
- **Album & Image Validation**: `designs/functions/create-entry-design.md` (Expected Logic section)

Validation rules are enforced at the application layer (Lambda functions) before database writes.

---

## Data Consistency & Concurrency

### Eventual Consistency Scenarios

1. **Image Count Mismatch**: 
   - Problem: `album.imageCount` may not match actual images in images table after concurrent deletes
   - Solution: Implement periodic reconciliation Lambda; accept eventual consistency for list view counts

2. **Stale Image Lists**:
   - Problem: Image list may not show newly uploaded image immediately
   - Solution: Client-side refresh; DynamoDB streams (future) for real-time updates

### Strong Consistency Requirements

1. **Create Album**: Must verify uniqueness before write → Strong consistency read before write
2. **Delete Image**: Must verify image exists before delete → Strong consistency read
3. **Album Metadata**: Always use strong consistency for reads to ensure correct imageCount display

---

## Backup & Recovery Strategy

| Entity | Backup Method | Recovery RPO | Recovery RTO |
|--------|---------------|--------------|--------------|
| **DynamoDB** | AWS Backup automated daily | 24 hours | <4 hours |
| **S3 Images** | S3 versioning enabled | Immediate | <1 minute |
| **S3 Static Assets** | Versioning enabled | Immediate | <5 minutes |

---

## Data Retention & Cleanup

| Data Type | Retention Policy | Cleanup Method |
|-----------|------------------|-----------------|
| **Albums** | Permanent until user deletes | On-demand deletion via DeleteAlbumFunction |
| **Images** | Permanent until user deletes | On-demand deletion via DeleteImageFunction; S3 object deleted immediately |
| **Thumbnails** | Same as images | Deleted with image via S3 lifecycle rule |
| **S3 Upload Staging** | 24 hours (incomplete uploads) | S3 Lifecycle Policy: delete incomplete multipart uploads after 1 day |
| **CloudWatch Logs** | 30 days (retention policy) | AWS managed |
| **X-Ray Traces** | 30 days (retention policy) | AWS managed |

---

## Technical Implementation Notes

### Image Processing (WebP Thumbnail Generation)

**Format**: WebP (modern, efficient, 30-40% smaller than JPEG for images)  
**Size**: 256x256 pixels (single size, not multiple variants)  
**Quality**: 90% for high-quality thumbnails with minimal file size

**Java Implementation Considerations**:
- ❌ Java's built-in `javax.imageio.ImageIO` does NOT natively support WebP format
- ✅ Use **Thumbnailator** (net.coobird:thumbnailator:0.4.19) - pure Java library with WebP support
- ✅ No external native bindings required (unlike Google's libwebp), works reliably in AWS Lambda cold starts
- ✅ FAT JAR deployment via Maven Shade Plugin (all dependencies bundled; ~50-60 MB per Lambda)

**S3 Storage Optimization**:
- Store original images in `albums-prod` bucket (user pays for originals)
- Store 256x256 WebP thumbnails in `image-thumbnails` bucket (separate for cost tracking)
- Single thumbnail per image (reduces complexity, fast generation)
- Presigned URLs for both upload (original) and download (thumbnail)

---

## Summary

✅ **1 Unified DynamoDB Table** (`albums`): Albums + Images with type-prefixed composite sort keys  
✅ **Sort Key Structure**: `ALBUM#{albumId}` and `IMAGE#{albumId}#{imageId}` for efficient range queries  
✅ **S3 Naming Aligned**: Object keys mirror DynamoDB structure (`ALBUM#`, `IMAGE#` prefixes)  
✅ **On-Demand Capacity**: Auto-scaling for unpredictable MVP load  
✅ **Efficient Queries**: 
   - Get all albums: `Query(PK=userId, SK begins_with "ALBUM#")`
   - Get album images: `Query(PK=userId, SK begins_with "IMAGE#{albumId}#")`  
✅ **Strong Consistency**: Used for critical operations (create, delete); eventual consistency for lists  
✅ **Validation Rules**: Enforced at application layer (Lambda) before DB writes  
✅ **State Management**: Image upload and album deletion state machines defined  
✅ **Backup & Recovery**: DynamoDB snapshots + S3 versioning enabled  

**Next Phase**: Define API contracts (OpenAPI specs) for all 7 Lambda functions
