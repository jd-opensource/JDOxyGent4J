package com.jd.oxygent.oxybank.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * File recursive processing utility
 * Provides recursive traversal of files and directories, MD5 calculation, file type detection, etc.
 * Supports the document ingestion process of the RAG system.
 * Converted from oxybank/utils/files_process.py
 */
@Slf4j
public class FilesProcess {
    
    // Supported file types set
    public static final Set<String> SUPPORTED_FILE_TYPES = Set.of(
        "txt", "md", "markdown", "rst", "csv",
        "xlsx", "xls", "pdf", "docx", "doc"
    );
    
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.getObjectMapper();
    
    /**
     * Calculate MD5 hash value of a file
     * 
     * @param filePath Absolute path of the file
     * @return MD5 hash value of the file (32-character hexadecimal string)
     * @throws FileNotFoundException If file does not exist
     * @throws SecurityException If no read permission
     * @throws IOException If an I/O error occurs
     */
    public static String calculateFileMd5(String filePath) throws FileNotFoundException, SecurityException, IOException {
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File does not exist: " + filePath);
        }
        
        if (!Files.isReadable(path)) {
            throw new SecurityException("No read permission: " + filePath);
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // Read file in chunks to avoid excessive memory usage for large files
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            try (InputStream is = Files.newInputStream(path)) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            // Convert to hexadecimal representation
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : md.digest()) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    /**
     * Extract file type (extension)
     * 
     * @param filePath File path
     * @return File extension (lowercase, without dot)
     */
    public static String extractFileType(String filePath) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * Check if file type is supported
     * 
     * @param filePath File path
     * @param supportedTypes Set of supported file types, defaults to SUPPORTED_FILE_TYPES
     * @return Whether file type is supported
     */
    public static boolean isSupportedFile(String filePath, Set<String> supportedTypes) {
        if (supportedTypes == null) {
            supportedTypes = SUPPORTED_FILE_TYPES;
        }
        
        String fileType = extractFileType(filePath);
        return supportedTypes.contains(fileType);
    }
    
    /**
     * Get relative path of a file
     * 
     * @param directoryPath Directory path
     * @param filePath File path
     * @return Relative path
     */
    public static String getRelativePath(String directoryPath, String filePath) {
        Path dirPath = Paths.get(directoryPath).normalize();
        Path fileAbsolutePath = Paths.get(filePath).normalize();
        
        String dirName = dirPath.getFileName().toString();
        Path relPath = dirPath.relativize(fileAbsolutePath);
        
        return "/" + dirName + "/" + relPath.toString().replace(File.separatorChar, '/');
    }
    
    /**
     * Process a single file and generate file information map
     * 
     * @param kbId Knowledge base ID
     * @param kbRelPath File path relative to knowledge base
     * @param filePath Absolute path of the file
     * @return Map containing file information
     * @throws Exception Raised when file processing fails
     */
    public static Map<String, Object> getFileInfo(String kbId, String kbRelPath, String filePath) throws Exception {
        try {
            // Ensure path is absolute
            Path absPath = Paths.get(filePath).toAbsolutePath();
            String absPathStr = absPath.toString();
            
            // Calculate MD5 for deduplication
            String md5Value = calculateFileMd5(absPathStr);
            
            // Extract file information
            String fileName = absPath.getFileName().toString();
            String fileType = extractFileType(absPathStr);
            
            // Generate file ID based on relative path in knowledge base
            String fileId = HashUtil.strToMd5(kbRelPath);
            
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("ori_file_id", fileId);
            fileInfo.put("kb_id", kbId);
            fileInfo.put("file_name", fileName);
            fileInfo.put("file_kb_path", kbRelPath);
            fileInfo.put("file_path", absPathStr);
            fileInfo.put("document_md5", md5Value);
            fileInfo.put("ori_file_type", fileType.isEmpty() ? "" : fileType);
            fileInfo.put("file_store_mode", "unstructured");
            fileInfo.put("file_extra_info", new HashMap<>());
            fileInfo.put("language", "zh");
            
            return fileInfo;
        } catch (Exception e) {
            throw new Exception("Failed to process file " + filePath + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Recursively traverse directory and collect all supported file information
     * 
     * @param directoryPath Directory path
     * @param supportedTypes Set of supported file types, defaults to SUPPORTED_FILE_TYPES
     * @param skipHidden Whether to skip hidden files and directories, defaults to true
     * @return List of file information maps
     * @throws NotDirectoryException If path is not a directory
     * @throws SecurityException If no access permission
     */
    public static List<Map<String, Object>> traverseDirectory(String directoryPath, Set<String> supportedTypes, boolean skipHidden)
            throws NotDirectoryException, SecurityException {
        Path dirPath = Paths.get(directoryPath);
        
        if (!Files.isDirectory(dirPath)) {
            throw new NotDirectoryException("Path is not a directory: " + directoryPath);
        }
        
        if (!Files.isReadable(dirPath)) {
            throw new SecurityException("No read permission: " + directoryPath);
        }
        
        if (supportedTypes == null) {
            supportedTypes = SUPPORTED_FILE_TYPES;
        }
        
        List<Map<String, Object>> fileList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // Generate kb_id from directory name
        String kbId = HashUtil.strToMd5(dirPath.getFileName().toString());
        
        // Use Files.walkFileTree to recursively traverse directory
        try {
            Set<String> finalSupportedTypes = supportedTypes;
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Skip hidden directories
                    if (skipHidden && dir.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    
                    // Skip hidden files
                    if (skipHidden && fileName.startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // Check if file type is supported
                    String filePathStr = file.toString();
                    if (!isSupportedFile(filePathStr, finalSupportedTypes)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    try {
                        // Get relative path from knowledge base directory
                        String kbRelPath = getRelativePath(directoryPath, filePathStr);
                        Map<String, Object> fileInfo = getFileInfo(kbId, kbRelPath, filePathStr);
                        fileList.add(fileInfo);
                    } catch (Exception e) {
                        // Collect errors but continue processing other files
                        errors.add("Failed to process file " + filePathStr + ": " + e.getMessage());
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    errors.add("Failed to access file " + file + ": " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new SecurityException("Error traversing directory " + directoryPath + ": " + e.getMessage(), e);
        }
        
        // Log warning if there are errors
        if (!errors.isEmpty()) {
            log.warn("Encountered {} errors during processing:", errors.size());
            for (int i = 0; i < Math.min(errors.size(), 10); i++) {
                log.warn("  - {}", errors.get(i));
            }
            if (errors.size() > 10) {
                log.warn("  ... and {} other errors", errors.size() - 10);
            }
        }
        
        return fileList;
    }
    
    /**
     * Save file list as JSON file
     * 
     * @param fileList List of file information
     * @param outputPath Output JSON file path
     * @throws IOException If an I/O error occurs
     */
    public static void saveToJsonFile(List<Map<String, Object>> fileList, String outputPath) throws IOException {
        Path outputPathObj = Paths.get(outputPath);
        
        // Create parent directory if it doesn't exist
        Path parentDir = outputPathObj.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        
        // Write JSON file with pretty formatting
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), fileList);
        log.info("Results saved to: {}", outputPath);
    }
}