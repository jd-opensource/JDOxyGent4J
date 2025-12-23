package com.jd.oxygent.core.oxygent.infra.impl.multimodal;

import com.jd.oxygent.core.oxygent.infra.multimodal.DocumentExtractor;
import com.jd.oxygent.core.oxygent.infra.multimodal.ExtractorType;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Word document extractor implementation
 * Supports text extraction for Word files in .doc and .docx formats
 *
 * <h3>Technical Architecture</h3>
 * <ul>
 *   <li><strong>Apache POI Integration</strong>: Complete support for Microsoft Word document formats</li>
 *   <li><strong>Multi-format Compatibility</strong>: Unified handling of common document formats like .doc, .docx</li>
 *   <li><strong>Error Recovery</strong>: Robust exception handling to provide useful feedback even when documents are corrupted</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class WordDocumentExtractor implements DocumentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(WordDocumentExtractor.class);
    private static final int MAX_LENGTH = 30000;

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of("doc", "docx");
    }

    @Override
    public ExtractorType getType() {
        return ExtractorType.WORD;
    }

    @Override
    public String extractText(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        try {
            // First try to process as DOCX format
            return extractDocxDocument(inputStream);
        } catch (Exception docxException) {
            logger.info("Failed to try DOCX format, trying DOC format: {}", docxException.getMessage());
            try {
                // Reset stream and try DOC format
                if (inputStream.markSupported()) {
                    inputStream.reset();
                } else {
                    logger.warn("Input stream does not support reset, may cause DOC format parsing failure");
                }
                return extractDocDocument(inputStream);
            } catch (Exception docException) {
                logger.error("Unable to recognize Word file format, DOCX error: {}, DOC error: {}",
                        docxException.getMessage(), docException.getMessage());
                throw new IOException("Unsupported Word file format: " + docException.getMessage(), docException);
            }
        }
    }

    /**
     * Extract .docx format document
     */
    private String extractDocxDocument(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();

            // Check if text was successfully extracted
            if (text == null || text.trim().isEmpty()) {
                return "[Word document may be empty or unable to extract text content]";
            }

            // Clean and limit text
            text = cleanExtractedText(text);
            if (text.length() > MAX_LENGTH) {
                text = text.substring(0, MAX_LENGTH) + "...\n[Document content has been truncated]";
            }

            return "Word Document Content:\n" + text;

        } catch (Exception e) {
            logger.error("Failed to extract DOCX document text", e);
            throw new IOException("Failed to extract DOCX document text: " + e.getMessage(), e);
        }
    }

    /**
     * Extract .doc format document
     */
    private String extractDocDocument(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {

            String text = extractor.getText();

            // Check if text was successfully extracted
            if (text == null || text.trim().isEmpty()) {
                return "[Word document may be empty or unable to extract text content]";
            }

            text = cleanExtractedText(text);
            if (text.length() > MAX_LENGTH) {
                text = text.substring(0, MAX_LENGTH) + "...\n[Document content has been truncated]";
            }

            return "Word Document Content:\n" + text;

        } catch (Exception e) {
            logger.error("Failed to extract DOC document text", e);
            throw new IOException("Failed to extract DOC document text: " + e.getMessage(), e);
        }
    }

    /**
     * Clean extracted text content
     *
     * @param rawText Raw extracted text
     * @return Cleaned text
     */
    private static String cleanExtractedText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "Document is empty or unable to extract text content";
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