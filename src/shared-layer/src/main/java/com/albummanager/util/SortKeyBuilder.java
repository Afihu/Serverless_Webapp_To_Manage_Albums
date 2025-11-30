package com.albummanager.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for building and parsing DynamoDB sort keys with type prefixes.
 * 
 * <p>Sort key formats:</p>
 * <ul>
 *   <li>Album: {@code ALBUM#<albumId>}</li>
 *   <li>Image: {@code IMAGE#<albumId>#<imageId>}</li>
 * </ul>
 * 
 * <p>This enables efficient querying by type prefix in DynamoDB's composite key structure.</p>
 */
public class SortKeyBuilder {

    private static final String ALBUM_PREFIX = "ALBUM#";
    private static final String IMAGE_PREFIX = "IMAGE#";
    private static final String DELIMITER = "#";

    // Regex patterns for parsing sort keys
    private static final Pattern ALBUM_PATTERN = Pattern.compile("^ALBUM#([a-f0-9\\-]{36})$");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^IMAGE#([a-f0-9\\-]{36})#([a-f0-9\\-]{36})$");

    /**
     * Private constructor to prevent instantiation.
     */
    private SortKeyBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds an album sort key.
     * 
     * @param albumId the album UUID
     * @return formatted sort key: {@code ALBUM#<albumId>}
     * @throws IllegalArgumentException if albumId is null or blank
     */
    public static String buildAlbumSortKey(String albumId) {
        if (albumId == null || albumId.isBlank()) {
            throw new IllegalArgumentException("albumId cannot be null or blank");
        }
        return ALBUM_PREFIX + albumId;
    }

    /**
     * Builds an image sort key.
     * 
     * @param albumId the album UUID
     * @param imageId the image UUID
     * @return formatted sort key: {@code IMAGE#<albumId>#<imageId>}
     * @throws IllegalArgumentException if albumId or imageId is null or blank
     */
    public static String buildImageSortKey(String albumId, String imageId) {
        if (albumId == null || albumId.isBlank()) {
            throw new IllegalArgumentException("albumId cannot be null or blank");
        }
        if (imageId == null || imageId.isBlank()) {
            throw new IllegalArgumentException("imageId cannot be null or blank");
        }
        return IMAGE_PREFIX + albumId + DELIMITER + imageId;
    }

    /**
     * Extracts the album ID from an album sort key.
     * 
     * @param sortKey the album sort key in format {@code ALBUM#<albumId>}
     * @return the album UUID
     * @throws IllegalArgumentException if sortKey is invalid or not an album sort key
     */
    public static String extractAlbumId(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            throw new IllegalArgumentException("sortKey cannot be null or blank");
        }

        Matcher matcher = ALBUM_PATTERN.matcher(sortKey);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid album sort key format. Expected: ALBUM#<albumId>, got: " + sortKey
            );
        }

        return matcher.group(1);
    }

    /**
     * Extracts the album ID from an image sort key.
     * 
     * @param sortKey the image sort key in format {@code IMAGE#<albumId>#<imageId>}
     * @return the album UUID
     * @throws IllegalArgumentException if sortKey is invalid or not an image sort key
     */
    public static String extractAlbumIdFromImage(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            throw new IllegalArgumentException("sortKey cannot be null or blank");
        }

        Matcher matcher = IMAGE_PATTERN.matcher(sortKey);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid image sort key format. Expected: IMAGE#<albumId>#<imageId>, got: " + sortKey
            );
        }

        return matcher.group(1);
    }

    /**
     * Extracts the image ID from an image sort key.
     * 
     * @param sortKey the image sort key in format {@code IMAGE#<albumId>#<imageId>}
     * @return the image UUID
     * @throws IllegalArgumentException if sortKey is invalid or not an image sort key
     */
    public static String extractImageId(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            throw new IllegalArgumentException("sortKey cannot be null or blank");
        }

        Matcher matcher = IMAGE_PATTERN.matcher(sortKey);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid image sort key format. Expected: IMAGE#<albumId>#<imageId>, got: " + sortKey
            );
        }

        return matcher.group(2);
    }

    /**
     * Builds a query prefix for albums belonging to a specific user.
     * Used for DynamoDB begins_with queries.
     * 
     * @return query prefix: {@code ALBUM#}
     */
    public static String albumQueryPrefix() {
        return ALBUM_PREFIX;
    }

    /**
     * Builds a query prefix for images belonging to a specific album.
     * Used for DynamoDB begins_with queries.
     * 
     * @param albumId the album UUID
     * @return query prefix: {@code IMAGE#<albumId>#}
     * @throws IllegalArgumentException if albumId is null or blank
     */
    public static String imageQueryPrefix(String albumId) {
        if (albumId == null || albumId.isBlank()) {
            throw new IllegalArgumentException("albumId cannot be null or blank");
        }
        return IMAGE_PREFIX + albumId + DELIMITER;
    }

    /**
     * Determines if a sort key represents an album.
     * 
     * @param sortKey the sort key to check
     * @return true if the sort key is an album sort key, false otherwise
     */
    public static boolean isAlbumSortKey(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return false;
        }
        return ALBUM_PATTERN.matcher(sortKey).matches();
    }

    /**
     * Determines if a sort key represents an image.
     * 
     * @param sortKey the sort key to check
     * @return true if the sort key is an image sort key, false otherwise
     */
    public static boolean isImageSortKey(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return false;
        }
        return IMAGE_PATTERN.matcher(sortKey).matches();
    }
}
