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
package com.jd.oxygent.core.oxygent.infra.dataobject.vearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * <h3>Vearch Vector Search Query Filter Condition Data Object</h3>
 *
 * <p>Data Transfer Object (DTO) specifically designed for building complex query conditions in Vearch vector database.
 * This class encapsulates various filter conditions and query parameters in vector search, supporting hybrid retrieval (vector search + scalar filtering) scenarios.</p>
 *
 * <h3>Technical Features</h3>
 * <ul>
 *   <li><strong>Official SDK Compatibility</strong>: Fully complies with Vearch official query API specifications, ensuring accurate parameter mapping</li>
 *   <li><strong>Hybrid Retrieval Support</strong>: Supports both vector similarity search and scalar field exact filtering</li>
 *   <li><strong>Performance Optimization</strong>: Supports advanced features like partition queries and load balancing to improve large-scale data query performance</li>
 *   <li><strong>Flexible Configuration</strong>: Provides rich query parameters to meet retrieval needs of different business scenarios</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li><strong>Semantic Retrieval</strong>: Retrieve similar questions and answers in intelligent customer service systems</li>
 *   <li><strong>Personalized Recommendation</strong>: Find similar users or products based on user behavior vectors</li>
 *   <li><strong>Content Deduplication</strong>: Detect duplicate or similar content in document management systems</li>
 *   <li><strong>Multimodal Search</strong>: Comprehensive search combining text, images and other modalities</li>
 * </ul>
 *
 * <h3>Query Process</h3>
 * <p>Typical vector search process is as follows:</p>
 * <ol>
 *   <li>Build query condition object, set filter conditions and vector query parameters</li>
 *   <li>Vearch engine first filters candidate set based on filters conditions</li>
 *   <li>Perform vector similarity calculation and sorting within candidate set</li>
 *   <li>Return TopK most similar results, including specified field content</li>
 * </ol>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Build compound query conditions
 * VdbQueryFilterDO queryFilter = VdbQueryFilterDO.builder()
 *     .filters(Map.of("user_id", "user123", "session_id", "session456"))
 *     .fields(List.of("_id", "text", "create_time"))
 *     .limit(10)
 *     .vectorValue(true)
 *     .build();
 *
 * // Execute search
 * List<VdbDO> results = vearchService.search(queryFilter);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VdbQueryFilterDO {

    /**
     * Vector query parameter record class
     * <p>
     * Encapsulates core parameters of vector queries, including query vector and minimum similarity threshold.
     * Uses Record type to ensure data immutability and thread safety.
     *
     * @param field    Vector field name, typically "vector"
     * @param feature  Query vector feature array, dimension must be consistent with vector dimension in index
     * @param minScore Minimum similarity threshold, results below this value will be filtered out
     */
    public record queryVector(String field, float[] feature, float minScore) {
    }

    /**
     * Partition identifier (optional)
     * <p>
     * Specifies searching within a specific partition, which can improve query performance.
     * In large-scale data scenarios, partition queries can reduce search scope and improve response speed.
     * If not specified, search will be performed across all partitions.
     */
    @JsonProperty("partition_id")
    private Integer partitionId;

    /**
     * Scalar field filter conditions
     * <p>
     * Used to pre-filter data before vector search, supporting combination of multiple filter conditions.
     * This hybrid retrieval approach can significantly improve query precision and relevance.
     *
     * <h3>Supported Filter Condition Formats</h3>
     * <ul>
     *   <li><strong>Exact Match</strong>: {"user_id": "user123"}</li>
     *   <li><strong>Range Query</strong>: {"create_time": {"gte": 1640995200000}}</li>
     *   <li><strong>List Match</strong>: {"category": {"in": ["tech", "business"]}}</li>
     *   <li><strong>Compound Conditions</strong>: {"user_id": "user123", "msg_turn": {"gte": 5}}</li>
     * </ul>
     */
    @JsonProperty("filters")
    private Map<String, Object> filters;

    /**
     * Return field list (required)
     * <p>
     * Specifies the field list to be returned in search results, which can reduce network transmission overhead.
     * It is recommended to return only business-necessary fields, especially in large-scale data retrieval.
     *
     * <h3>Common Field Configurations</h3>
     * <ul>
     *   <li><strong>Basic Information</strong>: ["_id", "user_id", "session_id"]</li>
     *   <li><strong>Content Fields</strong>: ["text", "index_text", "create_time"]</li>
     *   <li><strong>Complete Information</strong>: ["_id", "text", "create_time", "msg_turn"]</li>
     * </ul>
     */
    @JsonProperty("fields")
    private List<String> fields;

    /**
     * Whether to return vector values
     * <p>
     * Controls whether search results include original vector data.
     * Since vector data typically occupies large space, it is recommended to set to true only when secondary computation is needed.
     *
     * <strong>Default value</strong>: false, do not return vector data to save transmission bandwidth
     */
    @JsonProperty("vector_value")
    private Boolean vectorValue;

    /**
     * Return result count limit
     * <p>
     * Controls the maximum number of results returned by search, i.e., the K value in TopK search.
     * Setting this value appropriately can balance query performance and result completeness.
     *
     * <h3>Recommended Configurations</h3>
     * <ul>
     *   <li><strong>Real-time Query</strong>: 10-50, ensure response speed</li>
     *   <li><strong>Batch Analysis</strong>: 100-1000, get more comprehensive results</li>
     *   <li><strong>Data Mining</strong>: 1000+, for in-depth analysis</li>
     * </ul>
     *
     * <strong>Default value</strong>: 10
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * Whether to use brute force search
     * <p>
     * Controls whether to bypass vector index for brute force search. Brute force search has higher precision but poorer performance,
     * suitable for small datasets or scenarios with extremely high precision requirements.
     *
     * <h3>Applicable Scenarios</h3>
     * <ul>
     *   <li><strong>0 (default)</strong>: Use vector index, balance performance and precision</li>
     *   <li><strong>1</strong>: Brute force search, pursue highest precision</li>
     * </ul>
     *
     * <strong>Default value</strong>: 0, use index search
     */
    @JsonProperty("is_brute_search")
    private Integer isBruteSearch;

    /**
     * Load balancing strategy
     * <p>
     * Specifies load balancing strategy in multi-node cluster environment, used to optimize query performance and resource utilization.
     * Different strategies are suitable for different cluster scales and query patterns.
     *
     * <h3>Supported Strategies</h3>
     * <ul>
     *   <li><strong>random</strong>: Randomly select nodes, suitable for uniform load</li>
     *   <li><strong>round_robin</strong>: Round-robin strategy, ensure load balancing</li>
     *   <li><strong>leader_only</strong>: Use only master node, ensure data consistency</li>
     * </ul>
     */
    @JsonProperty("load_balance")
    private String loadBalance;

    /**
     * Result ranker configuration
     * <p>
     * Customize sorting logic for search results, supporting multi-field sorting and compound sorting strategies.
     * Can implement sorting requirements based on multiple dimensions like similarity, time, weight, etc.
     *
     * <h3>Sorting Configuration Example</h3>
     * <pre>{@code
     * {
     *   "type": "weighted_score",
     *   "weights": {
     *     "vector_score": 0.7,
     *     "time_decay": 0.3
     *   }
     * }
     * }</pre>
     */
    @JsonProperty("ranker")
    private Map<String, Object> ranker;

    /**
     * Index query parameters
     * <p>
     * Advanced query parameters for specific index types, used to fine-tune search behavior.
     * Different index types (HNSW, IVFPQ, etc.) support different parameter configurations.
     *
     * <h3>HNSW Index Parameter Example</h3>
     * <pre>{@code
     * {
     *   "ef": 100,           // Candidate list size during search
     *   "metric_type": "InnerProduct"  // Distance calculation method
     * }
     * }</pre>
     *
     * <h3>IVFPQ Index Parameter Example</h3>
     * <pre>{@code
     * {
     *   "nprobe": 10,        // Number of cluster centers to query
     *   "metric_type": "L2"  // Distance calculation method
     * }
     * }</pre>
     */
    @JsonProperty("index_params")
    private Map<String, Object> indexParams;

}