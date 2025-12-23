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

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.codec.base.Base64UrlCodec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Common utility class
 *
 * <p>Provides commonly used utility methods in system development, including file processing,
 * media conversion, URL operations, data masking, tree structure printing, JSON processing, etc.
 * This utility class is the core infrastructure of the system and is widely used in various
 * business scenarios.</p>
 *
 * <p>Main functional modules:</p>
 * <ul>
 *     <li>System information retrieval: OS detection, MAC address acquisition, timestamp generation</li>
 *     <li>Data processing: list chunking, JSON extraction, data type filtering</li>
 *     <li>File processing: file reading, media format conversion, Base64 encoding</li>
 *     <li>Network operations: URL construction, HTTP requests, resource downloading</li>
 *     <li>Data masking: phone number, email, ID card, bank card number masking</li>
 *     <li>Tree structure: data visualization printing, hierarchical relationship display</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Get system information
 * String timestamp = CommonUtils.getTimestamp();
 * boolean isLinux = CommonUtils.isLinux();
 *
 * // Data chunking processing
 * List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
 * List<List<Integer>> chunks = CommonUtils.chunkList(data, 2);
 *
 * // Data masking
 * String maskedPhone = CommonUtils.maskPhone("13812345678");
 * String maskedEmail = CommonUtils.maskEmail("user@example.com");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class CommonUtils {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);
    private static final OkHttpClient httpClient = new OkHttpClient();

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 200;
    private static final int QUEUE_CAPACITY = 1024;
    private static final long KEEP_ALIVE_MS = 60_000; // 60 seconds
    private static final Pattern JSON_PATTERN = Pattern.compile("```[\\n]*json(.*?)```", Pattern.DOTALL);
    private static final Pattern JSONTOSTRING_PATTERN = Pattern.compile("\\{.*\\}", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern MIXEDSTRING_PATTERN = Pattern.compile("(!)?\\[([^\\]]*)\\]\\(([^)]+)\\)|([a-zA-Z0-9_./-]+\\.[a-zA-Z]{2,4})(?![a-zA-Z0-9_./-])");

    private static final ThreadFactory COMMON_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "commonutil-pool-" + index.getAndIncrement());
            t.setDaemon(true); // ← 关键修改
            t.setUncaughtExceptionHandler((thread, ex) ->
                    System.err.println("Uncaught exception in thread " + thread.getName() + ": " + ex)
            );
            return t;
        }
    };

    private static final ExecutorService executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_MS, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            COMMON_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy()
    );


    // ========== System Information ==========

    /**
     * Detect whether the current operating system is Linux
     *
     * <p>Determines whether the current runtime environment is a Linux operating system by checking the system property os.name.
     * This method is commonly used in cross-platform applications where different logic needs to be executed based on the operating system type.</p>
     *
     * @return Returns true if the current operating system is Linux, otherwise returns false
     * @since 1.0.0
     */
    public static boolean isLinux() {
        var osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("linux");
    }

    /**
     * Get local MAC address
     *
     * <p>Gets the MAC address of the local network interface for device uniqueness identification.
     * Returns a default MAC address format if acquisition fails.</p>
     *
     * @return MAC address string in format XX-XX-XX-XX-XX-XX，returns "00-00-00-00-00-00" if failed
     * @since 1.0.0
     */
    public static String getMacAddress() {
        try {
            var localHost = java.net.InetAddress.getLocalHost();
            var networkInterface = NetworkInterface.getByInetAddress(localHost);
            if (networkInterface == null) {
                return "00-00-00-00-00-00";
            }

            var mac = networkInterface.getHardwareAddress();
            if (mac == null || mac.length == 0) {
                return "00-00-00-00-00-00";
            }

            var sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("Failed to get MAC address: {}", e.getMessage());
            return "00-00-00-00-00-00";
        }
    }

    /**
     * Get current Unix timestamp (in seconds)
     *
     * @return Current Unix timestamp string (in seconds)
     * @since 1.0.0
     */
    public static String getTimestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    /**
     * Get formatted current time (accurate to nanoseconds)
     *
     * <p>Returns a time string in format yyyy-MM-dd HH:mm:ss.nnnnnnnnn,
     * where the nanosecond part is fixed to 9 digits. This method is suitable for scenarios requiring high-precision time recording.</p>
     *
     * @return Formatted time string with nanosecond precision
     * @since 1.0.0
     */
    public static String getFormatTime() {
        var now = LocalDateTime.now();
        var nanoStr = String.format("%09d", now.getNano());
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.")) + nanoStr;
    }

    // ========== Data Processing ==========

    /**
     * Split list into chunks of specified size
     *
     * <p>Splits a large list into multiple smaller lists, each with a size not exceeding the specified chunkSize.
     * The last chunk may contain fewer than chunkSize elements. This method is useful for batch processing large datasets.</p>
     *
     * @param <T>       List element type
     * @param list      The original list to be chunked, cannot be null
     * @param chunkSize The size of each chunk, must be greater than 0
     * @return Collection of chunked lists, each sublist contains no more than chunkSize elements
     * @throws IllegalArgumentException When list is null or chunkSize is less than or equal to 0
     * @since 1.0.0
     */
    public static <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        Objects.requireNonNull(list, "List cannot be null");
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }

        var chunks = new ArrayList<List<T>>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(new ArrayList<>(list.subList(i, Math.min(i + chunkSize, list.size()))));
        }
        return chunks;
    }

    /**
     * Extract the first JSON string from text
     *
     * <p>This method first attempts to extract JSON content from Markdown code blocks (```json ... ```),
     * if no code block is found, it directly extracts the first complete JSON object from the original text.
     * Mainly used for parsing documents or response content containing JSON data.</p>
     *
     * @param text Original text containing JSON content, cannot be null
     * @return Extracted JSON string, returns original processing result if no valid JSON is found
     * @throws IllegalArgumentException When text is null
     * @since 1.0.0
     */
    public static String extractFirstJson(String text) {
        Objects.requireNonNull(text, "Text cannot be null");

        var matcher = JSON_PATTERN.matcher(text);
        var jsonTexts = new ArrayList<String>();

        while (matcher.find()) {
            jsonTexts.add(matcher.group(1).trim());
        }

        var jsonText = jsonTexts.isEmpty() ? text : jsonTexts.get(0);
        var start = jsonText.indexOf('{');
        var end = jsonText.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            jsonText = jsonText.substring(start, end + 1);
        }

        return jsonText;
    }

    /**
     * Extract JSON string from text
     *
     * <p>Uses regular expressions to extract the first complete JSON object string from the given text.
     * This method matches all content from the first '{' to the last '}'.</p>
     *
     * @param text Text containing JSON content, cannot be null or empty string
     * @return Extracted JSON string
     * @throws IllegalArgumentException When text is null, empty string, or no valid JSON is found
     * @since 1.0.0
     */
    public static String extractJsonStr(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty string");
        }

        var matcher = JSONTOSTRING_PATTERN.matcher(text.trim());

        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new IllegalArgumentException("Unable to extract JSON string from output: " + text);
        }
    }

    // ========== File/Network Operations ==========

    /**
     * Convert resource source (file path or URL) to byte array
     *
     * <p>This method can handle both local file paths and HTTP/HTTPS URL resource types.
     * For HTTP resources, it sends GET requests to obtain content; for local files, it directly reads file content.</p>
     *
     * @param source Resource source, can be local file path or HTTP/HTTPS URL, cannot be null or empty string
     * @return Byte array of resource content
     * @throws IllegalArgumentException When source is null or empty string
     * @throws RuntimeException         When resource reading fails
     * @since 1.0.0
     */
    public static byte[] sourceToBytes(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource source cannot be null or empty string");
        }

        try {
            if (source.startsWith("http")) {
                var request = new Request.Builder().url(source).build();
                try (var response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP request failed, status code: " + response.code() + ", URL: " + source);
                    }
                    var body = response.body();
                    if (body == null) {
                        throw new IOException("Response body is empty, URL: " + source);
                    }
                    return body.bytes();
                }
            } else {
                return Files.readAllBytes(Paths.get(source));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource: " + source, e);
        }
    }

    /**
     * Convert image to Base64 encoded string
     *
     * <p>This method converts image files (local or remote) to Base64 encoded Data URL format.
     * If the image pixel count exceeds the maximum limit, it will automatically perform proportional scaling to reduce image size.</p>
     *
     * @param source         Image source, can be file path or URL, cannot be null or empty string
     * @param maxImagePixels Maximum allowed pixel count for controlling image size, must be greater than 0
     * @return Base64 encoded Data URL string in format "data:image;base64,..."
     * @throws IllegalArgumentException When parameters are invalid
     * @throws RuntimeException         When image processing fails
     * @since 1.0.0
     */
    public static String imageToBase64(String source, int maxImagePixels) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Image source cannot be null or empty string");
        }
        if (maxImagePixels <= 0) {
            throw new IllegalArgumentException("Maximum pixel count must be greater than 0");
        }

        var imageBytes = sourceToBytes(source);
        try {
            var img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) {
                throw new RuntimeException("Unable to read image file, may not be a valid image format: " + source);
            }

            var width = img.getWidth();
            var height = img.getHeight();
            var currentPixels = width * height;

            // If exceeds maximum pixel limit, perform proportional scaling
            if (currentPixels > maxImagePixels) {
                var scale = Math.sqrt((double) maxImagePixels / currentPixels);
                var newWidth = Math.max(1, (int) (width * scale));
                var newHeight = Math.max(1, (int) (height * scale));

                var scaledImg = new BufferedImage(newWidth, newHeight, img.getType());
                var graphics = scaledImg.getGraphics();
                graphics.drawImage(img, 0, 0, newWidth, newHeight, null);
                graphics.dispose();
                img = scaledImg;
            }

            var baos = new ByteArrayOutputStream();
            var format = source.toLowerCase().endsWith(".png") ? "png" : "jpeg";
            ImageIO.write(img, format, baos);
            var outputBytes = baos.toByteArray();

            return "data:image;base64," + Base64.getEncoder().encodeToString(outputBytes);
        } catch (Exception e) {
            throw new RuntimeException("Image processing failed: " + source, e);
        }
    }

    public static String videoToBase64(String source, long maxVideoSize) {
        byte[] videoBytes = sourceToBytes(source);
        if (videoBytes.length > maxVideoSize) {
            return source;
        } else {
            return "data:video;base64," + Base64.getEncoder().encodeToString(videoBytes);
        }
    }

    public static String tableToBase64(String source, long maxTableSize) {
        byte[] tableBytes = sourceToBytes(source);
        if (tableBytes.length > maxTableSize) {
            throw new IllegalArgumentException("Table file size (" + tableBytes.length +
                    " bytes) exceeds maximum allowed size (" + maxTableSize + " bytes)");
        }

        String fileExt = getFileExtension(source).toLowerCase();
        Map<String, String> mimeMap = Map.of(
                ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ".xls", "application/vnd.ms-excel",
                ".csv", "text/csv",
                ".tsv", "text/tab-separated-values",
                ".ods", "application/vnd.oasis.opendocument.spreadsheet"
        );
        String mimeType = mimeMap.getOrDefault(fileExt, "application/octet-stream");

        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(tableBytes);


    }

    public static String fileToBase64(String source, long maxFileSize) {
        byte[] fileBytes = sourceToBytes(source);
        if (fileBytes.length > maxFileSize) {
            return source;
        }
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(Paths.get(source));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);

    }

    public static boolean validateTableFile(String filePath) {
        Set<String> supportedExtensions = Set.of(".xlsx", ".xls", ".csv", ".tsv", ".ods");
        return supportedExtensions.contains(getFileExtension(filePath).toLowerCase());
    }

    public static Map<String, Object> getTableFileInfo(String filePath) {
        Map<String, Object> info = new HashMap<>();
        try {
            if (!Files.exists(Paths.get(filePath)) && !filePath.startsWith("http")) {
                info.put("error", "File not found");
                return info;
            }

            Long fileSize = filePath.startsWith("http") ? null : Files.size(Paths.get(filePath));
            String fileExt = getFileExtension(filePath).toLowerCase();

            info.put("filename", new File(filePath).getName());
            info.put("extension", fileExt.substring(1)); // Remove the dot
            info.put("size", fileSize);
            info.put("is_supported", validateTableFile(filePath));
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }

    // ========== URL Operations ==========

    public static String appendUrlPath(String url, String path) throws URISyntaxException {
        URI uri = new URI(url);
        String finalPath = uri.getPath().replaceAll("/$", "") + "/" + path.replaceAll("^/", "");
        return new URI(uri.getScheme(), uri.getAuthority(), finalPath, uri.getQuery(), uri.getFragment()).toString();
    }

    public static String buildUrl(String baseUrl, String path, Map<String, Object> queryParams) throws URISyntaxException {
        URI baseUri = new URI(baseUrl);
        String finalPath = baseUri.getPath();
        if (path != null && !path.isEmpty()) {
            finalPath = finalPath.replaceAll("/$", "") + "/" + path.replaceAll("^/", "");
        }

        Map<String, String> mergedQuery = new HashMap<>();
        if (baseUri.getQuery() != null) {
            Arrays.stream(baseUri.getQuery().split("&"))
                    .forEach(param -> {
                        String[] parts = param.split("=", 2);
                        mergedQuery.put(parts[0], parts.length > 1 ? parts[1] : "");
                    });
        }
        if (queryParams != null) {
            queryParams.forEach((k, v) -> mergedQuery.put(k, v != null ? v.toString() : ""));
        }

        String finalQuery = mergedQuery.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        return new URI(baseUri.getScheme(), baseUri.getAuthority(), finalPath, finalQuery, baseUri.getFragment()).toString();
    }

    // ========== Tree Structure Printing ==========

    public static void printTree(Map<String, Object> node, String prefix, boolean isRoot, boolean isLast, Logger logger) {
        String branch = isLast ? "└── " : "├── ";
        if (isRoot) {
            branch = "";
        }
        String line = prefix + branch + node.getOrDefault("name", "");

        if (logger != null) {
            logger.info(line);
        } else {
            System.out.println(line);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.getOrDefault("children", List.of());
        for (int i = 0; i < children.size(); i++) {
            boolean childIsLast = i == children.size() - 1;
            String extension = isLast ? "    " : "│   ";
            if (isRoot) {
                extension = "";
            }
            printTree(children.get(i), prefix + extension, false, childIsLast, logger);
        }
    }

    public static void printTree(Map<String, Object> node, Logger logger) {
        printTree(node, "", true, true, logger);
    }

    // ========== JSON and Serialization ==========

    public static Map<String, Object> filterJsonTypes(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Boolean ||
                    value instanceof List || value instanceof Map || value == null) {
                result.put(entry.getKey(), value);
            } else {
                result.put(entry.getKey(), "...");
            }
        }
        return result;
    }

    public static Object msgpackPreprocess(Object obj) {
        if (obj == null || obj instanceof Boolean || obj instanceof Number || obj instanceof String || obj instanceof byte[]) {
            return obj;
        } else if (obj instanceof List || obj instanceof Set) {
            List<Object> newList = new ArrayList<>();
            for (Object item : (Iterable<?>) obj) {
                newList.add(msgpackPreprocess(item));
            }
            return newList;
        } else if (obj instanceof Map) {
            Map<String, Object> newMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                newMap.put(String.valueOf(entry.getKey()), msgpackPreprocess(entry.getValue()));
            }
            return newMap;
        } else {
            return obj.toString();
        }
    }

    public static String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Attachment Processing ==========

    public static List<Map<String, Object>> processAttachments(List<String> attachments) {
        List<Map<String, Object>> queryAttachments = new ArrayList<>();

        Set<String> imageExts = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".tiff");
        Set<String> videoExts = Set.of(".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mkv");
        Set<String> tableExts = Set.of(".xlsx", ".xls", ".csv", ".tsv", ".ods");
        Set<String> docExts = Set.of(".doc", ".docx");
        Set<String> pdfExts = Set.of(".pdf");
        Set<String> codeExts = Set.of(".py", ".md", ".json", ".txt");

        for (String attachment : attachments) {
            if (!(attachment.startsWith("http") || Files.exists(Paths.get(attachment)))) {
                logger.warn("Attachment file not found: {}", attachment);
                continue;
            }

            String ext = getFileExtension(attachment).toLowerCase();

            Map<String, Object> attachmentMap = new HashMap<>();
            if (imageExts.contains(ext)) {
                attachmentMap.put("type", "image_url");
                attachmentMap.put("image_url", Map.of("url", attachment));
            } else if (videoExts.contains(ext)) {
                attachmentMap.put("type", "video_url");
                attachmentMap.put("video_url", Map.of("url", attachment));
            } else if (tableExts.contains(ext)) {
                attachmentMap.put("type", "table_file");
                attachmentMap.put("table_file", Map.of("url", attachment, "format", ext.substring(1)));
            } else if (docExts.contains(ext)) {
                attachmentMap.put("type", "doc_file");
                attachmentMap.put("doc_file", Map.of("url", attachment, "format", ext.substring(1)));
            } else if (pdfExts.contains(ext)) {
                attachmentMap.put("type", "pdf_file");
                attachmentMap.put("pdf_file", Map.of("url", attachment, "format", "pdf"));
            } else if (codeExts.contains(ext)) {
                attachmentMap.put("type", "code_file");
                attachmentMap.put("code_file", Map.of("url", attachment, "format", ext.substring(1)));
            } else {
                attachmentMap.put("type", "file");
                attachmentMap.put("file", Map.of("url", attachment, "format", ext.substring(1)));
            }
            queryAttachments.add(attachmentMap);
        }

        return queryAttachments;
    }

    public static List<Map<String, Object>> composeQueryParts(Object originalQuery, List<String> attachments) {
        List<Map<String, Object>> parts = new ArrayList<>();

        // Add attachments
        for (String p : attachments) {
            String ctype = p.startsWith("http") ? "url" : "path";
            parts.add(Map.of("part", Map.of("content_type", ctype, "data", p)));
        }

        // Process original query
        if (originalQuery instanceof List) {
            parts.addAll((List<Map<String, Object>>) originalQuery);
        } else if (originalQuery instanceof Map) {
            parts.add((Map<String, Object>) originalQuery);
        } else {
            parts.add(Map.of("part", Map.of("content_type", "text/plain", "data", String.valueOf(originalQuery))));
        }

        return parts;
    }

    // ========== Resource Cleanup ==========
    public static void shutdown() {
        executor.shutdown();
    }

    // ... existing methods ...

    /**
     * Generate short UUID and return it encoded with Base64Url, timestamp indexed.
     *
     * @return Base64Url encoded short UUID string, 22 characters.
     */
    public static String generateShortUUID() {
        UUID uuid = UuidCreator.getShortPrefixComb();
        return new Base64UrlCodec().encode(uuid);
    }

    /**
     * Serialize object to JSON string.
     */
    public static String toJson(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return JsonUtils.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * Create a new HashMap.
     */
    public static <K, V> Map<K, V> newHashMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Convert Java object to map
     *
     * @param obj          Object to convert
     * @param camelOrSnake Naming convention: "camel" for camelCase, "snake" for snake_case
     * @return Converted map
     */
    public static Map<String, Object> objectToMap(Object obj, String camelOrSnake) {
        if (obj == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true); // Set private attributes accessible
            try {
                Object value = field.get(obj); // Get attribute value
                String key = field.getName();
                if ("snake".equals(camelOrSnake)) {
                    key = JsonUtils.toSnakeCase(key);
                } else if ("camel".equals(camelOrSnake)) {
                    key = JsonUtils.toCamelCase(key);
                }
                if (value != null && !isSimpleType(value)) { // If value is object and not simple type, recursively convert it
                    map.put(key, objectToMap(value, camelOrSnake)); // Recursively call objectToMap method to convert object value to Map
                } else {
                    map.put(key, value); // Simple types are directly put into Map
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * Determine if it is a simple type (primitive types and their wrapper classes, String, etc.). Extend as needed.
     *
     * @param value Value to check
     * @return true if it is a simple type, false otherwise
     */
    public static boolean isSimpleType(Object value) {
        return value instanceof String || value instanceof Date || value instanceof Timestamp ||
                value instanceof TimeZone || value instanceof Number || value instanceof Boolean;
    }

    // Generate 16-digit short UUID
    public static String generateShortUUID(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    // ========== Data Masking Tools ==========

    /**
     * Phone number masking regular expression
     * Matches various phone number formats: 13812345678, 138-1234-5678, 138 1234 5678, +86 138 1234 5678, etc.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?86[-\\s]?)?" +                    // Optional +86 country code
                    "(?:1[3-9]\\d)" +                         // First 3 digits of phone number
                    "[-\\s]?" +                               // Optional separator
                    "(\\d{4})" +                              // Middle 4 digits
                    "[-\\s]?" +                               // Optional separator
                    "(\\d{4})"                                // Last 4 digits
    );

    /**
     * Email masking regular expression
     * Matches standard email format
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+)" +                   // Username part
                    "@" +                                     // @ symbol
                    "([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"        // Domain part
    );

    /**
     * ID card number masking regular expression
     * Matches 18-digit ID card number
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "(\\d{6})" +                              // First 6 digits area code
                    "(\\d{8})" +                              // Middle 8 digits birthday
                    "(\\d{3}[0-9Xx])"                         // Last 4 digits sequence number and check code
    );

    /**
     * Bank card number masking regular expression
     * Matches various bank card number formats
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile(
            "(\\d{4})" +                              // First 4 digits
                    "[\\s-]?" +                               // Optional separator
                    "(\\d{4,})" +                             // Middle part (4 or more digits)
                    "[\\s-]?" +                               // Optional separator
                    "(\\d{4})"                                // Last 4 digits
    );

    /**
     * Mask phone number
     * Replace middle 4 digits of phone number with ****
     *
     * @param phone Original phone number
     * @return Masked phone number, e.g., 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        Matcher matcher = PHONE_PATTERN.matcher(phone);
        if (matcher.find()) {
            return matcher.replaceAll(matcher.group().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }
        return phone;
    }

    /**
     * Mask email address
     * Keep first 2 and last 1 characters of username, keep domain completely
     *
     * @param email Original email address
     * @return Masked email address, e.g., ab****c@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.find()) {
            String username = matcher.group(1);
            String domain = matcher.group(2);

            if (username.length() <= 3) {
                // Username too short, only show first character
                return username.charAt(0) + "***@" + domain;
            } else {
                // Keep first 2 and last 1 characters
                return username.substring(0, 2) + "****" + username.substring(username.length() - 1) + "@" + domain;
            }
        }
        return email;
    }

    /**
     * Mask ID card number
     * Keep first 6 and last 4 digits, replace middle 8 digits with ****
     *
     * @param idCard Original ID card number
     * @return Masked ID card number, e.g., 110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }

        Matcher matcher = ID_CARD_PATTERN.matcher(idCard);
        if (matcher.find()) {
            return matcher.group(1) + "********" + matcher.group(3);
        }
        return idCard;
    }

    /**
     * Mask bank card number
     * Keep first 4 and last 4 digits, replace middle with ****
     *
     * @param bankCard Original bank card number
     * @return Masked bank card number, e.g., 6222****1234
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.trim().isEmpty()) {
            return bankCard;
        }

        // Remove all non-digit characters for matching
        String cleanCard = bankCard.replaceAll("[^0-9]", "");
        if (cleanCard.length() >= 8) {
            String prefix = cleanCard.substring(0, 4);
            String suffix = cleanCard.substring(cleanCard.length() - 4);
            return prefix + "****" + suffix;
        }
        return bankCard;
    }

    /**
     * General text masking method
     * Automatically identify and mask phone numbers, emails, ID card numbers, and bank card numbers in text
     *
     * @param text Original text
     * @return Masked text
     */
    public static String maskSensitiveInfo(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String result = text;

        // Mask phone numbers
        result = PHONE_PATTERN.matcher(result).replaceAll(matchResult -> {
            String phone = matchResult.group();
            return maskPhone(phone);
        });

        // Mask email addresses
        result = EMAIL_PATTERN.matcher(result).replaceAll(matchResult -> {
            String email = matchResult.group();
            return maskEmail(email);
        });

        // Mask ID card numbers
        result = ID_CARD_PATTERN.matcher(result).replaceAll(matchResult -> {
            String idCard = matchResult.group();
            return maskIdCard(idCard);
        });

        // Mask bank card numbers
        result = BANK_CARD_PATTERN.matcher(result).replaceAll(matchResult -> {
            String bankCard = matchResult.group();
            return maskBankCard(bankCard);
        });

        return result;
    }


    /**
     * Intelligent masking based on field name
     * Determine possible sensitive information type based on field name and apply corresponding masking
     *
     * @param fieldName Field name
     * @param value     Field value
     * @return Masked value
     */
    public static String maskByFieldName(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String lowerFieldName = fieldName.toLowerCase();

        // Determine masking type based on field name
        if (lowerFieldName.contains("phone") || lowerFieldName.contains("mobile") ||
                lowerFieldName.contains("tel") || lowerFieldName.contains("手机")) {
            return maskPhone(value);
        } else if (lowerFieldName.contains("email") || lowerFieldName.contains("mail") ||
                lowerFieldName.contains("邮箱")) {
            return maskEmail(value);
        } else if (lowerFieldName.contains("idcard") || lowerFieldName.contains("identity") ||
                lowerFieldName.contains("身份证")) {
            return maskIdCard(value);
        } else if (lowerFieldName.contains("bank") || lowerFieldName.contains("card") ||
                lowerFieldName.contains("银行卡")) {
            return maskBankCard(value);
        } else {
            // Default to general masking
            return maskSensitiveInfo(value);
        }
    }

    /**
     * Parse mixed string
     */
    public static List<Map<String, Object>> parseMixedString(String content) {

        // Map file extensions to content types
        Map<String, String> urlToExt = new HashMap<>();
        urlToExt.put("image_url", String.join(",", "png", "jpg", "jpeg", "gif", "svg", "bmp", "webp", "tiff"));
        urlToExt.put("video_url", String.join(",", "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv"));

        Map<String, String> extToUrl = new HashMap<>();
        for (Map.Entry<String, String> entry : urlToExt.entrySet()) {
            String[] exts = entry.getValue().split(",");
            for (String ext : exts) {
                extToUrl.put(ext, entry.getKey());
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int lastEnd = 0;

        Matcher matcher = MIXEDSTRING_PATTERN.matcher(content);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Add text before match
            if (start > lastEnd) {
                String text = content.substring(lastEnd, start);
                if (!text.trim().isEmpty()) {
                    results.add(Map.of("type", "text", "content", text));
                }
            }

            // Process file/media
            String isImage = matcher.group(1);
            String desc = matcher.group(2);
            String link = matcher.group(3);
            String plainFilePath = matcher.group(4); // plain file path without brackets

            if (link != null) {
                // Markdown format: ![desc](link) or [desc](link)
                String fileExt = link.contains(".") ? link.substring(link.lastIndexOf(".") + 1).toLowerCase() : "";
                String contentType = extToUrl.getOrDefault(fileExt, "doc_url");

                Map<String, Object> mediaItem = new HashMap<>();
                mediaItem.put("type", contentType);
                mediaItem.put("content", (isImage != null ? "!" : "") + "[" + desc + "](" + link + ")");
                mediaItem.put("desc", desc);
                mediaItem.put("link", link);
                results.add(mediaItem);
            } else if (plainFilePath != null) {
                // Plain file path format: ./cache_dir/xfile/uploads/20251117132641_b7d11e6b-20da-4dad-b6cf-f3af345647a5.png
                String fileExt = plainFilePath.contains(".") ? plainFilePath.substring(plainFilePath.lastIndexOf(".") + 1).toLowerCase() : "";
                String contentType = extToUrl.getOrDefault(fileExt, "doc_url");

                // Extract filename as description
                String fileName = plainFilePath.contains("/") ? plainFilePath.substring(plainFilePath.lastIndexOf("/") + 1) : plainFilePath;
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                }

                Map<String, Object> mediaItem = new HashMap<>();
                mediaItem.put("type", contentType);
                mediaItem.put("content", plainFilePath);
                mediaItem.put("desc", fileName);
                mediaItem.put("link", plainFilePath);
                results.add(mediaItem);
            }

            lastEnd = end;
        }

        // Add remaining text
        if (lastEnd < content.length()) {
            String remainingText = content.substring(lastEnd);
            if (!remainingText.trim().isEmpty()) {
                results.add(Map.of("type", "text", "content", remainingText));
            }
        }

        // If no matches found, return entire content as text
        if (results.isEmpty()) {
            results.add(Map.of("type", "text", "content", content));
        }

        return results;
    }

    /**
     * Convert image URL to base64
     */
    public static String imageToBase64V2(String imageUrl, int maxPixels) {
        try {
            // Handle local file paths
            if (imageUrl.startsWith("file://")) {
                String filePath = imageUrl.substring(7);
                return convertLocalImageToBase64(filePath, maxPixels);
            }

            // Handle HTTP URLs (would need to implement HTTP client)
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                // For now, return original URL - would need HTTP client implementation
                return imageUrl;
            }

            // Handle local file paths without file:// prefix
            if (Files.exists(Paths.get(imageUrl))) {
                return convertLocalImageToBase64(imageUrl, maxPixels);
            }

            return imageUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert image to base64: " + imageUrl, e);
        }
    }

    /**
     * Convert local image file to base64
     */
    private static String convertLocalImageToBase64(String filePath, int maxPixels) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Image file not found: " + filePath);
        }

        // Check file size
        long fileSize = Files.size(path);
        if (fileSize > maxPixels * 3) { // Rough estimate: 3 bytes per pixel
            throw new IOException("Image file too large: " + fileSize + " bytes");
        }

        // Read and convert to base64
        byte[] imageBytes = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        // Detect MIME type
        String mimeType = detectImageMimeType(filePath);

        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * Detect image MIME type from file extension
     */
    private static String detectImageMimeType(String filePath) {
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
        } else {
            return "image/jpeg"; // Default
        }
    }
}
