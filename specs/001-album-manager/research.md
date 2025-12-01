# Research Phase: Image Album Manager

**Created**: 2025-11-08  
**Input**: Technical Context from plan.md + System Design Diagram  
**Output**: Design decisions and architectural guidance for Phase 1 (Design & Contracts)

---

## Design Decisions

### 1. Lambda Decomposition Strategy

**Decision**: One Maven project per Lambda function (7 total Lambda functions)

**Rationale**:
- **Constitution Principle III**: "One Maven Project Per Lambda" ensures isolated dependency management, independent versioning, and clean deployment boundaries
- **User Story Alignment**: Each Lambda maps directly to a functionality (create, upload, download, delete, etc.) which can then be used for each use case, except for the ProcessImage triggered by S3 events.
- **Scalability**: Each function can be optimized, versioned, and deployed independently
- **Maintainability**: Clear separation of concerns - each handler is focused on a single functionality

**Alternatives Considered**:
1. **REJECTED** Monolithic Lambda with all handlers (System per Lambda) - Violates constitution, creates hidden coupling
2. **REJECTED** Multiple handlers per Maven project - Easier initial development but harder to scale and version independently
3. **REJECTED**: One Lambda per operation on entity (e.g., CreateAlbum, ListAlbums, GetAlbum, etc.) - Too granular, leads to excessive Lambdas and code duplication. 
4. ✅ **SELECTED**: One Maven project per Lambda function with each handling a specific functionality aligned to user stories.

**Lambda Functions**:

| # | Lambda Function | User Story | Trigger | Notes |
|---|-----------------|------------|---------|-------|
| 1 | CreateEntryFunction | US1 + US3 | API Gateway POST | Creates album record in DynamoDB with SK: `ALBUM#{albumId}` |
| 2 | ListEntriesFunction | US1 + US3 | API Gateway GET | Queries DynamoDB: `Query(PK=userId, SK begins_with "ALBUM#")` |
| 3 | ReadEntryFunction | US1 + US3 | API Gateway POST | Returns presigned S3 download URL |
| 4 | GetUploadURLFunction | US2 | API Gateway POST | Returns presigned S3 upload URL |
| 5 | UpdateEntryFunction | US2 | API Gateway POST or S3 event (after upload completes) | Updates image metadata with SK: `IMAGE#{albumId}#{imageId}`, triggers thumbnail generation |
| 6 | GetDownloadURLFunction | US3 | API Gateway POST | Returns presigned S3 download URL |
| 7 | DeleteEntryFunction | US4 | API Gateway DELETE | Deletes image record from DynamoDB (SK: `IMAGE#...`) and S3 objects |
---

### 2. Storage Architecture

**Decision**: S3 for images + thumbnails, single DynamoDB table with type-prefixed sort keys for all metadata

**Rationale**:
- **S3 for Images**: Designed for large binary objects, built-in scalability, native support for presigned URLs, lifecycle policies
- **Single DynamoDB Table**: Simplified operations, efficient range queries via type-prefixed sort keys (`ALBUM#` and `IMAGE#`), no cross-table operations, reduced latency
- **Type-Prefixed Sort Keys**: Enable efficient queries per entity type while keeping data in one table

**DynamoDB Single Table Design**:
```
Table:  albums
PK:     userId              # All records partitioned by user
SK:     ALBUM#{id}          # Album records
        IMAGE#{albId}#{id}  # Image records
```

**S3 Structure** (aligned with DynamoDB):
```
albums-prod/
├── {userId}/IMAGE#{albumId}#{imageId}#OG

image-thumbnails/
├── {userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp
```

**Query Patterns**:
- All albums: `Query(userId, "ALBUM#")`
- Album images: `Query(userId, "IMAGE#{albumId}#")`
- Specific item: `GetItem(userId, "ALBUM#id")` or `GetItem(userId, "IMAGE#albId#id")`

---

### 3. File Upload Pattern

**Decision**: Presigned URL + S3 event-driven workflow (not direct Lambda upload)

**Rationale**:
- **Lambda Timeout Limitation**: Lambda has 15-minute max execution. Large file uploads handled via presigned URL bypass this (browser uploads directly to S3)
- **Cost Efficiency**: Avoids streaming large files through Lambda (expensive memory + compute time)
- **Event-Driven**: S3 triggers `ResizeImageFunction` when upload completes → generates thumbnail, updates DynamoDB with SK: `IMAGE#{albumId}#{imageId}`
- **Scalability**: S3 handles concurrent uploads natively; Lambda processes asynchronously

**Flow**:
1. User clicks "Upload Image" → Browser calls `GetUploadURLFunction`
2. Lambda returns presigned URL (valid 15 minutes) pointing to `{userId}/IMAGE#{albumId}#{imageId}#OG`
3. Browser uploads directly to S3 (bypasses Lambda)
4. S3 fires event → triggers `ResizeImageFunction`
5. Lambda generates 256x256 WebP thumbnail, stores in `image-thumbnails` bucket at `{userId}/IMAGE#{albumId}#{imageId}/thumb-256.webp`
6. Lambda updates DynamoDB image record with `resizeStatus: COMPLETED`

---

### 4. Thumbnail Generation

**Decision**: Asynchronous in `ResizeImageFunction` triggered by S3 event, storing 256x256 WebP thumbnail in dedicated bucket

**Rationale**:
- **Optimized Scope**: Single WebP thumbnail (256x256) provides excellent quality and compression
- **Eventual Consistency**: Image appears in album list with thumbnail within ~3 seconds
- **Separation of Concerns**: Dedicated `image-thumbnails` bucket for organized storage
- **Constitution Alignment**: No external services - all AWS SDK v2

**Approach**:
- S3 event triggers ResizeImageFunction when image uploaded to albums-prod
- Lambda generates single 256x256 WebP thumbnail (quality 90)
- Store in image-thumbnails bucket at `{userId}/{albumId}/{imageId}/thumb-256.webp`
- Update DynamoDB image record with `resizeStatus: COMPLETED`
- Client fetches thumbnail via presigned URL

---

### 5. Testing Strategy

**Decision**: Lambda test events as primary validation; skip unit/integration tests for MVP

**Rationale**:
- **Constitution Principle IV**: Tests written as Lambda test events, deployed to AWS
- **MVP Simplicity**: No mocking framework overhead, no Testcontainers, no LocalStack setup
- **Production Parity**: Real AWS service behavior (permissions, throttling, eventual consistency)
- **Fast Feedback**: Compile → Package → Deploy → Test (5-10 min cycle)
- **Cost**: Negligible (~$0.0000002 per invocation; free tier covers thousands)

**Testing Levels** (simplified):

| Level | Tool | Coverage |
|-------|------|----------|
| **Compilation** | Maven | Syntax, type checking |
| **Contract Tests** | Lambda test events (JSON) | Business logic, error handling, edge cases |
| **Observability** | CloudWatch Logs | Debugging, tracing, performance |

**Test Event Organization**:
```
test-events/
├── create-album/
│ ├── valid-create.json
│ ├── duplicate-name.json
│ ├── empty-name.json
│ ├── missing-auth.json
│ └── edge-case-255-chars.json
├── list-albums/
│ ├── valid-list.json
│ ├── no-albums.json
│ └── pagination-token.json
└── ...
```
**Test Execution**:
1. Write test event (JSON)
2. Deploy Lambda: `aws lambda update-function-code ...`
3. Invoke: `aws lambda invoke --function-name MyFunction --payload [](http://_vscodecontentref_/0) output.json`
4. Check result in output.json and CloudWatch Logs
5. Fix code, redeploy, re-test

**No Testcontainers, No LocalStack**: Developers deploy to AWS sandbox for testing.

---

### 6. Authentication & Authorization

**Decision**: API Gateway with AWS Cognito (This is a future enhancement, currently assuming the system is a trusted environment and single-tenant for MVP)
  
**Rationale**:
- **Simplicity**: API Gateway + Cognito handles auth validation before reaching Lambda
- **Security**: No hardcoded secrets in Lambda; use environment variables for bucket names and table names
- **Extensibility**: Lambda Authorizer can support OAuth2/OIDC later

**Approach**:
- Each Lambda receives `userId` in event context (extracted from JWT claim `sub`)
- All DynamoDB queries filtered by `userId` (partition key)
- S3 keys include `{userId}/` prefix to enforce isolation

---

### 7. Observability & Logging

**Decision**: SLF4J + Logback with JSON format per Constitution Principle V

**Rationale**:
- **CloudWatch Integration**: JSON logs automatically parsed by CloudWatch Logs Insights
- **Tracing**: X-Ray trace ID propagated across Lambda invocations
- **Performance Debugging**: Log `durationMs` for each Lambda execution

**Log Format** (per constitution):
```json
{
  "timestamp": "2025-11-08T12:00:00Z",
  "level": "INFO",
  "service": "AlbumService",
  "function": "CreateAlbumFunction",
  "requestId": "aws-request-id",
  "traceId": "x-ray-trace-id",
  "context": {
    "userId": "user-123",
    "albumId": "album-xyz"
  },
  "message": "Album created successfully",
  "durationMs": 145,
  "services": ["DYNAMODB"]
}
```

---

### 8. Error Handling

**Decision**: Structured error responses with retry logic for transient failures

**Rationale**:
- **User Feedback**: Consistent error format (HTTP status + error message)
- **Resilience**: Exponential backoff for DynamoDB throttling or S3 transient errors
- **Debugging**: All errors logged with stack traces

**Error Response Format**:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Album name is required",
    "details": {
      "field": "albumName",
      "constraint": "NOT_EMPTY"
    }
  }
}
```

---

### 9. Performance Optimization

**Decision**: Presigned URL caching + thumbnail generation + DynamoDB read consistency tuning

**Rationale**:
- **Presigned URLs**: Cache 5-minute URLs to avoid repeated API calls
- **DynamoDB**: Use eventually consistent reads for list operations (cheaper, faster); strongly consistent reads for critical updates

**Targets**:
- Thumbnail load: <1s (S3)
- Full-resolution load: <3s (S3 presigned URL direct)
- Album list load: <500ms (DynamoDB eventually consistent read)

---

### 10. Lambda Layer Architecture & IAM Permission Pattern

**Decision**: Use Lambda Layers for shared-layer (AWS SDK v2, utility code); each Lambda has distinct IAM role with minimal required permissions.

**Rationale**:
- **Lambda Layer Scalability**: AWS supports up to 5 layers per Lambda function; each layer is versioned independently
- **Dependency Sharing**: Single source of truth for AWS SDK v2, SLF4J, utilities; no duplication across 8 Lambda deployment packages
- **Reduced Package Size**: Lambdas ship with only handler code (~50-100 KB) vs. bundled dependencies (~50-100 MB per function)
- **Faster Cold Start**: Smaller packages = faster downloads and initialization
- **IAM Security**: Each Lambda has minimal required permissions; no over-privilege

**Layer Architecture**:

```
Lambda Layer: album-manager-layer
├── java/lib/
│   ├── software.amazon.awssdk:*.jar (AWS SDK v2 modules)
│   ├── org.slf4j:slf4j-api-*.jar
│   ├── ch.qos.logback:logback-*.jar
│   └── shared-layer.jar (Album, Image models; DynamoDBService; S3Service; utilities)
```

**Lambda Layer Deployment**:
1. Build shared-layer: `mvn clean package` → copies dependencies to `target/lib/`
2. Package layer: Create `layer.zip` with `java/lib/*.jar` structure
3. Publish: `aws lambda publish-layer-version --layer-name album-manager-layer --zip-file fileb://layer.zip --compatible-runtimes java21`
4. Attach to Lambdas: Each Lambda's `update-function-configuration` specifies layer ARN

**Answer to User Question**: 
- **How many Lambdas can share one layer?** Unlimited. A single Lambda Layer can be attached to any number of Lambda functions (only limited by the 5-layer-per-function constraint and account quotas).
- **Do Lambdas need to package AWS SDK if using a layer?** No. During development, Lambda pom.xmls include shared-layer as a Maven dependency for compilation and testing. At deployment in AWS, Lambda runtime loads shared-layer classes from the attached Lambda Layer, not from the function's deployment package. This avoids duplication and reduces package sizes.

### IAM Permission Pattern

**Decision**: Each Lambda has a dedicated IAM execution role with minimal required permissions (least privilege principle).

**Pattern**:

```
CreateEntryFunction
├── Role: CreateEntryLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   ├── dynamodb:PutItem (create album)
│   │   ├── dynamodb:Query (check unique album name)
│   │   └── dynamodb:GetItem (verify album exists)
│   ├── Policy: CloudWatch Logs
│   │   ├── logs:CreateLogGroup
│   │   ├── logs:CreateLogStream
│   │   └── logs:PutLogEvents
│   └── Policy: CloudWatch Metrics
│       └── cloudwatch:PutMetricData

ListAlbumsFunction
├── Role: ListAlbumsLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   └── dynamodb:Query (query by userId, filter by "ALBUM#" prefix)
│   ├── Policy: CloudWatch Logs (same as above)
│   └── Policy: CloudWatch Metrics

GetUploadURLFunction
├── Role: GetUploadURLLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   ├── dynamodb:GetItem (verify album exists)
│   │   └── dynamodb:PutItem (create image record with PENDING status)
│   ├── Policy: S3 (albums-prod bucket)
│   │   └── s3:GetObject (for presigned URL generation internally)
│   ├── Policy: CloudWatch Logs
│   └── Policy: CloudWatch Metrics

UpdateEntryFunction (S3 event trigger)
├── Role: UpdateImageDataLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   └── dynamodb:UpdateItem (update image metadata after validation)
│   ├── Policy: S3 (albums-prod + image-thumbnails)
│   │   ├── s3:GetObject (read original image)
│   │   └── s3:PutObject (write to image-thumbnails for thumbnail)
│   ├── Policy: CloudWatch Logs
│   └── Policy: CloudWatch Metrics

ResizeImageFunction (S3 event trigger)
├── Role: ResizeImageLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   └── dynamodb:UpdateItem (update resizeStatus to COMPLETED)
│   ├── Policy: S3 (albums-prod + image-thumbnails)
│   │   ├── s3:GetObject (read original image)
│   │   └── s3:PutObject (write 256x256 thumbnail)
│   ├── Policy: CloudWatch Logs
│   └── Policy: CloudWatch Metrics

GetDownloadURLFunction
├── Role: GetDownloadURLLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   └── dynamodb:GetItem (verify image exists, check ownership)
│   ├── Policy: S3 (albums-prod + image-thumbnails)
│   │   └── s3:GetObject (for presigned URL generation)
│   ├── Policy: CloudWatch Logs
│   └── Policy: CloudWatch Metrics

DeleteImageFunction
├── Role: DeleteImageLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   ├── dynamodb:DeleteItem (delete image record)
│   │   └── dynamodb:UpdateItem (decrement album imageCount)
│   ├── Policy: S3 (albums-prod + image-thumbnails)
│   │   └── s3:DeleteObject (delete both original and thumbnail)
│   ├── Policy: CloudWatch Logs
│   └── Policy: CloudWatch Metrics

DeleteAlbumFunction
├── Role: DeleteAlbumLambdaExecutionRole
│   ├── Policy: DynamoDB (albums table)
│   │   ├── dynamodb:Query (query all images in album)
│   │   ├── dynamodb:DeleteItem (delete album and image records)
│   │   └── dynamodb:Scan (optional, for batch operations)
│   ├── Policy: S3 (albums-prod + image-thumbnails)
│   │   └── s3:DeleteObject (delete all image objects)
│   ├── Policy: CloudWatch Logs
│   └── Policy: CloudWatch Metrics
```

**Principle**: 
- Each Lambda only gets permissions it needs
- No cross-service over-privilege
- DynamoDB operations restricted to specific table and actions
- S3 operations restricted to specific buckets and actions
- Logs and metrics permissions standard across all Lambdas

**Deployment**: IAM roles and policies defined in Terraform or SAM template; attached to each Lambda function during deployment.

---

## Technology Stack Confirmed

| Component | Technology | Version | Notes |
|-----------|-----------|---------|-------|
| **Runtime** | AWS Lambda | Java 21 managed | Per constitution |
| **Language** | Java | 21+ | Per constitution |
| **Build** | Maven | 3.9+ | Per constitution, one pom.xml per Lambda |
| **AWS SDK** | aws-sdk-java-v2 | 2.25+ | Per constitution |
| **Logging** | SLF4J + Logback | Latest | Structured according to the constitution |
| **Testing** | JUnit 5 | 5.10+ | Unit tests (Currently not in use) |
| **Integration Testing** | Test Events | Latest | Written before implementation and run after deployment |
| **API** | API Gateway | REST | HTTP interface to Lambdas |
| **Storage** | DynamoDB | On-demand | Metadata tables |
| **Files** | S3 | Standard | Image + thumbnail storage |
| **Frontend** | Static HTML/JS/CSS | Vanilla JS | Served from S3 directly |
| **IaC** | Terraform | Latest | Infrastructure as code (Currently Out-of-Scope) |
| **CI/CD** | GitHub Actions | Built-in | Build, test, deploy workflow |

---

## Design Decisions Summary

✅ **8 Independent Lambda Functions** (one Maven project each, per constitution)  
✅ **S3 + DynamoDB** for storage (images + metadata)  
✅ **Presigned URL Pattern** for scalable uploads/downloads  
✅ **Event-Driven Thumbnail Generation** (async after S3 upload)  
✅ **Test-First Development** (unit + integration + AWS deployment tests)  
✅ **JWT + API Gateway Auth** for security  
✅ **SLF4J JSON Logging** to CloudWatch (per constitution)  
✅ **Structured Error Handling** with retries for resilience    
✅ **Lambda Layer Architecture** for shared code (AWS SDK v2, utilities)  
✅ **Least Privilege IAM Roles** for each Lambda function  
✅ **Terraform or SAM IaC** for infrastructure management  

---

## Next Steps (Phase 1)

1. **Data Model** (`data-model.md`): Define Album and Image entities with all attributes, relationships, validation rules
2. **API Contracts** (`contracts/`): OpenAPI/REST specs for all 6 Lambda functions
3. **Agent Context**: Update copilot context with confirmed tech stack and Lambda architecture
