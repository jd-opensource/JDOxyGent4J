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
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
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
        super(baseUrl, apiKey, modelName, null, null, null);
        this.httpClient = new OkHttpClient();
    }

    public OpenAiLlm(String baseUrl, String apiKey, String modelName, Integer timeout, Map<String, Object> llmParams, String name) {
        super(baseUrl, apiKey, modelName, Duration.ofSeconds(timeout), llmParams, null);
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
        Map<String, Objects> llmConfig = new HashMap<>();

        Map<String, Object> payload = new HashMap<>(Map.of(
                "messages", this._getMessages(oxyRequest),
                "model", this.modelName,
                "stream", false
        ));
        payload.putAll(llmConfig);
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
                String output = root.path("choices").path(0).path("message").path("content").asText("");
                log.info("root: {}", root);
                Map<String, Object> extra = new HashMap<>();
                extra.put("usage", root.path("usage"));
                extra.put("tokens", root.path("usage").path("total_tokens").asInt());

                return OxyResponse.builder()
                        .state(OxyState.COMPLETED)
                        .output(output)
                        .extra(extra)
                        .oxyRequest(oxyRequest)
                        .build();
            } catch (IOException e) {
                log.info("Request error: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


    }


}
