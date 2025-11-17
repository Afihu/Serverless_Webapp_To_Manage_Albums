# API Contracts - Photo Album Manager

**Version**: 1.0.0  
**Last Updated**: 2025-11-17

## Overview

As of v1.0.0, the Photo Album Manager API uses a **modular, decoupled contract structure** where each endpoint has its own dedicated OpenAPI specification file for better maintainability, clarity, and future extensibility.

## Contract Files

| Contract | Purpose | Endpoint |
|----------|---------|----------|
| `CreateAlbum.yaml` | Create a new photo album | `POST /albums` |
| `ListAlbums.yaml` | List all albums for user | `GET /albums` |
| `GetUploadURL.yaml` | Get presigned S3 upload URL | `POST /albums/{albumId}/presigned-upload` |
| `UpdateImageMetadata.yaml` | Update photo metadata (S3 event trigger) | `PUT /photos/{photoId}/metadata` (internal) |
| `GetDownloadURL.yaml` | Get presigned S3 download URL (original or thumbnail) | `POST /photos/{photoId}/presigned-download` |
| `DeleteImage.yaml` | Delete a photo | `DELETE /photos/{photoId}` |
| `DeleteAlbum.yaml` | Delete an entire album and all its photos | `DELETE /albums/{albumId}` |
| `UpdateAlbum.yaml` | Update album metadata (name, description) | `PUT /albums/{albumId}` |

## Key Features

- **Modular Design**: Each endpoint is independently documented and versioned
- **Single Responsibility**: Each contract file focuses on one operation
- **Easier Maintenance**: Changes to one endpoint don't require touching others
- **Clear Dependencies**: Cross-references to related contracts are explicit
- **Presigned URL Architecture**: `GetDownloadURL` returns presigned URLs; clients access S3 directly without additional Lambda invocations
- **Schema Reusability**: Common schemas (Album, Photo, ErrorResponse) are duplicated for independence; consider extracting to shared `components.yaml` if needed in future versions

## How to Use

1. **For Implementation**: Refer to the specific contract file for each Lambda handler
2. **For API Gateway**: Import each contract or aggregate them into a single OpenAPI spec
3. **For Integration Testing**: Use the contract file to generate test cases

## Integration with API Gateway

To deploy this API on AWS API Gateway:
- Import each contract file individually, or
- Aggregate all contracts into a single OpenAPI spec (see `openapi.yaml` template for reference structure)

## Future Enhancements

- Extract common schemas (`Album`, `Photo`, `ErrorResponse`) into a shared `components.yaml`
- Add security scheme definitions to a separate file
- Implement contract versioning (e.g., `v1.0.0/`, `v2.0.0/`)
- Add request/response examples for each contract
