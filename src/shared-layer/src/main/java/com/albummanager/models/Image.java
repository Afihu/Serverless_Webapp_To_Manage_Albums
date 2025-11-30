package com.albummanager.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable entity representing an image entry.
 * Implements Entry interface for polymorphic handling in services.
 */
public class Image implements Entry {

    private final String userId;
    private final String imageId;          // UUID v4
    private final String albumId;          // UUID v4
    private final String fileName;         // 1-255 chars
    private final long fileSize;           // bytes, > 0, <= 52,428,800 (50 MB)
    private final ImageFormat fileFormat;  // Enum: JPEG, PNG, WEBP, GIF, HEIC
    private final String mimeType;         // Must correspond to fileFormat
    private final int width;               // pixels, > 0
    private final int height;              // pixels, > 0
    private final UploadStatus uploadStatus;
    private final ResizeStatus resizeStatus;
    private final String checksumMd5;      // 32-char hex string
    private final String s3Key;            // S3 object key
    private final String thumbnailS3Key;   // S3 thumbnail key, optional
    private final long uploadedAt;         // Unix timestamp
    private final Long resizeCompletedAt;  // Unix timestamp, optional (null if not completed)

    private static final long MAX_FILE_SIZE = 52_428_800L; // 50 MB in bytes

    /**
     * Private constructor - use Builder to create instances.
     */
    private Image(Builder builder) {
        this.userId = builder.userId;
        this.imageId = builder.imageId;
        this.albumId = builder.albumId;
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.fileFormat = builder.fileFormat;
        this.mimeType = builder.mimeType;
        this.width = builder.width;
        this.height = builder.height;
        this.uploadStatus = builder.uploadStatus;
        this.resizeStatus = builder.resizeStatus;
        this.checksumMd5 = builder.checksumMd5;
        this.s3Key = builder.s3Key;
        this.thumbnailS3Key = builder.thumbnailS3Key;
        this.uploadedAt = builder.uploadedAt;
        this.resizeCompletedAt = builder.resizeCompletedAt;
        validate();
    }

    /**
     * Validates all fields according to business rules.
     * 
     * @throws IllegalArgumentException if any validation fails
     */
    private void validate() {
        // userId validation
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }

        // imageId validation
        if (imageId == null || imageId.isBlank()) {
            throw new IllegalArgumentException("imageId cannot be null or blank");
        }
        try {
            UUID.fromString(imageId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("imageId must be a valid UUID v4 format", e);
        }

        // albumId validation
        if (albumId == null || albumId.isBlank()) {
            throw new IllegalArgumentException("albumId cannot be null or blank");
        }
        try {
            UUID.fromString(albumId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("albumId must be a valid UUID v4 format", e);
        }

        // fileName validation
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName cannot be null or blank");
        }
        if (fileName.length() < 1 || fileName.length() > 255) {
            throw new IllegalArgumentException("fileName must be between 1 and 255 characters");
        }

        // fileSize validation
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize must be greater than 0");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("fileSize cannot exceed 52,428,800 bytes (50 MB)");
        }

        // fileFormat validation
        if (fileFormat == null) {
            throw new IllegalArgumentException("fileFormat cannot be null");
        }

        // mimeType validation
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        if (!fileFormat.getMimeType().equalsIgnoreCase(mimeType)) {
            throw new IllegalArgumentException(
                String.format("mimeType '%s' does not match fileFormat '%s' (expected '%s')",
                    mimeType, fileFormat, fileFormat.getMimeType())
            );
        }

        // width and height validation
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than 0");
        }

        // uploadStatus validation
        if (uploadStatus == null) {
            throw new IllegalArgumentException("uploadStatus cannot be null");
        }

        // resizeStatus validation
        if (resizeStatus == null) {
            throw new IllegalArgumentException("resizeStatus cannot be null");
        }

        // checksumMd5 validation (32-char hex string)
        if (checksumMd5 == null || checksumMd5.isBlank()) {
            throw new IllegalArgumentException("checksumMd5 cannot be null or blank");
        }
        if (!checksumMd5.matches("^[0-9a-fA-F]{32}$")) {
            throw new IllegalArgumentException("checksumMd5 must be a 32-character hexadecimal string");
        }

        // s3Key validation
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("s3Key cannot be null or blank");
        }

        // resizeCompletedAt validation
        if (resizeCompletedAt != null && resizeStatus != ResizeStatus.COMPLETED) {
            throw new IllegalArgumentException("resizeCompletedAt can only be set when resizeStatus is COMPLETED");
        }
    }

    // Getters

    @Override
    public String getUserId() {
        return userId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String getId() {
        return imageId;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public ImageFormat getFileFormat() {
        return fileFormat;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public ResizeStatus getResizeStatus() {
        return resizeStatus;
    }

    public String getChecksumMd5() {
        return checksumMd5;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getThumbnailS3Key() {
        return thumbnailS3Key;
    }

    @Override
    public long getCreatedAt() {
        return uploadedAt;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public Long getResizeCompletedAt() {
        return resizeCompletedAt;
    }

    @Override
    public String getEntityType() {
        return ENTITY_TYPE_IMAGE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return fileSize == image.fileSize &&
               width == image.width &&
               height == image.height &&
               uploadedAt == image.uploadedAt &&
               Objects.equals(userId, image.userId) &&
               Objects.equals(imageId, image.imageId) &&
               Objects.equals(albumId, image.albumId) &&
               Objects.equals(fileName, image.fileName) &&
               fileFormat == image.fileFormat &&
               Objects.equals(mimeType, image.mimeType) &&
               uploadStatus == image.uploadStatus &&
               resizeStatus == image.resizeStatus &&
               Objects.equals(checksumMd5, image.checksumMd5) &&
               Objects.equals(s3Key, image.s3Key) &&
               Objects.equals(thumbnailS3Key, image.thumbnailS3Key) &&
               Objects.equals(resizeCompletedAt, image.resizeCompletedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, imageId, albumId, fileName, fileSize, fileFormat, mimeType,
                          width, height, uploadStatus, resizeStatus, checksumMd5, s3Key,
                          thumbnailS3Key, uploadedAt, resizeCompletedAt);
    }

    @Override
    public String toString() {
        return "Image{" +
               "userId='" + userId + '\'' +
               ", imageId='" + imageId + '\'' +
               ", albumId='" + albumId + '\'' +
               ", fileName='" + fileName + '\'' +
               ", fileSize=" + fileSize +
               ", fileFormat=" + fileFormat +
               ", mimeType='" + mimeType + '\'' +
               ", width=" + width +
               ", height=" + height +
               ", uploadStatus=" + uploadStatus +
               ", resizeStatus=" + resizeStatus +
               ", checksumMd5='" + checksumMd5 + '\'' +
               ", s3Key='" + s3Key + '\'' +
               ", thumbnailS3Key='" + thumbnailS3Key + '\'' +
               ", uploadedAt=" + uploadedAt +
               ", resizeCompletedAt=" + resizeCompletedAt +
               '}';
    }

    /**
     * Creates a new Builder instance.
     * 
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for creating immutable Image instances.
     */
    public static class Builder {
        private String userId;
        private String imageId;
        private String albumId;
        private String fileName;
        private long fileSize;
        private ImageFormat fileFormat;
        private String mimeType;
        private int width;
        private int height;
        private UploadStatus uploadStatus;
        private ResizeStatus resizeStatus;
        private String checksumMd5;
        private String s3Key;
        private String thumbnailS3Key;
        private long uploadedAt;
        private Long resizeCompletedAt;

        private Builder() {}

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder imageId(String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder albumId(String albumId) {
            this.albumId = albumId;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder fileFormat(ImageFormat fileFormat) {
            this.fileFormat = fileFormat;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder uploadStatus(UploadStatus uploadStatus) {
            this.uploadStatus = uploadStatus;
            return this;
        }

        public Builder resizeStatus(ResizeStatus resizeStatus) {
            this.resizeStatus = resizeStatus;
            return this;
        }

        public Builder checksumMd5(String checksumMd5) {
            this.checksumMd5 = checksumMd5;
            return this;
        }

        public Builder s3Key(String s3Key) {
            this.s3Key = s3Key;
            return this;
        }

        public Builder thumbnailS3Key(String thumbnailS3Key) {
            this.thumbnailS3Key = thumbnailS3Key;
            return this;
        }

        public Builder uploadedAt(long uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public Builder resizeCompletedAt(Long resizeCompletedAt) {
            this.resizeCompletedAt = resizeCompletedAt;
            return this;
        }

        /**
         * Builds and validates the Image instance.
         * 
         * @return a new Image instance
         * @throws IllegalArgumentException if validation fails
         */
        public Image build() {
            return new Image(this);
        }
    }
}
