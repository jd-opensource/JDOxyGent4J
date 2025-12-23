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
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * <h3>Vearch Vector Database Data Object</h3>
 *
 * <p>Streamlined vector database data object designed based on Vearch official SDK, specifically for vector storage and retrieval functions in OxyGent framework.
 * This class adopts <strong>Domain Driven Design (DDD)</strong> pattern, encapsulating core business entities of vector database.</p>
 *
 * <h3>Technical Features</h3>
 * <ul>
 *   <li><strong>Official Compatibility</strong>: Fully based on Vearch official field design, ensuring perfect integration with Vearch cluster</li>
 *   <li><strong>High Performance Design</strong>: Contains only core fields, reducing network transmission and storage overhead</li>
 *   <li><strong>JSON Serialization</strong>: Uses Jackson annotations to support efficient JSON serialization/deserialization</li>
 *   <li><strong>Vector Index Optimization</strong>: Supports multiple vector index algorithms like HNSW, IVFPQ to meet different scenario requirements</li>
 *   <li><strong>Field Auto Mapping</strong>: Provides toFields() method to automatically generate Vearch space field configuration</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li>Semantic memory storage and retrieval in intelligent dialogue systems</li>
 *   <li>Vectorized representation and similarity matching of multi-turn dialogue context</li>
 *   <li>Vectorized storage of entities and relationships in knowledge graphs</li>
 *   <li>Vectorized representation of user behavior and product features in recommendation systems</li>
 * </ul>
 *
 * <h3>Architecture Design</h3>
 * <p>This class follows the following design principles:</p>
 * <ul>
 *   <li><strong>Minimization Principle</strong>: Only retain core fields necessary for business</li>
 *   <li><strong>Extensibility Principle</strong>: Reserve extension fields to support future feature enhancements</li>
 *   <li><strong>Performance Optimization</strong>: Use float[] array to store vector data, improving computational efficiency</li>
 *   <li><strong>Data Security</strong>: Support data isolation in multi-tenant scenarios (user_id, session_id)</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create vector data object
 * VdbDO vdbDO = VdbDO.builder()
 *     .id("memory_001")
 *     .userId("user123")
 *     .sessionId("session456")
 *     .msgTurn(1)
 *     .text("Original text input by user")
 *     .indexText("Processed text for vectorization")
 *     .vector(new float[]{0.1f, 0.2f, 0.3f, ...})
 *     .createTime(System.currentTimeMillis())
 *     .build();
 *
 * // Get Vearch space field configuration
 * List<Field> fields = VdbDO.toFields();
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
public class VdbDO {

    /* ---------- Business Fields ---------- */

    /**
     * Business primary key, uniquely identifies vector data record
     * <p>
     * This field corresponds to document ID in Vearch, typically uses memory_id as business identifier.
     * In OxyGent framework, used to identify specific memory fragments or dialogue nodes.
     */
    @JsonProperty("_id")
    private String id;

    /**
     * User unique identifier
     * <p>
     * Used to implement multi-tenant data isolation, ensuring vector data of different users are independent.
     * Supports data filtering and permission control based on user dimension.
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Session unique identifier
     * <p>
     * Identifies specific dialogue session, used to logically group related dialogue fragments.
     * In multi-turn dialogue scenarios, historical context of the same session can be retrieved through session_id.
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * Message turn number
     * <p>
     * In the same session, identifies the sequence order of messages. Used for temporal analysis and context association of dialogues.
     * Larger values indicate newer messages, can be used for sorting and time window retrieval.
     */
    @JsonProperty("msg_turn")
    private Integer msgTurn;

    /**
     * Record creation timestamp
     * <p>
     * Uses Unix timestamp (milliseconds) to record data creation time, used for temporal dimension analysis and expiration management.
     * Can implement data lifecycle management and historical data cleanup based on creation time.
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * Original text content
     * <p>
     * Saves original text input by user or generated by system, without any preprocessing.
     * This field is mainly used for data backtracking and manual review, typically no search index is built to save storage space.
     */
    @JsonProperty("text")
    private String text;

    /**
     * Vectorized index text
     * <p>
     * Text content after preprocessing (cleaning, tokenization, standardization) used to generate vector representation.
     * This field is the foundation of vector retrieval, will build full-text search index to support hybrid retrieval (vector + keywords).
     */
    @JsonProperty("index_text")
    private String indexText;

    /**
     * High-dimensional vector data
     * <p>
     * Uses float array to store vectorized representation of text, typically 512 or 1024 dimensional dense vectors.
     * This field is the core of similarity search, supporting cosine similarity, inner product and other distance calculation methods.
     *
     * <strong>Performance Optimization</strong>:
     * <ul>
     *   <li>Use float[] instead of List<Float>, reducing object overhead</li>
     *   <li>Support SIMD instruction accelerated vector operations</li>
     *   <li>Fully compatible with Vearch's native storage format</li>
     * </ul>
     */
    @JsonProperty("vector")
    private float[] vector;

    /**
     * Extension data field (reserved)
     * <p>
     * Reserved extension field for storing business-related metadata information, stored in JSON format.
     * Can be used to store tags, categories, weights and other auxiliary information, supporting complex business query requirements.
     *
     * Note: Currently not enabled in this version, will be extended according to business requirements in future versions.
     */
    // private Map<String, Object> payload;

    /* ---------- One-click Generate Vearch fields ---------- */

    /**
     * Automatically generate Vearch space field configuration
     * <p>
     * Based on the field structure of current data object, automatically generate field configuration list
     * compliant with Vearch official specifications.
     * This method implements automatic mapping from business objects to Vearch storage structure,
     * simplifying vector space creation and maintenance work.
     *
     * <h3>Field Mapping Strategy</h3>
     * <ul>
     *   <li><strong>Scalar Fields</strong>: Automatically identify string, integer, long integer fields and create corresponding indexes</li>
     *   <li><strong>Vector Fields</strong>: Configure high-dimensional vector storage and HNSW index, supporting efficient similarity search</li>
     *   <li><strong>Index Optimization</strong>: Intelligently choose whether to build index according to field usage, balancing query performance and storage overhead</li>
     * </ul>
     *
     * <h3>Index Configuration</h3>
     * <ul>
     *   <li>user_id, session_id: Build SCALAR index, support exact matching and filtering</li>
     *   <li>msg_turn, create_time: Build SCALAR index, support range queries and sorting</li>
     *   <li>index_text: Build SCALAR index, support keyword search</li>
     *   <li>text: No index built, only used for data backtracking</li>
     *   <li>vector: Build HNSW vector index, using InnerProduct distance calculation</li>
     * </ul>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * // Create complete configuration for Vearch space
     * Map<String, Object> spaceConfig = new HashMap<>();
     * spaceConfig.put("name", "oxygent_memory_space");
     * spaceConfig.put("partition_num", 1);
     * spaceConfig.put("replica_num", 3);
     * spaceConfig.put("fields", VdbDO.toFields());
     *
     * // Use configuration to create space
     * vearchClient.createSpace(spaceConfig);
     * }</pre>
     *
     * @return Field object list containing all field configurations, can be directly used for Vearch space creation
     * @see Field Vearch official field configuration class
     * @see Index Vearch index configuration class
     */
    public static List<Field> toFields() {
        List<Field> list = new ArrayList<>();

        /* 2. Regular scalar fields */
        list.add(scalar("user_id", "string", true));
        list.add(scalar("session_id", "string", true));
        list.add(scalar("msg_turn", "integer", true));
        list.add(scalar("create_time", "long", true));
        list.add(scalar("text", "string", false));      // Index on demand
        list.add(scalar("index_text", "string", true));
        // payload stores JSON string
//        list.add(scalar("payload", "string", false));

        /* 3. Vector field */
        list.add(vector("vector", 1024, "HNSW", "InnerProduct"));
        return list;
    }


    /* ---------- Utility Methods ---------- */

    /**
     * Create scalar field configuration
     * <p>
     * Create configuration object for scalar type fields in Vearch space, supporting basic data types
     * like string, integer, long integer.
     * Decide whether to build index for this field according to business requirements,
     * balancing query performance and storage cost.
     *
     * <h3>Supported Data Types</h3>
     * <ul>
     *   <li><strong>string</strong>: Variable length string, supporting exact matching and prefix search</li>
     *   <li><strong>integer</strong>: 32-bit signed integer, supporting range queries and sorting</li>
     *   <li><strong>long</strong>: 64-bit signed long integer, suitable for timestamps and other large values</li>
     * </ul>
     *
     * <h3>Index Strategy</h3>
     * <ul>
     *   <li><strong>Build Index</strong>: Suitable for fields frequently used for filtering and sorting (like user_id, create_time)</li>
     *   <li><strong>No Index</strong>: Suitable for fields only used for data backtracking (like original text), saving storage space</li>
     * </ul>
     *
     * @param name      Field name, must be consistent with JSON serialization field name
     * @param type      Field data type, supports "string", "integer", "long", etc.
     * @param needIndex Whether to build SCALAR index for this field
     * @return Configured Field object, containing field definition and index configuration
     */
    private static Field scalar(String name, String type, boolean needIndex) {
        Field f = new Field();
        f.setName(name);
        f.setType(type);
        if (needIndex) {
            Index idx = new Index();
            idx.setName(name);
            idx.setType("SCALAR");
            f.setIndex(idx);
        }
        return f;
    }

    /**
     * Create vector field configuration
     * <p>
     * Create configuration object for high-dimensional vector fields in Vearch space,
     * supporting multiple vector index algorithms and distance calculation methods.
     * This method is the core of vector search functionality, directly affecting the performance
     * and accuracy of similarity retrieval.
     *
     * <h3>Supported Index Types</h3>
     * <ul>
     *   <li><strong>HNSW</strong>: Hierarchical Navigable Small World graph algorithm, fast query speed, moderate memory usage, suitable for most scenarios</li>
     *   <li><strong>IVFPQ</strong>: Inverted File + Product Quantization, small memory footprint, suitable for ultra-large scale datasets</li>
     * </ul>
     *
     * <h3>Distance Calculation Methods</h3>
     * <ul>
     *   <li><strong>InnerProduct</strong>: Inner product distance, suitable for normalized vectors, high computational efficiency</li>
     *   <li><strong>L2</strong>: Euclidean distance, suitable for general vector comparison, high precision</li>
     *   <li><strong>Cosine</strong>: Cosine similarity, suitable for text vectors, not affected by vector length</li>
     * </ul>
     *
     * <h3>Performance Tuning</h3>
     * <p>For different data scales and query requirements, the following configurations are recommended:</p>
     * <ul>
     *   <li><strong>Small scale(<100k)</strong>: HNSW + InnerProduct, pursuing query speed</li>
     *   <li><strong>Medium scale(100k-1M)</strong>: HNSW + L2, balancing precision and performance</li>
     *   <li><strong>Large scale(>1M)</strong>: IVFPQ + appropriate parameter tuning, controlling memory usage</li>
     * </ul>
     *
     * @param name      Vector field name, typically "vector"
     * @param dimension Vector dimension, must be consistent with embedding model output dimension (like 512, 1024, etc.)
     * @param indexType Vector index type, recommend using "HNSW"
     * @param metric    Distance calculation method, recommend using "InnerProduct"
     * @return Configured vector Field object, containing index algorithm and retrieval parameters
     * @see Hnsw HNSW index parameter configuration class
     * @see RetrievalParam Retrieval parameter configuration class
     */
    private static Field vector(String name, int dimension, String indexType, String metric) {
        Field f = new Field();
        f.setName(name);
        f.setType("vector");
        f.setDimension(dimension);

        Index idx = new Index();
        idx.setName(name);
        idx.setType(indexType);
        // Official recommended parameters
        RetrievalParam params = new RetrievalParam();
        params.setMetricType(metric);
        if ("HNSW".equals(indexType)) {
            Hnsw hnsw = new Hnsw();
            params.setHnsw(hnsw);
        } else if ("IVFPQ".equals(indexType)) {
            params.setMetricType(metric);
            params.setNcentroids(2048);
            params.setNsubvector(64);
        }
        idx.setRetrievalParam(params);
        f.setIndex(idx);
        return f;
    }
}
