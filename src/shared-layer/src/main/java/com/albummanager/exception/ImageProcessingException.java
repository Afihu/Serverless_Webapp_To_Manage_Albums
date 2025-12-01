package com.albummanager.exception;

import java.util.Map;

/**
 * Exception thrown when image processing operations fail.
 * HTTP Status: 500 Internal Server Error
 * 
 * Use cases:
 * - Image resizing failures
 * - Image validation failures (corrupt file, unsupported format)
 * - Thumbnail generation failures
 * - Image dimension extraction failures
 */
public class ImageProcessingException extends AlbumManagerException {

    private static final int HTTP_STATUS = 500;
    private static final String DEFAULT_ERROR_CODE = "IMAGE_PROCESSING_FAILED";

    /**
     * Constructs an ImageProcessingException with a message.
     *
     * @param message Human-readable error message
     */
    public ImageProcessingException(String message) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS);
    }

    /**
     * Constructs an ImageProcessingException with a cause.
     *
     * @param message Human-readable error message
     * @param cause   The underlying cause
     */
    public ImageProcessingException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS, cause);
    }

    /**
     * Constructs an ImageProcessingException with a specific error code.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code
     */
    public ImageProcessingException(String message, String errorCode) {
        super(message, errorCode, HTTP_STATUS);
    }

    /**
     * Constructs an ImageProcessingException with context and cause.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code
     * @param context   Additional context (imageId, format, etc.)
     * @param cause     The underlying cause
     */
    public ImageProcessingException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, HTTP_STATUS, context, cause);
    }

    /**
     * Factory method for image resize failures.
     *
     * @param imageId      The image ID being resized
     * @param targetWidth  The target width for resize
     * @param targetHeight The target height for resize
     * @param cause        The underlying cause
     * @return An ImageProcessingException configured for resize failure
     */
    public static ImageProcessingException resizeFailed(String imageId, int targetWidth, 
                                                         int targetHeight, Throwable cause) {
        ImageProcessingException ex = new ImageProcessingException(
                "Failed to resize image: " + imageId + " to " + targetWidth + "x" + targetHeight,
                "IMAGE_RESIZE_FAILED", null, cause);
        ex.addContext("imageId", imageId);
        ex.addContext("targetWidth", targetWidth);
        ex.addContext("targetHeight", targetHeight);
        ex.addContext("operation", "resize");
        return ex;
    }

    /**
     * Factory method for thumbnail generation failures.
     *
     * @param imageId The image ID
     * @param cause   The underlying cause
     * @return An ImageProcessingException configured for thumbnail failure
     */
    public static ImageProcessingException thumbnailFailed(String imageId, Throwable cause) {
        ImageProcessingException ex = new ImageProcessingException(
                "Failed to generate thumbnail for image: " + imageId,
                "THUMBNAIL_GENERATION_FAILED", null, cause);
        ex.addContext("imageId", imageId);
        ex.addContext("operation", "generateThumbnail");
        return ex;
    }

    /**
     * Factory method for corrupt image file errors.
     *
     * @param imageId  The image ID
     * @param fileName The original file name
     * @param cause    The underlying cause
     * @return An ImageProcessingException configured for corrupt file
     */
    public static ImageProcessingException corruptFile(String imageId, String fileName, Throwable cause) {
        ImageProcessingException ex = new ImageProcessingException(
                "Image file is corrupt or unreadable: " + fileName,
                "CORRUPT_IMAGE_FILE", null, cause);
        ex.addContext("imageId", imageId);
        ex.addContext("fileName", fileName);
        ex.addContext("operation", "validate");
        return ex;
    }

    /**
     * Factory method for unsupported format errors.
     *
     * @param imageId   The image ID
     * @param format    The unsupported format
     * @param mimeType  The detected MIME type
     * @return An ImageProcessingException configured for unsupported format
     */
    public static ImageProcessingException unsupportedFormat(String imageId, String format, String mimeType) {
        ImageProcessingException ex = new ImageProcessingException(
                "Unsupported image format: " + format + " (MIME type: " + mimeType + ")",
                "UNSUPPORTED_IMAGE_FORMAT");
        ex.addContext("imageId", imageId);
        ex.addContext("format", format);
        ex.addContext("mimeType", mimeType);
        ex.addContext("operation", "validate");
        return ex;
    }

    /**
     * Factory method for dimension extraction failures.
     *
     * @param imageId The image ID
     * @param cause   The underlying cause
     * @return An ImageProcessingException configured for dimension extraction failure
     */
    public static ImageProcessingException dimensionExtractionFailed(String imageId, Throwable cause) {
        ImageProcessingException ex = new ImageProcessingException(
                "Failed to extract dimensions from image: " + imageId,
                "DIMENSION_EXTRACTION_FAILED", null, cause);
        ex.addContext("imageId", imageId);
        ex.addContext("operation", "extractDimensions");
        return ex;
    }

    /**
     * Factory method for file size exceeded errors.
     *
     * @param imageId  The image ID
     * @param fileSize The actual file size in bytes
     * @param maxSize  The maximum allowed size in bytes
     * @return An ImageProcessingException configured for file size exceeded
     */
    public static ImageProcessingException fileSizeExceeded(String imageId, long fileSize, long maxSize) {
        ImageProcessingException ex = new ImageProcessingException(
                "Image file size " + fileSize + " bytes exceeds maximum allowed " + maxSize + " bytes",
                "FILE_SIZE_EXCEEDED");
        ex.addContext("imageId", imageId);
        ex.addContext("fileSize", fileSize);
        ex.addContext("maxSize", maxSize);
        ex.addContext("operation", "validate");
        return ex;
    }

    /**
     * Factory method for WebP conversion failures.
     *
     * @param imageId      The image ID
     * @param sourceFormat The source format
     * @param cause        The underlying cause
     * @return An ImageProcessingException configured for WebP conversion failure
     */
    public static ImageProcessingException webpConversionFailed(String imageId, String sourceFormat, Throwable cause) {
        ImageProcessingException ex = new ImageProcessingException(
                "Failed to convert image from " + sourceFormat + " to WebP",
                "WEBP_CONVERSION_FAILED", null, cause);
        ex.addContext("imageId", imageId);
        ex.addContext("sourceFormat", sourceFormat);
        ex.addContext("targetFormat", "WebP");
        ex.addContext("operation", "convert");
        return ex;
    }
}
