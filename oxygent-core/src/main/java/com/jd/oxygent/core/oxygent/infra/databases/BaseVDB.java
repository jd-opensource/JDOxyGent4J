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

import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbDO;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbQueryFilterDO;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Database;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.Space;

import java.util.List;

/**
 * Vector Database Base Interface
 * <p>
 * Provides unified vector database access interface for OxyGent system, built based on Vearch official SDK capabilities.
 * Focuses on vector storage, retrieval and management functions for RAG scenarios, providing enterprise-level vector data services.
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *     <li>Layered Abstraction: Base interface -> Vearch adaptation -> RAG business logic</li>
 *     <li>SDK Integration: Directly based on Vearch official SDK to ensure API stability and compatibility</li>
 *     <li>Performance Optimization: Support batch operations, parallel queries, connection pool management</li>
 *     <li>Fault Tolerance: Integrate retry strategies, health checks, failover</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *     <li>Vector Storage: Efficient storage and indexing of high-dimensional vector data</li>
 *     <li>Similarity Search: Based on cosine similarity, Euclidean distance and other algorithms</li>
 *     <li>Hybrid Retrieval: Combined queries of vector retrieval and scalar filtering</li>
 *     <li>Real-time Updates: Support real-time insertion, update and deletion of vector data</li>
 * </ul>
 *
 * <h3>RAG Application Scenarios</h3>
 * <ul>
 *     <li>Knowledge Base Retrieval: Intelligent document retrieval based on semantic similarity</li>
 *     <li>Conversation Memory: Vectorized storage and retrieval of user session history</li>
 *     <li>Content Recommendation: Personalized recommendations based on user behavior and content features</li>
 *     <li>Similarity Matching: Similarity calculation for multimodal content like text and images</li>
 * </ul>
 *
 * <h3>Performance Optimization Recommendations</h3>
 * <ul>
 *     <li>Vector Dimensions: Choose appropriate dimensions according to model, commonly 512/768/1024 dimensions</li>
 *     <li>Index Configuration: HNSW index suitable for high-precision queries, IVFPQ suitable for large-scale data</li>
 *     <li>Batch Operations: Use batch insertion and queries to improve throughput</li>
 *     <li>Sharding Strategy: Large datasets recommend using multi-sharding to improve concurrent performance</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BaseVDB {

    /**
     * Batch insert or update vector records
     * <p>
     * Batch process multiple vector data objects, significantly improving insertion efficiency for large amounts of data.
     * Uses transactional operations, either all succeed or all fail.
     *
     * <h4>Batch Operation Advantages</h4>
     * <ul>
     *     <li>Network Efficiency: Reduce network round trips, improve throughput</li>
     *     <li>Index Optimization: Batch build indexes, improve index efficiency</li>
     *     <li>Memory Utilization: Reasonable use of memory buffers, reduce disk IO</li>
     *     <li>Consistency Guarantee: Ensure atomicity of batch operations</li>
     * </ul>
     *
     * <h4>Batch Size Recommendations</h4>
     * <ul>
     *     <li>Small Batch: 100-500 records, suitable for scenarios with high real-time requirements</li>
     *     <li>Medium Batch: 1000-5000 records, balance performance and memory usage</li>
     *     <li>Large Batch: 5000+ records, suitable for offline data import</li>
     * </ul>
     *
     * @param dbName    Database name, cannot be null or empty string
     * @param spaceName Space name, cannot be null or empty string
     * @param dataList  Vector data list to batch insert, cannot be null or empty list
     * @return List of successfully inserted record IDs, order corresponds to input data
     * @throws IllegalArgumentException When parameters are invalid or data format is incorrect
     * @throws RuntimeException         When batch insertion operation fails
     */
    List<String> batchUpsert(String dbName, String spaceName, List<VdbDO> dataList);

    /**
     * Batch get vector records by ID list
     * <p>
     * Get multiple records in one query, has better performance compared to multiple single queries.
     * Order of returned results remains consistent with input ID list order.
     *
     * <h4>Batch Query Features</h4>
     * <ul>
     *     <li>Parallel Processing: Internal parallel query of multiple records</li>
     *     <li>Missing Handling: Non-existent IDs return null at corresponding positions</li>
     *     <li>Order Guarantee: Result order consistent with input ID order</li>
     *     <li>Partial Success: Partial ID query failures do not affect other results</li>
     * </ul>
     *
     * @param dbName        Database name, cannot be null or empty string
     * @param spaceName     Space name, cannot be null or empty string
     * @param ids           Record ID list, cannot be null or empty list
     * @param withEmbedding Whether to return vector embedding data
     * @return Vector data object list, non-existent records have null at corresponding positions
     * @throws IllegalArgumentException When parameters are invalid
     * @throws RuntimeException         When batch query operation fails
     */
    List<VdbDO> batchGetById(String dbName, String spaceName, List<String> ids, boolean withEmbedding);

    /**
     * Vector similarity search
     * <p>
     * Search based on vector similarity, this is the core function of vector database. Support multi-vector queries,
     * hybrid retrieval (vector + scalar filtering), relevance ranking and other advanced features.
     *
     * <h4>Similarity Algorithms</h4>
     * <ul>
     *     <li>Cosine Similarity: Suitable for text vectors, not affected by vector length</li>
     *     <li>Euclidean Distance: Suitable for image vectors, considers absolute position of vectors</li>
     *     <li>Inner Product Similarity: Suitable for normalized vector data</li>
     *     <li>Manhattan Distance: Suitable for high-dimensional sparse vectors</li>
     * </ul>
     *
     * <h4>Retrieval Strategies</h4>
     * <ul>
     *     <li>Pure Vector Retrieval: Only based on vector similarity ranking</li>
     *     <li>Hybrid Retrieval: Vector retrieval + scalar field filtering</li>
     *     <li>Multi-vector Fusion: Weighted combination of multiple query vectors</li>
     *     <li>Re-ranking: Secondary ranking based on business rules</li>
     * </ul>
     *
     * <h4>Performance Optimization</h4>
     * <ul>
     *     <li>TopK Limit: Set reasonable return count, avoid large result sets</li>
     *     <li>Pre-filtering: Use scalar filtering to reduce vector computation scope</li>
     *     <li>Parallel Query: Multiple query vectors can be processed in parallel</li>
     *     <li>Cache Utilization: Popular query vectors will be automatically cached</li>
     * </ul>
     *
     * <h4>RAG Scenario Applications</h4>
     * <ul>
     *     <li>Knowledge Retrieval: Find most relevant knowledge fragments according to question vectors</li>
     *     <li>Conversation Memory: Retrieve historical records most relevant to current conversation</li>
     *     <li>Content Recommendation: Recommend similar content based on user interest vectors</li>
     *     <li>Semantic Deduplication: Find semantically similar duplicate content</li>
     * </ul>
     *
     * <h4>Query Optimization Recommendations</h4>
     * <ul>
     *     <li>Vector Quality: Use high-quality embedding models to generate query vectors</li>
     *     <li>Threshold Setting: Set appropriate similarity thresholds to filter low-quality results</li>
     *     <li>Result Diversity: Avoid returning overly similar duplicate results</li>
     *     <li>Business Filtering: Combine business logic for secondary filtering and ranking</li>
     * </ul>
     *
     * @param dbName       Database name, cannot be null or empty string
     * @param spaceName    Space name, cannot be null or empty string
     * @param queryVectors Query vector list, support multi-vector queries, cannot be null or empty list
     * @param filter       Query filter conditions including scalar filtering, return fields, TopK configuration, cannot be null
     * @return Search result list sorted by similarity, including similarity scores and original data
     * @throws IllegalArgumentException When parameters are invalid or query vector dimensions do not match
     * @throws RuntimeException         When vector search operation fails
     */
    List<VdbDO> searchByVector(String dbName, String spaceName, List<VdbQueryFilterDO.queryVector> queryVectors, VdbQueryFilterDO filter);

}