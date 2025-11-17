<!-- 
SYNC IMPACT REPORT - Constitution v1.0.0
========================================
Version Change: Template v0.0.0 → v1.0.0 (INITIAL)
Modified Principles: All (5 core principles defined)
Added Sections: Technical Stack, Deployment Model
Removed Sections: None (initial constitution)
Templates Updated:
  ✅ plan-template.md (Java/AWS context added to guidance)
  ✅ spec-template.md (applicable for serverless features)
  ✅ tasks-template.md (Lambda-per-Maven-project model integrated)
Follow-up TODOs: None
-->

# Serverless Webapp to Manage Albums Constitution

## Core Principles

### I. AWS-First Architecture
Every feature is built on AWS services as the primary platform. Specifically:
- **AWS Lambda**: All business logic deployable as serverless functions, especially the fact of async nature of the architecture.
- **Amazon S3**: Media storage and retrieval for album assets
- **Amazon DynamoDB**: Persistent data store for album metadata and user data
- Core design must assume stateless, event-driven Lambda execution with eventual consistency

### II. Java with AWS SDK v2
All code is written in Java using the official AWS SDK for Java v2 (software.amazon.awssdk.*). Java is mandatory to ensure type safety, build consistency, and strong AWS integration. No polyglot runtimes permitted without documented architectural justification and approval.

### III. One Maven Project Per Lambda
Each Lambda deployment unit (function) has its own Maven project (pom.xml) at repository root as a separate module. This ensures:
- Isolated dependency management per Lambda function
- Deployable .jar packaging without extraneous dependencies
- Independent versioning and release cycles
- Clear separation of concerns and deployment boundaries

### IV. Test And Re-deployment Cycle
- All tests are written as Lambda test events and must be present before implementation as there are currently no concrete method for testing locally.
- The expected cycle is to: write test events -> implement code-> package via Maven -> deploy to AWS Lambda -> run the tests -> fix issues if any -> redeploy until all tests pass.

### V. Observability & Monitoring
All Lambda functions MUST emit structured logs using SLF4J with JSON format (CloudWatch compatible). Logging must include:
- Request trace IDs for end-to-end tracing
- Lambda context (function name, request ID, memory allocation)
- Log levels: 
    - `INFO` for general operational messages, like successful operations
    - `WARN` for early warnings or recoverable issues, like operations taking longer than expected
    - `ERROR` for critical errors impacting function execution, like database access failures
    - `FATAL` for system-wide failures, like infrastructure outages or system crashes
- Flag `service` indicating services being accessed
    - `DYNAMODB` for DynamoDB operations
    - `S3` for S3 operations.
- All errors with stack traces captured before function exit
- Performance metrics (execution time, cold starts if applicable)
- Expected log format:
```json
{
  "one-liner": "[INFO] Album created successfully",
  "timestamp": "2025-11-03T12:00:00Z",
  "level": "INFO",
  "service": "AlbumService",
  "function": "CreateAlbumFunction",
  "requestId": "abcd-1234-efgh-5678",
  "traceId": "trace-xyz-9876",
  "context": {
    "albumId": "album-xyz",
    "userId": "user-123"
  },
  "message": "Album created successfully",
  "durationMs": 150,
  "service": ["DYNAMODB", "S3"]
}
```

## Technical Stack & Deployment Model

**Runtime**: AWS Lambda (Java 21 managed runtime or later)  
**Primary Language**: Java 21+  
**Build System**: Maven 3.9+  
**AWS SDK**: software.amazon.awssdk:* (Java SDK v2)  
**Logging**: SLF4J with Logback, JSON output    
**Deployment Target**: AWS Lambda + S3 + DynamoDB  

**Repository Structure**:
```
repo-root/
├── [lambda-name-1]/          # Maven module for Lambda function 1
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
├── [lambda-name-2]/          # Maven module for Lambda function 2
│   ├── pom.xml
│   ├── src/main/java/
│   ├── src/test/java/
│   └── ...
├── docs/                     # Shared documentation
└── .github/workflows/        # CI/CD pipelines
```

## Development Workflow & Governance

**Build & Package**: Each Lambda module produces a standalone .jar (fat JAR) via Maven assembly plugin or Gradle shadowJar equivalent. The .jar is deployable directly to Lambda without external dependency management.

**Code Review Checklist**:
- Compiled successfully (MUST verify locally)
- AWS SDK calls are properly error-handled (retries, exponential backoff)
- Logs are structured JSON with trace IDs
- DynamoDB queries respect partition key design (no full table scans)
- S3 operations include proper error handling (access denied, not found, etc.)
- No hardcoded credentials or secrets (use AWS Secrets Manager or Lambda environment)
- Maven build succeeds with `mvn clean package`

**Complexity Justification**: If a Lambda function exceeds 1000 lines of core logic (excluding tests), document the business case and consider refactoring into multiple functions or extracting a shared utility library.

**Breaking Changes**: AWS SDK version bumps, DynamoDB schema changes, or S3 key structure changes require a MINOR or MAJOR version bump and migration documentation.

## Governance

This constitution supersedes all other project practices and guidelines. All PRs must verify compliance with these principles before merge:

1. **Architecture**: Feature uses AWS Lambda/S3/DynamoDB as specified
2. **Language**: Code is Java 21 with AWS SDK v2; no polyglot without exception
3. **Build Model**: Each Lambda has its own Maven pom.xml, builds to deployable .jar
4. **Testing**: Test events written first, deployed to Lambda, tested on the platform.
5. **Observability**: Structured logging via SLF4J in place, trace IDs present

Amendments to this constitution require:
- Documentation of rationale (in GitHub issue or PR)
- Approval from project maintainers
- Migration plan for existing code (if backward-incompatible)
- Update to this file with new version and amendment date

For runtime development guidance, see `.specify/templates/plan-template.md` and associated spec/tasks templates.

**Version**: 1.0.0 | **Ratified**: 2025-11-03 | **Last Amended**: 2025-11-03
