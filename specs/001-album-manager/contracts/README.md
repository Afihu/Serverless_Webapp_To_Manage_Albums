# API Contracts - Photo Album Manager

**Version**: 2.0.0 (Operation-Based Architecture)  
**Last Updated**: 2025-11-24

## Overview

As of v2.0.0, the Photo Album Manager API has been refactored to use **operation-based Lambda functions** rather than entity-specific ones. Each operation (Create, Read, Update, Delete, List) is handled by a single Lambda function that accepts a `type` discriminator parameter to determine whether it's operating on an `album` or `image` entity.

This design:
- Maximizes code reuse across similar entity types
- Maintains operation-level scalability
- Reduces boilerplate and duplicate error handling
- Is compatible with GraalVM native image compilation

## Contract Files & Mappings

| Contract | Purpose | Endpoints | Lambda Function | Type Discriminator |
|----------|---------|-----------|-----------------|-------------------|
| `CreateEntry.yaml` | Create album or image | `POST /albums` (album) | `create-entry` | `type: album \| image` |
| | | `POST /albums/{albumId}/photos` (image) | | |
| `ReadEntry.yaml` | Get entry details & presigned URLs | `GET /albums/{albumId}` (album) | `read-entry` | `type: album \| image` |
| | | `GET /albums/{albumId}/photos` (thumbnails) | | |
| | | `POST /photos/{photoId}/presigned-download` (original/thumbnail) | | |
| `UpdateEntry.yaml` | Update album or image metadata | `PUT /albums/{albumId}` (album) | `update-entry` | `type: album \| image` |
| | | `PUT /photos/{photoId}/metadata` (image) | | |
| `DeleteEntry.yaml` | Delete album or image | `DELETE /albums/{albumId}` (album) | `delete-entry` | `type: album \| image` |
| | | `DELETE /albums/{albumId}/photos/{photoId}` (image) | | |
| `ListEntries.yaml` | List albums or images with pagination | `GET /albums` (albums) | `list-entries` | `type: album \| image` |
| | | `GET /albums/{albumId}/photos` (images in album) | | |
| `GetUploadURL.yaml` | Generate presigned S3 upload URL | `POST /albums/{albumId}/photos/presigned-upload` | `get-upload-url` | N/A (image only) |
| `ProcessImage.yaml` | S3 event: resize & generate thumbnails | S3 bucket event (internal) | `process-image` | N/A (event-driven) |

## Type Discriminator Pattern

All CreateEntry, ReadEntry, UpdateEntry, DeleteEntry, and ListEntries functions use a `type` parameter in the request to determine entity behavior:

```json
// Example: CreateEntry request
{
  "type": "album",
  "albumName": "Family Trip 2025",
  "description": "Summer vacation"
}

// OR

{
  "type": "image",
  "albumId": "550e8400-e29b-41d4-a716-446655440000",
  "fileUrl": "s3://bucket/path/image.jpg"
}
```

Valid type values:
- `album` - Album entity
- `image` - Photo/Image entity

## Key Architectural Changes from v1.0.0

- **From**: 9 entity-specific Lambda functions (CreateAlbum, DeleteAlbum, CreateImage, etc.)
- **To**: 7 operation-based Lambda functions (CreateEntry, ReadEntry, UpdateEntry, DeleteEntry, ListEntries, GetUploadURL, ProcessImage)
- **Routing**: HTTP requests route to a single Lambda per operation; the `type` discriminator determines entity handling
- **GetDownloadURL Consolidation**: Merged into `ReadEntry` for album info + image retrieval
- **Event-Driven Processing**: `process-image` handles S3 events asynchronously for thumbnail generation

## Security

All endpoints (except `ProcessImage`, which is S3-event-driven) require JWT authentication via the `Authorization: Bearer <token>` header.

```yaml
security:
  - BearerAuth: []

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

## Integration with API Gateway

### Option 1: Individual Contract Import
Import each contract file separately into API Gateway and route to the corresponding Lambda function.

### Option 2: Aggregated Specification
Combine all contracts into a single OpenAPI spec for bulk import:
```yaml
# master-openapi.yaml
openapi: 3.0.0
info:
  title: Photo Album Manager API
  version: 2.0.0
paths:
  /albums:
    post:
      # ... (from CreateEntry.yaml)
    get:
      # ... (from ListEntries.yaml)
  /albums/{albumId}:
    get:
      # ... (from ReadEntry.yaml - album details)
    put:
      # ... (from UpdateEntry.yaml - album update)
    delete:
      # ... (from DeleteEntry.yaml - album delete)
  # ... etc
```

## Contract Versioning

Contracts follow semantic versioning tied to the API:
- **Patch (x.x.Z)**: Bug fixes, documentation updates
- **Minor (x.Y.0)**: New fields, new optional parameters, backward compatible
- **Major (X.0.0)**: Breaking changes (e.g., type discriminator enum changes, endpoint removal)

## Future Enhancements

- Extract common schemas (`Album`, `Image`, `ErrorResponse`) into shared `components.yaml`
- Add request/response examples for each type discriminator value
- Implement contract versioning in URL paths (e.g., `/v2/albums`)
- Add rate limiting and pagination metadata to responses
- Document error handling for type discriminator mismatches
