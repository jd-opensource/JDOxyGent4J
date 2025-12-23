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

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * <h3>Document Extractor Factory Interface</h3>
 *
 * <p>ExtractorFactory is the factory interface for managing and creating document extractors
 * in the OxyGent framework. This interface defines standard methods for obtaining appropriate
 * document extractors, supporting automatic selection based on file types and unified
 * extractor management.</p>
 *
 * <h3>Design Goals</h3>
 * <ul>
 *   <li><strong>Unified Management</strong>: Centrally manage all available document extractor implementations</li>
 *   <li><strong>Automatic Selection</strong>: Automatically select the most suitable extractor based on file type</li>
 *   <li><strong>Extension Support</strong>: Support dynamic registration and discovery of new extractor implementations</li>
 *   <li><strong>Performance Optimization</strong>: Provide efficient extractor lookup and caching mechanisms</li>
 * </ul>
 *
 * <h3>Implementation Strategy</h3>
 * <ul>
 *   <li><strong>SPI Mechanism</strong>: Automatically discover and load extractor implementations through Java SPI</li>
 *   <li><strong>Priority Sorting</strong>: Support priority-based extractor selection</li>
 *   <li><strong>Caching Mechanism</strong>: Cache loaded extractor instances to improve performance</li>
 *   <li><strong>Thread Safety</strong>: Ensure safe usage in multi-threaded environments</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Get factory instance
 * ExtractorFactory factory = new DefaultExtractorFactory();
 *
 * // Get extractor by file extension
 * Optional<DocumentExtractor> extractor = factory.getExtractor("pdf");
 * if (extractor.isPresent()) {
 *     String content = extractor.get().extractText(pdfFile);
 * }
 *
 * // Get extractor by file object
 * Optional<DocumentExtractor> extractor2 = factory.getExtractor(file);
 *
 * // Get all available extractors
 * List<DocumentExtractor> allExtractors = factory.getAllExtractors();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ExtractorFactory {

    /**
     * Get appropriate document extractor by file extension
     *
     * <p>Find and return the most suitable document extractor based on the specified file extension.
     * If multiple extractors support the same extension, the most suitable one will be selected
     * based on priority.</p>
     *
     * <h3>Lookup Rules</h3>
     * <ul>
     *   <li>Extension matching: Exact match of supported file extensions</li>
     *   <li>Priority sorting: Lower values have higher priority</li>
     *   <li>Version compatibility: Prefer newer version extractors</li>
     * </ul>
     *
     * @param fileExtension File extension (without dot), such as "pdf", "docx", etc.
     * @return Optional containing matching extractor, or empty Optional if none found
     * @throws IllegalArgumentException If fileExtension is null or empty string
     */
    Optional<DocumentExtractor> getExtractor(String fileExtension);

    /**
     * Get appropriate document extractor by file object
     *
     * <p>Find the most suitable document extractor based on the file object's extension
     * and other characteristics. This method automatically extracts the extension from
     * the filename and calls the corresponding lookup logic.</p>
     *
     * @param file File object to process
     * @return Optional containing matching extractor, or empty Optional if none found
     * @throws IllegalArgumentException If file is null
     */
    Optional<DocumentExtractor> getExtractor(File file);

    /**
     * Get all available document extractors
     *
     * <p>Returns all document extractor instances managed by the current factory.
     * The returned list is sorted by extractor type and priority, making it convenient
     * for callers to understand the system's processing capabilities.</p>
     *
     * <h3>Sorting Rules</h3>
     * <ul>
     *   <li>Grouped by extractor type</li>
     *   <li>Sorted by priority within the same type</li>
     *   <li>Sorted by version number for same priority</li>
     * </ul>
     *
     * @return List of all available extractors, will not be null but may be empty
     */
    List<DocumentExtractor> getAllExtractors();

    /**
     * Get extractor list by extractor type
     *
     * <p>Returns all extractor instances of the specified type. This method is mainly
     * used for scenarios that require specific type extractors, such as only needing
     * PDF extractors or Excel extractors.</p>
     *
     * @param extractorType Extractor type
     * @return List of extractors of specified type, will not be null but may be empty
     * @throws IllegalArgumentException If extractorType is null
     */
    List<DocumentExtractor> getExtractorsByType(ExtractorType extractorType);

    /**
     * Check if specified file extension is supported
     *
     * <p>Quickly check whether the current factory has extractors that support the
     * specified file extension. This method is more lightweight than getExtractor
     * and is suitable for pre-checking.</p>
     *
     * @param fileExtension File extension (without dot)
     * @return Returns true if supported, otherwise false
     */
    boolean supports(String fileExtension);

    /**
     * Check if specified file is supported
     *
     * <p>Check whether the current factory has extractors that support the specified file.
     * This method considers the file's extension and other characteristics.</p>
     *
     * @param file File to check
     * @return Returns true if supported, otherwise false
     */
    boolean supports(File file);

    /**
     * Get all supported file extensions
     *
     * <p>Returns the set of all file extensions supported by all extractors managed
     * by the current factory. This method is mainly used for system capability
     * display and configuration validation.</p>
     *
     * @return Set of supported file extensions, will not be null
     */
    java.util.Set<String> getSupportedExtensions();

    /**
     * Refresh extractor cache
     *
     * <p>Reload all extractor implementations and clear existing cache. This method
     * is mainly used for dynamic deployment scenarios, called when new extractor
     * implementations are deployed.</p>
     */
    void refresh();

    /**
     * Get factory statistics
     *
     * <p>Returns statistical information of the current factory, including number
     * of extractors, number of supported file types, etc. This method is mainly
     * used for monitoring and debugging.</p>
     *
     * @return String representation of factory statistics
     */
    String getStatistics();
}