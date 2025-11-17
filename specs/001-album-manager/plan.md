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
| **III. One Maven Per Lambda** | Each Lambda = separate Maven module | ✅ PASS | See Project Structure below - 5 separate Lambda projects |
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
├── contracts/             # Phase 1 API contracts (✅ COMPLETED)
│   ├── CreateAlbum.yaml                # ✅ Decouple: Create album endpoint
│   ├── ListAlbums.yaml                 # ✅ Decouple: List albums with pagination
│   ├── GetUploadURL.yaml               # ✅ Decouple: Get presigned upload URL
│   ├── UpdateImageMetadata.yaml        # ✅ Decouple: S3 event handler for metadata update
│   ├── GetDownloadURL.yaml             # ✅ Decouple + Enhanced: Get presigned URL (original|thumbnail via query param)
│   ├── DeleteImage.yaml                # ✅ Decouple: Delete photo endpoint
│   ├── DeleteAlbum.yaml                # ✅ Decouple: Delete album endpoint
│   └── UpdateAlbum.yaml                # ✅ Decouple: Update album metadata (name, description)
├── openapi.yaml           # (deprecated) Master OpenAPI spec—replaced by individual contracts above
└── checklists/
    └── requirements.md    # Specification validation (completed)
```

### Source Code (repository root)

Based on the system design diagram and constitution requirement (One Maven Per Lambda), the repository will have one Maven module per Lambda function plus shared infrastructure:

```text
./src/
├── create-album/                    # Maven module: CreateAlbumFunction
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── CreateAlbumHandler.java
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/
│       └── com/album/lambda/
│           └── CreateAlbumHandlerTest.java
│
├── list-albums/                     # Maven module: ListAlbumsFunction
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── ListAlbumsHandler.java
│   ├── src/main/resources/
│   └── src/test/java/
│
├── get-upload-url/                  # Maven module: GetUploadURLFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
│
├── update-image-data/               # Maven module: UpdateImageDataFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
│
├── resize-image/                    # Maven module: ResizeImageFunction
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/lambda/
│   │       └── ResizeImageHandler.java
│   ├── src/main/resources/
│   │   ├── logback.xml
│   │   └── resize-config.properties # Resize dimensions (512x512 WebP)
│   └── src/test/java/
│       └── com/album/lambda/
│           └── ResizeImageHandlerTest.java
│
├── get-download-url/                # Maven module: GetDownloadURLFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
│
├── delete-image/                    # Maven module: DeleteImageFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
│
├── delete-album/                    # Maven module: DeleteAlbumFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
│
├── update-album/                    # Maven module: UpdateAlbumFunction
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
│
├── shared-layer/                    # Shared utilities (Lambda Layer in .jar form)
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/album/
│   │       ├── models/              # Album, Photo entities
│   │       ├── services/            # DynamoDBService, S3Service, ImageProcessingService
│   │       ├── util/                # Logging, Auth utils
│   │       └── exception/           # Custom exceptions
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

**Structure Decision**: 
- Serverless web application with **8 independent Lambda functions** (one Maven project each) + 1 shared utility library (to be packaged as Lambda Layer or as a dependency) + 1 function triggered by S3 events (ResizeImage). 
- **Total of 9 Lambda functions**: CreateAlbumFunction, ListAlbumsFunction, GetUploadURLFunction, UpdateImageDataFunction, ResizeImageFunction (S3 event-triggered), GetDownloadURLFunction, DeleteImageFunction, DeleteAlbumFunction, UpdateAlbumFunction
- Frontend served from S3 (static assets). 
- Each Lambda corresponds to a user action from the spec (create album, list albums, delete album, update album, etc.). 
- Shared layer contains common models, services, and utilities to avoid code duplication across Lambda functions while maintaining independence per constitution.