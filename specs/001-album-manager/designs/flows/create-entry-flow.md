**This file documents the client-driven flows for album creation and image upload.**

# For album creation Flow

## Client-Side Steps

1. User is currently authenticated in the web app and on the main page.
2. User clicks "Create New Album" button in the web app UI.
3. A modal dialog opens, prompting the user to enter an album name and an optional description.
4. User enters the album name (required) and description (optional).
5. **Client validates**:
   - Album name is not empty/whitespace and 1-255 characters
   - Check cached album list (populated when main page loaded)
   - If cache is empty or stale: Fetch existing album names via `ListEntriesFunction` (type: "album")
   - Check if the entered album name already exists in the list
   - If duplicate found: Show warning to user and prevent submission (user must choose different name)
6. User confirms the creation after validation passes.
7. The web app generates the event payload as specified in `specs\001-album-manager\designs\functions\create-entry-design.md` with `type: "album"`.
8. **Client makes HTTP request**: `POST /albums` with the album creation payload.

## Server-Side Steps (CreateEntryFunction)

9. The `CreateEntryFunction` processes the request, validates the payload, and creates a new album entry in the database.
    - **Server-side validation** (defense-in-depth):
      - Verify album name is not empty/whitespace and 1-255 chars
      - Verify album name is unique per `userId` (strong consistency DynamoDB read)
      - If validation fails, return appropriate HTTP error (400 or 409) and exit
10. Upon successful creation of the album entry and write to DynamoDB table, the `CreateEntryFunction` returns HTTP 201 success response with the newly created `albumId`.

## Client Receives Response

11. Client receives HTTP 201 success response with the new `albumId`.
12. Client updates UI (adds new album to list, closes modal, navigates to album if desired).

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
6. **Client checks for duplicate filenames** (user experience improvement):
   - Check cached image list for the current album (populated when album page loaded)
   - If cache is empty or stale: Fetch existing image filenames via `ListEntriesFunction` (type: "image", albumId: current)
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
    - For each imageId, constructs S3 key: `{userId}/ALBUM#{albumId}/original/{fileName}`
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
      - Downloads original image from S3
      - Generates 256x256 WebP thumbnail
      - Uploads thumbnail to `image-thumbnails` bucket
      - Updates image record in DynamoDB with `resizeStatus: COMPLETED` and `resizeCompletedAt` timestamp

## Client-Side Steps - Part 3: Poll for Thumbnail Completion

17. Client polls `ReadEntryFunction` to check `resizeStatus` for each image:
    - Uses exponential backoff (starting at 200ms, max 5000ms)
    - Continues until `resizeStatus == COMPLETED` or `resizeStatus == FAILED` or timeout (60 attempts)
    - For each image, detects when thumbnail generation completes
    - Updates UI with final thumbnail (replaces temporary thumbnail)
18. User sees all images in album with thumbnails once processing completes