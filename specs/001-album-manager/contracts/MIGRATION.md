# API Contracts Migration: v1.0.0 → v2.0.0

**Completed**: 2025-11-24  
**Branch**: `001-album-manager`

## Summary

Successfully migrated from 9 entity-based contract files to 7 operation-based contract files that align with the new Lambda architecture.

## Changes

### Old Structure (v1.0.0) - Deleted
- `CreateAlbum.yaml` (CREATE album endpoint)
- `ListAlbums.yaml` (LIST albums endpoint)
- `GetUploadURL.yaml` (GET presigned upload URL)
- `GetThumbnails.yaml` (GET thumbnail gallery)
- `GetOriginalImage.yaml` (GET original image URL)
- `UpdateImageMetadata.yaml` (UPDATE image metadata)
- `UpdateAlbum.yaml` (UPDATE album metadata)
- `DeleteAlbum.yaml` (DELETE album)
- `DeleteImage.yaml` (DELETE image)

### New Structure (v2.0.0) - Created
| File | Lambda Function | Endpoints | Type Discriminator |
|------|-----------------|-----------|-------------------|
| `CreateEntry.yaml` | `create-entry` | POST /albums, POST /albums/{albumId}/photos | album \| image |
| `ReadEntry.yaml` | `read-entry` | GET /albums/{albumId}, GET /albums/{albumId}/photos, POST /photos/{photoId}/presigned-download | album \| image |
| `UpdateEntry.yaml` | `update-entry` | PUT /albums/{albumId}, PUT /photos/{photoId}/metadata | album \| image |
| `DeleteEntry.yaml` | `delete-entry` | DELETE /albums/{albumId}, DELETE /albums/{albumId}/photos/{photoId} | album \| image |
| `ListEntries.yaml` | `list-entries` | GET /albums, GET /albums/{albumId}/photos | album \| image |
| `GetUploadURL.yaml` | `get-upload-url` | POST /albums/{albumId}/photos/presigned-upload | N/A |
| `ProcessImage.yaml` | `process-image` | S3 event (internal) | N/A |

## Key Improvements

1. **Type Discriminator Pattern**: All CRUD operations use `type` parameter to determine entity (album vs image)
2. **Reduced Duplication**: 7 files instead of 9, with consolidated related operations
3. **Clearer Semantics**: Operation names (Create, Read, Update, Delete, List) match Lambda function purposes
4. **S3 Event Documentation**: `ProcessImage.yaml` formally documents internal event-driven processing
5. **Better Error Codes**: Added `INVALID_TYPE_DISCRIMINATOR` for type validation errors

## Breaking Changes for Clients

### Before (v1.0.0)
```bash
# Create album
POST /albums
{
  "albumName": "Trip 2025",
  "description": "..."
}

# Create image
POST /albums/{albumId}/photos
{
  "fileUrl": "s3://...",
  "fileName": "photo.jpg"
}
```

### After (v2.0.0)
```bash
# Create album - now includes type discriminator
POST /albums
{
  "type": "album",
  "albumName": "Trip 2025",
  "description": "..."
}

# Create image - now includes type discriminator
POST /albums/{albumId}/photos
{
  "type": "image",
  "fileUrl": "s3://...",
  "fileName": "photo.jpg"
}
```

## Testing

- ✅ README.md updated with operation-based mappings
- ✅ All 7 new contract files created with complete OpenAPI 3.0.0 specs
- ✅ Type discriminator pattern documented in each contract
- ✅ Error responses include `INVALID_TYPE_DISCRIMINATOR` code
- ✅ Example requests provided for both type values
- ✅ All old contract files removed

## Migration Checklist

- [x] Update README.md
- [x] Delete 9 old entity-based contracts
- [x] Create 7 new operation-based contracts
- [ ] Update client code to include `type` discriminator
- [ ] Update API Gateway routes to new Lambda functions
- [ ] Update integration tests
- [ ] Update API documentation (e.g., Postman collection)
- [ ] Deploy v2.0.0 to staging
- [ ] Run integration tests against staging
- [ ] Notify API consumers of breaking changes
