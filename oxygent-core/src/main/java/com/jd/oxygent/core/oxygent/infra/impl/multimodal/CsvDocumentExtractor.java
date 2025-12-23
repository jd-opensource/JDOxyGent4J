package com.jd.oxygent.core.oxygent.infra.impl.multimodal;

import com.jd.oxygent.core.oxygent.infra.multimodal.DocumentExtractor;
import com.jd.oxygent.core.oxygent.infra.multimodal.ExtractorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * CSV document extractor implementation
 * Supports table content extraction and formatting for CSV format files
 *
 * <h3>Technical Architecture</h3>
 * <ul>
 *   <li><strong>Standard CSV Parsing</strong>: Supports CSV documents in standard RFC 4180 format</li>
 *   <li><strong>Intelligent Encoding Detection</strong>: Automatically handles common encoding formats like UTF-8, GBK</li>
 *   <li><strong>Memory Optimization</strong>: Stream processing for large files to avoid memory overflow risks</li>
 *   <li><strong>Error Recovery</strong>: Robust exception handling to provide useful information even when files are corrupted</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class CsvDocumentExtractor implements DocumentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CsvDocumentExtractor.class);

    // Number formatter to avoid scientific notation
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##########");

    private static final int MAX_LENGTH = 30000;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COLUMNS = 50;

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of("csv");
    }


    @Override
    public ExtractorType getType() {
        return ExtractorType.CSV;
    }

    @Override
    public String extractText(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        try {
            // Read CSV content
            String csvContent = readStreamContent(inputStream);
            if (csvContent == null || csvContent.trim().isEmpty()) {
                return "[CSV file is empty or cannot be read]";
            }

            StringBuilder result = new StringBuilder();
            result.append("CSV Document Content:\n\n");

            // Parse CSV content
            List<List<String>> tableData = parseCsvContent(csvContent);

            if (tableData.isEmpty()) {
                result.append("[CSV file is empty]\n");
                return result.toString();
            }

            // Limit the number of rows and columns to process
            int maxRowsToProcess = Math.min(tableData.size(), MAX_ROWS);
            int maxColumnsToProcess = getMaxColumns(tableData);
            maxColumnsToProcess = Math.min(maxColumnsToProcess, MAX_COLUMNS);

            logger.info("CSV file will process {} rows {} columns", maxRowsToProcess, maxColumnsToProcess);

            // Truncate data
            List<List<String>> processedData = new ArrayList<>();
            for (int i = 0; i < maxRowsToProcess; i++) {
                List<String> row = tableData.get(i);
                List<String> processedRow = new ArrayList<>();
                for (int j = 0; j < Math.min(row.size(), maxColumnsToProcess); j++) {
                    processedRow.add(row.get(j));
                }
                processedData.add(processedRow);
            }

            // Format output table
            result.append(formatTableAsText(processedData, maxColumnsToProcess < getMaxColumns(tableData)));

            // Add truncation information
            if (maxRowsToProcess < tableData.size()) {
                result.append(String.format("\n[Table too long, truncated to show first %d rows out of %d total rows]\n", maxRowsToProcess, tableData.size()));
            }

            // If content is too long, truncate it
            if (result.length() > MAX_LENGTH) {
                result.setLength(MAX_LENGTH);
                result.append("\n[Content too long, truncated]");
            }

            return result.toString();

        } catch (Exception e) {
            logger.error("Failed to extract CSV document text", e);
            throw new IOException("Failed to extract CSV document text: " + e.getMessage(), e);
        }
    }

    /**
     * Read input stream content, try multiple encoding formats
     */
    private String readStreamContent(InputStream inputStream) throws IOException {
        // First try UTF-8 encoding
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            byte[] bytes = buffer.toByteArray();

            // Try different encodings
            String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
            for (String encoding : encodings) {
                try {
                    String content = new String(bytes, encoding);
                    // Simple validation of whether content is reasonable (doesn't contain too many garbled characters)
                    if (isValidContent(content)) {
                        logger.info("Successfully read CSV content using encoding {}", encoding);
                        return content;
                    }
                } catch (Exception e) {
                    logger.info("Failed to read using encoding {}: {}", encoding, e.getMessage());
                }
            }

            // If all encodings fail, use UTF-8 as default
            return new String(bytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            logger.error("Failed to read input stream", e);
            throw e;
        }
    }

    /**
     * Simple validation of whether content is reasonable
     */
    private boolean isValidContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // Check if it contains too many garbled characters (simple heuristic)
        int invalidChars = 0;
        int totalChars = Math.min(content.length(), 1000); // Only check first 1000 characters

        for (int i = 0; i < totalChars; i++) {
            char c = content.charAt(i);
            if (c == 0xFFFD || (c < 32 && c != '\n' && c != '\r' && c != '\t')) {
                invalidChars++;
            }
        }

        return (double) invalidChars / totalChars < 0.1; // Less than 10% garbled characters
    }

    /**
     * Parse CSV content into a two-dimensional list
     */
    private List<List<String>> parseCsvContent(String csvContent) {
        List<List<String>> result = new ArrayList<>();

        // Detect delimiter
        char delimiter = detectDelimiter(csvContent);
        logger.info("Detected CSV delimiter: '{}'", delimiter);

        String[] lines = csvContent.split("\r?\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }

            List<String> row = parseCsvLine(line, delimiter);
            result.add(row);
        }

        return result;
    }

    /**
     * Detect CSV delimiter
     */
    private char detectDelimiter(String csvContent) {
        char[] possibleDelimiters = {',', ';', '\t', '|'};
        int[] counts = new int[possibleDelimiters.length];

        // Only check the first few lines to determine delimiter
        String[] lines = csvContent.split("\r?\n");
        int linesToCheck = Math.min(5, lines.length);

        for (int i = 0; i < linesToCheck; i++) {
            String line = lines[i];
            for (int j = 0; j < possibleDelimiters.length; j++) {
                counts[j] += countOccurrences(line, possibleDelimiters[j]);
            }
        }

        // Choose the delimiter with the most occurrences
        int maxCount = 0;
        char bestDelimiter = ','; // Default to comma

        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                bestDelimiter = possibleDelimiters[i];
            }
        }

        return bestDelimiter;
    }

    /**
     * Count occurrences of a character in a string
     */
    private int countOccurrences(String str, char ch) {
        int count = 0;
        boolean inQuotes = false;

        for (char c : str.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ch && !inQuotes) {
                count++;
            }
        }

        return count;
    }

    /**
     * Parse a single line of CSV data
     */
    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                // Field delimiter
                result.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        result.add(currentField.toString().trim());

        return result;
    }

    /**
     * Get the maximum number of columns in the table
     */
    private int getMaxColumns(List<List<String>> tableData) {
        int maxColumns = 0;
        for (List<String> row : tableData) {
            maxColumns = Math.max(maxColumns, row.size());
        }
        return maxColumns;
    }

    /**
     * Format table data into readable text form
     */
    private String formatTableAsText(List<List<String>> tableData, boolean hasMoreColumns) {
        if (tableData.isEmpty()) {
            return "[Table has no data]";
        }

        StringBuilder result = new StringBuilder();

        // First analyze which columns are completely empty
        int columnCount = getMaxColumns(tableData);
        boolean[] isColumnEmpty = new boolean[columnCount];
        Arrays.fill(isColumnEmpty, true);

        for (List<String> row : tableData) {
            for (int i = 0; i < Math.min(row.size(), columnCount); i++) {
                String cellValue = i < row.size() ? row.get(i) : "";
                if (!cellValue.trim().isEmpty()) {
                    isColumnEmpty[i] = false;
                }
            }
        }

        // Build list of valid column indices
        List<Integer> validColumns = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            if (!isColumnEmpty[i]) {
                validColumns.add(i);
            }
        }

        if (validColumns.isEmpty()) {
            return "[All table columns are empty]";
        }

        // Calculate maximum width of valid columns
        int[] columnWidths = new int[validColumns.size()];

        for (List<String> row : tableData) {
            for (int i = 0; i < validColumns.size(); i++) {
                int colIndex = validColumns.get(i);
                String cellValue = colIndex < row.size() ? row.get(colIndex) : "";
                columnWidths[i] = Math.max(columnWidths[i], cellValue.length());
            }
        }

        // Limit column width
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = Math.min(columnWidths[i], 20);
            columnWidths[i] = Math.max(columnWidths[i], 1);
        }

        // Output table
        int consecutiveEmptyRows = 0;
        final int maxDisplayEmptyRows = 2;

        for (int rowIndex = 0; rowIndex < tableData.size(); rowIndex++) {
            List<String> row = tableData.get(rowIndex);

            // Check if current row is empty
            boolean isEmptyRow = true;
            for (int validColIndex : validColumns) {
                String cellValue = validColIndex < row.size() ? row.get(validColIndex) : "";
                if (!cellValue.trim().isEmpty()) {
                    isEmptyRow = false;
                    break;
                }
            }

            // Skip too many consecutive empty rows
            if (isEmptyRow) {
                consecutiveEmptyRows++;
                if (consecutiveEmptyRows > maxDisplayEmptyRows) {
                    continue;
                }
            } else {
                consecutiveEmptyRows = 0;
            }

            // Output valid columns
            for (int i = 0; i < validColumns.size(); i++) {
                int colIndex = validColumns.get(i);
                String cellValue = colIndex < row.size() ? row.get(colIndex) : "";

                // Truncate overly long cell content
                if (cellValue.length() > columnWidths[i]) {
                    cellValue = cellValue.substring(0, columnWidths[i] - 3) + "...";
                }

                // Format output
                int width = Math.max(columnWidths[i], 1);
                result.append(String.format("%-" + width + "s", cellValue));

                if (i < validColumns.size() - 1) {
                    result.append(" | ");
                }
            }

            if (hasMoreColumns) {
                result.append(" | [More columns...]");
            }

            result.append("\n");

            // Add separator line after first row
            if (rowIndex == 0 && tableData.size() > 1) {
                for (int i = 0; i < validColumns.size(); i++) {
                    int width = Math.max(columnWidths[i], 1);
                    result.append("-".repeat(width));
                    if (i < validColumns.size() - 1) {
                        result.append("-+-");
                    }
                }
                if (hasMoreColumns) {
                    result.append("-+-[More columns...]");
                }
                result.append("\n");
            }
        }

        // If some empty columns were omitted, add explanation
        int omittedColumns = columnCount - validColumns.size();
        if (omittedColumns > 0) {
            result.append(String.format("\n[Omitted %d empty columns to save space]\n", omittedColumns));
        }

        return result.toString();
    }
}