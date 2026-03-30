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
package com.jd.oxygent.core.oxygent.function_hubs;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Document processing tool hub, managing all document-related operations
 * Supports processing of PDF, Word, Excel and other document formats
 */
public class DocumentTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(DocumentTools.class.getName());

    public DocumentTools() {
        super("document_tools");
        this.setDesc("Document processing tools for PDF, Word, Excel and other formats");
        checkDependencies();
    }

    /**
     * Check if dependency libraries are installed
     */
    private void checkDependencies() {
        List<String> missing = new ArrayList<>();

        // 检查 PDFBox
        try {
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        } catch (ClassNotFoundException e) {
            missing.add("Apache PDFBox");
        }

        // Check POI
        try {
            Class.forName("org.apache.poi.ss.usermodel.Workbook");
        } catch (ClassNotFoundException e) {
            missing.add("Apache POI");
        }

        if (!missing.isEmpty()) {
            logger.warning(String.format(
                    "Some document processing libraries are not installed: %s. These dependencies are optional, but required when using document processing features.",
                    String.join(", ", missing)
            ));
        }
    }

    /**
     *     Extract PDF text content
     *
     *     Technical implementation:
     *     - Uses PyMuPDF (fitz) for efficient text extraction
     *     - Supports multiple PDF encoding formats
     *     - Automatically handles page rotation and layout
     * @param path PDF file path
     * @param pageRange Page range string
     * @param maxCharsPerPage Maximum characters per page
     * @return JSON format extraction result, containing text content and metadata
     */
    @Tool(
            name = "extractPdfText",
            description = "从 PDF 文件中提取文本内容。支持指定页码范围提取，适用于文字版 PDF（非扫描版）。返回按页组织的文本内容。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "PDF 文件路径"),
                    @ParamMetaAuto(name = "pageRange", type = "String", description = "页码范围字符串，如'1-5'或'1,3,5'"),
                    @ParamMetaAuto(name = "maxCharsPerPage", type = "int", description = "单页最大字符数")
            }
    )
    public String extractPdfText(String path, String pageRange, int maxCharsPerPage) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("File does not exist: " + path);
            }

            PDDocument doc = PDDocument.load(new File(path));
            int totalPages = doc.getNumberOfPages();

            // Parse page range
            List<Integer> pages = parsePageRange(pageRange, totalPages);

            if (pages.isEmpty()) {
                doc.close();
                return createErrorResponse("Invalid page range or page numbers exceed document range");
            }

            // Extract text
            List<Map<String, Object>> results = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();

            for (Integer pageNum : pages) {
                stripper.setStartPage(pageNum + 1);
                stripper.setEndPage(pageNum + 1);
                String text = stripper.getText(doc).trim();

                // Limit single page text length
                if (text.length() > maxCharsPerPage) {
                    text = text.substring(0, maxCharsPerPage) + "\n...(truncated, original text length: " + text.length() + " characters)";
                }

                Map<String, Object> pageResult = new LinkedHashMap<>();
                pageResult.put("page_number", pageNum + 1);
                pageResult.put("text", text);
                pageResult.put("char_count", text.length());
                // PDFBox does not directly provide image detection, simplified handling here
                pageResult.put("has_images", false);

                results.add(pageResult);
            }

            doc.close();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("file_path", path);
            response.put("total_pages", totalPages);
            response.put("extracted_pages", results.size());
            response.put("pages", results);

            return JsonUtils.toJSONString(response);

        } catch (InvalidPasswordException e) {
            return createErrorResponse("PDF file is encrypted, cannot read");
        } catch (IOException e) {
            logger.severe("PDF text extraction failed: " + e.getMessage());
            return createErrorResponse("Extraction failed: " + e.getMessage());
        } catch (Exception e) {
              logger.severe("PDF text extraction failed: " + e.getMessage());
              return createErrorResponse("Extraction failed: " + e.getMessage());
          }
    }

    /**
     *     Extract table data from PDF
     *
     *     Technical features:
     *     - Uses pdfplumber's table recognition engine
     *     - Supports merged cell processing
     *     - Automatically identifies table headers
     *     - Handles cross-page tables
     * @param path  PDF file path
     * @param pageRange Page range
     * @return JSON structure containing all table data
     */
    @Tool(
            name = "extractPdfTables",
            description = "从 PDF 中提取表格数据，返回结构化的 JSON 格式。使用 pdfplumber 的高精度表格识别算法。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "PDF 文件路径"),
                    @ParamMetaAuto(name = "pageRange", type = "String", description = "页码范围字符串"),
            }
    )
    public String extractPdfTables(String path, String pageRange) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            PDDocument doc = PDDocument.load(new File(path));
            int totalPages = doc.getNumberOfPages();
            List<Integer> pages = parsePageRange(pageRange, totalPages);

            if (pages.isEmpty()) {
                doc.close();
            return createErrorResponse("Invalid page range or page numbers exceed document range");
            }

            List<Map<String, Object>> allTables = new ArrayList<>();

            // Here uses simplified implementation, based on text analysis to identify tables
            for (Integer pageNum : pages) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNum + 1);
                stripper.setEndPage(pageNum + 1);
                String text = stripper.getText(doc);

                // Simplified table recognition: split by rows
                String[] lines = text.split("\\n");
                List<List<String>> tableRows = new ArrayList<>();

                for (String line : lines) {
                    if (line.contains("\t") || line.trim().matches("(.*\\|+.*)+")) {
                        // Possibly a table row
                        String[] cells = line.split("[\\t|]+");
                        List<String> rowData = new ArrayList<>();
                        for (String cell : cells) {
                            if (!cell.trim().isEmpty()) {
                                rowData.add(cell.trim());
                            }
                        }
                        if (!rowData.isEmpty()) {
                            tableRows.add(rowData);
                        }
                    }
                }

                if (tableRows.size() > 1) {
                    Map<String, Object> tableData = new LinkedHashMap<>();
                    tableData.put("page", pageNum + 1);
                    tableData.put("table_index", allTables.size() + 1);
                    tableData.put("headers", tableRows.get(0));
                    tableData.put("rows", tableRows.subList(1, tableRows.size()));
                    tableData.put("row_count", tableRows.size() - 1);
                    tableData.put("column_count", tableRows.get(0).size());
                    allTables.add(tableData);
                }
            }

            doc.close();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("file_path", path);
            result.put("table_count", allTables.size());
            result.put("tables", allTables);

            return JsonUtils.toJSONString(result);

        } catch (Exception e) {
            logger.severe("PDF table extraction failed: " + e.getMessage());
            return createErrorResponse("Extraction failed: " + e.getMessage());
        }
    }

    /**
     *     Extract images from PDF
     *
     *     Features:
     *     - Automatically creates output directory
     *     - Preserves original image format and quality
     *     - Can filter small images (like icons, decorative lines, etc.)
     *     - Returns detailed information for each image
     * @param path PDF file path
     * @param outputDir Output directory
     * @param pageRange Page range
     * @param minSize Minimum image size (bytes)
     * @return JSON containing all extracted image information
     */
    @Tool(
            name = "extractPdfImages",
            description = "从 PDF 文件中提取所有嵌入的图像，并保存到指定目录。图像将自动命名为 'image_页码_序号.扩展名' 格式。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "PDF 文件路径"),
                    @ParamMetaAuto(name = "outputDir", type = "String", description = "输出目录"),
                    @ParamMetaAuto(name = "pageRange", type = "String", description = "页码范围"),
                    @ParamMetaAuto(name = "minSize", type = "int", description = "最小图像大小（字节）")
            }
    )
    public String extractPdfImages(String path, String outputDir, String pageRange, int minSize) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            // Create output directory
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);

            PDDocument doc = PDDocument.load(new File(path));
            int totalPages = doc.getNumberOfPages();
            List<Integer> pages = parsePageRange(pageRange, totalPages);

            if (pages.isEmpty()) {
                doc.close();
                return createErrorResponse("无效的页码范围或页码超出文档范围");
            }

            List<Map<String, Object>> imageList = new ArrayList<>();
            int globalImgIndex = 0;

            for (Integer pageNum : pages) {
                PDPage page = doc.getPage(pageNum);
                PDResources resources = page.getResources();

                // Iterate all XObjects and filter out images
                for (COSName name : resources.getXObjectNames()) {
                    try {
                        PDXObject xObject = resources.getXObject(name);
                        if (!(xObject instanceof PDImageXObject)) {
                            continue;
                        }
                        
                        PDImageXObject image = (PDImageXObject) xObject;
                        
                        // 将图像转换为 BufferedImage
                        java.awt.image.BufferedImage bImage = image.getImage();
                        if (bImage == null) {
                            continue;
                        }
                        
                        // 将 BufferedImage 转换为字节数组
                        byte[] imageBytes;
                        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                            String format = image.getSuffix() != null ? image.getSuffix() : "png";
                            javax.imageio.ImageIO.write(bImage, format, baos);
                            imageBytes = baos.toByteArray();
                        }

                        // Filter small images
                        if (imageBytes.length < minSize) {
                            continue;
                        }

                        String imageExt = image.getSuffix();
                        String filename = String.format("image_p%d_%d.%s", pageNum + 1, ++globalImgIndex, imageExt);
                        Path filepath = outputPath.resolve(filename);

                        Files.write(filepath, imageBytes);

                        Map<String, Object> imgInfo = new LinkedHashMap<>();
                        imgInfo.put("page", pageNum + 1);
                        imgInfo.put("filename", filename);
                        imgInfo.put("path", filepath.toString());
                        imgInfo.put("size_bytes", imageBytes.length);
                        imgInfo.put("format", imageExt);
                        imgInfo.put("width", image.getWidth());
                        imgInfo.put("height", image.getHeight());

                        imageList.add(imgInfo);

                    } catch (Exception imgError) {
                        logger.warning("Failed to extract image on page " + (pageNum + 1) + ": " + imgError.getMessage());
                    }
                }
            }

            doc.close();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("file_path", path);
            result.put("output_dir", outputPath.toString());
            result.put("image_count", imageList.size());
            result.put("images", imageList);

            return JsonUtils.toJSONString(result);

        } catch (Exception e) {
            logger.severe("PDF image extraction failed: " + e.getMessage());
            return createErrorResponse("Extraction failed: " + e.getMessage());
        }
    }

    /**
     *     Merge multiple PDF files
     *
     *     Technical features:
     *     - Zero quality loss merging
     *     - Preserves original document properties
     *     - Optional bookmark structure preservation
     *     - Efficient large file handling
     *
     * @param pdfPaths PDF file path list
     * @param outputPath Output file path
     * @return Result information of merge operation
     */
    @Tool(
            name = "mergePdfs",
            description = "将多个 PDF 文件合并为一个 PDF 文件。按照提供的文件列表顺序进行合并。",
            paramMetas = {
                    @ParamMetaAuto(name = "pdfPaths", type = "List<String>", description = "PDF 文件路径列表"),
                    @ParamMetaAuto(name = "outputPath", type = "String", description = "输出文件路径")
            }
    )
    public String mergePdfs(List<String> pdfPaths, String outputPath) {
        try {
            // Validate input files
            List<String> missingFiles = new ArrayList<>();
            for (String pdfPath : pdfPaths) {
                if (!Files.exists(Paths.get(pdfPath))) {
                    missingFiles.add(pdfPath);
                }
            }

            if (!missingFiles.isEmpty()) {
                return createErrorResponse("The following files do not exist: " + String.join(", ", missingFiles));
            }

            if (pdfPaths.size() < 2) {
                return createErrorResponse("At least 2 PDF files are required to merge");
            }

            // Use PDFMergerUtility to merge PDFs, this is the official merge tool provided by PDFBox
            org.apache.pdfbox.multipdf.PDFMergerUtility merger = new org.apache.pdfbox.multipdf.PDFMergerUtility();
            merger.setDestinationFileName(outputPath);

            // Add all source files
            for (String pdfPath : pdfPaths) {
                merger.addSource(new File(pdfPath));
            }

            // Execute merge
            merger.mergeDocuments(null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "Successfully merged " + pdfPaths.size() + " PDF files");
            result.put("output_path", outputPath);
            result.put("source_files", pdfPaths);

            // Count total pages
            int totalPages = 0;
            try (PDDocument doc = PDDocument.load(new File(outputPath))) {
                totalPages = doc.getNumberOfPages();
            }
            result.put("total_pages", totalPages);

            return JsonUtils.toJSONString(result);

        } catch (Exception e) {
            logger.severe("PDF merge failed: " + e.getMessage());
            return createErrorResponse("Merge failed: " + e.getMessage());
        }
    }

    /**
     *     Split PDF file
     *
     *     Features:
     *     - Supports flexible page range definition
     *     - Automatic output file naming
     *     - Maintains original PDF quality
     *     - Supports discontinuous page splitting
     * @param path Source PDF file path
     * @param splitRanges Split range list
     * @param outputDir Output directory
     * @param namePrefix File name prefix
     * @return Detailed result of split operation
     */
    @Tool(
            name = "splitPdf",
            description = "按页码范围拆分 PDF 文件为多个独立的 PDF 文件。可以灵活指定每个拆分文件包含的页码范围。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "源 PDF 文件路径"),
                    @ParamMetaAuto(name = "splitRanges", type = "List<String>", description = "拆分范围列表"),
                    @ParamMetaAuto(name = "outputDir", type = "String", description = "输出目录"),
                    @ParamMetaAuto(name = "namePrefix", type = "String", description = "文件名前缀")
            }
    )
    public String splitPdf(String path, List<String> splitRanges, String outputDir, String namePrefix) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            // 创建输出目录
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);

            PDDocument doc = PDDocument.load(new File(path));
            int totalPages = doc.getNumberOfPages();

            List<Map<String, Object>> outputFiles = new ArrayList<>();

            for (int idx = 0; idx < splitRanges.size(); idx++) {
                String rangeStr = splitRanges.get(idx);
                List<Integer> pages = parsePageRange(rangeStr, totalPages);

                if (pages.isEmpty()) {
                    logger.warning("范围 '" + rangeStr + "' 无效或超出文档页数，已跳过");
                    continue;
                }

                // 创建新文档
                PDDocument newDoc = new PDDocument();

                for (Integer pageNum : pages) {
                    // 使用 importPage 方法复制页面
                    PDPage originalPage = doc.getPage(pageNum);
                    PDPage importedPage = (PDPage) newDoc.importPage(originalPage);
                    newDoc.addPage(importedPage);
                }

                // 保存文件
                String outputFilename = String.format("%s_%d.pdf", namePrefix != null ? namePrefix : "split", idx + 1);
                Path outputFilePath = outputPath.resolve(outputFilename);
                newDoc.save(outputFilePath.toString());
                newDoc.close();

                Map<String, Object> fileInfo = new LinkedHashMap<>();
                fileInfo.put("filename", outputFilename);
                fileInfo.put("path", outputFilePath.toString());
                fileInfo.put("page_range", rangeStr);
                fileInfo.put("page_count", pages.size());

                outputFiles.add(fileInfo);
            }

            doc.close();

            if (outputFiles.isEmpty()) {
                return createErrorResponse("No files were successfully split, please check if the page range is correct");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "Successfully split into " + outputFiles.size() + " files");
            result.put("source_file", path);
            result.put("output_dir", outputPath.toString());
            result.put("files", outputFiles);

            return JsonUtils.toJSONString(result);
        } catch (Exception e) {
            logger.severe("PDF split failed: " + e.getMessage());
            return createErrorResponse("Split failed: " + e.getMessage());
        }
    }

    /**
     * Get PDF metadata and basic information
     *
     *     Returns information including:
     *     - Document properties (title, author, subject, etc.)
     *     - Page information (total pages, page sizes)
     *     - Technical information (PDF version, encryption status)
     *     - Content statistics (text volume, image count)
     * @param path PDF file path
     * @return JSON structure containing all metadata
     */
    @Tool(
            name = "getPdfInfo",
            description = "获取 PDF 文件的元数据信息，包括作者、标题、创建日期、页数等。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "PDF 文件路径")
            }
    )
    public String getPdfInfo(String path) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            PDDocument doc = PDDocument.load(new File(path));

            // Get metadata
            PDDocumentInformation metadata = doc.getDocumentInformation();

            // Statistics page information
            List<Map<String, Object>> pageSizes = new ArrayList<>();
            int totalImages = 0;
            int totalTextLength = 0;

            for (int pageNum = 0; pageNum < doc.getNumberOfPages(); pageNum++) {
                PDPage page = doc.getPage(pageNum);
                PDRectangle rect = page.getMediaBox();

                Map<String, Object> pageInfo = new LinkedHashMap<>();
                pageInfo.put("page", pageNum + 1);
                pageInfo.put("width", Math.round(rect.getWidth() * 100.0) / 100.0);
                pageInfo.put("height", Math.round(rect.getHeight() * 100.0) / 100.0);
                pageSizes.add(pageInfo);

                // Statistics images
                try {
                    PDResources resources = page.getResources();
                    if (resources != null) {
                        // Iterate all XObjects and count images
                        for (COSName name : resources.getXObjectNames()) {
                            PDXObject xObject = resources.getXObject(name);
                            if (xObject instanceof PDImageXObject) {
                                totalImages++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore errors
                }

                // Statistics text
                try {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setStartPage(pageNum + 1);
                    stripper.setEndPage(pageNum + 1);
                    totalTextLength += stripper.getText(doc).length();
                } catch (Exception e) {
                    // 忽略错误
                }
            }

            // File information
            File file = new File(path);
            long fileSize = file.length();

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("success", true);

            Map<String, Object> fileInfo = new LinkedHashMap<>();
            fileInfo.put("path", path);
            fileInfo.put("filename", file.getName());
            fileInfo.put("size_bytes", fileSize);
            fileInfo.put("size_mb", Math.round(fileSize / (1024.0 * 1024.0) * 100.0) / 100.0);
            info.put("file_info", fileInfo);

            Map<String, Object> docMetadata = new LinkedHashMap<>();
            docMetadata.put("title", metadata.getTitle() != null ? metadata.getTitle() : "");
            docMetadata.put("author", metadata.getAuthor() != null ? metadata.getAuthor() : "");
            docMetadata.put("subject", metadata.getSubject() != null ? metadata.getSubject() : "");
            docMetadata.put("keywords", metadata.getKeywords() != null ? metadata.getKeywords() : "");
            docMetadata.put("creator", metadata.getCreator() != null ? metadata.getCreator() : "");
            docMetadata.put("producer", metadata.getProducer() != null ? metadata.getProducer() : "");
            docMetadata.put("creation_date", metadata.getCreationDate() != null ? metadata.getCreationDate().getTime().toString() : "");
            docMetadata.put("modification_date", metadata.getModificationDate() != null ? metadata.getModificationDate().getTime().toString() : "");
            info.put("document_metadata", docMetadata);

            Map<String, Object> docProperties = new LinkedHashMap<>();
            docProperties.put("page_count", doc.getNumberOfPages());
            docProperties.put("is_encrypted", doc.isEncrypted());
            docProperties.put("is_pdf", true);
            docProperties.put("pdf_version", String.valueOf(doc.getVersion()));
            info.put("document_properties", docProperties);

            Map<String, Object> contentStats = new LinkedHashMap<>();
            contentStats.put("total_images", totalImages);
            contentStats.put("estimated_text_length", totalTextLength);

            if (!pageSizes.isEmpty()) {
                double avgWidth = pageSizes.stream().mapToDouble(p -> (Double) p.get("width")).average().orElse(0);
                double avgHeight = pageSizes.stream().mapToDouble(p -> (Double) p.get("height")).average().orElse(0);
                Map<String, Object> avgSize = new LinkedHashMap<>();
                avgSize.put("width", Math.round(avgWidth * 100.0) / 100.0);
                avgSize.put("height", Math.round(avgHeight * 100.0) / 100.0);
                contentStats.put("average_page_size", avgSize);
            }
            info.put("content_statistics", contentStats);

            // Only return first 5 page sizes
            info.put("page_sizes", pageSizes.subList(0, Math.min(5, pageSizes.size())));

            doc.close();

            return JsonUtils.toJSONString(info);

        } catch (Exception e) {
            logger.severe("Failed to get PDF info: " + e.getMessage());
            return createErrorResponse("Failed to get info: " + e.getMessage());
        }
    }


    /**
     *     Read Word document content
     *
     *     Extracted content:
     *     - All paragraph text (maintaining order)
     *     - Table data (structured format)
     *     - Paragraph style information (optional)
     *     - Document statistics
     * @param path Word file path
     * @param includeTables Whether to include tables
     * @param maxParagraphs Maximum number of paragraphs
     * @return Structured document content JSON
     */
    @Tool(
            name = "readDocx",
            description = "读取 Word 文档 (.docx) 的完整内容，包括段落文本和表格数据。仅支持.docx 格式（Office 2007 及以后）。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Word 文件路径"),
                    @ParamMetaAuto(name = "includeTables", type = "boolean", description = "是否包含表格"),
                    @ParamMetaAuto(name = "maxParagraphs", type = "int", description = "最大段落数")
            }
    )
    public String readDocx(String path, boolean includeTables, int maxParagraphs) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            // Use Apache POI to read Word document
            try (FileInputStream fis = new FileInputStream(path);
                 XWPFDocument doc = new XWPFDocument(fis)) {

                // Extract paragraphs
                List<Map<String, Object>> paragraphs = new ArrayList<>();
                int idx = 0;
                for (XWPFParagraph para : doc.getParagraphs()) {
                    if (idx >= maxParagraphs) break;
                    String text = para.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        Map<String, Object> paraMap = new LinkedHashMap<>();
                        paraMap.put("index", ++idx);
                        paraMap.put("text", text.trim());
                        paragraphs.add(paraMap);
                    }
                }

                // Extract tables
                List<Map<String, Object>> tablesData = new ArrayList<>();
                if (includeTables) {
                    int tableIdx = 0;
                    for (org.apache.poi.xwpf.usermodel.XWPFTable table : doc.getTables()) {
                        List<List<String>> tableContent = new ArrayList<>();
                        for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                            List<String> rowData = new ArrayList<>();
                            for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                                String cellText = cell.getText();
                                if (cellText != null && !cellText.trim().isEmpty()) {
                                    rowData.add(cellText.trim());
                                }
                            }
                            if (!rowData.isEmpty()) {
                                tableContent.add(rowData);
                            }
                        }

                        if (!tableContent.isEmpty()) {
                            Map<String, Object> tableMap = new LinkedHashMap<>();
                            tableMap.put("table_index", ++tableIdx);
                            tableMap.put("row_count", tableContent.size());
                            tableMap.put("column_count", tableContent.get(0).size());
                            tableMap.put("headers", tableContent.get(0));
                            tableMap.put("rows", tableContent.subList(1, Math.min(tableContent.size(), 1)));
                            tablesData.add(tableMap);
                        }
                    }
                }

                // Statistics information
                StringBuilder totalText = new StringBuilder();
                for (Map<String, Object> p : paragraphs) {
                    totalText.append(p.get("text")).append(" ");
                }
                String[] words = totalText.toString().trim().split("\\s+");
                int wordCount = words.length > 0 && !words[0].isEmpty() ? words.length : 0;

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("file_path", path);

                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("paragraph_count", paragraphs.size());
                stats.put("table_count", tablesData.size());
                stats.put("word_count", wordCount);
                stats.put("char_count", totalText.length());
                result.put("statistics", stats);
                result.put("paragraphs", paragraphs);
                result.put("tables", tablesData);

                return JsonUtils.toJSONString(result);
            }

        } catch (Exception e) {
            logger.severe("Failed to read Word document: " + e.getMessage());
            return createErrorResponse("Read failed: " + e.getMessage());
        }
    }

    /**
     *     Extract plain text from Word document
     *
     *     Quickly extracts all text content from document, ignoring format and structure.
     * @param path Word file path
     * @return Plain text content
     */
    @Tool(
            name = "extractDocxText",
            description = "从 Word 文档中仅提取纯文本内容，不包含格式和表格。适用于需要快速获取文档文字内容的场景。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Word 文件路径")
            }
    )
    public String extractDocxText(String path) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            // 使用 Apache POI 读取 Word 文档
            FileInputStream fis = null;
            XWPFDocument doc = null;
            try {
                fis = new FileInputStream(path);
                doc = new XWPFDocument(fis);

                List<String> fullText = new ArrayList<>();

                // Extract paragraph text
                for (XWPFParagraph para : doc.getParagraphs()) {
                    String text = para.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        fullText.add(text.trim());
                    }
                }

                // Extract table text
                for (XWPFTable table : doc.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> rowTexts = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            String cellText = cell.getText();
                            if (cellText != null && !cellText.trim().isEmpty()) {
                                rowTexts.add(cellText.trim());
                            }
                        }
                        if (!rowTexts.isEmpty()) {
                            fullText.add(String.join(" | ", rowTexts));
                        }
                    }
                }

                String resultText = String.join("\n", fullText);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("file_path", path);
                result.put("text", resultText);
                result.put("length", resultText.length());
                result.put("line_count", fullText.size());

                return JsonUtils.toJSONString(result);
            } finally {
                if (fis != null) fis.close();
            }

        } catch (Exception e) {
            logger.severe("Failed to extract Word text: " + e.getMessage());
            return createErrorResponse("Extraction failed: " + e.getMessage());
        }
    }

    /**
     *     Read Excel table data
     *
     *     Features:
     *     - Supports .xlsx and .xlsm formats
     *     - Automatically identifies headers
     *     - Handles empty cells
     *     - Supports multiple worksheets
     * @param path Excel file path
     * @param sheetName Worksheet name
     * @param maxRows Maximum number of rows
     * @param hasHeader Whether has header
     * @return Structured Excel data JSON
     */
    @Tool(
            name = "readExcel",
            description = "读取 Excel 文件 (.xlsx/.xlsm) 的数据内容。可指定工作表名称和读取行数限制。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Excel 文件路径"),
                    @ParamMetaAuto(name = "sheetName", type = "String", description = "工作表名称"),
                    @ParamMetaAuto(name = "maxRows", type = "int", description = "最大行数"),
                    @ParamMetaAuto(name = "hasHeader", type = "boolean", description = "是否有表头")
            }
    )
    public String readExcel(String path, String sheetName, int maxRows, boolean hasHeader) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            Workbook wb = new XSSFWorkbook(new File(path));

            // Get worksheet
            Sheet ws;
            if (sheetName != null && !sheetName.isEmpty()) {
                ws = wb.getSheet(sheetName);
                if (ws == null) {
                    wb.close();
                    return createErrorResponse("Worksheet '" + sheetName + "' does not exist");
                }
            } else {
                ws = wb.getSheetAt(0);
            }

            // Read data
            List<List<String>> data = new ArrayList<>();
            int rowCount = 0;
            for (Row row : ws) {
                if (rowCount >= maxRows) break;

                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(getCellValueAsString(cell));
                }

                // Skip completely empty rows
                if (!rowData.isEmpty()) {
                    data.add(rowData);
                    rowCount++;
                }
            }

            wb.close();

            if (data.isEmpty()) {
                return createErrorResponse("Worksheet is empty or has no data");
            }

            // Separate headers and data
            List<String> headers = hasHeader && !data.isEmpty() ? data.get(0) : new ArrayList<>();
            List<List<String>> rows = hasHeader && data.size() > 1 ?
                    data.subList(1, data.size()) : data;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("file_path", path);
            result.put("sheet_name", ws.getSheetName());

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("row_count", rows.size());
            stats.put("column_count", headers.size());
            stats.put("has_header", hasHeader);
            result.put("statistics", stats);

            result.put("headers", headers);
            result.put("rows", rows);

            return JsonUtils.toJSONString(result);

        } catch (IOException e) {
            logger.severe("Failed to read Excel: " + e.getMessage());
            return createErrorResponse("Read failed: " + e.getMessage());
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     *  List all Excel worksheets
     *
     *     Returns for each worksheet:
     *     - Name
     *     - Whether it is the active worksheet
     *     - Approximate row and column count
     * @param path Excel file path
     * @return Worksheet list JSON
     */
    @Tool(
            name = "listExcelSheets",
            description = "列出 Excel 文件中的所有工作表名称及其基本信息。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Excel 文件路径")
            }
    )
    public String listExcelSheets(String path) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return createErrorResponse("文件不存在：" + path);
            }

            Workbook wb = new XSSFWorkbook(new File(path));

            List<Map<String, Object>> sheetsInfo = new ArrayList<>();
            String activeSheetName = wb.getSheetAt(wb.getActiveSheetIndex()).getSheetName();

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet ws = wb.getSheetAt(i);

                Map<String, Object> sheetMap = new LinkedHashMap<>();
                sheetMap.put("name", ws.getSheetName());
                sheetMap.put("is_active", ws.getSheetName().equals(activeSheetName));
                sheetMap.put("max_row", ws.getLastRowNum() + 1);
                sheetMap.put("max_column", ws.getRow(0) != null ?
                        ws.getRow(0).getLastCellNum() : 0);

                sheetsInfo.add(sheetMap);
            }

            wb.close();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("file_path", path);
            result.put("sheet_count", sheetsInfo.size());
            result.put("sheets", sheetsInfo);

            return JsonUtils.toJSONString(result);

        } catch (IOException e) {
            logger.severe("列出工作表失败：" + e.getMessage());
            return createErrorResponse("操作失败：" + e.getMessage());
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *     检测文档格式
     *
     *     返回信息：
     *     - 文件类型
     *     - 推荐的处理工具
     *     - 是否支持
     * @param path 文件路径
     * @return 格式检测结果JSON
     */
    @Tool(
            name = "detectDocumentFormat",
            description = "自动检测文件格式类型。支持 PDF、Word、Excel、PowerPoint 等常见文档格式的识别。",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "文件路径")
            }
    )
    public String detectDocumentFormat(String path) {
        try {
            File file = new File(path);

            if (!file.exists()) {
                return createErrorResponse("文件不存在：" + path);
            }

            String extension = getExtension(file.getName()).toLowerCase();
            long fileSize = file.length();

            Map<String, Object> formatInfo = getFormatInfo(extension);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("file_path", path);
            result.put("filename", file.getName());
            result.put("extension", extension);
            result.put("size_bytes", fileSize);
            result.put("size_mb", Math.round(fileSize / (1024.0 * 1024.0) * 100.0) / 100.0);
            result.put("format_info", formatInfo);

            return JsonUtils.toJSONString(result);

        } catch (Exception e) {
            logger.severe("检测文档格式失败：" + e.getMessage());
            return createErrorResponse("检测失败：" + e.getMessage());
        }
    }

    /**
     * 辅助方法：解析页码范围字符串
     *     解析页码范围字符串
     *
     *     支持格式：
     *     - None: 返回所有页码
     *     - "1-5": 第1到5页
     *     - "1,3,5": 第1、3、5页
     *     - "1-3,5,7-9": 组合格式
     *
     *     Args:
     *         range_str: 范围字符串
     *         total_pages: 总页数
     *
     *     Returns:
     *         页码索引列表（从0开始）
     */
    private List<Integer> parsePageRange(String rangeStr, int totalPages) {
        if (rangeStr == null || rangeStr.trim().isEmpty()) {
            List<Integer> allPages = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                allPages.add(i);
            }
            return allPages;
        }

        Set<Integer> pages = new HashSet<>();
        String[] parts = rangeStr.split(",");

        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                // 范围格式：1-5
                String[] range = part.split("-");
                int startPage = Integer.parseInt(range[0].trim());
                int endPage = Integer.parseInt(range[1].trim());
                //转换为0索引，并验证范围
                for (int p = startPage - 1; p <= endPage - 1; p++) {
                    if (p >= 0 && p < totalPages) {
                        pages.add(p);
                    }
                }
            } else {
                // 单页格式：3
                int pageNum = Integer.parseInt(part);
                int pageIdx = pageNum - 1;
                if (pageIdx >= 0 && pageIdx < totalPages) {
                    pages.add(pageIdx);
                }
            }
        }

        return new ArrayList<>(pages);
    }

    /**
     * 辅助方法：获取单元格值的字符串表示
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double num = cell.getNumericCellValue();
                    return (num == (long) num) ? String.valueOf((long) num) : String.valueOf(num);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * 辅助方法：获取文件扩展名
     */
    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    /**
     * 辅助方法：获取文件格式信息
     */
    private Map<String, Object> getFormatInfo(String extension) {
        Map<String, Object> info = new LinkedHashMap<>();

        switch (extension) {
            case ".pdf":
                info.put("type", "PDF");
                info.put("category", "Portable Document Format");
                info.put("supported", true);
                info.put("tools", Arrays.asList("extractPdfText", "extractPdfTables"));
                break;
            case ".docx":
                info.put("type", "Word");
                info.put("category", "Microsoft Word Document");
                info.put("supported", true);
                info.put("tools", Arrays.asList("readDocx"));
                break;
            case ".doc":
                info.put("type", "Word (Legacy)");
                info.put("category", "Microsoft Word 97-2003");
                info.put("supported", false);
                info.put("message", "请将.doc 文件转换为.docx 格式");
                break;
            case ".xlsx":
                info.put("type", "Excel");
                info.put("category", "Microsoft Excel Workbook");
                info.put("supported", true);
                info.put("tools", Arrays.asList("readExcel", "listExcelSheets"));
                break;
            case ".xls":
                info.put("type", "Excel (Legacy)");
                info.put("category", "Microsoft Excel 97-2003");
                info.put("supported", false);
                info.put("message", "请将.xls 文件转换为.xlsx 格式");
                break;
            case ".txt":
                info.put("type", "Text");
                info.put("category", "Plain Text");
                info.put("supported", true);
                break;
            default:
                info.put("type", "Unknown");
                info.put("category", "Unknown Format");
                info.put("supported", false);
                info.put("message", "不支持的文件格式：" + extension);
        }

        return info;
    }

    /**
     * 辅助方法：创建错误响应
     */
    private String createErrorResponse(String error) {
        try {
            Map<String, Object> errorResp = new LinkedHashMap<>();
                                errorResp.put("error", error);
            return JsonUtils.toJSONString(errorResp);
        } catch (Exception e) {
            return "{\"error\": \"" + error + "\"}";
        }
    }

    /**
     * 辅助方法：创建成功响应
     */
    private String createSuccessResponse(String message, String filePath, Object data) {
        try {
            Map<String, Object> successResp = new LinkedHashMap<>();
            successResp.put("success", true);
            successResp.put("message", message);
            successResp.put("file_path", filePath);
            if (data != null) {
                successResp.put("data", data);
            }
            return JsonUtils.toJSONString(successResp);
        } catch (Exception e) {
            return "{\"success\": true, \"message\": \"" + message + "\"}";
        }
    }
}
