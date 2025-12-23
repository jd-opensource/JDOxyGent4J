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

import com.jd.oxygent.core.oxygent.infra.databases.BaseVDB;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbDO;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbQueryFilterDO;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.model.LLMCfg;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.rag.chunker.Tokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * <h3>Optimized RAG Retrieval-Augmented Generation Service</h3>
 *
 * <p>The refactored RAG service class solves design issues from the previous version and provides better maintainability, scalability, and performance.</p>
 *
 * <h3>Main Improvements:</h3>
 * <ul>
 *   <li><strong>Dependency Injection</strong>: Uses Spring dependency injection instead of static configuration</li>
 *   <li><strong>Responsibility Separation</strong>: Splits retrieval, reranking, and other functions into independent methods</li>
 *   <li><strong>Builder Pattern</strong>: Uses RagRequest to simplify parameter passing</li>
 *   <li><strong>Exception Handling</strong>: Comprehensive parameter validation and exception handling mechanism</li>
 *   <li><strong>Performance Optimization</strong>: LLM instance caching and batch optimization</li>
 *   <li><strong>Configuration Management</strong>: Supports configuration files and environment variables</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Build request using Builder pattern
 * RagRequest request = RagRequest.builder()
 *     .query("What is RAG technology?")
 *     .topK(10)
 *     .spaceName("knowledge_base")
 *     .embeddingCfg(embeddingConfig)
 *     .rerankerCfg(rerankerConfig)
 *     .build();
 *
 * // Execute RAG query
 * List<String> results = ragService.search(request);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@Slf4j
public class RagService {

    @Autowired
    private BaseVDB baseVDB;


    @Value("${oxygent.vearch.database:oxygent_java_memory}")
    private String dbName;

    /**
     * LLM instance cache to avoid repeated creation
     */
    private final Map<String, CachedLLM> llmCache = new ConcurrentHashMap<>();

    /**
     * LLM cache wrapper class
     */
    private static class CachedLLM {
        private final RagLLM llm;
        private final long createTime;

        public CachedLLM(RagLLM llm) {
            this.llm = llm;
            this.createTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createTime > RagConfig.LLM_CACHE_EXPIRE_TIME;
        }

        public RagLLM getLlm() {
            return llm;
        }
    }

    @PostConstruct
    public void init() {
        log.info("RAG service initialized, database name: {}", dbName);
    }

    @PreDestroy
    public void destroy() {
        // Clear LLM cache
        llmCache.clear();
        log.info("RAG service closed, resources cleaned up");
    }

    /**
     * Execute RAG query using default configuration
     *
     * @param query     Query text
     * @param topK      Number of results to return
     * @param spaceName Vector space name
     * @return List of query results
     */
    public List<String> defaultRag(String query, Integer topK, String spaceName) {
        try {
            LLMCfg embeddingCfg = createDefaultEmbeddingConfig();
            LLMCfg rerankerCfg = createDefaultRerankerConfig();

            RagRequest request = RagRequest.builder()
                    .query(query)
                    .topK(topK)
                    .spaceName(spaceName)
                    .embeddingCfg(embeddingCfg)
                    .rerankerCfg(rerankerCfg)
                    .build();

            return search(request);
        } catch (Exception e) {
            log.error("Default RAG query failed: query={}, topK={}, spaceName={}", query, topK, spaceName, e);
            throw new RuntimeException("RAG query execution failed", e);
        }
    }

    /**
     * Execute RAG query (main entry method)
     *
     * @param request RAG request parameters
     * @return List of query results
     */
    public List<String> search(RagRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Parameter validation
            request.validate();

            log.info("Start RAG query: query={}, topK={}, spaceName={}",
                    request.getQuery(), request.getTopK(), request.getSpaceName());

            // 1. Vector retrieval (coarse ranking)
            List<VdbDO> candidates = vectorSearch(request);
            long vectorSearchTime = System.currentTimeMillis();
            log.info("Vector retrieval completed, time: {}ms, candidate count: {}",
                    vectorSearchTime - startTime, candidates.size());

            // 2. Reranking (fine ranking)
            List<String> results;
            if (request.isEnableReranking() && !candidates.isEmpty()) {
                results = rerank(request, candidates);
                long rerankTime = System.currentTimeMillis();
                log.info("Reranking completed, time: {}ms", rerankTime - vectorSearchTime);
            } else {
                results = candidates.stream()
                        .map(VdbDO::getText)
                        .limit(request.getTopK())
                        .collect(Collectors.toList());
                log.info("Skip reranking, return vector retrieval results directly");
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("RAG query completed, total time: {}ms, result count: {}", totalTime, results.size());

            return results;
        } catch (Exception e) {
            log.error("RAG query failed: {}", request, e);
            throw new RuntimeException("RAG query execution failed", e);
        }
    }

    /**
     * Vector retrieval (coarse ranking stage)
     *
     * @param request RAG request parameters
     * @return List of candidate documents
     */
    private List<VdbDO> vectorSearch(RagRequest request) {
        try {
            // Get or create Embedding LLM instance
            RagLLM embeddingLLM = getOrCreateLLM(request.getEmbeddingCfg());

            // Generate query vector
            int dimension = RagConfig.DEFAULT_DIMENSION;
            float[] queryVector = embeddingLLM.embedding(request.getQuery(), dimension);

            // Perform vector query
            return performVectorQuery(queryVector, request.getCandidateCount(),
                    request.getSpaceName(), request.getSimilarityThreshold());
        } catch (Exception e) {
            log.error("Vector retrieval failed", e);
            throw new RuntimeException("Vector retrieval execution failed", e);
        }
    }

    /**
     * Reranking (fine ranking stage)
     *
     * @param request    RAG request parameters
     * @param candidates List of candidate documents
     * @return List of reranked results
     */
    private List<String> rerank(RagRequest request, List<VdbDO> candidates) {
        try {
            if (candidates.isEmpty()) {
                return Collections.emptyList();
            }

            // Get or create Reranker LLM instance
            RagLLM rerankerLLM = getOrCreateLLM(request.getRerankerCfg());

            // Extract text content
            List<String> texts = candidates.stream()
                    .map(VdbDO::getText)
                    .collect(Collectors.toList());

            // Perform reranking
            return rerankerLLM.reranker(request.getQuery(), texts, request.getTopK());
        } catch (Exception e) {
            log.error("Reranking failed", e);
            throw new RuntimeException("Reranking execution failed", e);
        }
    }

    /**
     * Batch index documents using default configuration
     *
     * @param vdbDOList List of documents to be indexed
     * @param spaceName Vector space name
     * @return List of index results
     */
    public List<String> defaultIndex(List<VdbDO> vdbDOList, String spaceName) {
        try {
            if (vdbDOList == null || vdbDOList.isEmpty()) {
                log.warn("Document list for indexing is empty, skipping index operation");
                return Collections.emptyList();
            }

            LLMCfg embeddingCfg = createDefaultEmbeddingConfig();
            return batchIndex(embeddingCfg, vdbDOList, spaceName);
        } catch (Exception e) {
            log.error("Default batch indexing failed: spaceName={}, document count={}", spaceName, vdbDOList.size(), e);
            throw new RuntimeException("Batch indexing execution failed", e);
        }
    }

    /**
     * Batch index documents (optimized version)
     *
     * @param cfg       LLM configuration
     * @param vdbDOList List of documents to be indexed
     * @param spaceName Vector space name
     * @return List of index results
     */
    public List<String> batchIndex(LLMCfg cfg, List<VdbDO> vdbDOList, String spaceName) {
        if (vdbDOList == null || vdbDOList.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            log.info("Start batch indexing: document count={}, spaceName={}", vdbDOList.size(), spaceName);
            long startTime = System.currentTimeMillis();

            // Get or create LLM instance
            RagLLM embeddingLLM = getOrCreateLLM(cfg);

            // Intelligent batch embedding
            List<float[]> allVectors = performBatchEmbedding(embeddingLLM, vdbDOList);
            long embeddingTime = System.currentTimeMillis();
            log.info("Embedding completed, time: {}ms", embeddingTime - startTime);

            // Set vectors to document objects
            IntStream.range(0, Math.min(vdbDOList.size(), allVectors.size()))
                    .forEach(i -> vdbDOList.get(i).setVector(allVectors.get(i)));

            // Batch write to database
            List<String> result = batchUpsert(vdbDOList, spaceName);
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Batch indexing completed, total time: {}ms, successfully indexed: {}", totalTime, result.size());

            return result;
        } catch (Exception e) {
            log.error("Batch indexing failed: spaceName={}, document count={}", spaceName, vdbDOList.size(), e);
            throw new RuntimeException("Batch indexing execution failed", e);
        }
    }

    /**
     * Intelligent batch vectorization
     *
     * @param embeddingLLM LLM instance
     * @param vdbDOList    document list
     * @return vector list
     */
    private List<float[]> performBatchEmbedding(RagLLM embeddingLLM, List<VdbDO> vdbDOList) {
        int dimension = RagConfig.DEFAULT_DIMENSION;
        int maxTokenPerBatch = RagConfig.DEFAULT_BATCH_TOKEN_LIMIT;
        List<float[]> allVectors = new ArrayList<>(vdbDOList.size());

        List<VdbDO> currentBatch = new ArrayList<>();
        int currentTokenCount = 0;

        for (VdbDO doc : vdbDOList) {
            int tokenCount = Tokenizer.count(doc.getIndexText());

            // Skip documents that exceed the limit and log warning
            if (tokenCount > maxTokenPerBatch) {
                log.warn("Single document token count ({}) exceeds limit ({}), skipping index: id={}",
                        tokenCount, maxTokenPerBatch, doc.getId());
                continue;
            }

            // If adding current document would exceed batch limit, process current batch first
            if (currentTokenCount + tokenCount > maxTokenPerBatch && !currentBatch.isEmpty()) {
                List<String> batchTexts = currentBatch.stream()
                        .map(VdbDO::getIndexText)
                        .collect(Collectors.toList());
                allVectors.addAll(embeddingLLM.embedding(batchTexts, dimension));

                // Reset batch
                currentBatch.clear();
                currentTokenCount = 0;
            }

            currentBatch.add(doc);
            currentTokenCount += tokenCount;
        }

        // Process the last batch
        if (!currentBatch.isEmpty()) {
            List<String> batchTexts = currentBatch.stream()
                    .map(VdbDO::getIndexText)
                    .collect(Collectors.toList());
            allVectors.addAll(embeddingLLM.embedding(batchTexts, dimension));
        }

        return allVectors;
    }

    /**
     * Get or create LLM instance (with cache)
     *
     * @param cfg LLM configuration
     * @return LLM instance
     */
    private RagLLM getOrCreateLLM(LLMCfg cfg) {
        String cacheKey = generateLLMCacheKey(cfg);

        CachedLLM cachedLLM = llmCache.get(cacheKey);
        if (cachedLLM != null && !cachedLLM.isExpired()) {
            return cachedLLM.getLlm();
        }

        // Create new LLM instance
        RagLLM newLLM = new RagLLM(cfg);
        llmCache.put(cacheKey, new CachedLLM(newLLM));

        // Clean expired cache
        cleanExpiredCache();

        return newLLM;
    }

    /**
     * Generate LLM cache key
     *
     * @param cfg LLM configuration
     * @return cache key
     */
    private String generateLLMCacheKey(LLMCfg cfg) {
        return String.format("%s_%s_%s", cfg.model(), cfg.baseUrl(), cfg.apiKey().hashCode());
    }

    /**
     * Clean expired LLM cache
     */
    private void cleanExpiredCache() {
        llmCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Execute vector query
     */
    private List<VdbDO> performVectorQuery(float[] vector, int topK, String spaceName, float threshold) {
        List<VdbQueryFilterDO.queryVector> vectors = Collections.singletonList(
                new VdbQueryFilterDO.queryVector("vector", vector, threshold)
        );

        VdbQueryFilterDO queryFilter = VdbQueryFilterDO.builder()
                .limit(topK)
                .build();

        return baseVDB.searchByVector(dbName, spaceName, vectors, queryFilter);
    }

    /**
     * Batch write to vector database
     */
    private List<String> batchUpsert(List<VdbDO> vdbDOList, String spaceName) {
        return baseVDB.batchUpsert(dbName, spaceName, vdbDOList);
    }

    /**
     * Create default Embedding configuration
     */
    private LLMCfg createDefaultEmbeddingConfig() {
        return new LLMCfg(
                RagConfig.DEFAULT_EMBEDDING_MODEL,
                RagConfig.DEFAULT_API_KEY,
                RagConfig.DEFAULT_BASE_URL,
                null
        );
    }

    /**
     * Create default Reranker configuration
     */
    private LLMCfg createDefaultRerankerConfig() {
        return new LLMCfg(RagConfig.DEFAULT_RERANKER_MODEL, RagConfig.DEFAULT_API_KEY, RagConfig.DEFAULT_BASE_URL,
                null
        );
    }
}