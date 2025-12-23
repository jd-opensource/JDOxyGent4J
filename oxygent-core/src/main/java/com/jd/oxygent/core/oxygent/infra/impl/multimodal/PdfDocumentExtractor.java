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
package com.jd.oxygent.core.oxygent.infra.impl.multimodal;

import com.jd.oxygent.core.oxygent.infra.multimodal.DocumentExtractor;
import com.jd.oxygent.core.oxygent.infra.multimodal.ExtractorType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * <h3>PDF Document Extractor Implementation</h3>
 *
 * <p>PdfDocumentExtractor is a PDF document content extractor implementation based on Apache PDFBox.
 * This class implements the DocumentExtractor interface, providing stable and reliable PDF text extraction functionality,
 * supporting the conversion of PDF documents to plain text format for LLM content analysis and understanding.</p>
 *
 * <h3>Technical Features</h3>
 * <ul>
 *   <li><strong>PDFBox Integration</strong>: Based on Apache PDFBox 2.x, mature and stable PDF processing library</li>
 *   <li><strong>Error Tolerance</strong>: Automatically handles common PDF exceptions such as missing fonts and encoding issues</li>
 *   <li><strong>Page-by-Page Recovery</strong>: When overall extraction fails, attempts page-by-page extraction to maximize content retrieval</li>
 *   <li><strong>Length Control</strong>: Built-in text length control to avoid memory issues caused by oversized documents</li>
 *   <li><strong>Format Optimization</strong>: Automatically cleans and formats extracted text content</li>
 * </ul>
 *
 * <h3>Processing Strategy</h3>
 * <ul>
 *   <li><strong>Font Error Tolerance</strong>: Ignores font errors and continues extracting available content</li>
 *   <li><strong>Pagination Processing</strong>: Supports page-by-page extraction, skipping problematic pages</li>
 *   <li><strong>Content Cleaning</strong>: Automatically removes excess whitespace while maintaining paragraph structure</li>
 *   <li><strong>Length Truncation</strong>: Intelligent truncation of overly long content with truncation notifications</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class PdfDocumentExtractor implements DocumentExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentExtractor.class);

    /**
     * Maximum text length limit (approximately 1 million characters to avoid exceeding LLM token limits)
     */
    private static final int MAX_LENGTH = 1000000;

    /**
     * Supported file extensions
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf");

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public int getPriority() {
        // High priority
        return 10;
    }

    @Override
    public ExtractorType getType() {
        return ExtractorType.PDF;
    }

    @Override
    public String extractText(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        logger.info("Starting to extract PDF document content from input stream");

        try {
            // Use Apache PDFBox library to extract PDF text, configured to ignore font errors
            try (PDDocument document = PDDocument.load(inputStream)) {
                // Set system property to ignore font errors
                System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

                PDFTextStripper pdfStripper = new PDFTextStripper();

                // Set extraction parameters
                pdfStripper.setSortByPosition(true);
                pdfStripper.setStartPage(1);
                pdfStripper.setEndPage(document.getNumberOfPages());

                String text = null;
                try {
                    text = pdfStripper.getText(document);
                } catch (Exception fontException) {
                    // Catch font-related exceptions, log warning but continue processing
                    logger.warn("PDF font processing warning (ignored): {}", fontException.getMessage());

                    // Try to extract text page by page, skipping problematic pages
                    text = extractTextPageByPage(document, pdfStripper, "input stream");
                }

                // Check if text was successfully extracted
                if (text == null || text.trim().isEmpty()) {
                    return "[PDF may be a scanned image version or has font issues, unable to extract text content]";
                }

                // Clean and format text
                text = cleanExtractedText(text);

                // Control text length
                if (text.length() > MAX_LENGTH) {
                    text = text.substring(0, MAX_LENGTH) + "...\n[PDF content has been truncated]";
                }

                return "PDF Document Content:\n" + text;

            }
        } catch (Exception e) {
            logger.error("Error occurred while extracting PDF text", e);
            throw new IOException("Failed to extract PDF text: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text content page by page
     *
     * @param document    PDF document object
     * @param pdfStripper PDF text stripper
     * @param fileName    File name (for logging)
     * @return Extracted text content
     */
    private String extractTextPageByPage(PDDocument document, PDFTextStripper pdfStripper, String fileName) {
        StringBuilder extractedText = new StringBuilder();
        int totalPages = document.getNumberOfPages();
        int successPages = 0;
        int failedPages = 0;

        logger.info("Starting page-by-page PDF content extraction: {} (total pages: {})", fileName, totalPages);

        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            try {
                pdfStripper.setStartPage(pageNum);
                pdfStripper.setEndPage(pageNum);
                String pageText = pdfStripper.getText(document);

                if (pageText != null && !pageText.trim().isEmpty()) {
                    extractedText.append(pageText);
                    successPages++;
                } else {
                    extractedText.append(String.format("\n[Page %d is empty]\n", pageNum));
                }

            } catch (Exception pageException) {
                failedPages++;
                logger.info("Skipping page {}, font error: {}", pageNum, pageException.getMessage());
                extractedText.append(String.format("\n[Page %d skipped due to font issues]\n", pageNum));
            }
        }

        logger.info("Page-by-page extraction completed: {} - Success: {}, Failed: {}", fileName, successPages, failedPages);

        // If all pages failed, return error message
        if (successPages == 0) {
            return String.format("PDF document: %s - All pages failed to extract text content", fileName);
        }

        return extractedText.toString();
    }

    /**
     * Clean extracted text content
     *
     * @param rawText Raw extracted text
     * @return Cleaned text
     */
    private String cleanExtractedText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "PDF document is empty or unable to extract text content";
        }

        // Clean excess whitespace characters while maintaining basic paragraph structure
        // Merge spaces and tabs
        String cleaned = rawText.replaceAll("[ \\t]+", " ");
        // Maintain paragraph separation
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\n\n");
        // Limit consecutive line breaks
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned.trim();
    }
}