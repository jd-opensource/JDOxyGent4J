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
package com.jd.oxygent.core.oxygent.schemas.contextengineer.rag;

import com.jd.oxygent.core.oxygent.schemas.contextengineer.model.LLMCfg;
import lombok.Builder;
import lombok.Data;

/**
 * <h3>RAG Request Parameter Wrapper Class</h3>
 *
 * <p>Encapsulates all RAG query request parameters using the Builder pattern, providing flexible parameter configuration and chain calls.</p>
 *
 * <h3>Main Features:</h3>
 * <ul>
 *   <li><strong>Builder Pattern</strong>: Supports chain calls, more flexible parameter configuration</li>
 *   <li><strong>Parameter Validation</strong>: Built-in parameter validity check</li>
 *   <li><strong>Default Value Support</strong>: Provides reasonable default configuration</li>
 *   <li><strong>Type Safety</strong>: Strongly typed parameters, avoids runtime errors</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * RagRequest request = RagRequest.builder()
 *     .query("How to use RAG technology?")
 *     .topK(10)
 *     .spaceName("knowledge_base")
 *     .embeddingCfg(embeddingConfig)
 *     .rerankerCfg(rerankerConfig)
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class RagRequest {

    /**
     * Query text content
     */
    private String query;

    /**
     * Number of results to return
     */
    @Builder.Default
    private int topK = RagConfig.DEFAULT_RERANK_SIZE;

    /**
     * Vector space name
     */
    private String spaceName;

    /**
     * Embedding model configuration
     */
    private LLMCfg embeddingCfg;

    /**
     * Reranker model configuration
     */
    private LLMCfg rerankerCfg;

    /**
     * Similarity threshold
     */
    @Builder.Default
    private float similarityThreshold = RagConfig.DEFAULT_SIMILARITY_THRESHOLD;

    /**
     * Candidate result multiplier (candidate count in coarse ranking = topK * candidateMultiplier)
     */
    @Builder.Default
    private int candidateMultiplier = RagConfig.DEFAULT_CANDIDATE_MULTIPLIER;

    /**
     * Whether to enable reranking
     */
    @Builder.Default
    private boolean enableReranking = true;

    /**
     * Validate the validity of request parameters
     *
     * @throws IllegalArgumentException If parameters are invalid
     */
    public void validate() {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be empty");
        }

        if (topK < RagConfig.MIN_TOP_K || topK > RagConfig.MAX_TOP_K) {
            throw new IllegalArgumentException(
                    String.format("topK must be between %d and %d", RagConfig.MIN_TOP_K, RagConfig.MAX_TOP_K)
            );
        }

        if (spaceName == null || spaceName.trim().isEmpty()) {
            throw new IllegalArgumentException("spaceName cannot be empty");
        }

        if (embeddingCfg == null) {
            throw new IllegalArgumentException("Embedding configuration cannot be null");
        }

        if (enableReranking && rerankerCfg == null) {
            throw new IllegalArgumentException("Reranker configuration cannot be null when reranking is enabled");
        }

        if (similarityThreshold < 0.0f || similarityThreshold > 1.0f) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }

        if (candidateMultiplier < 1) {
            throw new IllegalArgumentException("Candidate multiplier must be greater than or equal to 1");
        }
    }

    /**
     * Get the candidate count for coarse ranking
     *
     * @return candidate count
     */
    public int getCandidateCount() {
        return topK * candidateMultiplier;
    }
}