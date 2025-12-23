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
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * <h3>Document Content Extractor Interface</h3>
 *
 * <p>DocumentExtractor is the core interface for document content extraction in the OxyGent framework.
 * This interface defines unified document processing specifications, supports content extraction from
 * multiple document formats, and provides standardized document processing capabilities for multimodal
 * AI systems.</p>
 *
 * <h3>Design Principles</h3>
 * <ul>
 *   <li><strong>Unified Interface</strong>: Provides consistent processing interface for all document types</li>
 *   <li><strong>Type Safety</strong>: Ensures extractor type accuracy through type enumeration</li>
 *   <li><strong>Extension Friendly</strong>: Supports dynamic loading of new extractor implementations through SPI mechanism</li>
 *   <li><strong>Exception Transparency</strong>: Propagates underlying exceptions upward for caller handling</li>
 * </ul>
 *
 * <h3>Implementation Requirements</h3>
 * <ul>
 *   <li><strong>Thread Safety</strong>: Implementation classes should be thread-safe and support concurrent calls</li>
 *   <li><strong>Resource Management</strong>: Properly manage file handles and memory resources to avoid leaks</li>
 *   <li><strong>Error Handling</strong>: Should throw clear exceptions for files that cannot be processed</li>
 *   <li><strong>Performance Optimization</strong>: Should consider memory usage and processing time for large files</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Get extractor through SPI mechanism
 * ServiceLoader<DocumentExtractor> serviceLoader =
 *     ServiceLoader.load(DocumentExtractor.class);
 *
 * for (DocumentExtractor extractor : serviceLoader) {
 *     if (extractor.getSupportedExtensions().contains("pdf")) {
 *         try (InputStream inputStream = new FileInputStream(pdfFile)) {
 *             String content = extractor.extractText(inputStream);
 *             System.out.println("Extracted content: " + content);
 *         }
 *         break;
 *     }
 * }
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface DocumentExtractor {

    /**
     * Get supported file extension set
     *
     * <p>Returns all file extensions supported by the current extractor. Extensions should be in
     * lowercase form without dot prefix. For example: "pdf", "docx", "xlsx", etc.</p>
     *
     * <h3>Implementation Requirements</h3>
     * <ul>
     *   <li>The returned set should not be null</li>
     *   <li>Extensions should be in lowercase form</li>
     *   <li>Should not contain dot prefix</li>
     *   <li>The set should be immutable</li>
     * </ul>
     *
     * @return Set of supported file extensions, will not be null
     */
    Set<String> getSupportedExtensions();


    /**
     * Check if the specified file is supported
     *
     * <p>Determines whether the current extractor supports processing the specified file based on
     * file extension. This is a default implementation, subclasses can override this method to
     * provide more complex judgment logic as needed.</p>
     *
     * @param file File to check
     * @return Returns true if supported, otherwise false
     */
    default boolean supports(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        String fileName = file.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(lastDot + 1).toLowerCase();
        return getSupportedExtensions().contains(extension);
    }

    /**
     * Get extractor priority
     *
     * <p>When multiple extractors support the same file format, the system will select the
     * appropriate extractor based on priority. Lower values indicate higher priority.
     * Default priority is 100.</p>
     *
     * @return Extractor priority, lower values have higher priority
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Get extractor version information
     *
     * <p>Returns the version number of the current extractor implementation, used for version
     * management and compatibility checking. Returns "1.0.0" by default.</p>
     *
     * @return Version number string
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Get extractor type
     *
     * <p>Returns the type identifier of the current extractor, used to identify and categorize
     * different extractor implementations. This method is mainly used for logging, monitoring,
     * and debugging.</p>
     *
     * @return Extractor type, will not be null
     */
    ExtractorType getType();

    /**
     * Extract text content from input stream
     *
     * <p>This is the core method of the document extractor, responsible for extracting text content
     * from the given input stream. Implementation classes should perform appropriate processing
     * based on the specific document format.</p>
     *
     * <h3>Implementation Requirements</h3>
     * <ul>
     *   <li><strong>Stream Processing</strong>: Handle input stream correctly, do not close the passed stream</li>
     *   <li><strong>Exception Handling</strong>: Should throw IOException for content that cannot be processed</li>
     *   <li><strong>Encoding Handling</strong>: Correctly handle document character encoding</li>
     *   <li><strong>Memory Control</strong>: Should consider memory usage limits for large documents</li>
     * </ul>
     *
     * @param inputStream Document input stream, cannot be null
     * @return Extracted text content, will not be null
     * @throws IOException Thrown when document reading or parsing fails
     */
    String extractText(InputStream inputStream) throws IOException;
}