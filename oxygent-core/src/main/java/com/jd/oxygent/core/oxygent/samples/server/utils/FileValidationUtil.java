package com.jd.oxygent.core.oxygent.samples.server.utils;

import org.apache.tomcat.util.http.fileupload.FileItem;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class FileValidationUtil {

    // Allowed file types and corresponding extensions
    private static final Map<String, List<String>> ALLOWED_FILE_TYPES = new HashMap<>();

    static {
        // Image types
        ALLOWED_FILE_TYPES.put("image", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp"));
        // Video types
        ALLOWED_FILE_TYPES.put("video", Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"));
        // Document types
        ALLOWED_FILE_TYPES.put("document", Arrays.asList("pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx"));
        // Audio types
        ALLOWED_FILE_TYPES.put("audio", Arrays.asList("mp3", "wav", "ogg", "aac", "flac", "m4a"));
    }

    // MIME types corresponding to file types
    private static final Map<String, List<String>> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("image", Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
        ));
        MIME_TYPES.put("video", Arrays.asList(
                "video/mp4", "video/avi", "video/quicktime", "video/x-ms-wmv",
                "video/x-flv", "video/x-matroska", "video/webm"
        ));
        MIME_TYPES.put("document", Arrays.asList(
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain", "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ));
        MIME_TYPES.put("audio", Arrays.asList(
                "audio/mpeg", "audio/wav", "audio/ogg", "audio/aac",
                "audio/flac", "audio/mp4"
        ));
    }

    // Maximum size limits for various file types (in bytes)
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;      // 5MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;    // 100MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;  // 10MB
    private static final long MAX_AUDIO_SIZE = 20 * 1024 * 1024;     // 20MB

    // File magic numbers (file header signatures)
    private static final Map<String, byte[]> FILE_SIGNATURES = new HashMap<>();

    static {
        // Images
        FILE_SIGNATURES.put("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        FILE_SIGNATURES.put("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        FILE_SIGNATURES.put("gif", new byte[]{0x47, 0x49, 0x46});

        // Documents
        FILE_SIGNATURES.put("pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        FILE_SIGNATURES.put("doc", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0});
        FILE_SIGNATURES.put("docx", new byte[]{0x50, 0x4B, 0x03, 0x04});

        // Signatures for other file types to be added
    }

    public static void validateFile(FileItem file, String... allowedCategories) throws IOException {
        if (file == null || file.getName() == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        String originalFilename = file.getName();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        String fileType = getFileType(fileExtension);

        // Check if file type is allowed
        if (!isFileTypeAllowed(fileType, allowedCategories)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        // Check file size
        validateFileSize(file, fileType);

        // Check MIME type
        validateMimeType(file, fileType, fileExtension);

        // Advanced validation: file header validation
        validateFileSignature(file, fileExtension);

        // Additional validation for special types
        if ("image".equals(fileType)) {
            validateImageFile(file);
        }
    }

    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private static String getFileType(String extension) {
        for (Map.Entry<String, List<String>> entry : ALLOWED_FILE_TYPES.entrySet()) {
            if (entry.getValue().contains(extension)) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    private static boolean isFileTypeAllowed(String fileType, String[] allowedCategories) {
        if (allowedCategories.length == 0) {
            return !"unknown".equals(fileType); // If no categories specified, allow all defined types
        }
        return Arrays.asList(allowedCategories).contains(fileType);
    }

    private static void validateFileSize(FileItem file, String fileType) {
        long maxSize;
        switch (fileType) {
            case "image":
                maxSize = MAX_IMAGE_SIZE;
                break;
            case "video":
                maxSize = MAX_VIDEO_SIZE;
                break;
            case "document":
                maxSize = MAX_DOCUMENT_SIZE;
                break;
            case "audio":
                maxSize = MAX_AUDIO_SIZE;
                break;
            default:
                maxSize = 5 * 1024 * 1024; // Default 5MB
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(String.format("File size cannot exceed %dMB", maxSize / (1024 * 1024)));
        }
    }

    private static void validateMimeType(FileItem file, String fileType, String extension) {
        String contentType = file.getContentType();
        List<String> allowedMimeTypes = MIME_TYPES.get(fileType);

        if (contentType == null || allowedMimeTypes == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file MIME type: " + contentType);
        }

        // Check if extension and MIME type match
        if (!isExtensionMatchingMimeType(extension, contentType)) {
            throw new IllegalArgumentException("File extension and MIME type do not match");
        }
    }

    private static boolean isExtensionMatchingMimeType(String extension, String mimeType) {
        // More detailed extension and MIME type matching logic can be added here
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg".equals(mimeType);
            case "png":
                return "image/png".equals(mimeType);
            case "pdf":
                return "application/pdf".equals(mimeType);
            case "doc":
                return "application/msword".equals(mimeType);
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType);
            case "txt":
                return "text/plain".equals(mimeType);
            case "mp3":
                return "audio/mpeg".equals(mimeType);
            case "mp4":
                return "video/mp4".equals(mimeType);
            default:
                return true; // For other types, handle leniently for now
        }
    }

    private static void validateFileSignature(FileItem file, String extension) throws IOException {
        byte[] expectedSignature = FILE_SIGNATURES.get(extension);
        if (expectedSignature != null) {
            byte[] fileHeader = new byte[expectedSignature.length];
            try (var inputStream = file.getInputStream()) {
                int read = inputStream.read(fileHeader);
                if (read < expectedSignature.length || !Arrays.equals(fileHeader, expectedSignature)) {
                    throw new IllegalArgumentException("File signature mismatch, file may be corrupted or tampered");
                }
            }
        }
    }

    private static void validateImageFile(FileItem file) throws IOException {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("File is not a valid image");
            }

            // Validate image dimensions
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > 10000 || height > 10000) {
                throw new IllegalArgumentException("Image dimensions too large");
            }
            if (width < 10 || height < 10) {
                throw new IllegalArgumentException("Image dimensions too small");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read image file", e);
        }
    }

    // Get supported file type descriptions
    public static Map<String, List<String>> getAllowedFileTypes() {
        return new HashMap<>(ALLOWED_FILE_TYPES);
    }

    // Get file type category
    public static String getFileCategory(String extension) {
        return getFileType(extension);
    }
}