package com.albummanager.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the custom exception hierarchy.
 */
class ExceptionHierarchyTest {

    @Nested
    @DisplayName("AlbumManagerException Tests")
    class AlbumManagerExceptionTests {

        @Test
        @DisplayName("Create exception with error code and HTTP status")
        void testCreateExceptionWithErrorCodeAndHttpStatus() {
            AlbumManagerException ex = new AlbumManagerException(
                    "Test error message",
                    "TEST_ERROR",
                    400
            );

            assertEquals("Test error message", ex.getMessage());
            assertEquals("TEST_ERROR", ex.getErrorCode());
            assertEquals(400, ex.getHttpStatus());
            assertTrue(ex.getContext().isEmpty());
        }

        @Test
        @DisplayName("Exception with context data is properly captured")
        void testExceptionWithContextData() {
            Map<String, Object> context = new HashMap<>();
            context.put("userId", "auth0|user-123");
            context.put("albumId", "550e8400-e29b-41d4-a716-446655440000");

            AlbumManagerException ex = new AlbumManagerException(
                    "Resource not found",
                    "RESOURCE_NOT_FOUND",
                    404,
                    context
            );

            assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
            assertEquals(404, ex.getHttpStatus());
            assertEquals("auth0|user-123", ex.getContext().get("userId"));
            assertEquals("550e8400-e29b-41d4-a716-446655440000", ex.getContext().get("albumId"));
        }

        @Test
        @DisplayName("Context map is immutable after retrieval")
        void testContextMapIsImmutable() {
            AlbumManagerException ex = new AlbumManagerException(
                    "Test error",
                    "TEST_ERROR",
                    500
            );

            Map<String, Object> context = ex.getContext();
            assertThrows(UnsupportedOperationException.class, () -> context.put("key", "value"));
        }

        @Test
        @DisplayName("Add context using fluent method")
        void testAddContextFluent() {
            AlbumManagerException ex = new AlbumManagerException(
                    "Test error",
                    "TEST_ERROR",
                    500
            );

            ex.addContext("key1", "value1")
              .addContext("key2", 123);

            assertEquals("value1", ex.getContext().get("key1"));
            assertEquals(123, ex.getContext().get("key2"));
        }

        @Test
        @DisplayName("Add context with null key or value is ignored")
        void testAddContextWithNullIsIgnored() {
            AlbumManagerException ex = new AlbumManagerException(
                    "Test error",
                    "TEST_ERROR",
                    500
            );

            ex.addContext(null, "value")
              .addContext("key", null);

            assertTrue(ex.getContext().isEmpty());
        }

        @Test
        @DisplayName("Exception with cause captures underlying error")
        void testExceptionWithCause() {
            RuntimeException cause = new RuntimeException("Underlying error");
            AlbumManagerException ex = new AlbumManagerException(
                    "Wrapper error",
                    "WRAPPER_ERROR",
                    500,
                    cause
            );

            assertEquals(cause, ex.getCause());
            assertEquals("Underlying error", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("toString includes all context for debugging")
        void testToStringIncludesContext() {
            Map<String, Object> context = new HashMap<>();
            context.put("userId", "user-123");
            context.put("albumId", "album-456");

            AlbumManagerException ex = new AlbumManagerException(
                    "Test error",
                    "TEST_ERROR",
                    400,
                    context
            );

            String str = ex.toString();
            assertTrue(str.contains("TEST_ERROR"));
            assertTrue(str.contains("400"));
            assertTrue(str.contains("Test error"));
            assertTrue(str.contains("userId"));
            assertTrue(str.contains("user-123"));
            assertTrue(str.contains("albumId"));
            assertTrue(str.contains("album-456"));
        }

        @Test
        @DisplayName("toString includes cause information")
        void testToStringIncludesCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            AlbumManagerException ex = new AlbumManagerException(
                    "Wrapper error",
                    "WRAPPER_ERROR",
                    500,
                    cause
            );

            String str = ex.toString();
            assertTrue(str.contains("RuntimeException"));
            assertTrue(str.contains("Root cause"));
        }

        @Test
        @DisplayName("Error code cannot be null")
        void testErrorCodeCannotBeNull() {
            assertThrows(NullPointerException.class, () -> 
                new AlbumManagerException("Message", null, 400)
            );
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy Tests")
    class HierarchyTests {

        @Test
        @DisplayName("All exceptions extend AlbumManagerException")
        void testAllExceptionsExtendBase() {
            assertTrue(AlbumManagerException.class.isAssignableFrom(ValidationException.class));
            assertTrue(AlbumManagerException.class.isAssignableFrom(ResourceNotFoundException.class));
            assertTrue(AlbumManagerException.class.isAssignableFrom(DuplicateAlbumException.class));
            assertTrue(AlbumManagerException.class.isAssignableFrom(S3OperationException.class));
            assertTrue(AlbumManagerException.class.isAssignableFrom(DynamoDBOperationException.class));
            assertTrue(AlbumManagerException.class.isAssignableFrom(ImageProcessingException.class));
        }

        @Test
        @DisplayName("All exceptions extend Exception (checked)")
        void testAllExceptionsAreChecked() {
            assertTrue(Exception.class.isAssignableFrom(ValidationException.class));
            assertTrue(Exception.class.isAssignableFrom(ResourceNotFoundException.class));
            assertTrue(Exception.class.isAssignableFrom(DuplicateAlbumException.class));
            assertTrue(Exception.class.isAssignableFrom(S3OperationException.class));
            assertTrue(Exception.class.isAssignableFrom(DynamoDBOperationException.class));
            assertTrue(Exception.class.isAssignableFrom(ImageProcessingException.class));
        }

        @Test
        @DisplayName("Exceptions can be caught as AlbumManagerException")
        void testExceptionsCanBeCaughtAsBase() {
            try {
                throw new ValidationException("Test");
            } catch (AlbumManagerException ex) {
                assertEquals(400, ex.getHttpStatus());
            }

            try {
                throw new ResourceNotFoundException("Test");
            } catch (AlbumManagerException ex) {
                assertEquals(404, ex.getHttpStatus());
            }

            try {
                throw new DuplicateAlbumException("Test");
            } catch (AlbumManagerException ex) {
                assertEquals(409, ex.getHttpStatus());
            }
        }
    }

    @Nested
    @DisplayName("ValidationException Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Default HTTP status is 400")
        void testDefaultHttpStatus() {
            ValidationException ex = new ValidationException("Invalid input");
            assertEquals(400, ex.getHttpStatus());
            assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        }

        @Test
        @DisplayName("Factory method for missing field")
        void testMissingFieldFactory() {
            ValidationException ex = ValidationException.missingField("albumName");
            
            assertEquals(400, ex.getHttpStatus());
            assertEquals("MISSING_REQUIRED_FIELD", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("albumName"));
            assertEquals("albumName", ex.getContext().get("field"));
        }

        @Test
        @DisplayName("Factory method for invalid format")
        void testInvalidFormatFactory() {
            ValidationException ex = ValidationException.invalidFormat("albumId", "UUID v4");
            
            assertEquals(400, ex.getHttpStatus());
            assertEquals("INVALID_FORMAT", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("albumId"));
            assertTrue(ex.getMessage().contains("UUID v4"));
            assertEquals("albumId", ex.getContext().get("field"));
            assertEquals("UUID v4", ex.getContext().get("expectedFormat"));
        }

        @Test
        @DisplayName("Factory method for invalid length")
        void testInvalidLengthFactory() {
            ValidationException ex = ValidationException.invalidLength("albumName", 1, 255, 300);
            
            assertEquals(400, ex.getHttpStatus());
            assertEquals("INVALID_LENGTH", ex.getErrorCode());
            assertEquals("albumName", ex.getContext().get("field"));
            assertEquals(1, ex.getContext().get("minLength"));
            assertEquals(255, ex.getContext().get("maxLength"));
            assertEquals(300, ex.getContext().get("actualLength"));
        }

        @Test
        @DisplayName("Factory method for invalid enum value")
        void testInvalidEnumValueFactory() {
            ValidationException ex = ValidationException.invalidEnumValue(
                    "fileFormat", "BMP", "JPEG, PNG, WEBP, GIF, HEIC"
            );
            
            assertEquals(400, ex.getHttpStatus());
            assertEquals("INVALID_ENUM_VALUE", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("BMP"));
            assertTrue(ex.getMessage().contains("JPEG"));
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException Tests")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Default HTTP status is 404")
        void testDefaultHttpStatus() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
            assertEquals(404, ex.getHttpStatus());
            assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
        }

        @Test
        @DisplayName("Factory method for album not found")
        void testAlbumNotFoundFactory() {
            ResourceNotFoundException ex = ResourceNotFoundException.albumNotFound(
                    "auth0|user-123", "album-uuid"
            );
            
            assertEquals(404, ex.getHttpStatus());
            assertEquals("ALBUM_NOT_FOUND", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("album-uuid"));
            assertEquals("auth0|user-123", ex.getContext().get("userId"));
            assertEquals("album-uuid", ex.getContext().get("albumId"));
        }

        @Test
        @DisplayName("Factory method for image not found")
        void testImageNotFoundFactory() {
            ResourceNotFoundException ex = ResourceNotFoundException.imageNotFound(
                    "auth0|user-123", "album-uuid", "image-uuid"
            );
            
            assertEquals(404, ex.getHttpStatus());
            assertEquals("IMAGE_NOT_FOUND", ex.getErrorCode());
            assertEquals("auth0|user-123", ex.getContext().get("userId"));
            assertEquals("album-uuid", ex.getContext().get("albumId"));
            assertEquals("image-uuid", ex.getContext().get("imageId"));
        }

        @Test
        @DisplayName("Factory method for S3 object not found")
        void testS3ObjectNotFoundFactory() {
            ResourceNotFoundException ex = ResourceNotFoundException.s3ObjectNotFound(
                    "albums-prod", "user-123/IMAGE#album#image#OG"
            );
            
            assertEquals(404, ex.getHttpStatus());
            assertEquals("S3_OBJECT_NOT_FOUND", ex.getErrorCode());
            assertEquals("albums-prod", ex.getContext().get("bucket"));
        }
    }

    @Nested
    @DisplayName("DuplicateAlbumException Tests")
    class DuplicateAlbumExceptionTests {

        @Test
        @DisplayName("Default HTTP status is 409")
        void testDefaultHttpStatus() {
            DuplicateAlbumException ex = new DuplicateAlbumException("Album exists");
            assertEquals(409, ex.getHttpStatus());
            assertEquals("DUPLICATE_ALBUM", ex.getErrorCode());
        }

        @Test
        @DisplayName("Factory method for duplicate album name")
        void testForAlbumNameFactory() {
            DuplicateAlbumException ex = DuplicateAlbumException.forAlbumName(
                    "auth0|user-123", "Vacation Photos"
            );
            
            assertEquals(409, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("Vacation Photos"));
            assertEquals("auth0|user-123", ex.getContext().get("userId"));
            assertEquals("Vacation Photos", ex.getContext().get("albumName"));
        }

        @Test
        @DisplayName("Factory method for duplicate on rename")
        void testForRenameFactory() {
            DuplicateAlbumException ex = DuplicateAlbumException.forRename(
                    "auth0|user-123", "album-uuid", "New Album Name"
            );
            
            assertEquals(409, ex.getHttpStatus());
            assertEquals("DUPLICATE_ALBUM_ON_RENAME", ex.getErrorCode());
            assertEquals("album-uuid", ex.getContext().get("albumId"));
            assertEquals("New Album Name", ex.getContext().get("newAlbumName"));
        }
    }

    @Nested
    @DisplayName("S3OperationException Tests")
    class S3OperationExceptionTests {

        @Test
        @DisplayName("Default HTTP status is 500")
        void testDefaultHttpStatus() {
            S3OperationException ex = new S3OperationException("S3 error");
            assertEquals(500, ex.getHttpStatus());
            assertEquals("S3_OPERATION_FAILED", ex.getErrorCode());
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("Retryable exception has status 503")
        void testRetryableHasStatus503() {
            S3OperationException ex = new S3OperationException(
                    "Service unavailable", true, new RuntimeException("cause")
            );
            assertEquals(503, ex.getHttpStatus());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("Factory method for presigned URL failure")
        void testPresignedUrlFailedFactory() {
            RuntimeException cause = new RuntimeException("SDK error");
            S3OperationException ex = S3OperationException.presignedUrlFailed(
                    "albums-prod", "user-123/image.jpg", cause
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("albums-prod", ex.getContext().get("bucket"));
            assertEquals("user-123/image.jpg", ex.getContext().get("s3Key"));
            assertEquals("generatePresignedUrl", ex.getContext().get("operation"));
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("Factory method for upload failure (retryable)")
        void testUploadFailedFactory() {
            S3OperationException ex = S3OperationException.uploadFailed(
                    "albums-prod", "user-123/image.jpg", new RuntimeException()
            );
            
            assertEquals(503, ex.getHttpStatus());
            assertTrue(ex.isRetryable());
            assertEquals("upload", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for download failure (retryable)")
        void testDownloadFailedFactory() {
            S3OperationException ex = S3OperationException.downloadFailed(
                    "albums-prod", "user-123/image.jpg", new RuntimeException()
            );
            
            assertEquals(503, ex.getHttpStatus());
            assertTrue(ex.isRetryable());
            assertEquals("download", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for delete failure")
        void testDeleteFailedFactory() {
            S3OperationException ex = S3OperationException.deleteFailed(
                    "albums-prod", "user-123/image.jpg", new RuntimeException()
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertFalse(ex.isRetryable());
            assertEquals("delete", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("toString includes retryable flag")
        void testToStringIncludesRetryable() {
            S3OperationException ex = new S3OperationException("Error", true, null);
            assertTrue(ex.toString().contains("retryable=true"));
        }
    }

    @Nested
    @DisplayName("DynamoDBOperationException Tests")
    class DynamoDBOperationExceptionTests {

        @Test
        @DisplayName("Default HTTP status is 500")
        void testDefaultHttpStatus() {
            DynamoDBOperationException ex = new DynamoDBOperationException("DynamoDB error");
            assertEquals(500, ex.getHttpStatus());
            assertEquals("DYNAMODB_OPERATION_FAILED", ex.getErrorCode());
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("Retryable exception has status 503")
        void testRetryableHasStatus503() {
            DynamoDBOperationException ex = new DynamoDBOperationException(
                    "Throughput exceeded", true, new RuntimeException()
            );
            assertEquals(503, ex.getHttpStatus());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("Factory method for put item failure")
        void testPutItemFailedFactory() {
            DynamoDBOperationException ex = DynamoDBOperationException.putItemFailed(
                    "albums", "auth0|user-123", "ALBUM#album-uuid", new RuntimeException()
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("albums", ex.getContext().get("tableName"));
            assertEquals("auth0|user-123", ex.getContext().get("userId"));
            assertEquals("ALBUM#album-uuid", ex.getContext().get("sortKey"));
            assertEquals("putItem", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for query failure (retryable)")
        void testQueryFailedFactory() {
            DynamoDBOperationException ex = DynamoDBOperationException.queryFailed(
                    "albums", "auth0|user-123", new RuntimeException()
            );
            
            assertEquals(503, ex.getHttpStatus());
            assertTrue(ex.isRetryable());
            assertEquals("query", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for throughput exceeded (retryable)")
        void testThroughputExceededFactory() {
            DynamoDBOperationException ex = DynamoDBOperationException.throughputExceeded(
                    "albums", "query", new RuntimeException()
            );
            
            assertEquals(503, ex.getHttpStatus());
            assertTrue(ex.isRetryable());
            assertEquals("THROUGHPUT_EXCEEDED", ex.getContext().get("errorType"));
        }

        @Test
        @DisplayName("Factory method for album not empty")
        void testAlbumNotEmptyFactory() {
            DynamoDBOperationException ex = DynamoDBOperationException.albumNotEmpty(
                    "auth0|user-123", "album-uuid", 5
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("5 images"));
            assertEquals(5, ex.getContext().get("imageCount"));
            assertEquals("deleteAlbum", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("toString includes retryable flag")
        void testToStringIncludesRetryable() {
            DynamoDBOperationException ex = new DynamoDBOperationException("Error", true, null);
            assertTrue(ex.toString().contains("retryable=true"));
        }
    }

    @Nested
    @DisplayName("ImageProcessingException Tests")
    class ImageProcessingExceptionTests {

        @Test
        @DisplayName("Default HTTP status is 500")
        void testDefaultHttpStatus() {
            ImageProcessingException ex = new ImageProcessingException("Processing error");
            assertEquals(500, ex.getHttpStatus());
            assertEquals("IMAGE_PROCESSING_FAILED", ex.getErrorCode());
        }

        @Test
        @DisplayName("Factory method for resize failure")
        void testResizeFailedFactory() {
            ImageProcessingException ex = ImageProcessingException.resizeFailed(
                    "image-uuid", 256, 256, new RuntimeException()
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("IMAGE_RESIZE_FAILED", ex.getErrorCode());
            assertEquals("image-uuid", ex.getContext().get("imageId"));
            assertEquals(256, ex.getContext().get("targetWidth"));
            assertEquals(256, ex.getContext().get("targetHeight"));
            assertEquals("resize", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for thumbnail failure")
        void testThumbnailFailedFactory() {
            ImageProcessingException ex = ImageProcessingException.thumbnailFailed(
                    "image-uuid", new RuntimeException()
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("THUMBNAIL_GENERATION_FAILED", ex.getErrorCode());
            assertEquals("generateThumbnail", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for corrupt file")
        void testCorruptFileFactory() {
            ImageProcessingException ex = ImageProcessingException.corruptFile(
                    "image-uuid", "vacation.jpg", new RuntimeException()
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("CORRUPT_IMAGE_FILE", ex.getErrorCode());
            assertEquals("vacation.jpg", ex.getContext().get("fileName"));
            assertEquals("validate", ex.getContext().get("operation"));
        }

        @Test
        @DisplayName("Factory method for unsupported format")
        void testUnsupportedFormatFactory() {
            ImageProcessingException ex = ImageProcessingException.unsupportedFormat(
                    "image-uuid", "BMP", "image/bmp"
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("UNSUPPORTED_IMAGE_FORMAT", ex.getErrorCode());
            assertEquals("BMP", ex.getContext().get("format"));
            assertEquals("image/bmp", ex.getContext().get("mimeType"));
        }

        @Test
        @DisplayName("Factory method for file size exceeded")
        void testFileSizeExceededFactory() {
            long fileSize = 60_000_000L;
            long maxSize = 52_428_800L;
            ImageProcessingException ex = ImageProcessingException.fileSizeExceeded(
                    "image-uuid", fileSize, maxSize
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("FILE_SIZE_EXCEEDED", ex.getErrorCode());
            assertEquals(fileSize, ex.getContext().get("fileSize"));
            assertEquals(maxSize, ex.getContext().get("maxSize"));
        }

        @Test
        @DisplayName("Factory method for WebP conversion failure")
        void testWebpConversionFailedFactory() {
            ImageProcessingException ex = ImageProcessingException.webpConversionFailed(
                    "image-uuid", "HEIC", new RuntimeException()
            );
            
            assertEquals(500, ex.getHttpStatus());
            assertEquals("WEBP_CONVERSION_FAILED", ex.getErrorCode());
            assertEquals("HEIC", ex.getContext().get("sourceFormat"));
            assertEquals("WebP", ex.getContext().get("targetFormat"));
            assertEquals("convert", ex.getContext().get("operation"));
        }
    }
}
