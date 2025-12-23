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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <h3>Multimodal Content Extractor Facade Class</h3>
 *
 * <p>MultimodalExtractorFacade is the unified entry point for multimodal document processing
 * in the OxyGent framework, providing simplified APIs to handle content extraction from
 * various document formats. This class adopts the facade pattern, hiding the complexity
 * of underlying extractor factories and specific implementations.</p>
 *
 * <h3>Design Goals</h3>
 * <ul>
 *   <li><strong>Simplified Usage</strong>: Provides easy-to-use APIs, hiding complex internal logic</li>
 *   <li><strong>Unified Entry</strong>: Provides unified processing interface for all document types</li>
 *   <li><strong>Automatic Selection</strong>: Automatically selects appropriate extractor based on file type</li>
 *   <li><strong>Exception Handling</strong>: Provides friendly exception messages and error handling</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Automatic Recognition</strong>: Automatically selects appropriate extractor based on file extension</li>
 *   <li><strong>Flexible Configuration</strong>: Supports custom extractor factory implementations</li>
 *   <li><strong>Performance Optimization</strong>: Built-in caching and optimization mechanisms</li>
 *   <li><strong>Thread Safety</strong>: Supports multi-threaded concurrent calls</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Use default configuration
 * MultimodalExtractorFacade extractor = new MultimodalExtractorFacade();
 *
 * // Extract PDF document content
 * File pdfFile = new File("document.pdf");
 * String content = extractor.extractText(pdfFile);
 *
 * // Check if a file type is supported
 * boolean supported = extractor.supports("xlsx");
 *
 * // Get supported file types
 * Set<String> supportedTypes = extractor.getSupportedExtensions();
 *
 * // Use custom factory
 * ExtractorFactory customFactory = new CustomExtractorFactory();
 * MultimodalExtractorFacade customExtractor = new MultimodalExtractorFacade(customFactory);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class MultimodalExtractorFacade {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalExtractorFacade.class);

    /**
     * Extractor factory instance
     */
    private final ExtractorFactory extractorFactory;

    /**
     * Default constructor
     *
     * <p>Uses the default extractor factory implementation. The factory will automatically
     * discover and load all available document extractor implementations through SPI mechanism.</p>
     */
    public MultimodalExtractorFacade() {
        this(new DefaultExtractorFactory());
    }

    /**
     * Constructor with custom factory
     *
     * <p>Uses the specified extractor factory implementation. This allows users to provide
     * custom extractor management logic, such as special loading strategies or caching mechanisms.</p>
     *
     * @param extractorFactory Custom extractor factory implementation
     * @throws IllegalArgumentException If extractorFactory is null
     */
    public MultimodalExtractorFacade(ExtractorFactory extractorFactory) {
        if (extractorFactory == null) {
            throw new IllegalArgumentException("Extractor factory cannot be null");
        }
        this.extractorFactory = extractorFactory;

        logger.debug("Initializing MultimodalExtractorFacade, factory type: {}",
                extractorFactory.getClass().getSimpleName());
    }

    /**
     * Extract document content
     *
     * <p>This is the main content extraction method that automatically selects the appropriate
     * extractor based on file extension to process documents. Supports multiple formats including
     * PDF, Word, Excel, CSV, etc.</p>
     *
     * <h3>Processing Flow</h3>
     * <ol>
     *   <li>Validate file validity (existence, readability, etc.)</li>
     *   <li>Find appropriate extractor based on file extension</li>
     *   <li>Call extractor to perform content extraction</li>
     *   <li>Return extracted text content</li>
     * </ol>
     *
     * <h3>Exception Handling</h3>
     * <ul>
     *   <li><strong>File not exists</strong>: Throws IllegalArgumentException</li>
     *   <li><strong>Format not supported</strong>: Throws UnsupportedOperationException</li>
     *   <li><strong>Extraction failed</strong>: Throws RuntimeException wrapping original exception</li>
     * </ul>
     *
     * @param file File to extract content from, cannot be null
     * @return Extracted text content, will not be null but may be empty string
     * @throws IllegalArgumentException      If file is null, does not exist, or is not readable
     * @throws UnsupportedOperationException If file format is not supported
     * @throws RuntimeException              If other errors occur during extraction process
     */
    public String extractText(File file) throws Exception {
        // Parameter validation
        validateFile(file);

        String fileName = file.getName();
        logger.debug("Starting document content extraction: {}", fileName);

        try {
            // Get appropriate extractor
            Optional<DocumentExtractor> extractorOpt = extractorFactory.getExtractor(file);

            if (extractorOpt.isEmpty()) {
                String extension = getFileExtension(fileName);
                throw new UnsupportedOperationException(
                        String.format("Unsupported file type: %s, supported types: %s",
                                extension, extractorFactory.getSupportedExtensions()));
            }

            DocumentExtractor extractor = extractorOpt.get();
            logger.debug("Using extractor: {} to process file: {}",
                    extractor.getClass().getSimpleName(), fileName);

            // Execute content extraction
            long startTime = System.currentTimeMillis();
            String content;
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                content = extractor.extractText(fileInputStream);
            }
            long duration = System.currentTimeMillis() - startTime;

            // Log extraction results
            int contentLength = content != null ? content.length() : 0;
            logger.debug("Document content extraction completed: {} (duration: {}ms, content length: {})",
                    fileName, duration, contentLength);

            return content != null ? content : "";

        } catch (UnsupportedOperationException e) {
            // Re-throw unsupported operation exception
            throw e;
        } catch (Exception e) {
            logger.error("Exception occurred while extracting document content: {}", fileName, e);
            throw new RuntimeException("Document content extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if specified file extension is supported
     *
     * <p>Quickly check whether the current system supports processing files with the
     * specified extension. This method is more lightweight than extractText and is
     * suitable for pre-checking.</p>
     *
     * @param fileExtension File extension (without dot), such as "pdf", "docx", etc.
     * @return Returns true if supported, otherwise false
     */
    public boolean supports(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            return false;
        }

        return extractorFactory.supports(fileExtension);
    }

    /**
     * Check if specified file is supported
     *
     * <p>Check whether the current system supports processing the specified file.
     * This method considers the file's extension and other characteristics.</p>
     *
     * @param file File to check
     * @return Returns true if supported, otherwise false
     */
    public boolean supports(File file) {
        if (file == null) {
            return false;
        }

        return extractorFactory.supports(file);
    }

    /**
     * Get all supported file extensions
     *
     * <p>Returns the set of all file extensions supported by the current system.
     * This method is mainly used for system capability display and user interface hints.</p>
     *
     * @return Set of supported file extensions, will not be null
     */
    public Set<String> getSupportedExtensions() {
        return extractorFactory.getSupportedExtensions();
    }

    /**
     * Get all available document extractors
     *
     * <p>Returns all available document extractor instances in the current system.
     * This method is mainly used for system monitoring and debugging.</p>
     *
     * @return List of all available extractors, will not be null but may be empty
     */
    public List<DocumentExtractor> getAllExtractors() {
        return extractorFactory.getAllExtractors();
    }

    /**
     * Get extractor list by extractor type
     *
     * <p>Returns all extractor instances of the specified type.</p>
     *
     * @param extractorType Extractor type
     * @return List of extractors of specified type, will not be null but may be empty
     */
    public List<DocumentExtractor> getExtractorsByType(ExtractorType extractorType) {
        return extractorFactory.getExtractorsByType(extractorType);
    }

    /**
     * Refresh extractor cache
     *
     * <p>Reload all extractor implementations and clear existing cache. This method is mainly
     * used for dynamic deployment scenarios, called when new extractor implementations are deployed.</p>
     */
    public void refresh() {
        logger.info("Refreshing multimodal extractor cache");
        extractorFactory.refresh();
    }

    /**
     * Get system statistics
     *
     * <p>Returns statistical information of the current system, including number of extractors,
     * number of supported file types, etc. This method is mainly used for monitoring and debugging.</p>
     *
     * @return String representation of system statistics
     */
    public String getStatistics() {
        return extractorFactory.getStatistics();
    }

    /**
     * Validate file validity
     *
     * @param file File to validate
     * @throws IllegalArgumentException If file is invalid
     */
    private void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File object cannot be null");
        }

        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("Specified path is not a file: " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException("File is not readable: " + file.getAbsolutePath());
        }

        if (file.length() == 0) {
            logger.warn("File is empty: {}", file.getAbsolutePath());
        }
    }

    /**
     * Extract extension from filename
     *
     * @param fileName Filename
     * @return File extension, returns empty string if no extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDot + 1).toLowerCase();
    }
}