package com.albummanager.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable entity representing an image album.
 * Implements Entry interface for polymorphic handling in services.
 */
public class Album implements Entry {

    private final String userId;           // e.g., "auth0|user-123abc"
    private final String albumId;          // UUID v4
    private final String albumName;        // 1-255 chars, trimmed
    private final String description;      // Optional, 0-500 chars if present
    private final long createdAt;          // Unix timestamp in milliseconds
    private final long updatedAt;          // Unix timestamp in milliseconds
    private final int imageCount;          // Denormalized count, >= 0

    /**
     * Private constructor - use Builder to create instances.
     */
    private Album(Builder builder) {
        this.userId = builder.userId;
        this.albumId = builder.albumId;
        this.albumName = builder.albumName;
        this.description = builder.description;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.imageCount = builder.imageCount;
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
        if (userId.length() > 128) {
            throw new IllegalArgumentException("userId cannot exceed 128 characters");
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

        // albumName validation
        if (albumName == null || albumName.isBlank()) {
            throw new IllegalArgumentException("albumName cannot be null or blank");
        }
        if (albumName.length() < 1 || albumName.length() > 255) {
            throw new IllegalArgumentException("albumName must be between 1 and 255 characters");
        }

        // description validation (optional field)
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("description cannot exceed 500 characters");
        }

        // imageCount validation
        if (imageCount < 0) {
            throw new IllegalArgumentException("imageCount cannot be negative");
        }

        // timestamp validation
        if (createdAt > updatedAt) {
            throw new IllegalArgumentException("createdAt cannot be after updatedAt");
        }
    }

    // Getters

    public String getUserId() {
        return userId;
    }

    public String getAlbumId() {
        return albumId;
    }

    @Override
    public String getId() {
        return albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public int getImageCount() {
        return imageCount;
    }

    @Override
    public String getEntityType() {
        return ENTITY_TYPE_ALBUM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Album album = (Album) o;
        return createdAt == album.createdAt &&
               updatedAt == album.updatedAt &&
               imageCount == album.imageCount &&
               Objects.equals(userId, album.userId) &&
               Objects.equals(albumId, album.albumId) &&
               Objects.equals(albumName, album.albumName) &&
               Objects.equals(description, album.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, albumId, albumName, description, createdAt, updatedAt, imageCount);
    }

    @Override
    public String toString() {
        return "Album{" +
               "userId='" + userId + '\'' +
               ", albumId='" + albumId + '\'' +
               ", albumName='" + albumName + '\'' +
               ", description='" + description + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               ", imageCount=" + imageCount +
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
     * Builder pattern for creating immutable Album instances.
     */
    public static class Builder {
        private String userId;
        private String albumId;
        private String albumName;
        private String description;
        private long createdAt;
        private long updatedAt;
        private int imageCount = 0;

        private Builder() {}

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder albumId(String albumId) {
            this.albumId = albumId;
            return this;
        }

        public Builder albumName(String albumName) {
            this.albumName = albumName != null ? albumName.trim() : null;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder imageCount(int imageCount) {
            this.imageCount = imageCount;
            return this;
        }

        /**
         * Builds and validates the Album instance.
         * 
         * @return a new Album instance
         * @throws IllegalArgumentException if validation fails
         */
        public Album build() {
            return new Album(this);
        }
    }
}
