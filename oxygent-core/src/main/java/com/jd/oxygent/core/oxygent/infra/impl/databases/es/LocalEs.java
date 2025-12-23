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
package com.jd.oxygent.core.oxygent.infra.impl.databases.es;

import com.jd.oxygent.core.oxygent.infra.databases.BaseDB;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * <h3>Local Elasticsearch Simulation Implementation</h3>
 *
 * <p>LocalEs is an Elasticsearch simulator for local development and testing environments in the OxyGent framework.
 * This implementation stores JSON documents through the local file system, simulating core Elasticsearch functionality,
 * providing developers with a lightweight search engine solution without external dependencies.</p>
 *
 * <h3>Technical Architecture</h3>
 * <ul>
 *   <li><strong>File System Storage</strong>: Based on local file system, each index corresponds to one JSON file</li>
 *   <li><strong>Atomic Operations</strong>: Uses Files.move to ensure atomicity of write operations, avoiding data corruption</li>
 *   <li><strong>Cross-platform Compatibility</strong>: Fully compatible with Windows/Linux/MacOS, no POSIX dependencies</li>
 *   <li><strong>UTF-8 Safety</strong>: Full Unicode character support with automatic encoding conversion</li>
 *   <li><strong>Concurrency Safety</strong>: Uses ReentrantLock to ensure data consistency in multi-threaded environments</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Data Safety First</strong>: Never overwrites existing indexes, corrupted files automatically backed up as .bak format</li>
 *   <li><strong>Incremental Migration</strong>: Legacy encoding files automatically migrate to UTF-8</li>
 *   <li><strong>Intelligent Recovery</strong>: Supports automatic data recovery from backup files</li>
 *   <li><strong>Error Isolation</strong>: Corrupted data moved to .corrupt files, protecting historical logs</li>
 * </ul>
 *
 * <h3>Supported ES API Subset</h3>
 * <ul>
 *   <li><strong>Index Management</strong>: createIndex - Create indexes and mapping configurations</li>
 *   <li><strong>Document Operations</strong>: index, update - Document creation and updates</li>
 *   <li><strong>Query Functions</strong>: search, exists - Support complex queries and existence checks</li>
 *   <li><strong>Node Operations</strong>: getByNodeId, updateByNodeId - Node ID-based operations</li>
 * </ul>
 *
 * <h3>Query Capabilities</h3>
 * <ul>
 *   <li><strong>Exact Queries</strong>: term queries, supporting single field exact matching</li>
 *   <li><strong>Multi-value Queries</strong>: terms queries, supporting field value list matching</li>
 *   <li><strong>Boolean Queries</strong>: bool queries, supporting must/should/must_not logical combinations</li>
 *   <li><strong>Sorting Support</strong>: Multi-field sorting, supporting asc/desc directions</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li><strong>Local Development</strong>: Feature development without installing Elasticsearch</li>
 *   <li><strong>Unit Testing</strong>: Fast-starting lightweight testing environment</li>
 *   <li><strong>Prototype Validation</strong>: Rapid validation of search logic and data structure design</li>
 *   <li><strong>Offline Environments</strong>: Search functionality support in network-restricted environments</li>
 * </ul>
 *
 * <h3>Storage Structure</h3>
 * <pre>
 * cache_dir/local_es_data/
 * ├── index_name.json          # Index data file
 * ├── index_name_mapping.json  # Index mapping configuration
 * ├── index_name.json.bak      # Automatic backup file
 * └── index_name.json.corrupt  # Corrupted file backup
 * </pre>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Create index
 * Map<String, Object> mapping = Map.of("properties",
 *     Map.of("title", Map.of("type", "string")));
 * localEs.createIndex("articles", mapping);
 *
 * // Add document
 * Map<String, Object> doc = Map.of("title", "OxyGent Framework");
 * localEs.index("articles", "doc1", doc);
 *
 * // Search documents
 * Map<String, Object> query = Map.of("query",
 *     Map.of("term", Map.of("title", "OxyGent Framework")));
 * Map<String, Object> result = localEs.search("articles", query);
 * }</pre>
 *
 * <h3>Performance Characteristics</h3>
 * <ul>
 *   <li><strong>Startup Speed</strong>: Millisecond-level startup, no waiting for service startup</li>
 *   <li><strong>Memory Usage</strong>: Minimal memory usage, suitable for resource-constrained environments</li>
 *   <li><strong>Concurrent Processing</strong>: Supports moderate concurrency, suitable for small to medium datasets</li>
 *   <li><strong>Scalability</strong>: Single-machine file storage, suitable for data volumes under ten thousand records</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@ConditionalOnProperty(name = "oxygent.database.es", havingValue = "local", matchIfMissing = true)
@Service
public class LocalEs extends BaseDB implements BaseEs {
    private static final Logger logger = Logger.getLogger(LocalEs.class.getName());

    private final String dataDir;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Initialize Local Elasticsearch Simulator
     * <p>
     * The constructor is responsible for initializing the local data storage directory,
     * ensuring the basic structure of the file system is ready.
     * Uses cache_dir/local_es_data as the default storage path, supporting multi-environment isolation.
     *
     * <h3>Initialization Process</h3>
     * <ol>
     *   <li>Set data storage path to cache_dir/local_es_data</li>
     *   <li>Create necessary directory structure, ensuring file system is writable</li>
     *   <li>Initialize concurrent lock manager, supporting multi-threaded safe access</li>
     * </ol>
     *
     * @throws RuntimeException if unable to create data directory or directory is not writable
     */
    public LocalEs() {
        // Modified to cache_dir/cache_dir path
        this.dataDir = Paths.get("cache_dir", "local_es_data").toString();
        try {
            Files.createDirectories(Paths.get(dataDir));
        } catch (IOException e) {
            logger.severe("Failed to create data directory: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------
    // Utilities (paths, atomic IO helpers)
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Public ES‑like API
    // ------------------------------------------------------------------

    /**
     * Create Local Index and Mapping Configuration
     * <p>
     * Creates a new local index, including its mapping configuration and data storage file.
     * This method follows Elasticsearch index creation semantics but is implemented based on the local file system.
     *
     * <h3>Creation Process</h3>
     * <ol>
     *   <li>Validate index name and mapping configuration validity</li>
     *   <li>Persist mapping configuration to {indexName}_mapping.json file</li>
     *   <li>Create empty index data file {indexName}.json (only when it doesn't exist)</li>
     *   <li>Return operation confirmation result</li>
     * </ol>
     *
     * <h3>Data Safety Strategy</h3>
     * <ul>
     *   <li><strong>Non-destructive</strong>: Never overwrites existing index data, avoiding accidental data loss</li>
     *   <li><strong>Atomic Operations</strong>: Uses atomic writes to ensure configuration persistence integrity</li>
     *   <li><strong>Mapping Updates</strong>: Supports explicit mapping configuration updates</li>
     * </ul>
     *
     * <h3>File Structure</h3>
     * <pre>
     * {dataDir}/
     * ├── {indexName}.json          # Index data (created only when it doesn't exist)
     * └── {indexName}_mapping.json  # Mapping configuration (always updated)
     * </pre>
     *
     * @param indexName Index name, must be non-empty and trimmed
     * @param body      Index mapping configuration containing field definitions and index settings, must be non-empty
     * @return Operation confirmation Map containing acknowledged=true
     * @throws IllegalArgumentException if indexName or body is empty
     * @throws RuntimeException         if file system operations fail
     */
    @Override
    public Map<String, Object> createIndex(String indexName, Map<String, Object> body) {
        if (indexName == null || indexName.trim().isEmpty() || body == null || body.isEmpty()) {
            throw new IllegalArgumentException("indexName and body must not be empty");
        }

        try {
            // 1) persist mapping (overwrite OK – mapping updates should be explicit)
            FileUtils.writeJsonAtomic(dataDir, FileUtils.getMappingPath(dataDir, indexName), body);

            // 2) create empty index *only if it does not exist* – avoids wiping logs
            Path indexPath = FileUtils.getIndexPath(dataDir, indexName);
            if (!Files.exists(indexPath)) {
                FileUtils.writeJsonAtomic(dataDir, indexPath, new LinkedHashMap<>());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("acknowledged", true);
            return result;
        } catch (IOException e) {
            logger.severe("Failed to create index: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal Method for Executing Document Insert or Update Operations
     * <p>
     * This is the core method for document operations, responsible for handling all data write logic,
     * including concurrency control, data recovery, error handling, and other key functions.
     * This method ensures ACID properties of data operations.
     *
     * <h3>Operation Flow</h3>
     * <ol>
     *   <li><strong>Concurrency Control</strong>: Acquire index-level ReentrantLock to ensure thread safety</li>
     *   <li><strong>Data Loading</strong>: Load existing index data from disk, supporting error recovery</li>
     *   <li><strong>Automatic Recovery</strong>: Detect data corruption, automatically recover from .bak files</li>
     *   <li><strong>Data Mutation</strong>: Execute insert or update logic based on updateMode</li>
     *   <li><strong>Atomic Write</strong>: Create backup first, then atomically write new data</li>
     * </ol>
     *
     * <h3>Error Handling Strategy</h3>
     * <ul>
     *   <li><strong>Corruption Detection</strong>: Automatically detect JSON format errors and file corruption</li>
     *   <li><strong>Backup Recovery</strong>: Prioritize data recovery from .bak files</li>
     *   <li><strong>Corruption Isolation</strong>: Move unrecoverable files to .corrupt</li>
     *   <li><strong>Fresh Start</strong>: Create new empty index when data is completely corrupted</li>
     * </ul>
     *
     * <h3>Update Mode Differences</h3>
     * <ul>
     *   <li><strong>Insert Mode (false)</strong>: Completely replace document content</li>
     *   <li><strong>Update Mode (true)</strong>: Merge field content, preserve existing fields</li>
     * </ul>
     *
     * <h3>File Operation Safety</h3>
     * <ul>
     *   <li><strong>Atomicity</strong>: Uses Files.move to ensure atomicity of write operations</li>
     *   <li><strong>Backup Mechanism</strong>: Automatically create backup before each write</li>
     *   <li><strong>Cross-platform</strong>: Compatible with all mainstream operating system file systems</li>
     * </ul>
     *
     * @param indexName  Index name, used to locate data file and get corresponding lock
     * @param docId      Document unique identifier, used as key in index
     * @param body       Document content, JSON data in Map format
     * @param updateMode Operation mode, true for update mode (merge), false for insert mode (replace)
     * @return Map containing operation result, with _id and result fields
     * @throws RuntimeException if file system operations fail or data corruption is unrecoverable
     */
    private Map<String, Object> insert(
            String indexName, String docId, Map<String, Object> body, boolean updateMode) {
        Path dataPath = FileUtils.getIndexPath(dataDir, indexName);
        Path backupPath = Paths.get(dataPath.toString() + ".bak");

        ReentrantLock lock = locks.computeIfAbsent(indexName, k -> new ReentrantLock());
        lock.lock();
        try {
            // --- load existing data ---
            Map<String, Object> data = FileUtils.readJsonSafe(dataDir, dataPath);

            if (data == null) { // unrecoverable corruption; try backup once
                if (Files.exists(backupPath)) {
                    try {
                        Files.move(backupPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
                        data = FileUtils.readJsonSafe(dataDir, dataPath);
                    } catch (IOException e) {
                        logger.warning("Failed to restore from backup: " + e.getMessage());
                    }
                }
            }

            if (data == null) {
                // still corrupted – preserve original file, switch to fresh store
                Path corruptPath = Paths.get(dataPath.toString() + ".corrupt");
                try {
                    Files.move(dataPath, corruptPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.severe("Index " + indexName + " is corrupted – moved to " + corruptPath);
                } catch (IOException e) {
                    logger.severe("Failed to move corrupted file: " + e.getMessage());
                }
                data = new LinkedHashMap<>();
            }

            // --- apply mutation ---
            if (updateMode) {
                @SuppressWarnings("unchecked")
                Map<String, Object> existing = (Map<String, Object>) data.get(docId);
                if (existing == null) {
                    existing = new LinkedHashMap<>();
                }
                existing.putAll(body);
                data.put(docId, existing);
            } else {
                data.put(docId, body);
            }

            // --- backup & persist ---
            try {
                if (Files.exists(dataPath)) {
                    Files.move(dataPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                }
                FileUtils.writeJsonAtomic(dataDir, dataPath, data);
            } catch (IOException e) {
                logger.severe("Failed to persist data: " + e.getMessage());
                throw new RuntimeException(e);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("_id", docId);
            result.put("result", updateMode ? "updated" : "created");
            return result;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, Object> index(String indexName, String docId, Map<String, Object> body) {
        String[] parts = indexName.split("_");
        String lastPart = parts[parts.length - 1];
        if ("message".equals(lastPart)) {
            body.remove("body"); // es cannot use this field
        }
        // In local mode, all messages are retained
        return insert(indexName, docId, body, false);
    }

    @Override
    public Map<String, Object> update(String indexName, String docId, Map<String, Object> body) {
        return insert(indexName, docId, body, true);
    }

    @Override
    public Boolean exists(String indexName, String docId) {
        Map<String, Object> data = FileUtils.readJsonSafe(dataDir, FileUtils.getIndexPath(dataDir, indexName));
        if (data == null) {
            data = new LinkedHashMap<>();
        }
        return data.containsKey(docId);
    }

    @Override
    public Map<String, Object> search(String indexName, Map<String, Object> body) {
        Map<String, Object> data = FileUtils.readJsonSafe(dataDir, FileUtils.getIndexPath(dataDir, indexName));
        if (data == null) {
            data = new LinkedHashMap<>();
        }

        List<Map<String, Object>> docs = buildDocs(data);
        docs = filterDocs(docs, (Map<String, Object>) body.get("query"));
        docs = sortDocs(docs, (List<Map<String, Object>>) body.get("sort"));

        int size = (Integer) body.getOrDefault("size", 10);
        List<Map<String, Object>> limitedDocs = docs.stream()
                .limit(size)
                .collect(Collectors.toList());

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("hits", limitedDocs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hits", hits);
        return result;
    }

    // ------------------------------------------------------------------
    // Helpers for naive query execution
    // ------------------------------------------------------------------

    private List<Map<String, Object>> buildDocs(Map<String, Object> data) {
        return data.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("_id", entry.getKey());
                    doc.put("_source", entry.getValue());
                    return doc;
                })
                .collect(Collectors.toList());
    }

    /**
     * Multi-condition Query filtering
     */
    private List<Map<String, Object>> filterDocs(List<Map<String, Object>> docs, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return docs;
        }

        if (query.containsKey("term")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> term = (Map<String, Object>) query.get("term");
            Map.Entry<String, Object> entry = term.entrySet().iterator().next();
            String key = entry.getKey();
            Object value = entry.getValue();

            return docs.stream()
                    .filter(doc -> {
                        if ("_id".equals(key)) {
                            return Objects.equals(doc.get("_id"), value);
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> source = (Map<String, Object>) doc.get("_source");
                        return Objects.equals(source.get(key), value);
                    })
                    .collect(Collectors.toList());
        }

        if (query.containsKey("terms")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> terms = (Map<String, Object>) query.get("terms");
            Map.Entry<String, Object> entry = terms.entrySet().iterator().next();
            String key = entry.getKey();
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) entry.getValue();

            return docs.stream()
                    .filter(doc -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> source = (Map<String, Object>) doc.get("_source");
                        return values.contains(source.get(key));
                    })
                    .collect(Collectors.toList());
        }

        if (query.containsKey("bool")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> boolQuery = (Map<String, Object>) query.get("bool");

            if (boolQuery.containsKey("must")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> musts = (List<Map<String, Object>>) boolQuery.get("must");
                return docs.stream()
                        .filter(doc -> musts.stream().allMatch(cond -> matchSingleCondition(doc, cond)))
                        .collect(Collectors.toList());
            }

            if (boolQuery.containsKey("should")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> shoulds = (List<Map<String, Object>>) boolQuery.get("should");
                return docs.stream()
                        .filter(doc -> shoulds.stream().anyMatch(cond -> matchSingleCondition(doc, cond)))
                        .collect(Collectors.toList());
            }

            if (boolQuery.containsKey("must_not")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> mustNots = (List<Map<String, Object>>) boolQuery.get("must_not");
                return docs.stream()
                        .filter(doc -> mustNots.stream().noneMatch(cond -> matchSingleCondition(doc, cond)))
                        .collect(Collectors.toList());
            }
        }

        return docs;
    }

    private boolean matchSingleCondition(Map<String, Object> doc, Map<String, Object> condition) {
        if (condition.containsKey("term")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> term = (Map<String, Object>) condition.get("term");
            Map.Entry<String, Object> entry = term.entrySet().iterator().next();
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("_id".equals(key)) {
                return Objects.equals(doc.get("_id"), value);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) doc.get("_source");
            return Objects.equals(source.get(key), value);
        }

        if (condition.containsKey("terms")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> terms = (Map<String, Object>) condition.get("terms");
            Map.Entry<String, Object> entry = terms.entrySet().iterator().next();
            String key = entry.getKey();
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) entry.getValue();

            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) doc.get("_source");
            return values.contains(source.get(key));
        }

        return false;
    }

    private List<Map<String, Object>> sortDocs(List<Map<String, Object>> docs, List<Map<String, Object>> sortSpec) {
        if (sortSpec == null || sortSpec.isEmpty()) {
            return docs;
        }

        // Apply sorts in reverse order (last sort has highest priority)
        for (int i = sortSpec.size() - 1; i >= 0; i--) {
            Map<String, Object> sort = sortSpec.get(i);
            for (Map.Entry<String, Object> entry : sort.entrySet()) {
                String field = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> orderSpec = (Map<String, Object>) entry.getValue();
                String order = orderSpec.getOrDefault("order", "asc").toString();
                boolean reverse = "desc".equals(order);

                docs.sort((doc1, doc2) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> source1 = (Map<String, Object>) doc1.get("_source");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> source2 = (Map<String, Object>) doc2.get("_source");

                    Object val1 = source1.get(field);
                    Object val2 = source2.get(field);

                    @SuppressWarnings("unchecked")
                    int result = ((Comparable<Object>) val1).compareTo(val2);
                    return reverse ? -result : result;
                });
            }
        }

        return docs;
    }

    /**
     * Get document by node_id.
     */
    public Map<String, Object> getByNodeId(String indexName, String nodeId) {
        Map<String, Object> data = FileUtils.readJsonSafe(dataDir, FileUtils.getIndexPath(dataDir, indexName));
        if (data == null) {
            data = new LinkedHashMap<>();
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String docId = entry.getKey();
            Object docContent = entry.getValue();

            if (docContent instanceof Map docContentMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> docMap = docContentMap;
                if (Objects.equals(docMap.get("node_id"), nodeId)) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("_id", docId);
                    result.put("_source", docContent);
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Update document by node_id.
     */
    public Map<String, Object> updateByNodeId(
            String indexName, String nodeId, Map<String, Object> updates) {
        Path dataPath = FileUtils.getIndexPath(dataDir, indexName);
        Path backupPath = Paths.get(dataPath.toString() + ".bak");

        ReentrantLock lock = locks.computeIfAbsent(indexName, k -> new ReentrantLock());
        lock.lock();
        try {
            Map<String, Object> data = FileUtils.readJsonSafe(dataDir, dataPath);
            if (data == null) {
                data = new LinkedHashMap<>();
            }

            String targetDocId = null;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String docId = entry.getKey();
                Object docContent = entry.getValue();

                if (docContent instanceof Map docContentMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> docMap = docContentMap;
                    if (Objects.equals(docMap.get("node_id"), nodeId)) {
                        targetDocId = docId;
                        break;
                    }
                }
            }

            if (targetDocId == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("_id", "");
                result.put("result", "not_found");
                return result;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> targetDoc = (Map<String, Object>) data.get(targetDocId);
            targetDoc.putAll(updates);

            try {
                if (Files.exists(dataPath)) {
                    Files.move(dataPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                }
                FileUtils.writeJsonAtomic(dataDir, dataPath, data);
            } catch (IOException e) {
                logger.severe("Failed to persist data: " + e.getMessage());
                throw new RuntimeException(e);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("_id", targetDocId);
            result.put("result", "updated");
            return result;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        // Nothing to clean up
    }

}