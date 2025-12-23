package com.jd.oxygent.core.oxygent.infra.impl.multimodal;

import com.jd.oxygent.core.oxygent.infra.multimodal.DocumentExtractor;
import com.jd.oxygent.core.oxygent.infra.multimodal.ExtractorType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Excel document extractor implementation
 * Supports text extraction for Excel files in .xls and .xlsx formats
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ExcelDocumentExtractor implements DocumentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ExcelDocumentExtractor.class);

    // Number formatter to avoid scientific notation
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##########");

    // Limit parameters
    // Process at most 10 worksheets
    private static final int MAX_SHEETS = 10;
    // Process at most 1000 rows per worksheet
    private static final int MAX_ROWS_PER_SHEET = 1000;
    // Process at most 50 columns per worksheet
    private static final int MAX_COLUMNS_PER_SHEET = 50;
    // Maximum cell content length
    private static final int MAX_CELL_LENGTH = 1000;

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of("xls", "xlsx");
    }

    @Override
    public ExtractorType getType() {
        return ExtractorType.EXCEL;
    }

    @Override
    public String extractText(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        try (Workbook workbook = createWorkbook(inputStream)) {
            if (workbook == null) {
                throw new IOException("Unable to create workbook, may not be a valid Excel file");
            }

            StringBuilder result = new StringBuilder();
            int sheetCount = Math.min(workbook.getNumberOfSheets(), MAX_SHEETS);

            logger.info("Starting to process Excel file with {} worksheets", sheetCount);

            for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null) {
                    continue;
                }

                String sheetContent = extractSheetContent(sheet);
                if (!sheetContent.trim().isEmpty()) {
                    if (!result.isEmpty()) {
                        result.append("\n\n");
                    }

                    // Add worksheet title
                    String sheetName = sheet.getSheetName();
                    if (sheetName != null && !sheetName.trim().isEmpty()) {
                        result.append("=== Worksheet: ").append(sheetName).append(" ===\n");
                    } else {
                        result.append("=== Worksheet ").append(sheetIndex + 1).append(" ===\n");
                    }

                    result.append(sheetContent);
                }
            }

            if (result.isEmpty()) {
                return "[Excel file has no readable content]";
            }

            return result.toString();

        } catch (Exception e) {
            logger.error("Error occurred while extracting Excel text", e);
            throw new IOException("Failed to extract Excel text: " + e.getMessage(), e);
        }
    }

    /**
     * Create workbook object, automatically detect file format
     */
    private Workbook createWorkbook(InputStream inputStream) throws IOException {
        try {
            // First try to create XLSX workbook
            return new XSSFWorkbook(inputStream);
        } catch (Exception e) {
            logger.info("Failed to try XLSX format, trying XLS format: {}", e.getMessage());
            try {
                // Reset stream and try XLS format
                if (inputStream.markSupported()) {
                    inputStream.reset();
                } else {
                    logger.warn("Input stream does not support reset, may cause XLS format parsing failure");
                }
                return new HSSFWorkbook(inputStream);
            } catch (Exception e2) {
                logger.error("Unable to recognize Excel file format, XLSX error: {}, XLS error: {}",
                        e.getMessage(), e2.getMessage());
                throw new IOException("Unsupported Excel file format", e2);
            }
        }
    }

    /**
     * Extract worksheet content
     */
    private String extractSheetContent(Sheet sheet) {
        try {
            // Get valid data range
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = Math.min(sheet.getLastRowNum(), firstRowNum + MAX_ROWS_PER_SHEET - 1);

            if (firstRowNum < 0 || lastRowNum < firstRowNum) {
                return "[Worksheet has no data]";
            }

            // Collect table data
            List<List<String>> tableData = new ArrayList<>();
            int maxColumns = 0;
            boolean hasMoreColumns = false;

            for (int rowIndex = firstRowNum; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> rowData = new ArrayList<>();

                if (row != null) {
                    int lastCellNum = Math.min(row.getLastCellNum(), MAX_COLUMNS_PER_SHEET);
                    if (row.getLastCellNum() > MAX_COLUMNS_PER_SHEET) {
                        hasMoreColumns = true;
                    }

                    for (int colIndex = 0; colIndex < lastCellNum; colIndex++) {
                        Cell cell = row.getCell(colIndex);
                        String cellValue = getCellValueAsString(cell);

                        // Limit cell content length
                        if (cellValue.length() > MAX_CELL_LENGTH) {
                            cellValue = cellValue.substring(0, MAX_CELL_LENGTH - 3) + "...";
                        }

                        rowData.add(cellValue);
                    }
                }

                // Add even empty rows to maintain table structure
                tableData.add(rowData);
                maxColumns = Math.max(maxColumns, rowData.size());
            }

            // Standardize row data, ensure all rows have the same number of columns
            for (List<String> row : tableData) {
                while (row.size() < maxColumns) {
                    row.add("");
                }
            }

            return formatTableAsText(tableData, hasMoreColumns);

        } catch (Exception e) {
            logger.error("Error occurred while processing worksheet '{}'", sheet.getSheetName(), e);
            return "[Worksheet read error: " + e.getMessage() + "]";
        }
    }

    /**
     * Get cell value and convert to string
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Use formatter to avoid scientific notation
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue == Math.floor(numericValue)) {
                            // Integer
                            return String.valueOf((long) numericValue);
                        } else {
                            // Decimal
                            return NUMBER_FORMAT.format(numericValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        // Try to get formula calculation result
                        return getCellValueAsString(cell);
                    } catch (Exception e) {
                        return cell.getCellFormula();
                    }
                case BLANK:
                default:
                    return "";
            }
        } catch (Exception e) {
            logger.info("Error reading cell value: {}", e.getMessage());
            return "[Read error]";
        }
    }

    /**
     * Format table data into readable text form, intelligently handle empty cells
     */
    private static String formatTableAsText(List<List<String>> tableData, boolean hasMoreColumns) {
        if (tableData.isEmpty()) {
            return "[Table has no data]";
        }

        StringBuilder result = new StringBuilder();

        // First analyze which columns are completely empty, these columns will be omitted
        int columnCount = tableData.get(0).size();
        boolean[] isColumnEmpty = new boolean[columnCount];
        Arrays.fill(isColumnEmpty, true);

        for (List<String> row : tableData) {
            for (int i = 0; i < Math.min(row.size(), columnCount); i++) {
                String cellValue = row.get(i);
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

        // Limit column width, avoid too wide, ensure minimum width
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = Math.min(columnWidths[i], 20);
            // Ensure minimum width is 1 to avoid formatting exceptions
            columnWidths[i] = Math.max(columnWidths[i], 1);
        }

        // Output table, only display valid columns
        int consecutiveEmptyRows = 0;
        // Display at most 2 consecutive empty rows
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
                    continue; // Skip this row
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

                // Format output, ensure column width is greater than 0
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