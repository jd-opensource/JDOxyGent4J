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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.model.LLMCfg;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.rag.chunker.Tokenizer;
import com.jd.oxygent.core.oxygent.schemas.exception.OxyException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h3>RAG Large Language Model Client</h3>
 *
 * <p>This class is the core LLM client component in the RAG (Retrieval-Augmented Generation) system, providing capabilities for Embedding and Reranker.
 * Communicates with remote LLM services via standardized HTTP interfaces, supporting batch processing and high-performance text vectorization operations.</p>
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Text Embedding</strong>: Converts text into high-dimensional semantic vectors</li>
 *   <li><strong>Document Reranking</strong>: Sorts document lists by relevance based on queries</li>
 *   <li><strong>Batch Processing</strong>: Supports efficient batch text vectorization</li>
 *   <li><strong>Error Handling</strong>: Comprehensive exception handling and retry mechanisms</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Vectorized indexing of knowledge base documents</li>
 *   <li>Semantic search and similarity calculation</li>
 *   <li>Document retrieval and reranking in RAG systems</li>
 *   <li>Semantic understanding of multimodal content</li>
 * </ul>
 *
 * <h3>Performance Features:</h3>
 * <ul>
 *   <li>Supports custom vector dimensions</li>
 *   <li>Batch processing optimizes network requests</li>
 *   <li>Comprehensive token usage statistics</li>
 *   <li>Detailed performance logging</li>
 * </ul>
 * <p>
 * // Single text embedding
 * float[] vector = ragLLM.embedding("query text", 1024);
 * <p>
 * // Document reranking
 * List<String> rankedDocs = ragLLM.reranker(query, documents, 10);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Slf4j
public class RagLLM {

    /**
     * LLM configuration information, including model name, API key, base URL, etc.
     */
    private LLMCfg llmCfg;

    /**
     * Constructor - Create with basic parameters
     *
     * @param baseUrl Base URL address of LLM service
     * @param apiKey  API access key
     * @param model   Model name
     */
    public RagLLM(String baseUrl, String apiKey, String model) {
        this.llmCfg = new LLMCfg(model, apiKey, baseUrl, new HashMap<>());
    }

    /**
     * Constructor - Create with configuration object
     *
     * @param llmCfg LLM configuration object
     */
    public RagLLM(LLMCfg llmCfg) {
        this.llmCfg = llmCfg;
    }

    /**
     * JSON serialization tool for request body serialization
     */
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * HTTP client for network communication with LLM service
     */
    private OkHttpClient httpClient = new OkHttpClient();

    /**
     * JSON media type constant for HTTP request headers
     */
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /* -------------------- Public API -------------------- */

    /**
     * Convenience method for single text embedding
     * <p>Performs vectorization for a single text by internally calling the batch interface</p>
     *
     * @param input      The text content to be embedded
     * @param dimensions Vector dimension, uses model default if null
     * @return The vector array corresponding to the text
     * @throws OxyException If embedding fails or result is empty
     */
    public float[] embedding(String input, Integer dimensions) {
        List<float[]> batch = embedding(Collections.singletonList(input), dimensions);
        if (batch.isEmpty()) {                 // Fallback, theoretically should not happen
            throw new OxyException("embedding result is empty");
        }
        return batch.get(0);
    }

    /**
     * Get embedding vectors for the specified inputs.
     *
     * @param inputs     List of input strings
     * @param dimensions Embedding vector dimension (optional)
     * @return Corresponding embedding vector for each input
     */
    public List<float[]> embedding(List<String> inputs, Integer dimensions) {
        validate(inputs, dimensions);

        Map<String, Object> body = new HashMap<>();
        body.put("model", llmCfg.model());
        body.put("input", inputs);
        if (dimensions != null && dimensions > 0) {
            body.put("dimensions", dimensions);
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new OxyException("Failed to serialize request body", e);
        }

        Request request = new Request.Builder()
                .url(normalizeBaseUrl(llmCfg.baseUrl()) + "/embeddings")
                .addHeader("Authorization", "Bearer " + llmCfg.apiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload, JSON))
                .build();

        log.info("[embedding] request body: {}", Tokenizer.cut(body.toString(), 200));

        try (Response resp = httpClient.newCall(request).execute()) {
            String respStr = resp.body() == null ? null : resp.body().string();
            if (!resp.isSuccessful()) {
                throw new OxyException("Embedding HTTP " + resp.code() + " : " + respStr);
            }
            if (respStr == null || respStr.isBlank()) {
                throw new OxyException("Response body is empty");
            }

            JsonNode root = objectMapper.readTree(respStr);

            /* ---- usage ---- */
            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(0);

            /* ---- data[].embedding ---- */
            JsonNode dataArray = root.path("data");
            if (!dataArray.isArray() || dataArray.size() != inputs.size()) {
                log.warn("[embedding] Returned count {} does not match input count {}", dataArray.size(), inputs.size());
            }

            List<float[]> embeddings = new ArrayList<>(dataArray.size());
            for (int i = 0; i < dataArray.size(); i++) {
                JsonNode embNode = dataArray.get(i).path("embedding");
                if (!embNode.isArray()) {
                    throw new OxyException("data[" + i + "].embedding is not an array");
                }
                float[] vec = new float[embNode.size()];
                for (int j = 0; j < embNode.size(); j++) {
                    vec[j] = (float) embNode.get(j).asDouble();
                }
                if (dimensions != null && dimensions > 0 && vec.length != dimensions) {
                    log.warn("[embedding] Vector dimension of item {} ({}) does not match requested dimension {}", i, vec.length, dimensions);
                }
                embeddings.add(vec);
            }

            Map<String, Object> extra = new HashMap<>();
            extra.put("model", llmCfg.model());
            extra.put("prompt_tokens", promptTokens);
            extra.put("total_tokens", totalTokens);
            extra.put("count", embeddings.size());

            log.info("[embedding] Successfully returned {} vectors, total token={}", embeddings.size(), totalTokens);

            return embeddings;


        } catch (IOException e) {
            throw new OxyException("Embedding call failed", e);
        }
    }

    /**
     * Rerank the given document list using the LLM model and return the top K most relevant documents.
     *
     * @param query     Query statement
     * @param documents Document list to be reranked
     * @param topK      Top K documents to return
     * @return Reranked document list
     */
    public List<String> reranker(String query, List<String> documents, Integer topK) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", llmCfg.model());
        body.put("query", query);
        body.put("documents", documents);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new OxyException("Failed to serialize request body", e);
        }

        Request request = new Request.Builder()
                .url(normalizeBaseUrl(llmCfg.baseUrl()) + "/reranker")
                .addHeader("Authorization", "Bearer " + llmCfg.apiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload, JSON))
                .build();

        log.info("[embedding] request body: {}", Tokenizer.cut(body.toString(), 200));

        try (Response resp = httpClient.newCall(request).execute()) {
            String respStr = resp.body() == null ? null : resp.body().string();
            if (!resp.isSuccessful()) {
                throw new OxyException("Embedding HTTP " + resp.code() + " : " + respStr);
            }
            if (respStr == null || respStr.isBlank()) {
                throw new OxyException("Response body is empty");
            }

            JsonNode root = objectMapper.readTree(respStr);
            /* ---- usage ---- */
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);
            /* ---- results array ---- */
            JsonNode results = root.path("results");
            if (!results.isArray() || results.size() != documents.size()) {
                throw new OxyException("Returned count does not match input count");
            }
            /* Temporary object: index -> score */
            Map<Integer, Double> idx2score = new HashMap<>();
            for (JsonNode item : results) {
                int idx = item.path("index").asInt(-1);
                double score = item.path("relevance_score").asDouble(Double.NaN);
                if (idx < 0 || Double.isNaN(score)) {
                    throw new OxyException("results field missing or format error");
                }
                idx2score.put(idx, score);
            }
            /* Sort by score descending */
            List<Integer> sortedIdx = idx2score.entrySet()
                    .stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            /* Map back to original documents */
            List<String> rerankedDocs = sortedIdx.stream()
                    .map(documents::get)
                    .limit(topK)
                    .collect(Collectors.toList());
            log.info("[reranker] Successfully reranked {} documents, total token={}", rerankedDocs.size(), totalTokens);
            return rerankedDocs;


        } catch (IOException e) {
            throw new OxyException("Embedding call failed", e);
        }
    }

    /* -------------------- Private utilities -------------------- */

    private void validate(List<String> inputs, Integer dimensions) {
        if (llmCfg.baseUrl() == null || llmCfg.baseUrl().isBlank()) {
            throw new OxyException("baseUrl cannot be empty");
        }
        if (llmCfg.apiKey() == null || llmCfg.apiKey().isBlank()) {
            throw new OxyException("apiKey cannot be empty");
        }
        if (llmCfg.model() == null || llmCfg.model().isBlank()) {
            throw new OxyException("model cannot be empty");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new OxyException("inputs cannot be empty");
        }
        if (inputs.stream().anyMatch(Objects::isNull)) {
            throw new OxyException("inputs cannot contain null");
        }
    }

    private String normalizeBaseUrl(String url) {
        return url == null ? "" : (url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
    }

    private static final String TEMPLATE = """
            In the era of rapid digital economy development, artificial intelligence technology is penetrating into all industries at an unprecedented speed.
            From intelligent manufacturing to smart finance, from precision medicine to autonomous driving, the collaborative evolution of algorithms and computing power continues to refresh the upper limit of production efficiency.
            Data, as a new type of production factor, has its potential value continuously mined through deep learning models, thereby reshaping every link in the traditional industrial chain.
            At the same time, the maturity of emerging technologies such as privacy computing and federated learning provides a secure and compliant bridge for cross-institutional and cross-regional data circulation.
            In the next decade, artificial intelligence will no longer be limited to single-point applications, but will build a comprehensive intelligent operating system that becomes the underlying engine driving social progress.
            In such a grand narrative, computing networks, green energy, open source communities, and standard setting jointly constitute the four-dimensional fulcrum of technological evolution.
            Only by allowing innovation to iterate in a transparent environment can algorithms be made more fair, data more secure, and intelligence more inclusive.
            """.replace("\n", "");
    /**
     * Random character pool for inserting noise
     */
    private static final String NOISE = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ，。！？；：、“”‘’（）";

    /**
     * Generate a list of texts with specified count and token range
     *
     * @param size   How many items needed
     * @param minLen Minimum character count per item
     * @param maxLen Maximum character count per item
     */
    public static List<String> generate(int size, int minLen, int maxLen) {
        List<String> result = new ArrayList<>(size);
        Random rnd = new Random();
        for (int i = 0; i < size; i++) {
            int len = minLen + rnd.nextInt(maxLen - minLen + 1);
            StringBuilder sb = new StringBuilder(len);
            while (sb.length() < len) {
                // Random starting point to extract template
                int start = rnd.nextInt(Math.max(1, TEMPLATE.length() - 30));
                int end = Math.min(TEMPLATE.length(), start + 30 + rnd.nextInt(50));
                sb.append(TEMPLATE, start, end);
                // Randomly insert small amount of noise
                if (rnd.nextDouble() < 0.1) {
                    sb.append(NOISE.charAt(rnd.nextInt(NOISE.length())));
                }
            }
            // Finally truncate to target length
            result.add(sb.substring(0, len));
        }
        Collections.shuffle(result);
        return result;
    }
}
