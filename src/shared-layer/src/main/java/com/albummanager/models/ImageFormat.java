package com.albummanager.models;

/**
 * Supported image formats for image uploads.
 * Each format maps to its MIME type and file extension.
 */
public enum ImageFormat {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    GIF("image/gif", "gif"),
    HEIC("image/heic", "heic");

    private final String mimeType;
    private final String extension;

    ImageFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    /**
     * Gets the MIME type for this image format.
     *
     * @return the MIME type (e.g., "image/jpeg")
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the file extension for this image format.
     *
     * @return the file extension without dot (e.g., "jpg")
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Finds an ImageFormat by its MIME type.
     *
     * @param mimeType the MIME type to search for
     * @return the matching ImageFormat
     * @throws IllegalArgumentException if no format matches the given MIME type
     */
    public static ImageFormat fromMimeType(String mimeType) {
        if (mimeType == null) {
            throw new IllegalArgumentException("MIME type cannot be null");
        }
        String normalizedMimeType = mimeType.toLowerCase().trim();
        for (ImageFormat format : values()) {
            if (format.mimeType.equals(normalizedMimeType)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
    }

    /**
     * Finds an ImageFormat by its name (case-insensitive).
     *
     * @param name the format name to search for (e.g., "jpeg", "JPEG")
     * @return the matching ImageFormat
     * @throws IllegalArgumentException if no format matches the given name
     */
    public static ImageFormat fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Format name cannot be null");
        }
        try {
            return ImageFormat.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported image format: " + name);
        }
    }

    /**
     * Checks if a given MIME type is supported.
     *
     * @param mimeType the MIME type to check
     * @return true if the MIME type is supported, false otherwise
     */
    public static boolean isSupported(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String normalizedMimeType = mimeType.toLowerCase().trim();
        for (ImageFormat format : values()) {
            if (format.mimeType.equals(normalizedMimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that a MIME type matches this image format.
     *
     * @param mimeType the MIME type to validate
     * @return true if the MIME type matches this format
     */
    public boolean matchesMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return this.mimeType.equals(mimeType.toLowerCase().trim());
    }
}
