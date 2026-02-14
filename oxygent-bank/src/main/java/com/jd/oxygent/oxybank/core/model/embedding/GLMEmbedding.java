package com.jd.oxygent.oxybank.core.model.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Zhipu GLM Embedding for LlamaIndex.
 * 
 * Supports GLM embedding models:
 * - embedding-2: 1024 dimensions
 * 
 */
@Slf4j
@Data
public class GLMEmbedding {
    
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.getObjectMapper();
    
    private String modelName;
    private String apiKey;
    private String apiBase = "https://open.bigmodel.cn/api/paas/v4/embeddings";
    private int embedBatchSize = 10;
    private int maxRetries = 3;
    private float timeout = 60.0f;
    private boolean normalization = true;
    
    /**
     * Constructor
     * 
     * @param modelName GLM model name (e.g., embedding-2, embedding-3)
     * @param embedBatchSize Batch size for embedding requests
     * @param maxRetries Maximum number of retries
     * @param timeout Timeout for API requests in seconds
     * @param normalization Whether to normalize embeddings
     * @param apiKey Zhipu API key
     * @param apiBase API base URL
     */
    public GLMEmbedding(
            String modelName,
            int embedBatchSize,
            int maxRetries,
            float timeout,
            boolean normalization,
            String apiKey,
            String apiBase) {
        this.modelName = modelName != null ? modelName : "embedding-2";
        this.embedBatchSize = embedBatchSize > 0 && embedBatchSize <= 2048 ? embedBatchSize : 10;
        this.maxRetries = maxRetries >= 0 ? maxRetries : 3;
        this.timeout = timeout >= 0 ? timeout : 60.0f;
        this.normalization = normalization;
        this.apiKey = apiKey != null ? apiKey : "";
        this.apiBase = apiBase != null ? apiBase : "https://open.bigmodel.cn/api/paas/v4/embeddings";
    }
    
    /**
     * Constructor with default parameters
     */
    public GLMEmbedding() {
        this("embedding-2", 10, 3, 60.0f, true, "", "https://open.bigmodel.cn/api/paas/v4/embeddings");
    }
    
    /**
     * Embed input query synchronously.
     * 
     * @param query Query text
     * @return Embedding result as a list of floats
     */
    public List<Double> getQueryEmbedding(String query) {
        return getTextEmbedding(query);
    }
    
    /**
     * Embed input text synchronously.
     * 
     * @param text Text to embed
     * @return Embedding result as a list of floats
     */
    public List<Double> getTextEmbedding(String text) {
        List<List<Double>> embeddings = getTextEmbeddings(List.of(text));
        return embeddings.isEmpty() ? new ArrayList<>() : embeddings.get(0);
    }
    
    /**
     * Embed input sequence of text synchronously.
     * 
     * Supports batch processing for efficiency.
     * 
     * @param texts List of texts to embed
     * @return List of embeddings
     */
    public List<List<Double>> getTextEmbeddings(List<String> texts) {
        List<List<Double>> allEmbeddings = new ArrayList<>();
        
        for (int i = 0; i < texts.size(); i += embedBatchSize) {
            int endIndex = Math.min(i + embedBatchSize, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            List<List<Double>> batchEmbeddings = requestEmbedding(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }
        
        return allEmbeddings;
    }
    
    /**
     * Request GLM API for embeddings with retry logic.
     * 
     * @param texts List of texts to embed
     * @return List of embeddings (each embedding is a list of floats)
     * @throws RuntimeException If API request fails after all retries
     */
    private List<List<Double>> requestEmbedding(List<String> texts) {
        Map<String, Object> payload = Map.of(
            "model", modelName,
            "input", texts
        );
        
        Map<String, String> headers = Map.of(
            "Authorization", "Bearer " + apiKey,
            "Content-Type", "application/json"
        );
        
        String lastError = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String response = sendPostRequest(apiBase, payload, headers, (int) timeout);
                
                // Parse response
                Map<String, Object> result = OBJECT_MAPPER.readValue(response, Map.class);
                
                if (!result.containsKey("data")) {
                    Map<String, Object> error = (Map<String, Object>) result.getOrDefault("error", Map.of());
                    String errorMsg = (String) error.getOrDefault("message", "Unknown error");
                    lastError = "GLM API error: " + errorMsg;
                    continue;
                }
                
                List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                List<List<Double>> embeddings = new ArrayList<>();
                
                for (Map<String, Object> item : data) {
                    List<Double> embedding = (List<Double>) item.get("embedding");
                    embeddings.add(embedding);
                }
                
                return embeddings;
                
            } catch (IOException e) {
                lastError = "Request error: " + e.getMessage();
                log.warn("Attempt {} failed: {}", attempt + 1, lastError);
                
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Request interrupted", ie);
                    }
                }
            }
        }
        
        // If all retries failed
        throw new RuntimeException(
            "GLM API request failed after " + maxRetries + " attempts. Last error: " + lastError
        );
    }
    
    /**
     * Send POST request to API
     * 
     * @param urlString API URL
     * @param payload Request payload
     * @param headers Request headers
     * @param timeout Timeout in seconds
     * @return Response body as string
     * @throws IOException If an I/O error occurs
     */
    private String sendPostRequest(
            String urlString,
            Map<String, Object> payload,
            Map<String, String> headers,
            int timeout) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeout * 1000);
        conn.setReadTimeout(timeout * 1000);
        conn.setDoOutput(true);
        
        // Set headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        
        // Write payload
        String jsonPayload = OBJECT_MAPPER.writeValueAsString(payload);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Get response
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // Read error response
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new IOException(
                    "GLM API error: " + responseCode + " - " + response.toString()
                );
            }
        }
        
        // Read successful response
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
    
    /**
     * Return the class name.
     * 
     * @return Class name
     */
    public static String getClassName() {
        return "GLMEmbedding";
    }
}