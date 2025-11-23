# Implementation Plan: Photo Album Manager

**Branch**: `001-album-manager` | **Created on**: 2025-11-08 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-album-manager/spec.md`

**Note**: This plan is filled in by the `/speckit.plan` command following the constitution and system design diagram provided.

## Summary

A serverless web application for creating, managing, and organizing photo albums. Users can create albums, upload photos (JPEG, PNG, WebP, GIF up to 50 MB), view thumbnails and full-resolution images, download photos, and delete unwanted photos/albums. The system uses AWS Lambda for compute, S3 for photo storage, and DynamoDB for metadata persistence. Architecture follows the constitution: Java 21 + AWS SDK v2, one Maven project per Lambda, test-redeploment cycles development, and structured JSON logging to CloudWatch.

## Technical Context

**Language/Version**: Java 21+ (AWS Lambda managed runtime).  
**Primary Dependencies**: AWS SDK v2 (software.amazon.awssdk:*), SLF4J + Logback.  
**Storage**: DynamoDB (album metadata + photo metadata), S3 (photo assets + thumbnails + static web assets)  
**Testing**: Lambda test events (AWS deployment testing)  
**Target Platform**: AWS Lambda (Java 21 managed runtime), API Gateway for HTTP endpoints  
**Project Type**: Serverless web application with frontend (static HTML/CSS/JS served from S3) + backend (multiple Lambda functions)  
**Performance Goals**: Thumbnail load <1s, full-resolution load <3s, photo upload <5s, support 1000 photos per user  
**Constraints**: Lambda execution timeout 15 minutes max per function, S3 eventual consistency model, max 50 MB per photo file  
**Scale/Scope**: MVP for single user albums; designed for horizontal scaling via Lambda concurrency and S3 partitioning

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ GATE PASSED - All Constitutional Principles Satisfied

| Principle | Requirement | Status | Notes |
|-----------|-------------|--------|-------|
| **I. AWS-First** | Lambda, S3, DynamoDB mandatory | ✅ PASS | Design uses all three services per diagram |
| **II. Java + SDK v2** | Java 21+ with AWS SDK v2 only | ✅ PASS | Language/Dependencies specified above |
| **III. One Maven Per Lambda** | Each Lambda = separate Maven module | ✅ PASS | See Project Structure below - 6 operation-based Lambda projects |
| **IV. Test & Re-deployment** | Write test events first, deploy to AWS for testing | ✅ PASS | Testing strategy detailed in data-model phase |
| **V. Observability** | SLF4J JSON logging to CloudWatch | ✅ PASS | Logging spec in constitution; all Lambdas include logging |

**Compliance**: Feature aligns completely with constitution. No complexity tracking or justifications needed.

## Project Structure

### Documentation (this feature)

```text
specs/001-album-manager/
├── spec.md                # Feature specification (completed)
├── plan.md                # This file (in progress)
├── research.md            # Phase 0 research findings (completed)
├── data-model.md          # Phase 1 data model (completed)
├── quickstart.md          # Phase 1 quick start guide (completed)
├── contracts/             # Phase 1 API contracts (completed)
│   ├── CreateAlbum.yaml                # /POST Create album endpoint
│   ├── ListAlbums.yaml                 # /GET List albums with pagination
│   ├── UploadImage.yaml               # /GET Get presigned upload URL
│   ├── GetDownloadURL.yaml             # /GET Get presigned URL of an object for download/viewing
│   ├── UpdateImageData.yaml        # /POST S3 event handler for metadata update
│   ├── DeleteImage.yaml                # /DELETE Delete photo endpoint
│   ├── DeleteAlbum.yaml                # /DELETE Delete album endpoint
│   └── UpdateAlbum.yaml                # /POST Update album metadata (name, description)
├── openapi.yaml           # (deprecated) Master OpenAPI spec—replaced by individual contracts above
└── checklists/
    └── requirements.md    # Specification validation (completed)
```

## Structure Decision: 
- Serverless web application with **6 operation-based Lambda functions** (one Maven project each) + 1 shared utility library + 1 event-driven function (ResizeImage). 
- **Total of 8 Lambda functions**: 
    - Core CRUD Operations (type-discriminated):
        - `CreateEntryFunction` - Creates album or image (type: album | image)
        - `ReadEntryFunction`   - Retrieves entry's info and generates presigned URLs (type: album | image)
        - `UpdateEntryFunction` - Updates metadata for album or image (type: album | image)
        - `DeleteEntryFunction` - Deletes album or image, with cascading cleanup (type: album | image)
        - `ListEntriesFunction` - Lists albums or images with pagination (type: album | image)
    - Event-Driven Processing:
        - `ProcessImageFunction` (S3 event-triggered) - Resizes images, generates thumbnails, updates metadata
    - Additional HTTP Endpoints:
        - `GetUploadURLFunction` - Generates presigned S3 upload URLs (creates presigned POST)
- Frontend served from S3 (static assets). 
- **Type Discriminator Pattern**: Each operation Lambda accepts a `type` parameter to determine entity (album or image). This design:
  - Maximizes code reuse across similar entity types
  - Maintains operation-level scalability (can scale CreateEntry independently from DeleteEntry)
  - Reduces boilerplate and duplicate error handling across the system
- Shared layer contains: 
    - Common models (Album, Image, Entry) 
    - CRUD services (DynamoDBService, S3Service)
    - Validation and error handling utilities
    - Logging and observability helpers

### Source Code (repository root)

Based on the operation-based architecture and constitution requirement (One Maven Per Lambda), the repository will have one Maven module per operation Lambda plus shared infrastructure:

```text
./src/
├── create-entry/                    # Maven module: CreateEntryFunction
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── CreateEntryHandler.java
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│       └── com/album/lambda/
│           └── CreateEntryHandlerTest.java
│
├── read-entry/                      # Maven module: ReadEntryFunction
│   ├── pom.xml
│   │   # Handles /photos/{photoId}/presigned-download + /albums/{albumId}/photos
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── ReadEntryHandler.java
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│
├── update-entry/                    # Maven module: UpdateEntryFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   │   └── logback.xml 
│   └── src/test/java/
│
├── delete-entry/                    # Maven module: DeleteEntryFunction
│   ├── pom.xml
│   │   # Handles both HTTP DELETE + S3 event-triggered thumbnail cleanup
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── DeleteEntryHandler.java
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│
├── list-entries/                    # Maven module: ListEntriesFunction
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── ListEntriesHandler.java
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│
├── get-upload-url/                  # Maven module: GetUploadURLFunction
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── GetUploadURLHandler.java
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│
├── process-image/                   # Maven module: ProcessImageFunction (S3 event-triggered)
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── ProcessImageHandler.java
│   ├── src/main/resources/
│   │   ├── logback.xml
│   │   └── process-image-config.properties # Resize dimensions (512x512 WebP)
│   └── src/test/java/
│       └── com/album/lambda/
│           └── ProcessImageHandlerTest.java
│
├── shared-layer/                    # Shared utilities (Lambda Layer in .jar form)
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/
│   │       ├── models/              # Album, Image, Entry entities
│   │       ├── services/            # DynamoDBService, S3Service, ImageProcessingService
│   │       ├── util/                # Logging, Auth utils, TypeDiscriminator
│   │       ├── exception/           # Custom exceptions
│   │       └── handler/             # Base handler classes with common logic
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│
├── frontend/                        # Static web assets (HTML/CSS/JS)
│   ├── index.html
│   ├── styles.css
│   ├── app.js                       # Client-side logic, API calls
│   └── assets/
│       ├── images/
│       └── icons/
│
├── docs/                            # Project documentation
│   ├── ARCHITECTURE.md              # System design document
│   ├── SETUP.md                     # Local dev setup with LocalStack
│   └── DEPLOYMENT.md                # AWS deployment guide
│
└── .github/workflows/               # CI/CD pipelines
    ├── build-and-test.yml           # Build each Lambda module
    ├── deploy-to-aws.yml            # Deploy to AWS Lambda
    └── integration-tests.yml        # Run integration tests
```

**Key Architectural Changes from Entity-Based to Operation-Based**:

1. **Type Discriminator Pattern**: Each Lambda (except event-driven ones) uses a `type` parameter to determine entity behavior
   ```java
   // Example: CreateEntryHandler.java
   public APIGatewayProxyResponseEvent handleRequest(CreateEntryRequest request) {
       return switch (request.getType()) {
           case ALBUM -> createAlbum(request);
           case IMAGE -> createImage(request);
           default -> errorResponse("Invalid type");
       };
   }
   ```

2. **Shared Layer Enhancements**:
   - `TypeDiscriminator` utility for consistent type handling
   - Base handler classes with common CRUD patterns
   - Entry interface/superclass for polymorphic operations
   - DynamoDBService with generic CRUD methods (create, read, update, delete)

3. **Reduced Code Duplication**:
   - Before: 9 Lambda modules with similar patterns
   - After: 6 operation Lambdas + 1 event Lambda + shared layer
   - Shared logic centralized in shared-layer