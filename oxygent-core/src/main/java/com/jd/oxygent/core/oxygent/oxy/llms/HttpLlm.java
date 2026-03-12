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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jd.oxygent.core.oxygent.logging.AiLogger;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.SpringContextHolder;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
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
import java.util.function.Function;

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

    /** Http Client Version */
    private HttpClient.Version httpVersion = null;
    private static volatile HttpClient httpClient = null;
    @Builder.Default
    private String streamOutputType = "stream";

    @JsonIgnore
    private Function<Exception, OxyResponse> funcProcessLlmException;

    public HttpLlm(String baseUrl, String apiKey, String modelName, Duration timeout, Map<String, Object> llmParams, Map<String, String> headers) {
        super(baseUrl, apiKey, modelName, timeout, llmParams, headers, null);
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
        long timer = System.currentTimeMillis();
        aiLogger = SpringContextHolder.getBean(AiLogger.class);
        try {
            String url = buildUrl();
            boolean isGemini = url.contains("generativelanguage.googleapis.com");
            boolean useOpenai = (apiKey != null && !apiKey.isEmpty()) && !isGemini;

            Map<String, String> requestHeaders = buildHeaders(oxyRequest, isGemini, useOpenai);
            Map<String, Object> payload = buildPayload(oxyRequest, isGemini, useOpenai);

            // Check if streaming is requested
            boolean stream = Boolean.TRUE.equals(payload.get("stream"));
            OxyResponse oxyResponse;
            if (stream && (useOpenai || !isGemini)) {
                oxyResponse = executeStreamingRequest(url, requestHeaders, payload, oxyRequest, useOpenai);
            } else {
                oxyResponse = executeNonStreamingRequest(url, requestHeaders, payload, isGemini, useOpenai, oxyRequest);
            }
            if (aiLogger != null && aiLogger.isEnabled()) {
                aiLogger.log("llm", oxyResponse, this, Map.of("elapsedMillis", String.valueOf(System.currentTimeMillis() - timer)));
            }
            return oxyResponse;
        } catch (Exception e) {
            if (funcProcessLlmException != null) {
                return funcProcessLlmException.apply(e);
            } else {
                logger.error("LLM request exception", e);
                OxyResponse oxyResponse = OxyResponse.builder().state(OxyState.FAILED).output(e.getMessage()).oxyRequest(oxyRequest).build();
                if (aiLogger != null && aiLogger.isEnabled()) {
                    aiLogger.log("llm", oxyResponse, this, Map.of("error", e.getMessage(), "elapsedMillis", String.valueOf(System.currentTimeMillis() - timer)));
                }
                return oxyResponse;
            }
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
            payload.put("stream", true);
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
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds((long) this.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (this.httpVersion != null) {
            requestBuilder.version(this.httpVersion);
        }
        requestHeaders.forEach(requestBuilder::header);
        HttpRequest request = requestBuilder.build();
        long timer = System.currentTimeMillis();
        HttpResponse<java.io.InputStream> response = getHttpClient().send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        logger.debug("HttpLlm executeNonStreamingRequest cost:{}ms request:{}", System.currentTimeMillis() - timer, jsonBody);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = "";
            try (java.io.InputStream errorStream = response.body();
                 java.io.BufferedReader errorReader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream))) {
                errorBody = errorReader.lines().collect(java.util.stream.Collectors.joining("\n"));
            } catch (Exception e) {
                logger.warn("Failed to read error response body", e);
            }
            if (response.statusCode() == 429) { // rate limit
                // get Retry-After header
                long retryAfter = response.headers().firstValueAsLong("retry-after").orElse(0L);
                String output = retryAfter > 0 ? " retry-after:" + retryAfter + " ```json" + errorBody + "```" : errorBody;
                logger.error(output);
                return OxyResponse.builder()
                        .state(OxyState.RATE_LIMIT_EXCEEDED)
                        .output(output)
                        .oxyRequest(oxyRequest)
                        .build();
            }
            throw new RuntimeException("HTTP request failed, status code: " + response.statusCode() + ", response: " + errorBody);
        }

        StringBuilder result = new StringBuilder();
        String usage = null;
        boolean isGemini = url.contains("generativelanguage.googleapis.com");

        try (java.io.InputStream inputStream = response.body();
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String jsonData = line.substring(6).trim();

                    if (jsonData.isEmpty() || "[DONE]".equals(jsonData)) {
                        continue;
                    }

                    try {
                        JsonNode node = JsonUtils.readTree(jsonData);
                        String[] contentAndUsage = extractStreamContent(node, isGemini, useOpenai);
                        if (contentAndUsage != null && StringUtils.isNotBlank(contentAndUsage[0])) {
                            result.append(contentAndUsage[0]);

                            Map<String, Object> streamMessage = new HashMap<>();
                            streamMessage.put("type", this.getStreamOutputType());
                            Map<String, Object> contentMap = new HashMap<>();
                            contentMap.put("delta", contentAndUsage[0]);
                            contentMap.put("agent", oxyRequest.getCaller());
                            contentMap.put("node_id", oxyRequest.getNodeId());
                            streamMessage.put("content", contentMap);

                            oxyRequest.sendMessage(streamMessage);
                        }
                        if (contentAndUsage != null && StringUtils.isNotBlank(contentAndUsage[1])) {
                            usage = contentAndUsage[1];
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
                            String[] contentAndUsage = extractStreamContent(node, isGemini, useOpenai);
                            if (contentAndUsage != null && StringUtils.isNotBlank(contentAndUsage[0])) {
                                result.append(contentAndUsage[0]);

                                Map<String, Object> streamMessage = new HashMap<>();
                                streamMessage.put("type", "stream");
                                Map<String, Object> contentMap = new HashMap<>();
                                contentMap.put("delta", contentAndUsage[0]);
                                contentMap.put("agent", oxyRequest.getCaller());
                                contentMap.put("node_id", oxyRequest.getNodeId());
                                streamMessage.put("content", contentMap);
                                oxyRequest.sendMessage(streamMessage);
                            }
                            if (contentAndUsage != null && StringUtils.isNotBlank(contentAndUsage[1])) {
                                usage = contentAndUsage[1];
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

        Map<String, Object> streamMessage = new HashMap<>();
        streamMessage.put("type", "stream_end");
        streamMessage.put("delta", "");
        streamMessage.put("agent", oxyRequest.getCaller());
        streamMessage.put("node_id", oxyRequest.getNodeId());
        oxyRequest.sendMessage(streamMessage);

        return OxyResponse.builder()
                .state(OxyState.COMPLETED)
                .output(result.toString())
                .extra(usage != null ? new HashMap<>(Map.of("usage", usage)): null)
                .oxyRequest(oxyRequest)
                .build();
    }

    /**
     * Execute non-streaming request
     */
    public OxyResponse executeNonStreamingRequest(String url, Map<String, String> requestHeaders,
                                                  Map<String, Object> payload, boolean isGemini, boolean useOpenai, OxyRequest oxyRequest) throws Exception {
        payload.put("stream", false);
        String jsonBody = JsonUtils.writeValueAsString(payload);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds((long) this.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (this.httpVersion != null) {
            requestBuilder.version(this.httpVersion);
        }
        requestHeaders.forEach(requestBuilder::header);
        HttpRequest request = requestBuilder.build();

        try {
            long timer = System.currentTimeMillis();
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("HttpLlm executeNonStreamingRequest cost:{}ms request:{}", System.currentTimeMillis() - timer, jsonBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() == 429) { // rate limit
                    // get Retry-After header
                    long retryAfter = response.headers().firstValueAsLong("retry-after").orElse(0L);
                    String output = retryAfter > 0 ? " retry-after:" + retryAfter + " ```json" + response.body() + "```" : response.body();
                    logger.error(output);
                    return OxyResponse.builder()
                            .state(OxyState.RATE_LIMIT_EXCEEDED)
                            .output(output)
                            .oxyRequest(oxyRequest)
                            .build();
                }
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
                throw new RuntimeException("Response is not valid JSON format: " + e.getMessage(), e);
            }

            if (data.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) data.get("error");
                String errorMsg = error.getOrDefault("message", "Unknown error").toString();
                logger.error("LLM API error: {}", errorMsg);
                throw new RuntimeException("LLM API error: " + errorMsg);
            }

            String[] resultAndUsage = extractNonStreamContent(data, isGemini, useOpenai);

            if (resultAndUsage == null || (StringUtils.isBlank(resultAndUsage[0]) && StringUtils.isBlank(resultAndUsage[1]))) {
                logger.warn("Content extracted from response is empty, original response: {}", responseBody);
            }
            return OxyResponse
                    .builder()
                    .output(resultAndUsage[0] != null ? resultAndUsage[0] : "")
                    .extra(resultAndUsage[1] != null ? new HashMap<>(Map.of("usage", resultAndUsage[1])) : null)
                    .state(OxyState.COMPLETED)
                    .oxyRequest(oxyRequest)
                    .build();
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
     * @return String[] {content, usage}
     */
    private String[] extractStreamContent(JsonNode node, boolean isGemini, boolean useOpenai) {
        String result[] = new String[2];
        try {
            if (isGemini) {
                JsonNode candidates = node.get("candidates");
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && parts.size() > 0) {
                            JsonNode text = parts.get(0).get("text");
                            if (text == null || (text instanceof NullNode nn && nn.isNull())) {
                                result[0] = null;
                            } else {
                                result[0] = text.asText();
                            }
                        }
                    }
                }
                JsonNode usage = node.get("usageMetadata");
                if (usage != null && !usage.isNull() && !"null".equals(usage.toString())) {
                    result[1] = usage.toString();
                }
                return result;
            } else if (useOpenai) {
                JsonNode choices = node.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode delta = choices.get(0).get("delta");
                    if (delta != null) {
                        JsonNode content = delta.get("content");
                        if (content == null || content.isNull()) {
                            content = delta.get("reasoning_content");
                            if (content == null || content.isNull()) {
                                result[0] = null;
                            } else {
                                result[0] = content.asText();
                            }
                        } else {
                            result[0] = content.asText();
                        }
                    }
                }
                JsonNode usage = node.get("usage");
                if (usage != null && !usage.isNull() && !"null".equals(usage.toString())) {
                    result[1] = usage.toString();
                }
                return result;
            } else {
                JsonNode message = node.get("message");
                if (message != null && !message.isNull()) {
                    JsonNode content = message.get("content");
                    if (content == null || content.isNull()) {
                        return null;
                    } else {
                        return new String[]{content.asText(), null};
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract stream content", e);
        }
        return result;
    }

    /**
     * Extract content from non-streaming response
     * @return String[] {content, usage}
     */
    private String[] extractNonStreamContent(Map<String, Object> data, boolean isGemini, boolean useOpenai) {
        String result[] = new String[2];
        try {
            if (isGemini) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            result[0] = parts.get(0).getOrDefault("text", "").toString();
                        }
                    }
                }
                List<Map<String, Object>> usage = (List<Map<String, Object>>) data.get("usageMetadata");
                if (usage != null) {
                    result[1] = JsonUtils.toJSONString(usage);
                }
                return result;
            } else if (useOpenai) {
                Object choices = data.get("choices");
                if (choices != null && choices instanceof List && !((List<?>) choices).isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) ((Map<String, Object>) ((List<?>) choices).get(0)).get("message");
                    if (message != null) {
                        Object content = message.get("content");
                        result[0] = content != null ? content.toString() : message.getOrDefault("reasoning_content", "").toString();
                    }
                }
                Object usage = data.get("usage");
                if (usage != null) {
                    result[1] = JsonUtils.toJSONString(usage);
                }
                return result;
            } else {
                Map<String, Object> message = (Map<String, Object>) data.get("message");
                if (message != null) {
                    result[0] = message.getOrDefault("content", "").toString();
                }
                return result;
            }
        } catch (Exception e) {
            logger.warn("Failed to extract non-stream content", e);
        }
        return result;
    }
}
