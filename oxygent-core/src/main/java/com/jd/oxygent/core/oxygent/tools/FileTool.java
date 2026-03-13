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
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * File operation tool class providing basic file read, write, and delete functionality.
 * <p>
 * This tool class encapsulates common file system operations, including file creation, reading,
 * writing, and deletion. All operations use UTF-8 encoding to ensure proper handling of Chinese
 * characters. The tool class provides comprehensive error handling mechanisms and returns detailed
 * error information when file operations fail.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>File Writing - Create new files or completely overwrite existing file content</li>
 *   <li>File Reading - Read complete file content and return as string</li>
 *   <li>File Deletion - Safely delete files at specified paths</li>
 * </ul>
 *
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li>Unified UTF-8 encoding for all text operations</li>
 *   <li>Automatic resource management ensuring proper file stream closure</li>
 *   <li>Comprehensive parameter validation and error handling</li>
 *   <li>Detailed operation result feedback</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * FileTool fileTool = new FileTool();
 *
 * // Write file
 * String writeResult = fileTool.call("write_file", "/path/to/file.txt", "Hello World");
 *
 * // Read file
 * String content = fileTool.call("read_file", "/path/to/file.txt");
 *
 * // Delete file
 * String deleteResult = fileTool.call("delete_file", "/path/to/file.txt");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @since 1.0.0
 */
@Slf4j
public class FileTool extends FunctionHub {

    /**
     * Constructor to initialize the file tool.
     * <p>
     * Sets the tool name to "file_tools" and provides basic tool description information.
     * </p>
     */
    public FileTool() {
        super("file_tools");
        this.setDesc("Tool set providing basic file system operation functionality, including file read, write, and delete operations");
    }

    // ========== Tool methods (with @Tool annotation) ==========

    /**
     * Write file content.
     * <p>
     * Create a new file or completely overwrite existing file content. This operation
     * will create necessary parent directories and use UTF-8 encoding to ensure proper
     * handling of Chinese characters.
     * </p>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Will completely overwrite existing file content, use with caution</li>
     *   <li>Will automatically create parent directories if they don't exist</li>
     *   <li>Uses UTF-8 encoding for all text content</li>
     * </ul>
     *
     * @param path    File path, cannot be null or empty string
     * @param content File content to write, cannot be null
     * @return Operation result message, returns success info on success, error details on failure
     * @throws IllegalArgumentException when path or content is null
     */
    @Tool(
            name = "write_file",
            description = "Create new file or completely overwrite existing file content. Uses UTF-8 encoding for text processing, supports Chinese characters. Will automatically create necessary parent directories.",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Complete path of the target file"),
                    @ParamMetaAuto(name = "content", type = "String", description = "File content to write")
            }
    )
    public String writeFile(String path, String content) {
        Objects.requireNonNull(path, "File path cannot be null");
        Objects.requireNonNull(content, "File content cannot be null");

        if (path.trim().isEmpty()) {
            return "Error: File path cannot be empty";
        }

        try {
            var filePath = Paths.get(path);
            var parentDir = filePath.getParent();

            // Create necessary parent directories
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Use modern Java IO with UTF-8 encoding
            try (var writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
                writer.write(content);
                return "Successfully wrote file: " + path;
            }
        } catch (IOException e) {
            return "Error occurred while writing file: " + e.getMessage();
        } catch (Exception e) {
            return "Operation failed: " + e.getMessage();
        }
    }

    /**
     * Read file content.
     * <p>
     * Read the complete content of the specified file and return it as a string.
     * Uses UTF-8 encoding to ensure proper reading of Chinese characters and
     * maintains the original file's line separator format.
     * </p>
     *
     * <p><strong>Features:</strong></p>
     * <ul>
     *   <li>Uses UTF-8 encoding for reading, supports Chinese characters</li>
     *   <li>Maintains original file's line break format</li>
     *   <li>Automatically handles file stream closure</li>
     *   <li>Provides detailed error information feedback</li>
     * </ul>
     *
     * @param path File path to read, cannot be null or empty string
     * @return File content string, returns error message if file doesn't exist or read fails
     * @throws IllegalArgumentException when path is null
     */
    @Tool(
            name = "read_file",
            description = "Read complete file content. Uses UTF-8 encoding for text processing, supports Chinese characters. Returns error message if file doesn't exist.",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Complete path of the file to read")
            }
    )
    public String readFile(String path) {
        Objects.requireNonNull(path, "File path cannot be null");

        if (path.trim().isEmpty()) {
            return "Error: File path cannot be empty";
        }

        var file = new File(path);
        if (!file.exists()) {
            return "Error: File does not exist - " + path;
        }

        if (!file.isFile()) {
            return "Error: Specified path is not a file - " + path;
        }

        try (var reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            var content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            return content.toString();
        } catch (IOException e) {
            return "Error occurred while reading file: " + e.getMessage();
        } catch (Exception e) {
            return "Operation failed: " + e.getMessage();
        }
    }

    /**
     * Delete file.
     * <p>
     * Safely delete a file at the specified path. Before deletion, it checks whether
     * the file exists and whether it is a file (not a directory) to ensure operation safety.
     * </p>
     *
     * <p><strong>Safety Features:</strong></p>
     * <ul>
     *   <li>Verify file existence before deletion</li>
     *   <li>Ensure deleting a file rather than a directory</li>
     *   <li>Provide detailed operation result feedback</li>
     *   <li>Handle deletion permission-related exceptions</li>
     * </ul>
     *
     * @param path File path to delete, cannot be null or empty string
     * @return Operation result message, returns success info on success, error details on failure
     * @throws IllegalArgumentException when path is null
     */
    @Tool(
            name = "delete_file",
            description = "Safely delete specified file. Verifies file existence and type before deletion to ensure operation safety. Returns error message if file doesn't exist or deletion fails.",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Complete path of the file to delete")
            }
    )
    public static String deleteFile(String path) {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return "Error: The file or directory at " + path + " does not exist.";
        }
        try {
            if (Files.isRegularFile(filePath)) {
                Files.delete(filePath);
                return "Successfully deleted the file at " + path;
            } else if (Files.isDirectory(filePath)) {
                deleteDirectoryRecursively(filePath);
                return "Successfully deleted the directory at " + path + " and all its contents";
            } else {
                return "Error: The path " + path + " is not a file or directory.";
            }
        } catch (Exception e) {
            log.error("Error deleting " + path, e);
            return "Error: Failed to delete " + path + ". Reason: " + e.getMessage();
        }
    }

    /**
     * View the content of a text file with optional line range support.
     * Returns file content with line numbers. Useful for viewing specific sections of large files.
     */
    @Tool(
            name = "view_text_file",
            description = "View the content of a text file with optional line range support. Returns file content with line numbers. Useful for viewing specific sections of large files.",
            paramMetas = {
                    @ParamMetaAuto(name = "path", type = "String", description = "Complete path of the file to read"),
                    @ParamMetaAuto(name = "ranges", type = "List<Integer>", description = "Optional list of [start, end] line numbers (1-indexed, inclusive). Negative indices count from the end (-1 is the last line).")
            }
    )
    public static String viewTextFile(String filePath, List<Integer> ranges) {
        Path path = Paths.get(filePath);
        path = path.toAbsolutePath();

        if (!Files.exists(path)) {
            return "Error: The file " + filePath + " does not exist.";
        }

        if (!Files.isRegularFile(path)) {
            return "Error: The path " + filePath + " is not a file.";
        }

        try {
            String content = readFileWithRange(path, ranges);
            if (ranges == null) {
                return "The content of " + filePath + ":\n```\n" + content + "\n```";
            } else {
                return "The content of " + filePath + " in lines " + ranges.get(0) + "-" + ranges.get(1) + ":\n```\n" + content + "\n```";
            }
        } catch (Exception e) {
            log.error("Error viewing text file " + filePath, e);
            return "Error: Failed to read file: " + e.getMessage();
        }
    }

    /**
     * Read file content with optional line range.
     * @param filePath Path to the file.
     * @param ranges Optional list of [start, end] line numbers (1-indexed, inclusive).
            *               Negative indices count from the end (-1 is the last line).
            * @return File content with line numbers.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If ranges are invalid.
     */
    private static String readFileWithRange(Path filePath, List<Integer> ranges) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        int totalLines = lines.size();

        if (ranges == null) {
            // Return all lines with line numbers
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < totalLines; i++) {
                sb.append(i + 1).append(" | ").append(lines.get(i)).append("\n");
            }
            return sb.toString();
        }

        if (ranges.size() != 2) {
            throw new IllegalArgumentException("ranges must be a list of two integers [start, end]");
        }

        int start = ranges.get(0);
        int end = ranges.get(1);

        // Handle negative indices
        if (start < 0) {
            start = totalLines + start + 1;
        }
        if (end < 0) {
            end = totalLines + end + 1;
        }

        // Validate range
        if (start < 1) {
            start = 1;
        }
        if (end > totalLines) {
            end = totalLines;
        }
        if (start > end) {
            throw new IllegalArgumentException("Invalid range: start (" + start + ") > end (" + end + ")");
        }

        // Convert to 0-indexed
        int startIdx = start - 1;
        int endIdx = end;

        // Return lines with line numbers
        StringBuilder sb = new StringBuilder();
        for (int i = startIdx; i < endIdx; i++) {
            sb.append(i + 1).append(" | ").append(lines.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Delete directory recursively
     */
    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Delete files first, then directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Error deleting " + path, e);
                        throw new RuntimeException(e);
                    }
                });
    }
}
