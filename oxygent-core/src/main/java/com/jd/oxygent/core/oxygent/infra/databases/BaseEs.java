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
package com.jd.oxygent.core.oxygent.infra.databases;

import java.util.Map;

/**
 * Elasticsearch Database Base Interface
 * <p>
 * Provides unified Elasticsearch data access interface for OxyGent system, supporting index management,
 * document operations, query retrieval and other core functions.
 * This interface adopts contract-oriented design to ensure consistency across different ES
 * implementations (local/remote).
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *     <li>Layered Architecture: Base interface -> Local/Remote implementation -> Specific business logic</li>
 *     <li>Strategy Pattern: Support multiple ES deployment modes (standalone, cluster, cloud service)</li>
 *     <li>Adapter Pattern: Shield API differences between different ES versions</li>
 *     <li>Template Method: Provide unified error handling and performance monitoring</li>
 * </ul>
 *
 * <h3>Technical Features</h3>
 * <ul>
 *     <li>Index Management: Support dynamic index creation, mapping management, alias operations</li>
 *     <li>Document Operations: Provide CRUD operations, support batch processing and partial updates</li>
 *     <li>Advanced Queries: Support compound queries, aggregation analysis, similarity sorting</li>
 *     <li>Performance Optimization: Integrate query cache, connection pool, slow query monitoring</li>
 * </ul>
 *
 * <h3>Usage Recommendations</h3>
 * <ul>
 *     <li>Index Naming: Use lowercase letters and underscores, e.g., oxygent_messages_2025</li>
 *     <li>Mapping Design: Set field types appropriately, avoid performance issues from dynamic mapping</li>
 *     <li>Query Optimization: Use filters instead of queries for exact matches to improve performance</li>
 *     <li>Batch Operations: Use bulk API to process large amounts of data and improve throughput</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BaseEs {

    // ========== Basic Index Management Operations ==========

    /**
     * Create Elasticsearch index
     * <p>
     * Create a new index according to specified configuration, supporting custom mappings,
     * settings and aliases.
     * It is recommended to pre-create indexes in production environment to avoid performance
     * issues caused by dynamic mapping.
     *
     * <h4>Best Practices</h4>
     * <ul>
     *     <li>Set reasonable shard count: Single shard data volume recommended to be controlled at 20-50GB</li>
     *     <li>Configure replica count: Production environment recommended at least 1 replica for high availability</li>
     *     <li>Set field mapping: Explicitly specify field types to avoid type inference errors</li>
     *     <li>Configure analyzer: Choose appropriate Chinese analyzer according to business needs</li>
     * </ul>
     *
     * @param indexName Index name, cannot be null or empty string, recommend using lowercase and underscores
     * @param body      Index configuration, including mappings, settings, etc., cannot be null
     * @return Index creation result, including acknowledged and other status information
     * @throws IllegalArgumentException When parameters are invalid
     */
    Map<String, Object> createIndex(String indexName, Map<String, Object> body);

    /**
     * Index document to Elasticsearch
     * <p>
     * Add or update document to specified index. If document ID already exists, perform full replacement.
     * For large amounts of data, it is recommended to use batch index interface to improve performance.
     *
     * <h4>Performance Recommendations</h4>
     * <ul>
     *     <li>Batch Indexing: Single indexing recommended to control at 1000-5000 documents</li>
     *     <li>Document Size: Single document recommended not to exceed 100MB</li>
     *     <li>ID Strategy: Use business meaningful IDs for convenient subsequent queries and updates</li>
     *     <li>Time Fields: Use ISO 8601 format to store time data</li>
     * </ul>
     *
     * @param indexName Target index name, cannot be null or empty string
     * @param docId     Document unique identifier, cannot be null or empty string
     * @param body      Document content, cannot be null
     * @return Index operation result, including document ID, version number and other information
     * @throws IllegalArgumentException When parameters are invalid
     */
    Map<String, Object> index(String indexName, String docId, Map<String, Object> body);

    /**
     * Update Elasticsearch document
     * <p>
     * Perform partial field updates on specified document, more efficient compared to index operation.
     * Supports both script updates and document merge methods.
     *
     * <h4>Update Strategies</h4>
     * <ul>
     *     <li>Partial Update: Only update changed fields, reduce network transmission</li>
     *     <li>Optimistic Lock: Control concurrent updates based on version numbers</li>
     *     <li>Retry Mechanism: Automatically handle version conflict retries</li>
     *     <li>Script Update: Support complex field calculations and conditional updates</li>
     * </ul>
     *
     * @param indexName Index name, cannot be null or empty string
     * @param docId     Document ID, cannot be null or empty string
     * @param body      Update content, cannot be null
     * @return Update operation result, including version number and other information
     * @throws IllegalArgumentException When parameters are invalid
     */
    Map<String, Object> update(String indexName, String docId, Map<String, Object> body);

    /**
     * Execute Elasticsearch query
     * <p>
     * Support various complex queries, including full-text search, exact match, range query,
     * aggregation analysis, etc.
     * Query results include matched documents, scores, highlighting and other information.
     *
     * <h4>Query Optimization</h4>
     * <ul>
     *     <li>Use Filters: Use term queries for exact matches to improve performance</li>
     *     <li>Pagination Query: Use search_after to avoid deep pagination issues</li>
     *     <li>Field Filtering: Only return needed fields to reduce data transmission</li>
     *     <li>Query Cache: Same queries will be automatically cached to improve response speed</li>
     * </ul>
     *
     * @param indexName Target index name, cannot be null or empty string
     * @param body      Query DSL, including query conditions, sorting, aggregation, etc., cannot be null
     * @return Query results, including hits, aggregations and other information
     * @throws IllegalArgumentException When parameters are invalid
     */
    Map<String, Object> search(String indexName, Map<String, Object> body);

    /**
     * Check if document exists
     * <p>
     * Efficiently check if specified document exists in the index, only returns existence information
     * without returning document content.
     * Compared to get document operation, this method is more lightweight.
     *
     * @param indexName Index name, cannot be null or empty string
     * @param docId     Document ID, cannot be null or empty string
     * @return Whether document exists, true means exists, false means does not exist
     * @throws IllegalArgumentException When parameters are invalid
     */
    Boolean exists(String indexName, String docId);

    /**
     * Close ES client connection
     * <p>
     * Safely close ES client connections and release related resources including connection pools,
     * thread pools, etc.
     * This method must be called before application shutdown to avoid resource leaks.
     *
     * <h4>Cleanup Content</h4>
     * <ul>
     *     <li>Close HTTP connection pool</li>
     *     <li>Stop background threads</li>
     *     <li>Clear local cache</li>
     *     <li>Release JVM memory</li>
     * </ul>
     */
    void close();

}