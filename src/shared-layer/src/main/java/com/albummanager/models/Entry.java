package com.albummanager.models;

/**
 * Polymorphic base interface for Album and Image entities.
 * Enables unified handling in services and supports type-safe operations.
 */
public interface Entry {

    /**
     * Gets the user ID who owns this entry.
     *
     * @return the user ID (e.g., "auth0|user-123abc")
     */
    String getUserId();

    /**
     * Gets the unique identifier for this entry.
     * Returns albumId for Album, imageId for Image.
     *
     * @return the entry's unique ID (UUID v4 format)
     */
    String getId();

    /**
     * Gets the creation timestamp for this entry.
     *
     * @return Unix timestamp in milliseconds
     */
    long getCreatedAt();

    /**
     * Gets the entity type discriminator.
     *
     * @return "album" for Album entries, "image" for Image entries
     */
    String getEntityType();

    /**
     * Entity type constant for albums.
     */
    String ENTITY_TYPE_ALBUM = "album";

    /**
     * Entity type constant for images.
     */
    String ENTITY_TYPE_IMAGE = "image";
}
