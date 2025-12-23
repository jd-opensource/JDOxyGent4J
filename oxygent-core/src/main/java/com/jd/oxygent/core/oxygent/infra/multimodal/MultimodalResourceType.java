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
package com.jd.oxygent.core.oxygent.infra.multimodal;

import com.jd.oxygent.core.oxygent.schemas.FilesInfo;
import com.jd.oxygent.core.oxygent.utils.FileDownloadUtil;
import com.jd.oxygent.core.oxygent.utils.ImageBase64Converter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h3>Multimodal Resource Type Processing Strategy Enumeration</h3>
 *
 * <p>MultimodalResourceType is a core strategy enumeration class for handling multimodal resources
 * in the OxyGent framework. This class adopts the strategy pattern design, providing unified
 * processing interfaces for different types of multimodal resources (URLs, files, etc.),
 * ensuring various media types can be correctly converted to standard formats understandable by LLM.</p>
 *
 * <h3>Design Patterns</h3>
 * <ul>
 *   <li><strong>Strategy Pattern</strong>: Each enumeration value implements specific resource processing strategy</li>
 *   <li><strong>Factory Pattern</strong>: Creates appropriate processors through fromResourceType method</li>
 *   <li><strong>Chain Processing</strong>: Supports automatic recognition and chain conversion of resource types</li>
 *   <li><strong>Extensible Design</strong>: New resource types can be easily added to the enumeration</li>
 * </ul>
 *
 * <h3>Supported Resource Types</h3>
 * <ul>
 *   <li><strong>URLS</strong>: Network image links, converted to image_url format for LLM visual understanding</li>
 *   <li><strong>FILES</strong>: Local file paths, intelligently recognized and processed based on extensions</li>
 *   <li><strong>DEFAULT</strong>: Default processing strategy, converts unknown types to text format</li>
 * </ul>
 *
 * <h3>File Type Recognition Capabilities</h3>
 * <ul>
 *   <li><strong>Image files</strong>: jpg, jpeg, png, gif, bmp, webp → Base64 encoding</li>
 *   <li><strong>Video files</strong>: mp4, avi, mov, wmv, flv, webm → Video URL format</li>
 *   <li><strong>Audio files</strong>: mp3, wav, flac, aac, ogg → Audio URL format</li>
 *   <li><strong>PDF files</strong>: pdf → Text content extraction</li>
 *   <li><strong>Word documents</strong>: doc, docx, txt, rtf → Text content extraction</li>
 *   <li><strong>Excel spreadsheets</strong>: xls, xlsx → Table content extraction and formatting</li>
 *   <li><strong>CSV files</strong>: csv → Table content extraction and formatting</li>
 * </ul>
 *
 * <h3>Processing Flow</h3>
 * <ol>
 *   <li><strong>Resource Recognition</strong>: Select corresponding processing strategy based on resource type identifier</li>
 *   <li><strong>Format Detection</strong>: Analyze file extension or URL format to determine specific type</li>
 *   <li><strong>Content Extraction</strong>: Call specialized extractors to obtain resource content</li>
 *   <li><strong>Format Conversion</strong>: Convert extracted content to LLM standard format</li>
 *   <li><strong>Result Encapsulation</strong>: Return standardized mapping containing type and content</li>
 * </ol>
 *
 * <h3>Output Format Standards</h3>
 * <p>All processing strategies follow unified output format:</p>
 * <ul>
 *   <li><strong>type field</strong>: Clearly identifies content type (text, image_url, video, audio, etc.)</li>
 *   <li><strong>content field</strong>: Contains corresponding data fields based on type</li>
 *   <li><strong>LLM compatibility</strong>: Output format is compatible with mainstream LLM APIs (OpenAI, Claude, etc.)</li>
 * </ul>
 *
 * <h3>Integrated Processing Tools</h3>
 * <ul>
 *   <li><strong>ImageBase64Converter</strong>: Image file Base64 encoding conversion</li>
 *   <li><strong>PdfTextExtractor</strong>: PDF document text content extraction</li>
 *   <li><strong>WordTextExtractor</strong>: Word document text content extraction</li>
 *   <li><strong>ExcelTableExtractor</strong>: Excel spreadsheet structured content extraction</li>
 *   <li><strong>CsvTableExtractor</strong>: CSV spreadsheet structured content extraction</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li><strong>Multimedia Analysis</strong>: Process multimodal input containing images, videos, audio</li>
 *   <li><strong>Document Understanding</strong>: Convert various format documents to analyzable text</li>
 *   <li><strong>Content Aggregation</strong>: Uniformly process multiple resource types from different sources</li>
 *   <li><strong>Intelligent Parsing</strong>: Automatically recognize resource types and select optimal processing methods</li>
 * </ul>
 *
 * <h3>Extensibility Design</h3>
 * <ul>
 *   <li><strong>New Type Addition</strong>: Can easily add new resource type enumeration values</li>
 *   <li><strong>Processing Logic Customization</strong>: Each type can independently implement specific processing logic</li>
 *   <li><strong>Tool Integration</strong>: Can integrate new specialized processing tools</li>
 *   <li><strong>Format Extension</strong>: Supports new file formats and media types</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Process image URL list
 * List<String> imageUrls = Arrays.asList("https://example.com/image1.jpg", "https://example.com/image2.png");
 * MultimodalResourceType urlType = MultimodalResourceType.fromResourceType("urls");
 * List<Map<String, Object>> urlResults = urlType.processResources(imageUrls);
 *
 * // Process local file list
 * List<String> files = Arrays.asList("/path/to/document.pdf", "/path/to/image.jpg", "/path/to/data.xlsx");
 * MultimodalResourceType fileType = MultimodalResourceType.fromResourceType("files");
 * List<Map<String, Object>> fileResults = fileType.processResources(files);
 *
 * // Output format example:
 * // [{type=image_url, image_url={url=https://example.com/image1.jpg}},
 * //  {type=text, text=PDF document content...},
 * //  {type=text, text=Excel spreadsheet content...}]
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Getter
public enum MultimodalResourceType {

    /**
     * Represents a list of URLs pointing to resources, typically images.
     * <p>
     * Processes the input list of URLs by converting each URL into a structured map with "type" as "image_url"
     * and the actual URL under "image_url".
     * If the URL is null or invalid, it is filtered out from the final output.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code [{ "type": "image_url", "image_url": { "url": "http://example.com/image.jpg" } }, ...]}</pre>
     * </p>
     */
    URLS("urls") {
        @Override
        public List<Map<String, Object>> processResources(List<?> resourceList, boolean isConvertUrlToBase64) {
            return resourceList.stream()
                    .filter(Objects::nonNull)
                    .map(url -> {
                        String urlString = url.toString();
                        return Map.<String, Object>of("type", "image_url", "image_url", Map.of("url", urlString));
                    })
                    .collect(Collectors.toList());
        }
    },

    /**
     * Represents a list of local or remote files.
     * <p>
     * Processes the input list of files. For each file:
     * <ul>
     *   <li>If the file is an instance of {@code FilesInfo}, it attempts to download the file from its URL.</li>
     *   <li>Determines the file type based on its extension and processes accordingly (e.g., convert images to base64).</li>
     *   <li>Returns a structured map with details about the file, including any extracted text if applicable.</li>
     * </ul>
     * If processing fails, returns an error message.
     * </p>
     * <p><b>Output format examples:</b>
     * <pre>{@code
     * // For an image file:
     * { "type": "image_url", "image_url": { "url": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..." } }
     *
     * // For a PDF file:
     * { "type": "text", "text": "Extracted text from the PDF..." }
     * }</pre>
     * </p>
     */
    FILES("files") {
        @Override
        public List<Map<String, Object>> processResources(List<?> resourceList, boolean isConvertUrlToBase64) {
            return resourceList.stream()
                    .filter(Objects::nonNull)
                    .map(file -> {
                        try {
                            String localFilePath = "";
                            String fileName = "";
                            log.info("===============" + file.getClass() + "===============");
                            if (file instanceof FilesInfo) {
                                FilesInfo tmpFile = (FilesInfo) file;
                                String fileUrl = tmpFile.getFileUrl();
                                fileName = tmpFile.getFileName();
                                String fileType = tmpFile.getFileType();

                                // Download file from URL to local cache
                                logger.info("Processing file: {} from URL: {}", fileName, fileUrl);
                                localFilePath = FileDownloadUtil.downloadFile(fileUrl, fileName, fileType);

                                if (localFilePath == null) {
                                    logger.error("Failed to download file from URL: {}", fileUrl);
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", "File download failed: " + fileName
                                    );
                                }
                            } else {
                                localFilePath = file.toString();
                                fileName = extractFileNameFromPathOrUrl(localFilePath);
                            }

                            // Determine file type based on file extension
                            String detectedFileType = determineFileType(localFilePath);

                            if ("image_url".equals(detectedFileType)) {
                                // Image file: convert to base64 with compression functionality
                                try {
                                    String imageDataUrl = localFilePath;
                                    if (isConvertUrlToBase64) {
                                        imageDataUrl = ImageBase64Converter.convertToBase64WithCompression(localFilePath);
                                    }
                                    return Map.<String, Object>of(
                                            "type", "image_url",
                                            "image_url", Map.of("url", imageDataUrl)
                                    );
                                } catch (RuntimeException e) {
                                    // Compression failed, return specific error message
                                    logger.error("Image compression failed: {}, error: {}", fileName, e.getMessage());
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", "Image processing failed: " + fileName + " - " + e.getMessage()
                                    );
                                }
                            } else if ("pdf".equals(detectedFileType)) {
                                // PDF file: extract text content for LLM analysis
                                try {
                                    String pdfText = getExtractorFacade().extractText(new File(localFilePath));
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", pdfText != null ? pdfText : "PDF text extraction failed: " + fileName
                                    );
                                } catch (Exception e) {
                                    logger.error("PDF text extraction failed: {}, error: {}", fileName, e.getMessage());
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", "PDF text extraction failed: " + fileName + " - " + e.getMessage()
                                    );
                                }
                            } else if ("document".equals(detectedFileType)) {
                                // Document file: extract text content for LLM analysis
                                try {
                                    String documentText = getExtractorFacade().extractText(new File(localFilePath));
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", documentText != null ? documentText : "Document text extraction failed: " + fileName
                                    );
                                } catch (Exception e) {
                                    logger.error("Document text extraction failed: {}, error: {}", fileName, e.getMessage());
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", "Document text extraction failed: " + fileName + " - " + e.getMessage()
                                    );
                                }
                            } else if ("excel".equals(detectedFileType)) {
                                // Excel spreadsheet file: extract table content for LLM analysis
                                try {
                                    String excelContent = getExtractorFacade().extractText(new File(localFilePath));
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", excelContent != null ? excelContent : "Excel content extraction failed: " + fileName
                                    );
                                } catch (Exception e) {
                                    logger.error("Excel content extraction failed: {}, error: {}", fileName, e.getMessage());
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", "Excel content extraction failed: " + fileName + " - " + e.getMessage()
                                    );
                                }
                            } else if ("csv".equals(detectedFileType)) {
                                // CSV file: extract table content for LLM analysis
                                try {
                                    String csvContent = getExtractorFacade().extractText(new File(localFilePath));
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", csvContent != null ? csvContent : "CSV content extraction failed: " + fileName
                                    );
                                } catch (Exception e) {
                                    logger.error("CSV content extraction failed: {}, error: {}", fileName, e.getMessage());
                                    return Map.<String, Object>of(
                                            "type", "text",
                                            "text", "CSV content extraction failed: " + fileName + " - " + e.getMessage()
                                    );
                                }
                            } else if ("video".equals(detectedFileType)) {
                                // Video file: currently return file information, can extend video processing logic later
                                return Map.<String, Object>of(
                                        "type", "text",
                                        "text", "Video file downloaded: " + fileName + " (path: " + localFilePath + ")"
                                );
                            } else if ("audio".equals(detectedFileType)) {
                                // Audio file: currently return file information, can extend audio processing logic later
                                return Map.<String, Object>of(
                                        "type", "text",
                                        "text", "Audio file downloaded: " + fileName + " (path: " + localFilePath + ")"
                                );
                            } else {
                                // Other files: return file information
                                return Map.<String, Object>of(
                                        "type", "text",
                                        "text", "File downloaded: " + fileName + " (type: " + detectedFileType + ", path: " + localFilePath + ")"
                                );
                            }
                        } catch (Exception e) {
                            logger.error("Error processing file: {}", file, e);
                            return Map.<String, Object>of(
                                    "type", "text",
                                    "text", "File processing exception: " + e.getMessage()
                            );
                        }
                    })
                    .collect(Collectors.toList());
        }

        /**
         * Determine file type based on file path
         */
        private String determineFileType(String filePath) {
            String lowerPath = filePath.toLowerCase();
            if (lowerPath.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                return "image_url";
            } else if (lowerPath.matches(".*\\.(mp4|avi|mov|wmv|flv|webm)$")) {
                return "video";
            } else if (lowerPath.matches(".*\\.(mp3|wav|flac|aac|ogg)$")) {
                return "audio";
            } else if (lowerPath.matches(".*\\.(pdf)$")) {
                return "pdf";
            } else if (lowerPath.matches(".*\\.(doc|docx|txt|rtf)$")) {
                return "document";
            } else if (lowerPath.matches(".*\\.(xls|xlsx)$")) {
                return "excel";
            } else if (lowerPath.matches(".*\\.(csv)$")) {
                return "csv";
            } else {
                return "file";
            }
        }

        /**
         * Extract file name from file path or URL, compatible with both local paths and URLs
         *
         * @param pathOrUrl file path or URL string
         * @return extracted file name
         */
        private String extractFileNameFromPathOrUrl(String pathOrUrl) {
            if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
                return "unknown_file";
            }

            try {
                // Check if it's a URL
                if (pathOrUrl.toLowerCase().startsWith("http://") ||
                        pathOrUrl.toLowerCase().startsWith("https://")) {

                    // Handle URL: extract filename from the last segment
                    String[] urlParts = pathOrUrl.split("/");
                    String lastSegment = urlParts[urlParts.length - 1];

                    // Remove query parameters if present
                    if (lastSegment.contains("?")) {
                        lastSegment = lastSegment.substring(0, lastSegment.indexOf("?"));
                    }

                    // Remove fragment if present
                    if (lastSegment.contains("#")) {
                        lastSegment = lastSegment.substring(0, lastSegment.indexOf("#"));
                    }

                    return lastSegment.isEmpty() ? "downloaded_file" : lastSegment;
                } else {
                    // Handle local file path: use Paths.get() which works on all platforms
                    try {
                        return Paths.get(pathOrUrl).getFileName().toString();
                    } catch (Exception e) {
                        // If Paths.get() fails, fallback to manual extraction
                        logger.warn("Failed to extract filename using Paths.get() for: {}, using fallback method", pathOrUrl);

                        // Manual extraction as fallback
                        String normalized = pathOrUrl.replace("\\", "/");
                        String[] parts = normalized.split("/");
                        return parts[parts.length - 1];
                    }
                }
            } catch (Exception e) {
                logger.error("Error extracting filename from: {}, error: {}", pathOrUrl, e.getMessage());
                return "error_extracting_filename";
            }
        }
    },

    DEFAULT("default") {
        @Override
        public List<Map<String, Object>> processResources(List<?> resourceList, boolean isConvertUrlToBase64) {
            return resourceList.stream()
                    .filter(Objects::nonNull)
                    .map(resource -> Map.<String, Object>of("type", "text", "text", resource.toString()))
                    .collect(Collectors.toList());
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(MultimodalResourceType.class);
    /**
     * -- GETTER --
     * Get identifier
     *
     * @return Identifier string
     */
    private final String identifier;

    MultimodalResourceType(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Abstract method for processing resource list, implemented by each enumeration instance
     *
     * @param resourceList Resource list
     * @return List of processed resource mappings
     */
    public abstract List<Map<String, Object>> processResources(List<?> resourceList, boolean isConvertUrlToBase64);

    /**
     * Determine corresponding processing strategy based on resource type string
     *
     * @param resourceType Resource type string
     * @return Corresponding processing strategy enumeration
     */
    public static MultimodalResourceType fromResourceType(String resourceType) {
        return Arrays.stream(values())
                .filter(type -> type.identifier.equals(resourceType))
                .findFirst()
                .orElse(DEFAULT);
    }

    /**
     * Static factory method to get MultimodalExtractorFacade instance
     *
     * <p>Uses singleton pattern to ensure only one ExtractorFacade instance exists
     * throughout the application, avoiding performance overhead from repeated initialization.</p>
     *
     * @return MultimodalExtractorFacade instance
     */
    private static MultimodalExtractorFacade getExtractorFacade() {
        return ExtractorFacadeHolder.INSTANCE;
    }

    /**
     * Static inner class for implementing lazy initialization singleton pattern
     *
     * <p>This approach ensures thread safety while avoiding performance loss from synchronization,
     * and implements lazy loading at the same time.</p>
     */
    private static class ExtractorFacadeHolder {
        private static final MultimodalExtractorFacade INSTANCE = new MultimodalExtractorFacade();
    }
}