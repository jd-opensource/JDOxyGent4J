/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * <h3>Image Base64 Encoding Conversion Utility Class</h3>
 *
 * <p>ImageBase64Converter is a utility class in the OxyGent framework specifically designed for Base64 encoding conversion of image files.
 * This utility class provides complete functionality for image file reading, encoding conversion, and format standardization,
 * ensuring that image data can be correctly passed to LLM models that support visual understanding.</p>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Base64 Encoding</strong>: Converts image files to standard Base64 encoded strings</li>
 *   <li><strong>Data URL Format</strong>: Generates complete Data URLs containing MIME types</li>
 *   <li><strong>MIME Type Recognition</strong>: Automatically identifies correct MIME types based on file extensions</li>
 *   <li><strong>Format Validation</strong>: Checks if files are in supported image formats</li>
 *   <li><strong>File Information Retrieval</strong>: Provides detailed information about image files</li>
 * </ul>
 *
 * <h3>Supported Image Formats</h3>
 * <ul>
 *   <li><strong>JPEG</strong>: jpg, jpeg - Standard JPEG compression format</li>
 *   <li><strong>PNG</strong>: png - Lossless format supporting transparency</li>
 *   <li><strong>GIF</strong>: gif - Image format supporting animation</li>
 *   <li><strong>BMP</strong>: bmp - Windows bitmap format</li>
 *   <li><strong>WebP</strong>: webp - Modern efficient compression format</li>
 *   <li><strong>SVG</strong>: svg - Vector graphics format</li>
 *   <li><strong>ICO</strong>: ico - Icon file format</li>
 *   <li><strong>TIFF</strong>: tiff, tif - High quality image format</li>
 * </ul>
 *
 * <h3>Data URL Format Standard</h3>
 * <p>Output Data URLs comply with RFC 2397 standard:</p>
 * <pre>
 * data:[mediatype][;base64],data
 *
 * Examples:
 * data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/...
 * data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB...
 * </pre>
 *
 * <h3>Error Handling Strategy</h3>
 * <ul>
 *   <li><strong>File Not Found</strong>: Returns original file path, logs warning</li>
 *   <li><strong>Read Failure</strong>: Returns original file path, logs error</li>
 *   <li><strong>Unsupported Format</strong>: Uses default MIME type (image/jpeg)</li>
 *   <li><strong>Encoding Exception</strong>: Returns original path to ensure system stability</li>
 * </ul>
 *
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li><strong>Memory Usage</strong>: Large image files will consume corresponding memory space</li>
 *   <li><strong>Encoding Overhead</strong>: Base64 encoding increases data size by approximately 33%</li>
 *   <li><strong>File Size Limitation</strong>: Recommend reasonable limits on input file sizes</li>
 *   <li><strong>Caching Strategy</strong>: Consider result caching for frequently used images</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li><strong>LLM Visual Input</strong>: Prepare image data for visual models like GPT-4V, Claude</li>
 *   <li><strong>Image Content Analysis</strong>: Convert local images to transmittable format</li>
 *   <li><strong>Multimodal Dialogue</strong>: Embed image content in conversations</li>
 *   <li><strong>Document Processing</strong>: Process documents containing images</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Basic conversion
 * String imagePath = "/path/to/image.jpg";
 * String dataUrl = ImageBase64Converter.convertToBase64(imagePath);
 * System.out.println(dataUrl);
 * // Output: data:image/jpeg;base64,/9j/4AAQSkZJRg...
 *
 * // Format check
 * boolean isSupported = ImageBase64Converter.isSupportedImageFormat("image.png");
 * System.out.println(isSupported); // true
 *
 * // MIME type retrieval
 * String mimeType = ImageBase64Converter.getMimeTypeFromPath("photo.webp");
 * System.out.println(mimeType); // image/webp
 *
 * // File information retrieval
 * String info = ImageBase64Converter.getImageInfo("picture.jpg");
 * System.out.println(info);
 * // Output: Image file: picture.jpg\nType: image/jpeg\nSize: 102400 bytes (100.00 KB)
 * }</pre>
 *
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>File Size Control</strong>: Recommend image files not exceed 5MB to avoid oversized Base64 strings</li>
 *   <li><strong>Format Selection</strong>: Prioritize JPEG and PNG formats for best compatibility</li>
 *   <li><strong>Error Handling</strong>: Always check return results and handle conversion failures</li>
 *   <li><strong>Performance Monitoring</strong>: Monitor conversion operation time and memory usage</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ImageBase64Converter {
    private static final Logger logger = LoggerFactory.getLogger(ImageBase64Converter.class);

    /**
     * Convert image file to Base64 Data URL format
     *
     * @param filePath Image file path
     * @return Base64 encoded Data URL, returns original path if conversion fails
     */
    public static String convertToBase64(String filePath) {
        try {
            // Read image file
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                logger.warn("Image file does not exist: {}", filePath);
                return filePath; // If file doesn't exist, return original path
            }

            // Read file content
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            // Convert to Base64
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            // Determine MIME type based on file extension
            String mimeType = getMimeTypeFromPath(filePath);

            // Return complete data URL format
            return "data:" + mimeType + ";base64," + base64Data;

        } catch (Exception e) {
            logger.error("Failed to convert image to Base64: {}", filePath, e);
            return filePath; // Return original path on error
        }
    }

    /**
     * Determine MIME type based on file path
     *
     * @param filePath File path
     * @return Corresponding MIME type
     */
    public static String getMimeTypeFromPath(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerPath.endsWith(".ico")) {
            return "image/x-icon";
        } else if (lowerPath.endsWith(".tiff") || lowerPath.endsWith(".tif")) {
            return "image/tiff";
        } else {
            return "image/jpeg"; // Default type
        }
    }

    /**
     * Check if file is in supported image format
     *
     * @param filePath File path
     * @return true if supported image format, false otherwise
     */
    public static boolean isSupportedImageFormat(String filePath) {
        String lowerPath = filePath.toLowerCase();
        return lowerPath.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg|ico|tiff|tif)$");
    }

    /**
     * Get basic information about image file
     *
     * @param filePath Image file path
     * @return String containing file information
     */
    public static String getImageInfo(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                return "Image file does not exist: " + filePath;
            }

            String mimeType = getMimeTypeFromPath(filePath);
            long fileSize = file.length();

            return String.format("Image file: %s\nType: %s\nSize: %d bytes (%.2f KB)",
                    file.getName(), mimeType, fileSize, fileSize / 1024.0);

        } catch (Exception e) {
            logger.error("Failed to get image information: {}", filePath, e);
            return "Failed to get image information: " + e.getMessage();
        }
    }

    /**
     * Compress image and convert to Base64 Data URL format
     * If original image size exceeds limit, it will be automatically compressed below specified size
     *
     * @param filePath     Image file path
     * @param maxSizeBytes Maximum file size (bytes), default 5MB
     * @return Base64 encoded Data URL, returns original path if conversion fails
     */
    public static String convertToBase64WithCompression(String filePath, long maxSizeBytes) {
        try {
            // Read image file
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                logger.warn("Image file does not exist: {}", filePath);
                return filePath;
            }

            // Check original file size
            long originalSize = file.length();
            logger.info("Original image size: {} bytes ({})", originalSize, formatFileSize(originalSize));

            // First try direct conversion, check Base64 size
            // Base64 encoding increases size by approximately 33%, so need to check actual encoded size
            String directConversion = convertToBase64(filePath);
            if (directConversion != null && !directConversion.equals(filePath)) {
                // Calculate Base64 encoded size (excluding data URL prefix)
                String base64Data = directConversion.substring(directConversion.indexOf(",") + 1);
                long base64Size = base64Data.length() * 3 / 4; // Size after Base64 decoding
                long encodedSize = directConversion.getBytes().length; // Actual transmission size

                logger.info("Base64 encoded size: {} bytes ({})", encodedSize, formatFileSize(encodedSize));

                if (encodedSize <= maxSizeBytes) {
                    logger.info("Base64 encoded size within limit, using directly");
                    return directConversion;
                } else {
                    logger.info("Base64 encoded size exceeds limit, need to compress original image");
                }
            }

            // Need compression
            logger.info("Image size exceeds limit {} bytes, starting compression", maxSizeBytes);

            // Read image
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                logger.error("Cannot read image file: {}", filePath);
                throw new RuntimeException("Cannot read image file, may not be a valid image format");
            }

            // Get MIME type and format
            String mimeType = getMimeTypeFromPath(filePath);
            String format = getImageFormat(filePath);

            // Try different compression strategies
            byte[] compressedBytes = compressImage(originalImage, format, maxSizeBytes);

            if (compressedBytes == null) {
                logger.error("Image compression failed: {}", filePath);
                throw new RuntimeException("Image compression failed, cannot compress image below specified size");
            }

            logger.info("Compressed image size: {} bytes ({})", compressedBytes.length, formatFileSize(compressedBytes.length));

            // Convert to Base64 and check final size
            String base64Data = Base64.getEncoder().encodeToString(compressedBytes);
            String result = "data:" + mimeType + ";base64," + base64Data;
            long finalSize = result.getBytes().length;

            logger.info("Final Base64 encoded size: {} bytes ({})", finalSize, formatFileSize(finalSize));

            if (finalSize > maxSizeBytes) {
                logger.warn("Compressed Base64 encoding still exceeds limit: {} > {}", finalSize, maxSizeBytes);
                throw new RuntimeException("Image still exceeds size limit after compression, cannot process this image");
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to compress and convert image to Base64: {}", filePath, e);
            throw new RuntimeException("Image compression processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Compress image to below specified size
     * Note: maxSizeBytes here refers to the final size limit after Base64 encoding
     *
     * @param originalImage Original image
     * @param format        Image format
     * @param maxSizeBytes  Maximum size after Base64 encoding
     * @return Compressed byte array, returns null if compression fails
     */
    private static byte[] compressImage(BufferedImage originalImage, String format, long maxSizeBytes) throws IOException {
        // Since Base64 encoding increases size by approximately 33%, we need to compress to smaller target
        // Actual target size = maxSizeBytes / 1.37 (considering Base64 encoding overhead and data URL prefix)
        long targetImageSize = (long) (maxSizeBytes * 0.7); // Reserve 30% margin for Base64 encoding
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        logger.info("Starting image compression: {}x{}, target Base64 size: {} bytes, target image size: {} bytes",
                originalWidth, originalHeight, maxSizeBytes, targetImageSize);

        // Try different compression strategies
        // Strategy 1: Reduce image quality (only effective for JPEG)
        if ("jpg".equals(format) || "jpeg".equals(format)) {
            for (float quality = 0.9f; quality >= 0.1f; quality -= 0.1f) {
                byte[] compressed = compressWithQuality(originalImage, quality);
                if (compressed != null && compressed.length <= targetImageSize) {
                    logger.info("Quality compression successful, quality: {}, compressed size: {} bytes", quality, compressed.length);
                    return compressed;
                }
            }
        }

        // Strategy 2: Scale image dimensions
        double scaleFactor = 0.9;
        while (scaleFactor >= 0.1) {
            int newWidth = (int) (originalWidth * scaleFactor);
            int newHeight = (int) (originalHeight * scaleFactor);

            // Ensure minimum dimensions
            if (newWidth < 50 || newHeight < 50) {
                break;
            }

            BufferedImage scaledImage = scaleImage(originalImage, newWidth, newHeight);

            // First try quality compression (if JPEG)
            if ("jpg".equals(format) || "jpeg".equals(format)) {
                for (float quality = 0.9f; quality >= 0.3f; quality -= 0.1f) {
                    byte[] compressed = compressWithQuality(scaledImage, quality);
                    if (compressed != null && compressed.length <= targetImageSize) {
                        logger.info("Dimension scaling ({}) and quality compression ({}) successful, compressed size: {} bytes", scaleFactor, quality, compressed.length);
                        return compressed;
                    }
                }
            } else {
                // Non-JPEG format, direct output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaledImage, format, baos);
                byte[] compressed = baos.toByteArray();
                if (compressed.length <= targetImageSize) {
                    logger.info("Dimension scaling ({}) successful, compressed size: {} bytes", scaleFactor, compressed.length);
                    return compressed;
                }
            }

            scaleFactor -= 0.1;
        }

        return null; // Compression failed
    }

    /**
     * Compress JPEG image with specified quality
     */
    private static byte[] compressWithQuality(BufferedImage image, float quality) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Use ImageIO's JPEG writer for quality compression
            var writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                return null;
            }

            var writer = writers.next();
            var ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            var param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();

            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("JPEG quality compression failed", e);
            return null;
        }
    }

    /**
     * Scale image
     */
    private static BufferedImage scaleImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Set high quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return scaledImage;
    }

    /**
     * Get image format based on file path
     */
    private static String getImageFormat(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "jpg";
        } else if (lowerPath.endsWith(".png")) {
            return "png";
        } else if (lowerPath.endsWith(".gif")) {
            return "gif";
        } else if (lowerPath.endsWith(".bmp")) {
            return "bmp";
        } else if (lowerPath.endsWith(".webp")) {
            return "webp";
        } else {
            return "jpg"; // Default format
        }
    }

    /**
     * Format file size display
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Compress image and convert to Base64 using default 5MB limit
     */
    public static String convertToBase64WithCompression(String filePath) {
        return convertToBase64WithCompression(filePath, 5 * 1024 * 1024); // 5MB
    }
}