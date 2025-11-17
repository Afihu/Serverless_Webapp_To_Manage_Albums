# Feature Specification: Photo Album Manager

**Feature Branch**: `001-album-manager`  
**Created**: 2025-11-04  
**Status**: Draft  
**Input**: User description: "A simple web application to create and manage photo albums. Expected operations: List, Upload, Download, Delete photos/images."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and View Albums (Priority: P1)

User need a way to organize their photos into albums and see what albums they have. This is the foundation for all album management—without albums, photos have nowhere to be stored.

**Why this priority**: P1 - Core functionality. User cannot use the system without being able to create albums and view their collection. This is essential for MVP.

**Independent Test**: Can be fully tested by: (1) Creating a new album, (2) Verifying it appears in the album list, (3) Opening the album and confirming it's empty. Delivers the value of basic album organization.

**Acceptance Scenarios**:

1. **Given** a user is on the home page, **When** they click "Create Album", **Then** a dialog appears to name the new album
2. **Given** a user enters a valid album name (1-255 characters) and confirms, **When** they click "Create", **Then** the album is saved and appears in their album list
3. **Given** a user has created albums, **When** they load the home page, **Then** all their albums display with album names, creation date, and photo count
4. **Given** a user clicks on an album, **When** the album opens, **Then** an empty state message displays (if no photos) or the photos list displays (if photos exist)
5. **Given** a user enters an empty album name, **When** they try to create, **Then** an error message displays: "Album name is required"

---

### User Story 2 - Upload Photos to Album (Priority: P1)

User need to add photos to their albums. This is the primary value of the app—storing and organizing their photo collections.

**Why this priority**: P1 - Core functionality. Uploading photos is the primary user action. MVP cannot function without this capability.

**Independent Test**: Can be fully tested by: (1) Opening an album, (2) Uploading a photo file, (3) Verifying the photo appears in the album with thumbnail preview. Delivers the value of storing photos in albums.

**Acceptance Scenarios**:

1. **Given** a user has opened an album, **When** they click "Upload Photo", **Then** a file browser dialog appears allowing selection of image files
2. **Given** a user selects a valid image file (JPEG, PNG, WebP, GIF up to 50 MB), **When** they confirm, **Then** the file is uploaded and a thumbnail preview displays in the album
3. **Given** a user uploads a photo, **When** the upload completes, **Then** the photo appears in the album list with upload timestamp
4. **Given** a user tries to upload a non-image file or file larger than 50 MB, **When** they attempt upload, **Then** an error message displays: "Invalid file type" or "Image size exceeds 50 MB"
5. **Given** a photo is uploading, **When** the user navigates away, **Then** the upload continues in the background and completes without error

---

### User Story 3 - View and Download Photos (Priority: P2)

User need to view their photos clearly and download them for offline use or sharing. This enhances the usability of stored albums.

**Why this priority**: P2 - High-value feature. Viewing and downloading photos extends the usefulness of the collection. Not strictly MVP but expected for a functional album manager.

**Independent Test**: Can be fully tested by: (1) Opening a photo in the album, (2) Viewing the full-resolution image, (3) Downloading it to local storage. Delivers the value of retrieving and accessing photos.

**Acceptance Scenarios**:

1. **Given** a user clicks on a photo thumbnail in an album, **When** the photo opens, **Then** the full-resolution image displays in a lightbox or full-screen view
2. **Given** a photo is displayed, **When** the user clicks "Download", **Then** the photo file is downloaded to their device with the original filename
3. **Given** a user is viewing a photo in an album, **When** they have multiple photos in the same album, **Then** "Previous" and "Next" navigation buttons appear to browse through photos
4. **Given** a user views a photo, **When** the photo fails to load (network error), **Then** an error message displays: "Could not load photo. Please try again."

---

### User Story 4 - Delete Photos and Albums (Priority: P2)

User need the ability to remove unwanted photos and clean up empty or outdated albums. This gives user control over their data and storage.

**Why this priority**: P2 - Important for data management. Deletion is expected functionality but less critical than creation/upload. User can work around missing delete initially but will expect it soon after.

**Independent Test**: Can be fully tested by: (1) Right-clicking on a photo → delete, or (2) Deleting an album, and verifying it's removed from the list. Delivers the value of cleanup and data control.

**Acceptance Scenarios**:
- **For Photo Deletion**:
1. **Given** a user is viewing an album with photos, **When** they hover over a photo and click the "Delete" button, **Then** a confirmation dialog appears: "Delete this photo? This cannot be undone."
2. **Given** a user confirms photo deletion, **When** they click "Confirm Delete", **Then** the photo is removed from the album and the list updates immediately
3. **Given** a user deletes a photo from an album, **When** that was the last photo, **Then** the album transitions to the empty state view

- **For Album Deletion**:
1. **Given** a user is on the album list, **When** they right-click an album and select "Delete Album", **Then** a confirmation dialog appears: "Delete [Album Name]? The album, along with its photos, will be permanently deleted."
2. **Given** a user confirms album deletion, **When** they click "Confirm Delete", **Then** the album and all its photos are removed, and the album list updates

---

### Edge Cases

- What happens when user's internet connection drops during photo upload? (System will ignore incomplete upload; user can retry)
- What happens when user tries to upload duplicate photos to the same album? (System allows duplicates by renaming them with a postfix; user responsibility to manage)
- What happens if a user deletes an album while photos are still uploading to it? (Upload is cancelled; album and all contents removed)
- What happens when a user refreshes the page while viewing a photo? (Photo view persists; state is maintained)
- What happens when a photo file becomes corrupted or unavailable after upload? (Error message displays; user prompted to delete from album)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow user to create new albums with descriptive names (1-255 characters)
- **FR-002**: System MUST display all albums belonging to user on the home page with album name, creation date, and photo count
- **FR-003**: System MUST allow user to upload image files (JPEG, PNG, WebP, GIF) up to 50 MB to any album
- **FR-004**: System MUST automatically generate and display thumbnail previews for all uploaded photos within 10 seconds of upload
- **FR-005**: System MUST allow user to view full-resolution photos in a dedicated viewer with previous/next navigation
- **FR-006**: System MUST allow user to download any photo to their local device in original format
- **FR-007**: System MUST allow user to delete individual photos from albums with a confirmation prompt
- **FR-008**: System MUST allow user to delete entire albums with a confirmation prompt
- **FR-009**: System MUST persist all albums and photos durably using AWS storage services (S3 for photos, DynamoDB for metadata)
- **FR-010**: System MUST display appropriate error messages for invalid uploads (wrong file type, file too large, upload failed)
- **FR-011**: System MUST prevent duplicate album names per user (album names must be unique within a user's collection)
- **FR-012**: System MUST show upload progress indication during file uploads
- **FR-013**: System MUST support concurrent uploads of multiple photos to the same album

### Key Entities

- **Album**: A collection container with properties:
  - `albumId`: Unique identifier per user
  - `userId`: Owner of the album
  - `albumName`: User-provided name (1-255 chars, unique per user)
  - `createdAt`: ISO 8601 timestamp
  - `photoCount`: Number of photos in album
  - `updatedAt`: Last modified timestamp

- **Photo**: An image asset with properties:
  - `photoId`: Unique identifier within an album
  - `albumId`: Reference to parent album
  - `userId`: Owner of the photo (inherited from album)
  - `fileName`: Original filename provided at upload
  - `fileSize`: Size in bytes
  - `fileFormat`: Image format (JPEG, PNG, WebP, GIF)
  - `uploadedAt`: ISO 8601 timestamp
  - `s3Key`: Location in S3 bucket (for retrieval)
  - `thumbnailUrl`: Pre-signed URL to S3 thumbnail
  - `originalUrl`: Pre-signed URL to S3 full-resolution

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User can create an album and upload their first photo in under 2 minutes
- **SC-002**: Photo thumbnail previews load within 2 seconds of album open
- **SC-003**: Full-resolution photos load within 3 seconds in the viewer
- **SC-004**: User can successfully download any photo in under 5 seconds
- **SC-005**: 95% of photo uploads complete successfully on the first attempt (on stable connection)
- **SC-006**: The system supports storing up to 100,000 photos per user without performance degradation
- **SC-007**: User can complete a "create album, upload 3 photos, delete 1 photo" workflow without assistance
- **SC-008**: System maintains 99.9% availability (uptime measured monthly)

## Assumptions

- User have stable internet connectivity for uploads (>2 Mbps recommended)
- Photo uploads and retrievals use AWS Lambda and S3 for scalable, serverless architecture per project constitution
- Authentication method not specified in initial requirements (system will work as a single-user app initially; can be extended later)

## Phasing & Roadmap

### Phase 1 (MVP - Current)
- CRUD operations for albums
- CRUD operations for photos
  - Upload/download photos (presigned S3 URLs)
- Core DynamoDB and S3 infrastructure

### Phase 2 (Image Processing Enhancement)
- Thumbnail variant for efficient display
- S3 event-driven processing for automatic resize on upload
- View full-resolution photos through UI light boxes
- Dynamic fitting for UI (mobile/desktop screen)
- Display thumbnail previews in album view

### Phase 3 (Security and Advanced Features - Currently in Consideration) 
- Implement sign-in/sign-up
- Support multi-users with isolated album/photo data
- Support for HEIC image format

## Out of Scope
- Photo editing features (crop, rotate, filter, etc.)
- Photo sharing or collaboration features
- Advanced search or tagging functionality
- Face recognition or auto-organization
- Import/export in batch
- Mobile-specific app (web application only)
