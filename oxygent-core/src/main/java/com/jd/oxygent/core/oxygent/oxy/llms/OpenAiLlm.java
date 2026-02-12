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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.logging.AiLogger;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.SpringContextHolder;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Slf4j
public class OpenAiLlm extends RemoteLlm {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;

    public OpenAiLlm(String baseUrl, String apiKey, String modelName) {
        super(baseUrl, apiKey, modelName, null, null, null, null);
        this.httpClient = new OkHttpClient();
    }

    public OpenAiLlm(String baseUrl, String apiKey, String modelName, Integer timeout, Map<String, Object> llmParams, String name) {
        super(baseUrl, apiKey, modelName, Duration.ofSeconds(timeout), llmParams, null, null);
        super.setName(name);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(timeout, TimeUnit.SECONDS);
        builder.readTimeout(timeout, TimeUnit.SECONDS);
        builder.writeTimeout(timeout, TimeUnit.SECONDS);
        this.httpClient = builder.build();
    }

    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        Set<String> excludedKeys = Set.of(
                "cls", "base_url", "api_key", "name", "model_name"
        );
        Map<String, Object> payload = new HashMap<>(Map.of(
                "messages", this._getMessages(oxyRequest),
                "model", this.modelName,
                "stream", true
        ));
        payload.putAll(Config.getLlmConfigMap());
        if (this.llmParams != null) {
            payload.putAll(this.llmParams);
        }
        if (oxyRequest.getArguments() != null) {
            Map<String, Object> filteredArguments = oxyRequest.getArguments().entrySet().stream()
                    .filter(entry -> !"messages".equals(entry.getKey()) && !excludedKeys.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            payload.putAll(filteredArguments);
        }

        Request request;
        try {
            request = new Request.Builder()
                    .url(this.baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(payload),
                            MediaType.get("application/json")))
                    .build();
            try (Response resp = httpClient.newCall(request).execute()) {

                if (!resp.isSuccessful()) {
                    throw new IOException("HTTP " + resp.code() + ": " + (resp.body() != null ? resp.body().string() : null));
                }
                JsonNode root = objectMapper.readTree(resp.body() != null ? resp.body().byteStream() : null);
                long timer = System.currentTimeMillis();
                aiLogger = SpringContextHolder.getBean(AiLogger.class);
                if ("true".equalsIgnoreCase((String) payload.get("stream"))) {
                    StringBuilder answerAppend = new StringBuilder();
                    String answer = "";
                    boolean thinkStart = true;
                    boolean thinkEnd = false;
                    JsonNode reasoningContent = root.path("choices").path(0).path("delta").get("reasoning_content");
                    if (reasoningContent != null && reasoningContent.isNull()) {
                        if (thinkStart) {
                            Map<String, Object> message = new HashMap<>();
                            message.put("type", "stream");
                            Map<String, Object> _content = new HashMap<>();
                            _content.put("delta", "<think>");
                            _content.put("agent", oxyRequest.getCaller());
                            _content.put("node_id", oxyRequest.getNodeId());
                            message.put("content", _content);
                            oxyRequest.sendMessage(message);
                            answerAppend.append("<think>");
                            thinkStart = false;
                            thinkEnd = true;
                        }
                        answer = reasoningContent.asText();
                    } else {
                        JsonNode content = root.path("choices").path(0).path("delta").get("content");
                        if (thinkEnd) {
                            Map<String, Object> message = new HashMap<>();
                            message.put("type", "stream");
                            Map<String, Object> _content = new HashMap<>();
                            _content.put("delta", "<think>");
                            _content.put("agent", oxyRequest.getCaller());
                            _content.put("node_id", oxyRequest.getNodeId());
                            message.put("content", _content);
                            oxyRequest.sendMessage(message);
                            answerAppend.append("</think>");
                            thinkEnd = false;
                        }
                        answer = content.asText();
                    }
                    if (StringUtils.isNotBlank(answer)) {
                        answerAppend.append(answer);
                        Map<String, Object> message = new HashMap<>();
                        message.put("type", "stream");
                        Map<String, Object> _content = new HashMap<>();
                        _content.put("delta", answer);
                        _content.put("agent", oxyRequest.getCaller());
                        _content.put("node_id", oxyRequest.getNodeId());
                        message.put("content", _content);
                        oxyRequest.sendMessage(message);
                    }
                    Map<String, Object> message = new HashMap<>();
                    message.put("type", "stream_end");
                    Map<String, Object> _content = new HashMap<>();
                    _content.put("delta", "");
                    _content.put("agent", oxyRequest.getCaller());
                    _content.put("node_id", oxyRequest.getNodeId());
                    message.put("content", _content);
                    oxyRequest.sendMessage(message);

                    OxyResponse oxyResponse = OxyResponse.builder()
                            .state(OxyState.COMPLETED)
                            .output(answerAppend.toString())
                            .oxyRequest(oxyRequest)
                            .build();
                    if (aiLogger != null && aiLogger.isEnabled()) {
                        aiLogger.log("llm", oxyResponse, this, Map.of("elapsedMillis", System.currentTimeMillis() - timer));
                    }
                    return oxyResponse;
                } else {
                    String output = root.path("choices").path(0).path("message").path("content").asText("");
                    log.info("root: {}", root);
                    OxyResponse oxyResponse = OxyResponse.builder()
                            .state(OxyState.COMPLETED)
                            .output(output)
                            .oxyRequest(oxyRequest)
                            .build();
                    if (aiLogger != null && aiLogger.isEnabled()) {
                        aiLogger.log("llm", oxyResponse, this, Map.of("elapsedMillis", System.currentTimeMillis() - timer));
                    }
                    return oxyResponse;
                }
            } catch (IOException e) {
                log.error("Request error: {}", e);
                throw new RuntimeException(e);
            }
        } catch (JsonProcessingException e) {
            log.error("JsonProcessing error: {}", e);
            throw new RuntimeException(e);
        }
    }
}
