**This file documents the client-driven flows for album creation and image upload.**

# For album creation Flow

## Client-Side Steps

1. User is currently authenticated in the web app and on the main page.
2. User clicks "Create New Album" button in the web app UI.
3. A modal dialog opens, prompting the user to enter an album name and an optional description.
4. User enters the album name (required) and description (optional).
5. **Client validates upon user clicking "Create"**:
  - Album name is not empty/whitespace and 1-255 characters
  - Check cached album list (populated when main page loaded)
    - If cache is empty or stale: Fetch existing album names via `ListEntriesFunction` (userId: current, type: "album")
  - Check if the entered album name already exists in the list
    - If duplicate found: Show warning to user and prevent submission (user must choose different name)
  - This step repeats until user enters a valid, unique album name.
6. After validation passes, The web app generates the event payload as specified in `specs\001-album-manager\designs\functions\create-entry-design.md` with `type: "album"`.
7. **Client makes HTTP request**: `POST /albums` with the album creation payload.

## Server-Side Steps (CreateEntryFunction)

8. The `CreateEntryFunction` processes the request, validates the payload, and creates a new album entry in the database.
    - **Server-side validation** (defense-in-depth):
      - Verify album name is not empty/whitespace and 1-255 chars
      - Verify album name is unique per `userId` (strong consistency DynamoDB read)
      - If validation fails, return appropriate HTTP error (400 or 409) and exit
9. Upon successful creation of the album entry and write to DynamoDB table, the `CreateEntryFunction` returns HTTP 201 success response with the newly created `albumId`.

## Client Receives Response

10. Client receives HTTP 201 success response with the new `albumId`.
11. Client updates UI (closes modal, adds new album to list, navigates to its page).

---

# For image upload flow

## Client-Side Steps - Part 1: Metadata Preparation

1. User is currently authenticated in the web app and on the page of a specific album.
2. User clicks "Upload Images" button in the web app UI.
3. A file picker dialog opens, allowing the user to select multiple image files from their local device.
4. User selects one or more image files and confirms the selection.
5. The web app collects metadata for each selected image file, including:
  - Original filename
  - File size
  - File format (e.g., JPEG, PNG)
  - MIME type
  - Image dimensions (width and height)
  - MD5 checksum of the file
6. **Client checks for duplicate filenames after user selects files** (user experience improvement):
  - Check cached image list for the current album (populated when album page loaded)
    - If cache is empty or stale: Fetch existing image filenames via `ListEntriesFunction` (userId: current, type: "image", albumId: current)
  - For each selected file, check if filename already exists in the album
    - If duplicate found:
      - Automatically append `[1]`, `[2]`, etc. to the filename (e.g., `sunset.png` → `sunset[1].png`)
      - Show user the mapping: "sunset.png → sunset[1].png" (informational)
      - Store the mapping locally for upload tracking
   - If no duplicate: Use filename as-is
7. Generate the event payload with modified filenames as specified in `specs\001-album-manager\designs\functions\create-entry-design.md` with `type: "image"`.

## Server-Side Steps - Part 1: Metadata Creation (CreateEntryFunction)

8. **Client makes HTTP request**: `POST /albums/{albumId}/images` with the image metadata payload.
9. The `CreateEntryFunction` processes the request, validates the payload, and creates metadata entries for each uploaded image in the database.
    - **Server-side validation**:
      - Verify `albumId` exists in DynamoDB with strong consistency read
      - For each image in batch:
        - Verify required fields (fileName, fileSize, fileFormat, mimeType, width, height, checksumMd5)
        - Verify fileSize is 1-52,428,800 bytes (1 to 50 MB)
        - Verify fileFormat is one of: JPEG, PNG, WEBP, GIF, HEIC
        - Verify width and height are > 0
        - Verify checksumMd5 is 32-char hex string
      - If validation fails, return appropriate HTTP error (400 or 404) and exit
10. Upon successful creation of image metadata entries and write to DynamoDB table, the `CreateEntryFunction` returns HTTP 201 success response with:
    - List of created imageIds
    - Corresponding filenames

## Client Receives Metadata Response

11. Client receives HTTP 201 success response with imageIds and filenames.

## Client-Side Steps - Part 2: Generate Presigned Upload URLs

12. **Client makes second HTTP request**: `POST /presigned-urls` with:
    - `albumId`: The album identifier
    - `imageIds`: List of imageIds returned from previous request
    - `userId`: User identifier (or extract from context)

## Server-Side Steps - Part 2: Generate Presigned URLs (GetUploadUrlFunction)

13. The `GetUploadUrlFunction` generates presigned S3 upload URLs for each image:
    - For each imageId, constructs S3 key: `{userId}/IMAGE#{albumId}#{imageId}#OG`
    - Generates presigned POST URL for S3 bucket `albums-prod`
    - Returns HTTP 200 response with list of objects: `{imageId, fileName, presignedUrl}`

## Client Receives Presigned URLs

14. Client receives HTTP 200 response with presigned upload URLs.
15. **Client begins file uploads** to S3:
    - Maps each local file to its corresponding presignedUrl using imageId
    - Uploads each file to S3 in parallel using the presigned URL
    - Tracks upload progress per imageId
    - Shows upload status to user (e.g., "Uploading 3 of 5 images...")

## Server-Side Steps - Part 3: Async Image Processing (ProcessImageFunction via S3 Event)

16. Upon successful S3 upload for each file:
    - S3 fires `s3:ObjectCreated:Put` event
    - **ProcessImageFunction** is triggered (async) to generate thumbnail and update image metadata
    - ProcessImageFunction:
      - Downloads original image from S3 (`s3Key`: `{userId}/IMAGE#{albumId}#{imageId}#OG`)
      - Generates 256x256 WebP thumbnail (quality 90)
      - Uploads thumbnail to `image-thumbnails` bucket (`thumbnailS3Key`: `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp`)
      - Updates image record in DynamoDB:
        - `resizeStatus`: COMPLETED
        - `resizeCompletedAt`: ISO 8601 timestamp (server-generated, current time)

### Resizing Process State Machine

The image lifecycle involves two independent state tracks during upload and resize:

```
UPLOAD STATUS TRACK:                          RESIZE STATUS TRACK:
    ┌─────────────┐                               ┌─────────────┐
    │   PENDING   │ (GetUploadURLFunction)        │   PENDING   │ (Image uploaded, waiting for resize)
    └──────┬──────┘                               └──────┬──────┘
           │                                             │
           │ (Browser uploads to S3)                     │ (S3 event fires ProcessImageFunction)
           ▼                                             ▼
    ┌──────────────┐                           ┌──────────────────┐
    │  COMPLETED   │◄──────────────────────────│    PROCESSING    │
    │ (Image visible│   Thumbnail replaced      │ (Resizing,       │
    │  in album)   │   when resize completes   │  optimizing)     │
    └──────────────┘                           └────────┬─────────┘
                                                        │
                                              ┌─────────────────┐
                                              │    COMPLETED    │ (Final thumbnail ready)
                                              └─────────────────┘
```

**Upload Status Transitions**:
- **PENDING** → **COMPLETED**: Image successfully uploaded to S3, metadata record created in DynamoDB. **Image appears in album list immediately.**
- **PENDING** → **FAILED**: Upload timeout, file validation error, or S3 error. S3 cleanup occurs, user notified.

**Resize Status Transitions**:
- **PENDING** → **COMPLETED**: ProcessImageFunction generates 256x256 WebP thumbnail, uploads to S3, updates DynamoDB. **Temporary thumbnail replaced with final optimized version.**
- **PENDING** → **FAILED**: Resize processing error (invalid image, memory limit, timeout). Log error, retain temporary thumbnail for user.

**Album List Visibility**:
- Images appear when: `uploadStatus == COMPLETED` (regardless of `resizeStatus`)
- Images displayed with: Original image (temporary) if `resizeStatus == PENDING`, OR final 256x256 WebP if `resizeStatus == COMPLETED`
- Transition is transparent to user: temporary thumbnail automatically replaces with optimized version

## Client-Side Steps - Part 3: Poll for Thumbnail Completion

17. Client polls `ReadEntryFunction` to detect when resize completes and final thumbnail becomes available:
    - Enables real-time UI updates without server push infrastructure
    - Uses exponential backoff polling (starting at 200ms, max 5000ms)
    - Continues until `resizeStatus == COMPLETED` or `resizeStatus == FAILED` or timeout (60 attempts, ~30 seconds)
    - For each image, detects when thumbnail generation completes and updates UI with final thumbnail

### Polling Request/Response Format

**Polling Endpoint**: `POST /read-entry` (ReadEntryFunction)

**Request Body**:
```json
{
  "type": "image",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "imageId": "660f9511-f30c-52e5-b817-557766551111"
}
```

**Response Body**:
```json
{
  "imageId": "660f9511-f30c-52e5-b817-557766551111",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "uploadStatus": "COMPLETED",
  "resizeStatus": "PENDING|COMPLETED|FAILED",
  "temporaryThumbnailUrl": "https://albums-prod.s3.amazonaws.com/{userId}/IMAGE#{albumId}#{imageId}#OG?X-Amz-Expires=3600",
  "finalThumbnailUrl": "https://image-thumbnails.s3.amazonaws.com/{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp?X-Amz-Expires=3600",
  "uploadedAt": "2025-11-08T14:30:00Z",
  "resizeCompletedAt": "2025-11-08T14:31:15Z"
}
```

### Polling Algorithm

**Initial State**:
- `delay = 200` (milliseconds)
- `maxDelay = 5000` (milliseconds, 5 seconds cap)
- `maxAttempts = 60` (approximately 30 second timeout)
- `attemptCount = 0`

**Polling Loop**:
```
While attemptCount < maxAttempts:
  1. Wait delay milliseconds
  2. Send ReadEntry request for (type=image, albumId, imageId)
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
  - Polling timeout: keep temporary thumbnail visible
  - User can manually refresh to retry
```

**Backoff Progression** (10% increase per attempt):
- Attempt 1: 200ms delay
- Attempt 2: 220ms delay (200 × 1.1)
- Attempt 3: 242ms delay (220 × 1.1)
- Attempt 4: 266ms delay (242 × 1.1)
- Attempt 5: 293ms delay (266 × 1.1)
- Continues with 10% increase until capped at maxDelay (5000ms)

### Error Handling During Polling

| Scenario | Action |
|----------|--------|
| Network error on poll | Retry immediately; count toward maxAttempts |
| HTTP 404 (image not found) | Stop polling; log error; show error to user |
| HTTP 500 (server error) | Retry with exponential backoff; count toward maxAttempts |
| Polling timeout (60 attempts) | Stop polling; keep temporary thumbnail; allow manual refresh |
| resizeStatus == FAILED | Stop polling immediately; show warning to user |

### Frontend Implementation Notes

- **Start polling** immediately after upload completes (`uploadStatus == COMPLETED`)
- Use browser `fetch()` with 5-second timeout per request
- Debounce UI updates: only update DOM if thumbnail URL changes
- Store image metadata locally to preserve state across navigation
- Resume polling if user returns to album (fresh attempt counter)
- Show progress indicator while polling (e.g., "Processing thumbnail...")

18. User sees all images in album with final thumbnails once processing completes