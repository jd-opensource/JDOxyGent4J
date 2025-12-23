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
package com.jd.oxygent.core.oxygent.oxy.llms;

import com.fasterxml.jackson.databind.JsonNode;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP-based LLM implementation for remote language model APIs.
 * <p>
 * This class provides a concrete implementation of RemoteLLM for communicating
 * with remote LLM APIs over HTTP. It handles API authentication, request
 * formatting, and response parsing for OpenAI-compatible APIs.
 * <p>
 * Supports:
 * - OpenAI-compatible APIs
 * - Ollama APIs
 * - Google Gemini APIs
 * - Streaming responses
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class HttpLlm extends RemoteLlm {
    private static final Logger logger = LoggerFactory.getLogger(HttpLlm.class);

    private static volatile HttpClient httpClient = null;
    @Builder.Default
    private String streamOutputType = "stream";

    public HttpLlm(String baseUrl, String apiKey, String modelName, Duration timeout, Map<String, Object> llmParams, Map<String, String> headers) {
        super(baseUrl, apiKey, modelName, timeout, llmParams, headers);
        setSaveData(true);
    }

    /**
     * Get singleton HTTP client with proper timeout configuration
     */
    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (HttpLlm.class) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(30))
                            .build();
                }
            }
        }
        return httpClient;
    }

    protected Map<String, String> headers(OxyRequest oxyRequest) {
        HashMap<String, String> resultHeadMap = new HashMap<>();
        if (MapUtils.isNotEmpty(this.headers)) {
            resultHeadMap.putAll(this.headers);
        }
        return resultHeadMap;
    }

    /**
     * Execute an HTTP request to the remote LLM API.
     * <p>
     * Sends a formatted request to the remote LLM API and processes the response.
     * The method handles authentication, payload construction, and response parsing
     * for OpenAI-compatible APIs, Ollama APIs, and Google Gemini APIs.
     * Supports both streaming and non-streaming responses.
     *
     * @param oxyRequest The request object containing messages and parameters.
     * @return OxyResponse The response containing the LLM's output with COMPLETED state.
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        try {
            String url = buildUrl();
            boolean isGemini = url.contains("generativelanguage.googleapis.com");
            boolean useOpenai = (apiKey != null && !apiKey.isEmpty()) && !isGemini;

            Map<String, String> requestHeaders = buildHeaders(oxyRequest, isGemini, useOpenai);
            Map<String, Object> payload = buildPayload(oxyRequest, isGemini, useOpenai);

            // Check if streaming is requested
            boolean stream = Boolean.TRUE.equals(payload.get("stream"));

            if (stream && (useOpenai || !isGemini)) {
                return executeStreamingRequest(url, requestHeaders, payload, oxyRequest, useOpenai);
            } else {
                return executeNonStreamingRequest(url, requestHeaders, payload, isGemini, useOpenai);
            }

        } catch (Exception e) {
            logger.error("LLM request exception", e);
            OxyResponse oxyResponse = new OxyResponse();
            oxyResponse.setOutput("");
            return oxyResponse;
        }
    }

    /**
     * Build the API URL based on the provider type
     */
    private String buildUrl() {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        boolean isGemini = url.contains("generativelanguage.googleapis.com");
        boolean useOpenai = (apiKey != null && !apiKey.isEmpty()) && !isGemini;

        if (isGemini) {
            if (!url.endsWith(":generateContent")) {
                url = url + "/models/" + modelName + ":generateContent";
            }
        } else if (useOpenai) {
            if (!url.endsWith("/chat/completions")) {
                url = url + "/chat/completions";
            }
        } else {
            if (!url.endsWith("/api/chat")) {
                url = url + "/api/chat";
            }
        }
        return url;
    }

    /**
     * Build request headers based on the provider type
     */
    private Map<String, String> buildHeaders(OxyRequest oxyRequest, boolean isGemini, boolean useOpenai) {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json");

        if (isGemini) {
            requestHeaders.put("X-goog-api-key", apiKey);
        } else if (useOpenai) {
            requestHeaders.put("Authorization", "Bearer " + apiKey);
        }

        requestHeaders.putAll(headers(oxyRequest));

        return requestHeaders;
    }

    /**
     * Build request payload based on the provider type
     */
    private Map<String, Object> buildPayload(OxyRequest oxyRequest, boolean isGemini, boolean useOpenai) throws Exception {
        Map<String, Object> payload = new HashMap<>();

        if (isGemini) {
            List<Map<String, Object>> rawMsgs = this._getMessages(oxyRequest);
            List<Map<String, Object>> contents = new ArrayList<>();

            for (Map<String, Object> msg : rawMsgs) {
                if (msg.get("content") != null) {
                    Map<String, Object> content = new HashMap<>();
                    content.put("role", "user".equals(msg.get("role")) ? "user" : "model");
                    content.put("parts", List.of(Map.of("text", msg.get("content"))));
                    contents.add(content);
                }
            }

            payload.put("contents", contents);
            payload.putAll(llmParams);

            if (oxyRequest.getArguments() != null) {
                for (Map.Entry<String, Object> entry : oxyRequest.getArguments().entrySet()) {
                    if (!"messages".equals(entry.getKey())) {
                        payload.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } else {
            payload.put("messages", this._getMessages(oxyRequest));
            payload.put("model", modelName);
            payload.put("stream", false);
            if (llmParams != null) {
                payload.putAll(llmParams);
            }

            if (oxyRequest.getArguments() != null) {
                for (Map.Entry<String, Object> entry : oxyRequest.getArguments().entrySet()) {
                    if (!"messages".equals(entry.getKey())) {
                        payload.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return payload;
    }

    /**
     * Execute streaming request
     */
    public OxyResponse executeStreamingRequest(String url, Map<String, String> requestHeaders,
                                               Map<String, Object> payload, OxyRequest oxyRequest, boolean useOpenai) throws Exception {
        payload.put("stream", true);
        String jsonBody = JsonUtils.writeValueAsString(payload);
        logger.debug("HttpLlm executeStreamingRequest request:{}", jsonBody);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds((long) this.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        requestHeaders.forEach(requestBuilder::header);
        HttpRequest request = requestBuilder.build();

        HttpResponse<java.io.InputStream> response = getHttpClient().send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = "";
            try (java.io.InputStream errorStream = response.body();
                 java.io.BufferedReader errorReader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream))) {
                errorBody = errorReader.lines().collect(java.util.stream.Collectors.joining("\n"));
            } catch (Exception e) {
                logger.warn("Failed to read error response body", e);
            }
            throw new RuntimeException("HTTP request failed, status code: " + response.statusCode() + ", response: " + errorBody);
        }

        StringBuilder result = new StringBuilder();
        boolean isGemini = url.contains("generativelanguage.googleapis.com");

        try (java.io.InputStream inputStream = response.body();
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6).trim();

                    if (jsonData.isEmpty() || "[DONE]".equals(jsonData)) {
                        continue;
                    }

                    try {
                        JsonNode node = JsonUtils.readTree(jsonData);
                        String content = extractStreamContent(node, isGemini, useOpenai);
                        if (content != null && !content.isEmpty()) {
                            result.append(content);

                            Map<String, Object> streamMessage = new HashMap<>();
                            streamMessage.put("type", this.getStreamOutputType());
                            Map<String, Object> contentMap = new HashMap<>();
                            contentMap.put("delta", content);
                            contentMap.put("agent", oxyRequest.getCaller());
                            contentMap.put("node_id", oxyRequest.getNodeId());
                            contentMap.put("current_trace_id", oxyRequest.getCurrentTraceId());
                            streamMessage.put("content", contentMap);
                            streamMessage.put("_is_stored", false);

                            oxyRequest.sendMessage(streamMessage);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse streaming JSON: {}, error: {}", jsonData, e.getMessage());
                    }
                } else if (line.startsWith("event:") || line.startsWith("id:") || line.trim().isEmpty()) {
                    continue;
                } else {
                    try {
                        if (line.trim().startsWith("{")) {
                            JsonNode node = JsonUtils.readTree(line);
                            String content = extractStreamContent(node, isGemini, useOpenai);
                            if (content != null && !content.isEmpty()) {
                                result.append(content);

                                Map<String, Object> streamMessage = new HashMap<>();
                                streamMessage.put("type", "stream");
                                Map<String, Object> contentMap = new HashMap<>();
                                contentMap.put("delta", content);
                                contentMap.put("agent", oxyRequest.getCaller());
                                contentMap.put("node_id", oxyRequest.getNodeId());
                                contentMap.put("current_trace_id", oxyRequest.getCurrentTraceId());
                                streamMessage.put("content", contentMap);
                                streamMessage.put("_is_stored", false);

                                oxyRequest.sendMessage(streamMessage);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Skipping non-JSON line: {}", line);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading streaming response", e);
            throw new RuntimeException("Failed to read streaming response", e);
        }

        OxyResponse oxyResponse = new OxyResponse();
        oxyResponse.setOutput(result.toString());
        return oxyResponse;
    }

    /**
     * Execute non-streaming request
     */
    public OxyResponse executeNonStreamingRequest(String url, Map<String, String> requestHeaders,
                                                  Map<String, Object> payload, boolean isGemini, boolean useOpenai) throws Exception {
        payload.put("stream", false);
        String jsonBody = JsonUtils.writeValueAsString(payload);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds((long) this.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        requestHeaders.forEach(requestBuilder::header);
        HttpRequest request = requestBuilder.build();

        try {
            long timer = System.currentTimeMillis();
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("HttpLlm executeNonStreamingRequest cost:{}ms request:{}", System.currentTimeMillis() - timer, jsonBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMessage = String.format("HTTP request failed, status code: %d, URL: %s, response: %s",
                        response.statusCode(), url, response.body());
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("Received empty response body");
            }

            Map<String, Object> data;
            try {
                data = JsonUtils.readValue(responseBody, Map.class);
            } catch (Exception e) {
                logger.error("Failed to parse response JSON: {}", responseBody);
                throw new RuntimeException("Response is not valid JSON format: " + e.getMessage());
            }

            if (data.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) data.get("error");
                String errorMsg = error.getOrDefault("message", "Unknown error").toString();
                logger.error("LLM API error: {}", errorMsg);
                throw new RuntimeException("LLM API error: " + errorMsg);
            }

            String result = extractNonStreamContent(data, isGemini, useOpenai);

            if (result == null || result.trim().isEmpty()) {
                logger.warn("Content extracted from response is empty, original response: {}", responseBody);
            }

            OxyResponse oxyResponse = new OxyResponse();
            oxyResponse.setOutput(result != null ? result : "");
            return oxyResponse;

        } catch (IOException e) {
            logger.error("Network request failed: {}", e.getMessage());
            throw new RuntimeException("Network request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request was interrupted", e);
        }
    }

    /**
     * Extract content from streaming response
     */
    private String extractStreamContent(JsonNode node, boolean isGemini, boolean useOpenai) {
        try {
            if (isGemini) {
                JsonNode candidates = node.get("candidates");
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && parts.size() > 0) {
                            JsonNode text = parts.get(0).get("text");
                            return text != null ? text.asText() : null;
                        }
                    }
                }
            } else if (useOpenai) {
                JsonNode choices = node.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode delta = choices.get(0).get("delta");
                    if (delta != null) {
                        JsonNode content = delta.get("content");
                        return content != null ? content.asText() : null;
                    }
                }
            } else {
                JsonNode message = node.get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    return content != null ? content.asText() : null;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract stream content", e);
        }
        return null;
    }

    /**
     * Extract content from non-streaming response
     */
    private String extractNonStreamContent(Map<String, Object> data, boolean isGemini, boolean useOpenai) {
        try {
            if (isGemini) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return parts.get(0).getOrDefault("text", "").toString();
                        }
                    }
                }
            } else if (useOpenai) {
                Object choices = data.get("choices");
                if (choices instanceof List && !((List<?>) choices).isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) ((Map<String, Object>) ((List<?>) choices).get(0)).get("message");
                    if (message != null) {
                        Object content = message.get("content");
                        return content != null ? content.toString() : message.getOrDefault("reasoning_content", "").toString();
                    }
                }
            } else {
                Map<String, Object> message = (Map<String, Object>) data.get("message");
                if (message != null) {
                    return message.getOrDefault("content", "").toString();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract non-stream content", e);
        }
        return null;
    }
}
