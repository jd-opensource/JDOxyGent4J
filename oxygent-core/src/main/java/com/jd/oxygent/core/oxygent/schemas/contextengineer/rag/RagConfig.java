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

/**
 * <h3>RAG System Configuration Constants Class</h3>
 *
 * <p>Defines various constants and default configuration values used in the RAG system, providing unified configuration management.</p>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class RagConfig {

    /**
     * Default vector similarity threshold
     */
    public static final float DEFAULT_SIMILARITY_THRESHOLD = 0.3f;

    /**
     * Default rerank result size
     */
    public static final int DEFAULT_RERANK_SIZE = 5;

    /**
     * Default batch token limit
     */
    public static final int DEFAULT_BATCH_TOKEN_LIMIT = 32000;

    /**
     * Default retrieval candidate multiplier (used to get more candidates during coarse ranking)
     */
    public static final int DEFAULT_CANDIDATE_MULTIPLIER = 2;

    /**
     * Minimum TopK value
     */
    public static final int MIN_TOP_K = 1;

    /**
     * Maximum TopK value
     */
    public static final int MAX_TOP_K = 1000;

    /**
     * Maximum single token count
     */
    public static final int MAX_SINGLE_TOKEN = 32000;

    /**
     * LLM cache expiration time (milliseconds)
     */
    public static final long LLM_CACHE_EXPIRE_TIME = 30 * 60 * 1000L; // 30 minutes

    /**
     * Default embedding model name
     */
    public static final String DEFAULT_EMBEDDING_MODEL = "";

    /**
     * Default reranker model name
     */
    public static final String DEFAULT_RERANKER_MODEL = "";

    /**
     * Default vector dimension
     */
    public static final int DEFAULT_DIMENSION = 1024;

    /**
     * Default AI service base URL
     */
    public static final String DEFAULT_BASE_URL = "";

    /**
     * Default API key (for development and testing only)
     */
    public static final String DEFAULT_API_KEY = "";

    private RagConfig() {
        // Utility class, instantiation prohibited
    }
}