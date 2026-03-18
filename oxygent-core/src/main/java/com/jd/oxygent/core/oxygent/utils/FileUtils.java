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
package com.jd.oxygent.core.oxygent.utils;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File operation utility class
 *
 * <p>Provides safe and reliable file operation functions, specifically optimized for JSON file read/write operations.
 * This utility class supports advanced features such as automatic encoding detection and migration, atomic writes,
 * ensuring data integrity and consistency.</p>
 *
 * <p>Main functional modules:</p>
 * <ul>
 *     <li>File path management: index file path generation, mapping file path retrieval</li>
 *     <li>Safe file reading: supports encoding fallback, corrupted file recovery</li>
 *     <li>Atomic writing: prevents data corruption during write operations</li>
 *     <li>Encoding handling: UTF-8 priority, system default encoding as backup</li>
 *     <li>JSON file operations: specialized read/write optimization for JSON data</li>
 * </ul>
 *
 * <p>Security features:</p>
 * <ul>
 *     <li>Automatic encoding detection: prioritizes UTF-8, falls back to system default encoding on failure</li>
 *     <li>Atomic writes: uses temporary file + move operation to ensure write integrity</li>
 *     <li>Corrupted file handling: automatically detects and attempts to fix encoding issues</li>
 *     <li>Exception recovery: provides multi-level error handling and recovery mechanisms</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Get index file path
 * Path indexPath = FileUtils.getIndexPath("/data", "user_index");
 *
 * // Safely read JSON file
 * Map<String, Object> data = FileUtils.readJsonSafe("/data", indexPath);
 *
 * // Atomic write JSON file
 * FileUtils.writeJsonAtomic("/data", indexPath, data);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class FileUtils {

    /**
     * Get index file path
     *
     * <p>Constructs the complete path of the index file based on the data directory and index name.
     * Index files use the .json extension.</p>
     *
     * @param dataDir   Data directory path, cannot be null or empty string
     * @param indexName Index name, cannot be null or empty string
     * @return Path object of the index file
     * @throws IllegalArgumentException When parameters are null or empty string
     * @since 1.0.0
     */
    public static Path getIndexPath(String dataDir, String indexName) {
        if (dataDir == null || dataDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Data directory cannot be null or empty string");
        }
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name cannot be null or empty string");
        }
        return Paths.get(dataDir, indexName + ".json");
    }

    /**
     * Get all index file paths with specified prefix
     *
     * <p>Finds all file paths starting with the given prefix in the specified directory.
     * This method is commonly used for batch processing index files with the same prefix.</p>
     *
     * @param dataDir         Data directory path, cannot be null or empty string
     * @param indexNamePrefix Index name prefix, cannot be null
     * @return List of file paths matching the prefix
     * @throws IllegalArgumentException When dataDir is null or empty string
     * @throws RuntimeException         When directory access fails
     * @since 1.0.0
     */
    public static List<Path> getIndexPaths(String dataDir, String indexNamePrefix) {
        if (dataDir == null || dataDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Data directory cannot be null or empty string");
        }
        Objects.requireNonNull(indexNamePrefix, "Index name prefix cannot be null");

        try {
            return Files.list(Paths.get(dataDir))
                    .filter(path -> path.getFileName().toString().startsWith(indexNamePrefix))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get index file paths: " + dataDir, e);
        }
    }

    /**
     * Get all file paths in the data directory
     *
     * <p>Gets all file paths in the specified directory, equivalent to calling getIndexPaths(dataDir, "").</p>
     *
     * @param dataDir Data directory path, cannot be null or empty string
     * @return List of all file paths in the directory
     * @throws IllegalArgumentException When dataDir is null or empty string
     * @throws RuntimeException         When directory access fails
     * @since 1.0.0
     */
    public static List<Path> getIndexPaths(String dataDir) {
        return getIndexPaths(dataDir, "");
    }

    /**
     * Get mapping file path
     *
     * <p>Constructs the complete path of the mapping file based on the data directory and index name.
     * Mapping files use the "_mapping.json" suffix.</p>
     *
     * @param dataDir   Data directory path, cannot be null or empty string
     * @param indexName Index name, cannot be null or empty string
     * @return Path object of the mapping file
     * @throws IllegalArgumentException When parameters are null or empty string
     * @since 1.0.0
     */
    public static Path getMappingPath(String dataDir, String indexName) {
        if (dataDir == null || dataDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Data directory cannot be null or empty string");
        }
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name cannot be null or empty string");
        }
        return Paths.get(dataDir, indexName + "_mapping.json");
    }


    // ------------------------------------------------------------------
    // Encoding-aware safe reading methods (returns null for unrecoverable corrupted files)
    // ------------------------------------------------------------------

    /**
     * Safely read JSON file (supports encoding fallback)
     *
     * <p>Safely reads JSON files with automatic encoding detection and fallback mechanism. This method first
     * attempts to read using UTF-8 encoding, and falls back to system default encoding if it fails.
     * After successful reading, it automatically migrates the file to UTF-8 encoding.</p>
     *
     * <p>Processing flow:</p>
     * <ol>
     *     <li>Check if file exists, return empty Map if not</li>
     *     <li>Try to read file content using UTF-8 encoding and parse JSON</li>
     *     <li>If UTF-8 fails, retry using system default encoding</li>
     *     <li>After successful reading, automatically migrate file to UTF-8 encoding</li>
     *     <li>Return null when all attempts fail (unrecoverable corruption)</li>
     * </ol>
     *
     * @param dataDir Data directory path, used for creating temporary files, cannot be null
     * @param path    File path to read, cannot be null
     * @return Parsed JSON data Map, returns empty Map if file doesn't exist, null if corrupted
     * @throws IllegalArgumentException When parameters are null
     * @since 1.0.0
     */
    public static Map<String, Object> readJsonSafe(String dataDir, Path path) {
        Objects.requireNonNull(dataDir, "Data directory cannot be null");
        Objects.requireNonNull(path, "File path cannot be null");

        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }

        // First try UTF-8 encoding
        try {
            var bytes = Files.readAllBytes(path);
            var content = new String(bytes, StandardCharsets.UTF_8);
            return JsonUtils.parseJsonString(content);
        } catch (Exception e) {
            log.warn("Failed to read JSON file using UTF-8: " + path + ", error: " + e.getMessage());

            // Fallback to system default encoding
            try {
                var bytes = Files.readAllBytes(path);
                var content = new String(bytes); // Use system default charset
                var data = JsonUtils.parseJsonString(content);

                // Migrate to UTF-8 encoding after successful fallback
                try {
                    writeJsonAtomic(dataDir, path, data);
                    log.info("File migrated to UTF-8 encoding: " + path);
                } catch (IOException migrationError) {
                    log.warn("Unable to rewrite file to UTF-8 encoding " + path + ": " + migrationError.getMessage());
                }
                return data;
            } catch (Exception fallbackError) {
                log.error("writeJsonAtomic path:{}", path.toString(), fallbackError);
                return null; // Unrecoverable corruption
            }
        }
    }

    /**
     * Write data to path atomically, UTF‑8 encoded.
     */
    public static void writeJsonAtomic(String dataDir, Path path, String jsonString) throws IOException {
        Path tempFile = Files.createTempFile(Paths.get(dataDir), "temp", ".json");
        try {
            Files.write(tempFile, jsonString.getBytes(StandardCharsets.UTF_8));
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.error("writeJsonAtomic path:{}", path.toString(), e);
        } finally {
            // Clean up temp file if it still exists
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Write data to path atomically, UTF‑8 encoded.
     */
    public static void writeJsonAtomic(String dataDir, Path path, Object data) throws IOException {
        Path tempFile = Files.createTempFile(Paths.get(dataDir), "temp", ".json");
        try {
            String jsonString = JsonUtils.toJSONStringPretty(data);
            Files.write(tempFile, jsonString.getBytes(StandardCharsets.UTF_8));
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.error("writeJsonAtomic path:{}", path.toString(), e);
        } finally {
            // Clean up temp file if it still exists
            Files.deleteIfExists(tempFile);
        }
    }

    public static void delete(Path indexPath) throws IOException {
        Files.delete(indexPath);
    }

    /**
     * Get current project's target directory path
     *
     * @return Path object of the target directory
     * @throws IOException If directory cannot be created or accessed
     */
    public static Path getTargetDirPath() throws IOException {
        // 1. Try to get from class loader path (most accurate, compatible with multi-module projects)
        // Usually points to target/classes
        var resource = FileUtils.class.getClassLoader().getResource("");
        Path targetPath;

        if (resource != null) {
            try {
                Path classesPath = Paths.get(resource.toURI());
                // Go up two levels: classes -> target
                targetPath = classesPath.getParent();
            } catch (Exception e) {
                targetPath = fallbackToUserDir();
            }
        } else {
            targetPath = fallbackToUserDir();
        }

        // 2. Ensure directory exists (can be automatically recreated if mvn clean was executed)
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }

        return targetPath.toAbsolutePath();
    }

    private static Path fallbackToUserDir() {
        return Paths.get(System.getProperty("user.dir")).resolve("target");
    }

    /**
     * Safely create a subdirectory under target directory (e.g., for storing Skill-generated files)
     *
     * @param subDirName Subdirectory name
     * @return Full path of the subdirectory
     */
    public static Path createSubDirInTarget(String subDirName) throws IOException {
        Path target = getTargetDirPath();
        Path subDir = target.resolve(subDirName);

        if (!Files.exists(subDir)) {
            Files.createDirectories(subDir);
        }
        return subDir;
    }

    /**
     * Finds all matching Paths based on criteria.
     * Note: If the paths are inside a ZIP/JAR, the FileSystem must remain open to use them.
     */
    public static List<Path> findAllPaths(String path, Set<String> excludeDirs, String fileNamePattern)
            throws IOException, URISyntaxException {

        if (path.startsWith("classpath:")) {
            return findFromClasspath(path.substring(10), excludeDirs, fileNamePattern);
        }

        Path fsPath = Paths.get(path);

        if (isZipFile(fsPath)) {
            // For ZIP files, we need to handle the FileSystem lifecycle carefully.
            // Here we return paths from a new FileSystem.
            return findFromZipFile(fsPath, "/", excludeDirs, fileNamePattern);
        } else {
            return findFromFileSystem(fsPath, excludeDirs, fileNamePattern);
        }
    }

    public static List<Path> findFromClasspath(String resourcePath, Set<String> excludeDirs, String fileNamePattern)
            throws IOException, URISyntaxException {
        var loader = Thread.currentThread().getContextClassLoader();
        var url = loader.getResource(resourcePath);
        if (url == null) throw new IOException("Resource not found: " + resourcePath);

        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            // Note: In a production app, you might want to cache these FileSystems
            return findFromZipFile(Paths.get(resourcePath), "/", excludeDirs, fileNamePattern);
        }
        return findFromFileSystem(Paths.get(uri), excludeDirs, fileNamePattern);
    }

    public static List<Path> findFromZipFile(Path zipPath, String internalPath, Set<String> excludeDirs, String fileNamePattern) throws IOException {
        return new ArrayList<>();
//        URI uri = URI.create("jar:" + zipPath.toUri());
//
//        FileSystem zipFs = null;
//        boolean shouldClose = false;
//
//        try {
//            zipFs = FileSystems.getFileSystem(uri);
//        } catch (FileSystemNotFoundException e) {
//            zipFs = FileSystems.newFileSystem(uri, Collections.emptyMap());
//            shouldClose = true;
//        }
//
//        try {
//            return findFromFileSystem(zipFs.getPath(internalPath), excludeDirs, fileNamePattern);
//        } finally {
//            if (shouldClose && zipFs != null) {
//                zipFs.close();
//            }
//        }
    }

    public static List<Path> findFromFileSystem(Path rootPath, Set<String> excludeDirs, String fileNamePattern) throws IOException {
        try (Stream<Path> walk = Files.walk(rootPath)) {
            return walk
                    .filter(p -> {
                        for (Path section : p) {
                            if (excludeDirs.contains(section.toString())) return false;
                        }
                        return true;
                    })
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.equals(fileNamePattern) || name.endsWith(fileNamePattern);
                    })
                    // Collect into a List to realize the Stream before any auto-close happens
                    .collect(Collectors.toList());
        }
    }

    public static boolean isZipFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return Files.isRegularFile(path) && (name.endsWith(".zip") || name.endsWith(".jar"));
    }
}
