# Implementation Plan: Image Album Manager

**Branch**: `001-album-manager` | **Created on**: 2025-11-08 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-album-manager/spec.md`

**Note**: This plan is filled in by the `/speckit.plan` command following the constitution and system design diagram provided.

## Summary

A serverless web application for creating, managing, and organizing image albums. Users can create albums, upload images (JPEG, PNG, WebP, GIF up to 50 MB), view thumbnails and full-resolution images, download images, and delete unwanted images/albums. The system uses AWS Lambda for compute, S3 for image storage, and DynamoDB for metadata persistence. Architecture follows the constitution: Java 21 + AWS SDK v2, one Maven project per Lambda, test-redeploment cycles development, and structured JSON logging to CloudWatch.

## Technical Context

**Language/Version**: Java 21+ (AWS Lambda managed runtime).  
**Primary Dependencies**: AWS SDK v2 (software.amazon.awssdk:*), SLF4J + Logback.  
**Storage**: DynamoDB (album metadata + image metadata), S3 (image assets + thumbnails + static web assets)  
**Testing**: Lambda test events (AWS deployment testing)  
**Target Platform**: AWS Lambda (Java 21 managed runtime), API Gateway for HTTP endpoints  
**Project Type**: Serverless web application with frontend (static HTML/CSS/JS served from S3) + backend (multiple Lambda functions)  
**Performance Goals**: Thumbnail load <1s, full-resolution load <3s, image upload <5s, support 1000 images per user  
**Constraints**: Lambda execution timeout 15 minutes max per function, S3 eventual consistency model, max 50 MB per image file  
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
├── contracts/             # Phase 1 API contracts (completed - operation-based)
│   ├── CreateEntry.yaml               # POST /albums, POST /albums/{albumId}/images
│   ├── ReadEntry.yaml                 # GET /albums/{albumId}, GET /albums/{albumId}/images
│   ├── UpdateEntry.yaml               # PUT /albums/{albumId}, PUT /images/{imageId}/metadata
│   ├── DeleteEntry.yaml               # DELETE /albums/{albumId}, DELETE /albums/{albumId}/images/{imageId}
│   ├── ListEntries.yaml               # GET /albums, GET /albums/{albumId}/images
│   ├── GetUploadURL.yaml              # POST /albums/{albumId}/images/presigned-upload
│   ├── ProcessImage.yaml              # S3 event trigger for thumbnail generation
│   ├── README.md                      # Contract documentation and type discriminator pattern
│   └── MIGRATION.md                   # Migration from entity-based to operation-based architecture
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
    - Shared Utilities:
        - `SharedLayer` - Common models, services, utilities used across all Lambdas
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
- **The desgin for each Lambda function is detailed in the respective design file in `specs\001-album-manager\designs`.**
### Source Code (repository root)

Based on the operation-based architecture and constitution requirement (One Maven Per Lambda), the repository will have one Maven module per operation Lambda plus shared infrastructure:

```text
./src/
├── create-entry/                    # Maven module: CreateEntryFunction
│   ├── pom.xml
│   └── src
|      ├── main/
│      |    ├── java/com/albummanager/app/
│      |    |   └── CreateEntryHandler.java
│      |    └── resources/
│      |        └── logback.xml
│      └──test/java/
│               └── com/albummanager/app/
│                   └── CreateEntryHandlerTest.java
│
├── read-entry/                      # Maven module: ReadEntryFunction
│   ├── pom.xml
│   │   # Handles /images/{imageId}/presigned-download + /albums/{albumId}/images
│   └── src
│       ├── main/
│       │   ├── java/com/albummanager/app/
│       │   │   └── ReadEntryHandler.java
│       │   └── resources/
│       └── test/java/
│
├── update-entry/                    # Maven module: UpdateEntryFunction
│   ├── pom.xml
|   │   # Handles updates to both albums and images' metadata
│   └── src
│       ├── main/
│       │   ├── java/com/albummanager/app/
│       │   │   └── UpdateEntryHandler.java
|       |   └── resources/ 
│       └── test/java/
│
├── delete-entry/                    # Maven module: DeleteEntryFunction
│   ├── pom.xml
│   │   # Handles both HTTP DELETE + S3 event-triggered thumbnail cleanup
│   └── src
│       ├── main/
│       │   ├── java/com/albummanager/app/
│       │   │   └── DeleteEntryHandler.java
|       |   └── resources/ 
│       └── test/java/
│
├── list-entries/                    # Maven module: ListEntriesFunction
│   ├── pom.xml
|   | # Handles listing of both albums and images with pagination in a specified album/bucket
│   └── src
│       ├── main/
│       │   ├── java/com/albummanager/app/
│       │   │   └── ListEntriesHandler.java
|       |   └── resources/ 
│       └── test/java/
│
├── get-upload-url/                  # Maven module: GetUploadURLFunction
│   ├── pom.xml
│   └── src
│       ├── main/
│       │   ├── java/com/albummanager/app/
│       │   │   └── GetUploadURLHandler.java
|       |   └── resources/ 
│       └── test/java/
│
├── process-image/                   # Maven module: ProcessImageFunction (S3 event-triggered)
│   ├── pom.xml
│   └── src
│       ├── main/
│       │   ├── java/com/albummanager/app/
│       │   │   └── ProcessImageHandler.java
|       |   └── resources/
|       |       ├── process-image-config.properties # Resize dimensions (512x512 WebP)
│       │       └── logback.xml 
│       └── test/java/
│
├── shared-layer/                    # Shared utilities (Lambda Layer in .jar form)
│   ├── pom.xml
│   └── src
|       ├── main/
|       │   ├── java/com/albummanager/
|       │   |   ├── models/              # Album, Image, Entry entities
|       │   |   ├── services/            # DynamoDBService, S3Service, ImageProcessingService
|       │   |   ├── util/                # Logging, Auth utils, TypeDiscriminator
|       │   |   ├── exception/           # Custom exceptions
|       │   |   ├── handler/             # Base handler classes with common logic
|       │   └── resources/
│       └── test/java/
│
├── frontend/                       # Static web assets (HTML/CSS/JS)
│   ├── index.html                  # Main web page
│   ├── styles.css                  # Styling
│   ├── app.js                      # Client-side logic, API calls
│   └── assets/
│       ├── images/
│       └── icons/
│
├── docs/                            # Project documentation
│   ├── ARCHITECTURE.md              # System design document
│   ├── SETUP.md                     # Local dev setup with LocalStack
│   └── DEPLOYMENT.md                # AWS deployment guide
│
└── .github/workflows/               # CI/CD pipelines (currently out of scope)
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