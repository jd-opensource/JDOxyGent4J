package com.jd.oxygent.core.oxygent.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource URL Detector Utility
 *
 * <p>Utility class for detecting and extracting resource URLs from text content.
 * Supports various file formats including images, videos, documents, and audio files.</p>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ResourceUrlDetectorUtil {

    private static final Pattern PATTERN = Pattern.compile("part=\\{([^}]+)\\}");
    private static final Pattern DATA_PATTERN = Pattern.compile("data=([^,}]+)");

    // Common resource address regular expressions
    private static final Pattern[] RESOURCE_PATTERNS = {
            // Local file paths (Windows and Unix style)
            Pattern.compile("[a-zA-Z]:\\\\[^\\s]*\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)"),
            Pattern.compile("/([^/\\s]+/)*[^/\\s]*\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)"),
            Pattern.compile("\\./[^\\s]*\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)"),

            // Format with [Attachments] marker
            Pattern.compile("\\[Attachments?\\][\\s\\r\\n]+([^\\s\\r\\n]+\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav))", Pattern.CASE_INSENSITIVE),

            // HTTP/HTTPS URL
            Pattern.compile("https?://[^\\s]+\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)"),

            // Relative paths
            Pattern.compile("\\b(?:uploads?|files?|attachments?|images?|videos?|documents?)/[^\\s]+\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)", Pattern.CASE_INSENSITIVE),

            // Specific format provided: ./cache_dir/xfile/uploads/20251117112249_b7d11e6b-20da-4dad-b6cf-f3af345647a5.png
            Pattern.compile("\\./([^/\\s]+/)*[^/\\s]*_([a-f0-9-]+)\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)")
    };

    // Supported file extensions
    private static final String[] SUPPORTED_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp",  // Images
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",  // Videos
            "pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx",  // Documents
            "mp3", "wav", "ogg", "aac", "flac", "m4a"  // Audio
    };

    /**
     * Detect if text contains resource URLs
     */
    public static boolean containsResourceUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : RESOURCE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract all resource URLs from text
     */
    public static List<String> extractResourceUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return urls;
        }

        for (Pattern pattern : RESOURCE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String url = matcher.group();
                // For [Attachments] format, extract actual file path
                if (url.toLowerCase().startsWith("[attachment")) {
                    // Extract file path after attachment marker
                    String[] lines = text.split("\\r?\\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].toLowerCase().contains("[attachment")) {
                            if (i + 1 < lines.length) {
                                String filePath = lines[i + 1].trim();
                                if (isResourceFile(filePath)) {
                                    urls.add(filePath);
                                }
                            }
                        }
                    }
                } else {
                    urls.add(url);
                }
            }
        }

        return urls;
    }

    /**
     * Extract resource URLs of specific type
     */
    public static List<String> extractResourceUrlsByType(String text, String fileType) {
        List<String> allUrls = extractResourceUrls(text);
        List<String> filteredUrls = new ArrayList<>();

        for (String url : allUrls) {
            if (getFileCategory(url).equals(fileType)) {
                filteredUrls.add(url);
            }
        }

        return filteredUrls;
    }

    /**
     * Check if string is a resource file path
     */
    public static boolean isResourceFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        String lowerPath = path.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerPath.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file type category
     */
    public static String getFileCategory(String filePath) {
        if (filePath == null) {
            return "unknown";
        }

        String lowerPath = filePath.toLowerCase();
        if (lowerPath.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            return "image";
        } else if (lowerPath.matches(".*\\.(mp4|avi|mov|wmv|flv|mkv|webm)$")) {
            return "video";
        } else if (lowerPath.matches(".*\\.(pdf|doc|docx|txt|ppt|pptx|xls|xlsx)$")) {
            return "document";
        } else if (lowerPath.matches(".*\\.(mp3|wav|ogg|aac|flac|m4a)$")) {
            return "audio";
        } else {
            return "unknown";
        }
    }

    /**
     * Remove resource URLs from text
     */
    public static String removeResourceUrls(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String cleanedText = text;

        // Handle [Attachments] format
        cleanedText = cleanedText.replaceAll("\\[Attachments?\\][\\s\\r\\n]+[^\\s\\r\\n]+\\.(jpg|jpeg|png|gif|bmp|mp4|avi|mov|pdf|doc|docx|txt|mp3|wav)", "");

        // Handle other resource URLs
        for (Pattern pattern : RESOURCE_PATTERNS) {
            if (!pattern.pattern().toLowerCase().contains("attachment")) {
                cleanedText = pattern.matcher(cleanedText).replaceAll("");
            }
        }

        return cleanedText.trim();
    }

    /**
     * Get plain text content (text after removing resource URLs)
     */
    public static String getPlainText(String text) {
        return removeResourceUrls(text);
    }


    /**
     * Extract all paths and URLs from structured string
     * Input format example: "[{part={content_type=path, data=./cache_dir/xfile/uploads/20251119160930_20251103135928047.png}}, {part={content_type=text/plain, data=What is this?}}]"
     * Output: List containing only data values of path type
     */
    public static List<String> extractPathsFromStructuredString(String input) {
        List<String> paths = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            return paths;
        }

        // More generic regular expression, matching part blocks containing content_type=path and data=xxx
        // Supports arbitrary order of content_type and data fields
        Matcher partMatcher = PATTERN.matcher(input);

        while (partMatcher.find()) {
            String partContent = partMatcher.group(1);

            // Check if this part contains content_type=path or content_type=url
            if (partContent.contains("content_type=path") || partContent.contains("content_type=url")) {
                // Extract data field value
                Matcher dataMatcher = DATA_PATTERN.matcher(partContent);

                if (dataMatcher.find()) {
                    String path = dataMatcher.group(1).trim();
                    // Ensure extracted path or URL is valid
                    if (!path.isEmpty()) {
                        paths.add(path);
                    }
                }
            }
        }

        return paths;
    }
}