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
package com.jd.oxygent.core.oxygent.infra.rag;

import java.util.List;

/**
 * Knowledge Repository Retrieval Interface
 * <p>
 * Provides unified knowledge repository retrieval capabilities for the OxyGent system,
 * supporting intelligent document retrieval based on semantic similarity.
 * This interface is a core retrieval component in the RAG (Retrieval-Augmented Generation) architecture.
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *     <li>Retrieval Abstraction: Provides unified knowledge retrieval interface, hiding underlying storage differences</li>
 *     <li>Multi-source Fusion: Supports retrieval from multiple knowledge sources and result fusion</li>
 *     <li>Semantic Understanding: Implements semantic similarity retrieval based on vectorization technology</li>
 *     <li>Context Awareness: Combines application scenarios and user context to optimize retrieval effectiveness</li>
 * </ul>
 *
 * <h3>Core Functions</h3>
 * <ul>
 *     <li>Semantic Retrieval: Finds the most relevant knowledge fragments based on question semantic features</li>
 *     <li>Multimodal Support: Supports various knowledge types including text, code, documents, etc.</li>
 *     <li>Dynamic Ranking: Ranks results based on relevance, timeliness, authority and other factors</li>
 *     <li>Context Filtering: Filters results based on application scenarios and user permissions</li>
 * </ul>
 *
 * <h3>Application Scenarios</h3>
 * <ul>
 *     <li>Intelligent Q&A: Retrieves the most relevant answers or clues for user questions</li>
 *     <li>Code Assistant: Retrieves relevant code examples, API documentation, best practices</li>
 *     <li>Document Assistance: Recommends relevant reference materials during writing process</li>
 *     <li>Decision Support: Provides relevant historical data and experience for business decisions</li>
 * </ul>
 *
 * <h3>Performance Optimization</h3>
 * <ul>
 *     <li>Caching Strategy: Caches popular query results to improve response speed</li>
 *     <li>Preprocessing: Knowledge preprocessing and index optimization to improve retrieval efficiency</li>
 *     <li>Parallel Retrieval: Parallel queries across multiple knowledge sources to reduce overall latency</li>
 *     <li>Result Deduplication: Intelligent deduplication to avoid returning duplicate content</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface Knowledge {

    /**
     * Retrieves knowledge chunks based on query conditions
     * <p>
     * Retrieves the most relevant knowledge chunks from the knowledge base based on user query,
     * application context, and access key. This method is the core interface for knowledge retrieval,
     * supporting multiple retrieval strategies and result ranking methods.
     *
     * <h4>Retrieval Strategies</h4>
     * <ul>
     *     <li>Semantic Matching: Semantic retrieval based on vector similarity</li>
     *     <li>Keyword Matching: Exact or fuzzy matching based on keywords</li>
     *     <li>Hybrid Retrieval: Weighted combination of semantic and keyword retrieval</li>
     *     <li>Context Awareness: Combines application scenarios to optimize retrieval results</li>
     * </ul>
     *
     * <h4>Result Optimization</h4>
     * <ul>
     *     <li>Relevance Ranking: Ranks by degree of relevance to the query</li>
     *     <li>Diversity Guarantee: Avoids returning overly similar duplicate content</li>
     *     <li>Timeliness Consideration: Prioritizes returning the latest or most authoritative content</li>
     *     <li>Permission Filtering: Filters user-visible content based on access key</li>
     * </ul>
     *
     * @param query User query content, cannot be null or empty string, supports natural language questions
     * @param app   Application identifier, used to determine retrieval scope and context, cannot be null or empty string
     * @param key   Access key, used for permission control and personalized retrieval, cannot be null or empty string
     * @return List of knowledge chunks sorted by relevance, each chunk contains complete context information
     * @throws IllegalArgumentException when parameters are invalid
     * @throws RuntimeException         when retrieval operation fails
     */
    List<String> getChunkList(String query, String app, String key);

}
