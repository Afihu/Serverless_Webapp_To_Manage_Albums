# Tasks: Photo Album Manager

**Feature Branch**: `001-album-manager`  
**Created**: 2025-11-17  
**Status**: In Progress  
**Total Tasks**: 54

---

## Executive Summary

This task document provides an actionable, dependency-ordered breakdown of all work required to deliver the Photo Album Manager MVP. Tasks are organized by phase and user story to enable parallel development and independent testing of each feature increment.

### Key Metrics

- **Phase 1 (Setup)**: 8 tasks (includes 3 IAM definition tasks)
- **Phase 2 (Foundational)**: 9 tasks (includes layer packaging configuration)
- **Phase 3 (US1 - Create & View Albums)**: 11 tasks
- **Phase 4 (US2 - Upload Photos)**: 15 tasks
- **Phase 5 (US3 - View & Download Photos)**: 9 tasks
- **Phase 6 (US4 - Delete Photos & Albums)**: 11 tasks
- **Phase 7 (Polish & Deployment)**: 5 tasks (includes layer deployment)
- **Total**: 68 tasks

### MVP Scope (Recommended)

Start with **Phase 1 → Phase 2 → Phase 3 → Phase 4** to deliver core value (create albums, upload photos). Phases 5 and 6 can be added incrementally. Phase 7 focuses on production-readiness.

### Parallel Opportunities

- **Phase 3**: Tasks T016, T018, T020, T022 can run in parallel (independent Lambda modules)
- **Phase 4**: Tasks T027, T029, T031, T033, T035 can run in parallel (independent Lambda modules)
- **Phase 5**: Tasks T038, T042 can run in parallel; Task T045 blocks until T038 completes
- **Phase 6**: Task T048 can run while other stories complete

---

## Phase 1: Setup & Infrastructure

**Goal**: Initialize project structure, configure Maven, set up AWS infrastructure scaffolding, and define IAM roles for Lambda functions.

**Independent Test Criteria**: All Lambda modules compile without errors; shared-layer Maven module builds successfully; pom.xml files reference correct AWS SDK versions; IAM role definitions prepared for deployment.

### Tasks

- [ ] T001 Initialize Maven parent POM with AWS SDK v2 and shared dependencies at repository root
- [ ] T002 Create shared-layer Maven module (packaged as AWS Lambda Layer) with common models (Album, Photo entities) in `src/shared-layer/`
- [ ] T003 Create shared-layer utilities: DynamoDBService, S3Service, and logging configuration in `src/shared-layer/src/main/java/com/album/` (NOTE: shared-layer is a reusable library packaged as Lambda Layer .zip, NOT a Lambda function)
- [ ] T004 Configure logback.xml for JSON structured logging in each Lambda module template
- [ ] T005 Create frontend directory structure with index.html, styles.css, app.js scaffolds in `src/frontend/`
- [ ] T006 [P] Define IAM execution roles for each Lambda function in Terraform/SAM with minimal required permissions: CreateAlbumRole, ListAlbumsRole, GetUploadURLRole, UpdateImageDataRole, ResizeImageRole, GetDownloadURLRole, DeleteImageRole, DeleteAlbumRole in `infrastructure/iam-roles.tf` or SAM template
- [ ] T007 [P] Define IAM policy documents for DynamoDB access (tables: albums; operations: required per Lambda) in `infrastructure/policies/dynamodb-policy.json`
- [ ] T008 [P] Define IAM policy documents for S3 access (buckets: albums-prod, image-thumbnails; operations: required per Lambda) in `infrastructure/policies/s3-policy.json`

---

## Phase 2: Foundational Infrastructure & Common Services

**Goal**: Build reusable service classes in shared-layer and package as Lambda Layer for all functions to import.

**Dependencies**: Phase 1 must complete first.

**Independent Test Criteria**: DynamoDBService can CRUD album records; S3Service can generate presigned URLs and manage objects; environment variable configuration works; shared-layer successfully builds and packages as Lambda Layer .zip.

### Tasks

- [ ] T009 Implement DynamoDBService in shared-layer with methods: createAlbum(), queryAlbumsByUserId(), getAlbum(), updateAlbum(), deleteAlbum() in `src/shared-layer/src/main/java/com/album/services/DynamoDBService.java`
- [ ] T010 Implement DynamoDBService photo methods: createPhoto(), queryPhotosByAlbumId(), getPhoto(), updatePhoto(), deletePhoto() in `src/shared-layer/src/main/java/com/album/services/DynamoDBService.java`
- [ ] T011 Implement S3Service in shared-layer with methods: generatePresignedUploadUrl(), generatePresignedDownloadUrl(), uploadObject(), downloadObject(), deleteObject() in `src/shared-layer/src/main/java/com/album/services/S3Service.java`
- [ ] T012 Create Album and Photo entity models with validation logic in `src/shared-layer/src/main/java/com/album/models/`
- [ ] T013 Implement ErrorResponse and custom exception classes (AlbumNotFoundException, PhotoNotFoundException, ValidationException) in `src/shared-layer/src/main/java/com/album/exception/`
- [ ] T014 Add environment configuration utility to read AWS resource names (table name, bucket names) from Lambda environment variables in `src/shared-layer/src/main/java/com/album/util/`
- [ ] T015 Create CloudWatch logger utility for structured JSON logging in `src/shared-layer/src/main/java/com/album/util/CloudWatchLogger.java`
- [ ] T016 Configure shared-layer pom.xml to build Lambda Layer .zip with maven-dependency-plugin: dependencies copied to `target/lib/`, then packaged as `layer.zip` with `java/lib/` directory structure
- [ ] T017 Write integration tests for DynamoDBService and S3Service using mock AWS services in `src/shared-layer/src/test/java/com/album/services/`

---

## Phase 3: User Story 1 - Create and View Albums (Priority: P1)

**Goal**: Enable users to create albums and view their album collection.

**Dependencies**: Phase 2 must complete first.

**Independent Test Criteria**: 
1. User can create album with valid name → album appears in DynamoDB
2. User can list all albums for their userId → returns correct metadata (name, createdAt, photoCount)
3. User receives error for duplicate album names
4. User receives error for empty/invalid album names

**User Stories Covered**: US1

### Tasks - CreateAlbum Lambda

- [ ] T018 [P] [US1] Create create-album Maven module with pom.xml and standard project structure in `src/create-album/` (pom.xml includes shared-layer as Maven dependency; Lambda Layer attached separately during deployment)
- [ ] T019 [P] [US1] Implement CreateAlbumHandler Lambda function with validation for album name uniqueness and format in `src/create-album/src/main/java/com/album/lambda/CreateAlbumHandler.java`
- [ ] T020 [P] [US1] Implement album creation business logic: generate albumId (UUID), set createdAt/updatedAt, initialize photoCount=0 in `CreateAlbumHandler`
- [ ] T021 [US1] Write test cases for CreateAlbumHandler: valid creation, duplicate name, empty name, 255 char name in `src/create-album/src/test/java/com/album/lambda/CreateAlbumHandlerTest.java`

### Tasks - ListAlbums Lambda

- [ ] T022 [P] [US1] Create list-albums Maven module with pom.xml in `src/list-albums/`
- [ ] T023 [P] [US1] Implement ListAlbumsHandler Lambda function to query albums by userId with pagination support in `src/list-albums/src/main/java/com/album/lambda/ListAlbumsHandler.java`
- [ ] T024 [US1] Implement pagination: support lastEvaluatedKey for subsequent queries, default limit 20 albums in `ListAlbumsHandler`
- [ ] T025 [US1] Write test cases for ListAlbumsHandler: query with 0 albums, 1 album, 20+ albums, pagination token handling in `src/list-albums/src/test/java/com/album/lambda/ListAlbumsHandlerTest.java`

### Tasks - Frontend Integration for US1

- [ ] T026 [P] [US1] Implement frontend CreateAlbum dialog form with input validation in `src/frontend/app.js` (albumName text input, create button, cancel button)
- [ ] T027 [US1] Implement frontend album list view with album cards showing name, createdAt, photoCount in `src/frontend/app.js`
- [ ] T028 [US1] Implement API Gateway integration for CreateAlbum and ListAlbums endpoints in frontend fetch calls

---

## Phase 4: User Story 2 - Upload Photos to Album (Priority: P1)

**Goal**: Enable users to upload image files to albums with validation and progress tracking.

**Dependencies**: Phase 3 must complete first (users need albums to upload photos to).

**Independent Test Criteria**:
1. User can get presigned upload URL for valid album
2. User can upload valid image file (JPEG, PNG, WebP, GIF, ≤50MB) to S3
3. Photo metadata is created in DynamoDB with uploadStatus=PENDING
4. Album photoCount is incremented
5. User receives error for invalid file type or oversized file (>50MB)
6. Thumbnail is generated after upload completes

**User Stories Covered**: US2

### Tasks - GetUploadURL Lambda

- [ ] T029 [P] [US2] Create get-upload-url Maven module with pom.xml in `src/get-upload-url/`
- [ ] T030 [P] [US2] Implement GetUploadURLHandler Lambda to generate presigned S3 upload URL in `src/get-upload-url/src/main/java/com/album/lambda/GetUploadURLHandler.java`
- [ ] T031 [P] [US2] Validate album existence and userId ownership; create Photo record with uploadStatus=PENDING; return presigned URL + photoId in `GetUploadURLHandler`
- [ ] T032 [US2] Write test cases for GetUploadURLHandler: valid album, invalid albumId, missing authorization in `src/get-upload-url/src/test/java/com/album/lambda/GetUploadURLHandlerTest.java`

### Tasks - UpdateImageMetadata Lambda (S3 Event Trigger)

- [ ] T033 [P] [US2] Create update-image-data Maven module with pom.xml in `src/update-image-data/`
- [ ] T034 [P] [US2] Implement UpdateImageDataHandler triggered by S3 PutObject event to validate file and extract metadata in `src/update-image-data/src/main/java/com/album/lambda/UpdateImageDataHandler.java`
- [ ] T035 [P] [US2] Validate file type (MIME type check), file size (≤50MB), extract image dimensions; update Photo record with fileFormat, width, height, uploadStatus=COMPLETED in `UpdateImageDataHandler`
- [ ] T036 [US2] Write test cases for UpdateImageDataHandler: valid JPEG/PNG/WebP/GIF, invalid file type, oversized file, corrupted image in `src/update-image-data/src/test/java/com/album/lambda/UpdateImageDataHandlerTest.java`

### Tasks - ResizeImage Lambda (S3 Event Trigger)

- [ ] T037 [P] [US2] Create resize-image Maven module with pom.xml in `src/resize-image/`
- [ ] T038 [P] [US2] Implement ResizeImageHandler Lambda to resize uploaded photo to 512×512 WebP thumbnail in `src/resize-image/src/main/java/com/album/lambda/ResizeImageHandler.java`
- [ ] T039 [P] [US2] Generate 512×512 WebP thumbnail at quality 90; upload to image-thumbnails S3 bucket; update Photo.resizeStatus=COMPLETED in `ResizeImageHandler`
- [ ] T040 [US2] Write test cases for ResizeImageHandler: valid JPEG to WebP, PNG to WebP, oversized image downsampling in `src/resize-image/src/test/java/com/album/lambda/ResizeImageHandlerTest.java`

### Tasks - Frontend Integration for US2

- [ ] T041 [P] [US2] Implement frontend upload photo UI: file input, progress bar, upload button in `src/frontend/app.js`
- [ ] T042 [US2] Implement API call to GetUploadURL; get presigned URL and upload file directly to S3 from browser in frontend `app.js`
- [ ] T043 [US2] Implement upload progress tracking and error handling (invalid file, oversized file, upload failure) in frontend

---

## Phase 5: User Story 3 - View and Download Photos (Priority: P2)

**Goal**: Enable users to view photos in full resolution and download them locally.

**Dependencies**: Phase 4 must complete first (photos must exist to view/download).

**Independent Test Criteria**:
1. User can open photo thumbnail and view full-resolution image
2. User can download photo to local device
3. Photo viewer displays previous/next navigation buttons for browsing
4. Error message displays if photo fails to load
5. Presigned URLs are correctly generated for both original and thumbnail variants

**User Stories Covered**: US3

### Tasks - GetDownloadURL Lambda

- [ ] T044 [P] [US3] Create get-download-url Maven module with pom.xml in `src/get-download-url/`
- [ ] T045 [P] [US3] Implement GetDownloadURLHandler Lambda to generate presigned S3 download URL for original or thumbnail variant in `src/get-download-url/src/main/java/com/album/lambda/GetDownloadURLHandler.java`
- [ ] T046 [US3] Support query parameter `variant=original|thumbnail`; validate photo existence and userId ownership; return presigned download URL in `GetDownloadURLHandler`
- [ ] T047 [US3] Write test cases for GetDownloadURLHandler: valid photo original, valid thumbnail, missing photo, variant parameter handling in `src/get-download-url/src/test/java/com/album/lambda/GetDownloadURLHandlerTest.java`

### Tasks - Frontend Integration for US3

- [ ] T048 [P] [US3] Implement frontend photo viewer lightbox modal in `src/frontend/app.js` (full-resolution image display, prev/next buttons, close button)
- [ ] T049 [US3] Implement API call to GetDownloadURL; load thumbnail on album view and full-resolution on photo click in frontend
- [ ] T050 [US3] Implement previous/next navigation between photos in the same album in frontend photo viewer
- [ ] T051 [US3] Implement download button to trigger browser download of original photo file in frontend

### Tasks - Error Handling for US3

- [ ] T052 [US3] Implement error boundary in frontend to display "Could not load photo. Please try again." on S3 access failures or network errors

---

## Phase 6: User Story 4 - Delete Photos and Albums (Priority: P2)

**Goal**: Enable users to delete individual photos and entire albums with confirmation.

**Dependencies**: Phase 4 must complete first; Phase 5 recommended for complete user experience.

**Independent Test Criteria**:
1. User can delete individual photo with confirmation → photo removed from DynamoDB and S3
2. Album photoCount is decremented after photo deletion
3. User can delete entire album with confirmation → album and all photos removed
4. Error message displayed if deletion fails
5. Album transitions to empty state if last photo is deleted

**User Stories Covered**: US4

### Tasks - DeleteImage Lambda

- [ ] T053 [P] [US4] Create delete-image Maven module with pom.xml in `src/delete-image/`
- [ ] T054 [P] [US4] Implement DeleteImageHandler Lambda to delete photo from DynamoDB and S3 bucket in `src/delete-image/src/main/java/com/album/lambda/DeleteImageHandler.java`
- [ ] T055 [US4] Decrement album photoCount after deletion; delete both original and thumbnail from S3; validate ownership before deletion in `DeleteImageHandler`
- [ ] T056 [US4] Write test cases for DeleteImageHandler: valid photo deletion, non-existent photo, unauthorized deletion, S3 cleanup in `src/delete-image/src/test/java/com/album/lambda/DeleteImageHandlerTest.java`

### Tasks - DeleteAlbum Lambda

- [ ] T057 [P] [US4] Create delete-album Maven module with pom.xml in `src/delete-album/`
- [ ] T058 [US4] Implement DeleteAlbumHandler Lambda to delete album and all associated photos from DynamoDB and S3 in `src/delete-album/src/main/java/com/album/lambda/DeleteAlbumHandler.java`
- [ ] T059 [US4] Query all photos in album; delete each photo's S3 objects; delete photo records; delete album record; validate ownership in `DeleteAlbumHandler`
- [ ] T060 [US4] Write test cases for DeleteAlbumHandler: delete album with 0 photos, delete album with multiple photos, non-existent album, unauthorized deletion in `src/delete-album/src/test/java/com/album/lambda/DeleteAlbumHandlerTest.java`

### Tasks - Frontend Integration for US4

- [ ] T061 [US4] Implement delete photo UI: hover over photo to show delete button; click triggers confirmation dialog in `src/frontend/app.js`
- [ ] T062 [US4] Implement delete album UI: right-click album card or delete button; confirmation dialog with album name in `src/frontend/app.js`
- [ ] T063 [US4] Implement API calls to DeleteImage and DeleteAlbum endpoints in frontend; handle success and error responses

---

## Phase 7: Polish, Integration & Deployment

**Goal**: Finalize frontend UX, integrate all Lambda functions with API Gateway, package and deploy shared-layer as Lambda Layer, and prepare for AWS deployment.

**Dependencies**: All previous phases.

**Independent Test Criteria**: All endpoints callable via API Gateway; frontend successfully communicates with all Lambda functions; shared-layer Lambda Layer deployed and attached to all 8 Lambdas; error handling works end-to-end.

### Tasks

- [ ] T064 Build shared-layer Lambda Layer .zip: run `mvn clean package` in shared-layer; copy dependencies to `java/lib/`; create layer.zip with `java/lib/*.jar` structure
- [ ] T065 Publish shared-layer Lambda Layer to AWS: run `aws lambda publish-layer-version --layer-name album-manager-layer --zip-file fileb://layer.zip --compatible-runtimes java21`
- [ ] T066 Update all Lambda function deployment configuration (SAM or Terraform) to attach shared-layer Lambda Layer ARN to each of the 8 Lambda functions (create-album, list-albums, get-upload-url, update-image-data, resize-image, get-download-url, delete-image, delete-album)
- [ ] T067 Create/update SAM template (template.yaml or CloudFormation) to deploy all Lambda modules (with shared-layer attached), API Gateway, S3 buckets, DynamoDB table, and IAM roles in repository root
- [ ] T068 Integrate frontend with API Gateway endpoints: update app.js with correct base URL; test all CRUD operations end-to-end in local environment with LocalStack or AWS sandbox

---

## Dependency Graph

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational Services)
    ↓
Phase 3 (US1 - Create & View Albums)
    ├─→ Can proceed to Phase 4 or 5 (independent)
    ↓
Phase 4 (US2 - Upload Photos)
    ├─→ Required before Phase 5 & 6
    ├─→ Parallel: GetUploadURL, UpdateImageData, ResizeImage can develop simultaneously
    ↓
Phase 5 (US3 - View & Download Photos)
Phase 6 (US4 - Delete Photos & Albums)
    ├─→ Can develop in parallel after Phase 4
    ↓
Phase 7 (Polish & Deployment)
    └─→ Final integration and deployment to AWS
```

## Parallel Execution Examples

### Example 1: Accelerate Phase 3 (US1 - Albums)

**Day 1-2**: 
- Team A: T014-T017 (CreateAlbum Lambda)
- Team B: T018-T021 (ListAlbums Lambda)
- Team C: T022-T024 (Frontend US1)

All three teams work independently; sync on APIs before integration testing.

### Example 2: Accelerate Phase 4 (US2 - Upload)

**Day 3-4**:
- Team A: T025-T028 (GetUploadURL Lambda)
- Team B: T029-T032 (UpdateImageData Lambda)
- Team C: T033-T036 (ResizeImage Lambda)
- Team D: T037-T039 (Frontend Upload UI)

All Lambda modules complete independently; frontend uses GetUploadURL presigned URL for direct S3 upload.

### Example 3: US3 & US4 in Parallel

**After Phase 4**:
- Team E: T040-T048 (US3 - View & Download Photos)
- Team F: T049-T059 (US4 - Delete Photos & Albums)

Both user stories have no blocking dependencies on each other.

---

## Success Criteria Mapping

| Success Criteria | Related Tasks | Phase |
|------------------|---------------|-------|
| **SC-001**: Create album & upload first photo in <2 min | T015-T020, T025-T031, T037-T038 | 3-4 |
| **SC-002**: Thumbnail loads <2s after album open | T034-T035, T044-T045 | 4-5 |
| **SC-003**: Full-resolution load <3s in viewer | T041-T045 | 5 |
| **SC-004**: Download photo <5s | T041-T042, T046-T047 | 5 |
| **SC-005**: 95% upload success rate | T027-T031 (validation) | 4 |
| **SC-006**: Support 100K photos per user | T006-T007 (DynamoDB scaling) | 2 |
| **SC-007**: Complete workflow without assistance | T015-T059 (all tasks) | 3-6 |
| **SC-008**: 99.9% availability | T060-T061 (deployment) | 7 |

---

## Acceptance Criteria Mapping

### User Story 1 - Create and View Albums

| AC Scenario | Related Tasks |
|-------------|---------------|
| AC1: Click "Create Album" → dialog appears | T022-T024 |
| AC2: Valid album name → saved and appears in list | T015-T017, T023 |
| AC3: Load home page → all albums display | T019-T020, T023 |
| AC4: Click album → opens with empty or photo list | T019-T023 |
| AC5: Empty album name → error message | T015-T017 |

### User Story 2 - Upload Photos to Album

| AC Scenario | Related Tasks |
|-------------|---------------|
| AC1: Click "Upload Photo" → file browser appears | T037-T038 |
| AC2: Upload valid image → thumbnail appears | T026-T027, T029-T035, T037-T038 |
| AC3: Upload completes → photo appears with timestamp | T030-T031, T037 |
| AC4: Invalid file or >50MB → error message | T030-T031 |
| AC5: Navigate away during upload → continues in background | T038-T039 |

### User Story 3 - View and Download Photos

| AC Scenario | Related Tasks |
|-------------|---------------|
| AC1: Click photo thumbnail → full-resolution displays | T041-T045 |
| AC2: Click "Download" → file downloads to device | T041-T046-T047 |
| AC3: Multiple photos → prev/next buttons appear | T044-T046 |
| AC4: Photo fails to load → error message displays | T048 |

### User Story 4 - Delete Photos and Albums

| AC Scenario | Related Tasks |
|-------------|---------------|
| AC1 (Photo): Hover & click delete → confirmation | T057 |
| AC2 (Photo): Confirm delete → removed immediately | T050-T051, T057 |
| AC3 (Photo): Last photo deleted → empty state | T051, T057 |
| AC4 (Album): Right-click → delete confirmation | T058 |
| AC5 (Album): Confirm delete → album and photos removed | T054-T055, T058 |

---

## Technical Implementation Notes

### Environment Variables (Required for All Lambdas)

Each Lambda module must read from Lambda environment variables:
- `ALBUMS_TABLE`: DynamoDB table name (default: `albums`)
- `PHOTOS_BUCKET`: S3 bucket for original photos (default: `albums-prod`)
- `THUMBNAILS_BUCKET`: S3 bucket for thumbnails (default: `image-thumbnails`)
- `STATIC_ASSETS_BUCKET`: S3 bucket for frontend (default: `albums-static`)
- `AWS_REGION`: AWS region (default: `us-east-1`)

### Testing Strategy

1. **Unit Tests**: Test individual Lambda handlers with mock DynamoDB/S3 (Tasks T017, T021, T028, T032, T036, T043, T052, T056)
2. **Integration Tests**: Test DynamoDBService and S3Service with LocalStack (Task T013)
3. **End-to-End Tests**: Deploy to AWS sandbox and test complete workflows (Phase 7 - T061)

### Build & Package

**Shared-Layer (Lambda Layer)**:
- Built with `maven-dependency-plugin` to copy all dependencies to `target/lib/`
- Packaged as `layer.zip` with structure: `java/lib/*.jar`
- Published to AWS Lambda as a layer (e.g., `album-manager-layer:1`)
- Each Lambda function pom.xml includes shared-layer as a Maven dependency during development/testing
- At deployment, each Lambda has the shared-layer Lambda Layer ARN attached (up to 5 layers per Lambda; AWS standard)

**Lambda Functions**:
- Each Lambda Maven module includes shared-layer as a Maven dependency for compilation and local testing
- At runtime in AWS Lambda, shared-layer classes are loaded from the attached Lambda Layer (not from the function's deployment package)
- This avoids duplicating dependencies across 8 Lambda .zip files, reducing deployment package size
- Each Lambda is packaged as a simple .zip or .jar with only handler code (dependencies come from layer)

**Benefits**:
- Single source of truth for AWS SDK v2, SLF4J, and utility code
- Smaller Lambda deployment packages (~50-100 KB per function instead of ~50-100 MB with bundled dependencies)
- Consistent versions across all Lambdas
- Faster deployment and cold start times

---

## Checklist Status Legend

- `[ ]` - Not started
- `[x]` - Completed
- `[~]` - In progress

All tasks are currently marked as not started. Mark tasks as in-progress when work begins, and completed upon verification.
