package com.albummanager.models;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImageTest {

    private static final String VALID_USER_ID = "auth0|user-123abc";
    private static final String VALID_IMAGE_ID = UUID.randomUUID().toString();
    private static final String VALID_ALBUM_ID = UUID.randomUUID().toString();
    private static final String VALID_FILE_NAME = "vacation-image.jpg";
    private static final long VALID_FILE_SIZE = 1024000L; // 1 MB
    private static final ImageFormat VALID_FORMAT = ImageFormat.JPEG;
    private static final String VALID_MIME_TYPE = "image/jpeg";
    private static final int VALID_WIDTH = 1920;
    private static final int VALID_HEIGHT = 1080;
    private static final String VALID_CHECKSUM = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String VALID_S3_KEY = "images/vacation-image.jpg";
    private static final long VALID_UPLOADED_AT = System.currentTimeMillis();

    @Test
    void testValidImageCreationWithAllRequiredFields() {
        Image image = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(VALID_FILE_SIZE)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .build();

        assertEquals(VALID_USER_ID, image.getUserId());
        assertEquals(VALID_IMAGE_ID, image.getImageId());
        assertEquals(VALID_IMAGE_ID, image.getId());
        assertEquals(VALID_ALBUM_ID, image.getAlbumId());
        assertEquals(VALID_FILE_NAME, image.getFileName());
        assertEquals(VALID_FILE_SIZE, image.getFileSize());
        assertEquals(VALID_FORMAT, image.getFileFormat());
        assertEquals(VALID_MIME_TYPE, image.getMimeType());
        assertEquals(VALID_WIDTH, image.getWidth());
        assertEquals(VALID_HEIGHT, image.getHeight());
        assertEquals(UploadStatus.COMPLETED, image.getUploadStatus());
        assertEquals(ResizeStatus.PENDING, image.getResizeStatus());
        assertEquals(VALID_CHECKSUM, image.getChecksumMd5());
        assertEquals(VALID_S3_KEY, image.getS3Key());
        assertEquals(VALID_UPLOADED_AT, image.getUploadedAt());
        assertEquals(VALID_UPLOADED_AT, image.getCreatedAt());
        assertNull(image.getResizeCompletedAt());
        assertEquals(Entry.ENTITY_TYPE_IMAGE, image.getEntityType());
    }

    @Test
    void testValidImageWithNullResizeCompletedAt() {
        Image image = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(VALID_FILE_SIZE)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .resizeCompletedAt(null)
            .build();

        assertNull(image.getResizeCompletedAt());
    }

    @Test
    void testValidImageWithResizeCompletedAt() {
        long resizeCompletedAt = VALID_UPLOADED_AT + 5000;
        Image image = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(VALID_FILE_SIZE)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.COMPLETED)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .thumbnailS3Key("thumbnails/vacation-image-thumb.jpg")
            .uploadedAt(VALID_UPLOADED_AT)
            .resizeCompletedAt(resizeCompletedAt)
            .build();

        assertEquals(resizeCompletedAt, image.getResizeCompletedAt());
        assertEquals("thumbnails/vacation-image-thumb.jpg", image.getThumbnailS3Key());
    }

    @Test
    void testImageWithMaxFileSize() {
        long maxFileSize = 52_428_800L; // Exactly 50 MB
        Image image = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(maxFileSize)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .build();

        assertEquals(maxFileSize, image.getFileSize());
    }

    @Test
    void testImageWithMinFileSize() {
        Image image = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(1L)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .build();

        assertEquals(1L, image.getFileSize());
    }

    @Test
    void testInvalidFileSizeTooLarge() {
        long tooLargeFileSize = 52_428_801L; // Over 50 MB
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(tooLargeFileSize)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(VALID_WIDTH)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5(VALID_CHECKSUM)
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("fileSize cannot exceed"));
    }

    @Test
    void testInvalidFileSizeZero() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(0L)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(VALID_WIDTH)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5(VALID_CHECKSUM)
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("fileSize must be greater than 0"));
    }

    @Test
    void testInvalidMimeTypeDoesNotMatchFormat() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(VALID_FILE_SIZE)
                .fileFormat(ImageFormat.JPEG)
                .mimeType("image/png") // Mismatch
                .width(VALID_WIDTH)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5(VALID_CHECKSUM)
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("mimeType"));
        assertTrue(exception.getMessage().contains("does not match fileFormat"));
    }

    @Test
    void testInvalidWidthZero() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(VALID_FILE_SIZE)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(0)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5(VALID_CHECKSUM)
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("width must be greater than 0"));
    }

    @Test
    void testInvalidHeightNegative() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(VALID_FILE_SIZE)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(VALID_WIDTH)
                .height(-1)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5(VALID_CHECKSUM)
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("height must be greater than 0"));
    }

    @Test
    void testInvalidChecksumNotHex() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(VALID_FILE_SIZE)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(VALID_WIDTH)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5("not-a-valid-md5-checksum-here!")
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("checksumMd5 must be a 32-character hexadecimal string"));
    }

    @Test
    void testInvalidChecksumTooShort() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(VALID_FILE_SIZE)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(VALID_WIDTH)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5("abc123")
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("checksumMd5 must be a 32-character hexadecimal string"));
    }

    @Test
    void testInvalidResizeCompletedAtWithoutCompletedStatus() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Image.builder()
                .userId(VALID_USER_ID)
                .imageId(VALID_IMAGE_ID)
                .albumId(VALID_ALBUM_ID)
                .fileName(VALID_FILE_NAME)
                .fileSize(VALID_FILE_SIZE)
                .fileFormat(VALID_FORMAT)
                .mimeType(VALID_MIME_TYPE)
                .width(VALID_WIDTH)
                .height(VALID_HEIGHT)
                .uploadStatus(UploadStatus.COMPLETED)
                .resizeStatus(ResizeStatus.PENDING)
                .checksumMd5(VALID_CHECKSUM)
                .s3Key(VALID_S3_KEY)
                .uploadedAt(VALID_UPLOADED_AT)
                .resizeCompletedAt(VALID_UPLOADED_AT + 1000)
                .build()
        );
        assertTrue(exception.getMessage().contains("resizeCompletedAt can only be set when resizeStatus is COMPLETED"));
    }

    @Test
    void testEqualsAndHashCode() {
        Image image1 = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(VALID_FILE_SIZE)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .build();

        Image image2 = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(VALID_FILE_SIZE)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .build();

        assertEquals(image1, image2);
        assertEquals(image1.hashCode(), image2.hashCode());
    }

    @Test
    void testImplementsEntry() {
        Image image = Image.builder()
            .userId(VALID_USER_ID)
            .imageId(VALID_IMAGE_ID)
            .albumId(VALID_ALBUM_ID)
            .fileName(VALID_FILE_NAME)
            .fileSize(VALID_FILE_SIZE)
            .fileFormat(VALID_FORMAT)
            .mimeType(VALID_MIME_TYPE)
            .width(VALID_WIDTH)
            .height(VALID_HEIGHT)
            .uploadStatus(UploadStatus.COMPLETED)
            .resizeStatus(ResizeStatus.PENDING)
            .checksumMd5(VALID_CHECKSUM)
            .s3Key(VALID_S3_KEY)
            .uploadedAt(VALID_UPLOADED_AT)
            .build();

        assertTrue(image instanceof Entry);
        Entry entry = image;
        assertEquals(VALID_USER_ID, entry.getUserId());
        assertEquals(VALID_IMAGE_ID, entry.getId());
        assertEquals(VALID_UPLOADED_AT, entry.getCreatedAt());
        assertEquals(Entry.ENTITY_TYPE_IMAGE, entry.getEntityType());
    }
}
