package com.jd.oxygent.core.oxygent.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * File download utility class
 * Responsible for downloading files from URLs to local cache directory, supports caching mechanism to avoid duplicate downloads
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class FileDownloadUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileDownloadUtil.class);

    // Default cache directory
    private static final String DEFAULT_CACHE_DIR = "./cache_dir/multimodal";

    // HTTP client configuration
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * Download file from URL to local cache
     * Supports automatic switching retry mechanism between internal and external links
     *
     * @param fileUrl  File URL
     * @param fileName File name
     * @param fileType File type (MIME type)
     * @return Local file path, returns null if download fails
     */
    public static String downloadFile(String fileUrl, String fileName, String fileType) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            logger.warn("File URL is empty");
            return null;
        }

        try {
            // Create cache directory
            Path cacheDir = Paths.get(DEFAULT_CACHE_DIR);
            Files.createDirectories(cacheDir);

            // Generate cached file name (based on URL hash to avoid conflicts)
            String urlHash = generateHash(fileUrl);
            String extension = getFileExtension(fileName, fileType);
            String cachedFileName = urlHash + extension;
            Path cachedFilePath = cacheDir.resolve(cachedFileName);

            // Check if cached file already exists
            if (Files.exists(cachedFilePath)) {
                logger.info("File already cached: {}", cachedFilePath);
                return cachedFilePath.toString();
            }

            // Try to download file, supports automatic switching between internal and external links
            String result = downloadWithFallback(fileUrl, cachedFilePath);
            if (result != null) {
                logger.info("File downloaded successfully: {}", cachedFilePath);
                return result;
            } else {
                logger.error("Failed to download file from both internal and external URLs: {}", fileUrl);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error downloading file from URL: {}", fileUrl, e);
            return null;
        }
    }

    /**
     * Download file and support automatic switching between internal and external links
     *
     * @param originalUrl    Original URL
     * @param cachedFilePath Cached file path
     * @return Returns file path on success, null on failure
     */
    private static String downloadWithFallback(String originalUrl, Path cachedFilePath) {
        // First try original URL
        String result = attemptDownload(originalUrl, cachedFilePath);
        if (result != null) {
            return result;
        }

        // If original URL fails, try switching between internal and external links
        String alternativeUrl = convertUrl(originalUrl);
        if (!alternativeUrl.equals(originalUrl)) {
            logger.info("Original URL failed, trying alternative URL: {}", alternativeUrl);
            result = attemptDownload(alternativeUrl, cachedFilePath);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Attempt to download file from specified URL
     *
     * @param url            Download URL
     * @param cachedFilePath Cached file path
     * @return Returns file path on success, null on failure
     */
    private static String attemptDownload(String url, Path cachedFilePath) {
        try {
            logger.info("Attempting to download from URL: {}", url);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.warn("Download failed with HTTP {}: {}", response.code(), url);
                    return null;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    logger.warn("Response body is null for URL: {}", url);
                    return null;
                }

                // Save file to cache directory
                try (InputStream inputStream = body.byteStream()) {
                    Files.copy(inputStream, cachedFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return cachedFilePath.toString();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception occurred while downloading from URL: {}, error: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Convert URL to switch between internal and external links
     * Internal links contain "-internal", external links do not
     *
     * @param originalUrl Original URL
     * @return Converted URL
     */
    private static String convertUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return originalUrl;
        }

        // Check if it's JD Cloud OSS URL format
        if (originalUrl.contains(".jdcloud-oss.com/")) {
            if (originalUrl.contains(".s3-internal.")) {
                // Internal to external: remove "-internal"
                String externalUrl = originalUrl.replace(".s3-internal.", ".s3.");
                logger.debug("Converting internal URL to external: {} -> {}", originalUrl, externalUrl);
                return externalUrl;
            } else if (originalUrl.contains(".s3.")) {
                // External to internal: add "-internal"
                String internalUrl = originalUrl.replace(".s3.", ".s3-internal.");
                logger.debug("Converting external URL to internal: {} -> {}", originalUrl, internalUrl);
                return internalUrl;
            }
        }

        // If not supported URL format, return original URL
        return originalUrl;
    }

    /**
     * Get file extension
     *
     * @param fileName File name
     * @param fileType MIME type
     * @return File extension (including dot)
     */
    private static String getFileExtension(String fileName, String fileType) {
        // Priority: get extension from file name
        if (fileName != null && fileName.contains(".")) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0 && lastDot < fileName.length() - 1) {
                return fileName.substring(lastDot);
            }
        }

        // Infer extension from MIME type
        if (fileType != null) {
            switch (fileType.toLowerCase()) {
                case "application/pdf":
                    return ".pdf";
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return ".docx";
                case "application/msword":
                    return ".doc";
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                    return ".xlsx";
                case "application/vnd.ms-excel":
                    return ".xls";
                case "text/plain":
                    return ".txt";
                case "image/jpeg":
                    return ".jpg";
                case "image/png":
                    return ".png";
                case "image/gif":
                    return ".gif";
                case "image/webp":
                    return ".webp";
                case "video/mp4":
                    return ".mp4";
                case "video/avi":
                    return ".avi";
                case "audio/mpeg":
                    return ".mp3";
                case "audio/wav":
                    return ".wav";
                default:
                    return ".bin"; // Default extension
            }
        }

        return ".bin"; // Default extension
    }

    /**
     * Generate hash value of URL for use as cached file name
     *
     * @param url URL string
     * @return Hash value string
     */
    private static String generateHash(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // If hash generation fails, use simple replacement method
            return url.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(32, url.length()));
        }
    }

    /**
     * Clean old files in cache directory
     *
     * @param maxAgeHours Maximum file retention time (hours)
     */
    public static void cleanCache(int maxAgeHours) {
        try {
            Path cacheDir = Paths.get(DEFAULT_CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                return;
            }

            long cutoffTime = System.currentTimeMillis() - (maxAgeHours * 60L * 60L * 1000L);

            Files.walk(cacheDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.info("Deleted old cached file: {}", path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete cached file: {}", path, e);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error cleaning cache", e);
        }
    }

    /**
     * Check if URL is a valid HTTP/HTTPS link
     *
     * @param url URL string
     * @return Whether it is a valid URL
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol().toLowerCase();
            return "http".equals(protocol) || "https".equals(protocol);
        } catch (Exception e) {
            return false;
        }
    }
}