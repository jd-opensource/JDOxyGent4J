package com.jd.oxygent.infra.databases.vearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.infra.databases.BaseVDB;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbDO;
import com.jd.oxygent.core.oxygent.infra.dataobject.vearch.VdbQueryFilterDO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Vector database implementation based on Vearch official SDK
 * <p>
 * Directly uses Vearch provided RESTful API for all operations
 * Supports core functions such as vector search, text search, and conditional filtering
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@Slf4j
public class RemoteVearch implements BaseVDB {

    @Autowired
    private VearchConfig vearchConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public List<String> batchUpsert(String dbName, String spaceName, List<VdbDO> dataList) {
        if (dbName == null || dbName.isBlank()) {
            throw new IllegalArgumentException("dbName cannot be empty");
        }
        if (spaceName == null || spaceName.isBlank()) {
            throw new IllegalArgumentException("spaceName cannot be empty");
        }
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> docs = dataList.stream()
                .map(d -> objectMapper.convertValue(d, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                }))
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("db_name", dbName);
        body.put("space_name", spaceName);
        body.put("documents", docs);
        String resp = vearchPost("/document/upsert", body);
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode docIds = root.path("data").path("document_ids");
            if (!docIds.isArray() || docIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> ids = new ArrayList<>();
            docIds.forEach(n -> ids.add(n.path("_id").asText()));
            return ids;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("parse vearch response failed", e);
        }

    }

    @Override
    public List<VdbDO> batchGetById(String dbName, String spaceName, List<String> ids, boolean withEmbedding) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("db_name", dbName);
        body.put("space_name", spaceName);
        body.put("document_ids", ids);          // Primary key list
        body.put("vector_value", Boolean.TRUE.equals(withEmbedding));        // Don't pull vectors
        body.put("limit", ids.size());          // Maximum ids records

        String resp = vearchPost("/document/query", body);
        // 3. Parse response with Jackson
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode docs = root.path("data").path("documents");
            if (!docs.isArray() || docs.isEmpty()) {
                return Collections.emptyList();
            }
            List<VdbDO> result = new ArrayList<>();
            for (JsonNode node : docs) {
                result.add(toVdbDo(node));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("parse vearch response failed", e);
        }
    }

    @Override
    public List<VdbDO> searchByVector(String dbName, String spaceName, List<VdbQueryFilterDO.queryVector> queryVectors, VdbQueryFilterDO filter) {
        if (dbName == null || dbName.isBlank()) {
            throw new IllegalArgumentException("dbName cannot be empty");
        }
        if (spaceName == null || spaceName.isBlank()) {
            throw new IllegalArgumentException("spaceName cannot be empty");
        }
        // 1. Build request body
        Map<String, Object> body = objectMapper.convertValue(filter, Map.class);
        body.put("db_name", dbName);
        body.put("space_name", spaceName);

        log.info(cut(queryVectors.toString(), 200));

        body.put("vectors",
                queryVectors.stream()
                        .map(v -> {
                            Map<String, Object> vectorMap = new HashMap<>();

                            vectorMap.put("field", v.field());

                            // ⭐ Fix: Use IntStream.range to handle float[], avoiding the problem of being unable to parse stream(float[])
                            vectorMap.put("feature",
                                    IntStream.range(0, v.feature().length)
                                            .mapToObj(i -> v.feature()[i]) // v.feature()[i] is float, will be boxed to Float
                                            .collect(Collectors.toList()));

                            vectorMap.put("min_score", v.minScore());

                            return vectorMap;
                        })
                        .collect(Collectors.toList()));

        String resp = vearchPost("/document/search", body);

        log.info(cut(queryVectors.toString(), 200));
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode outer = root.path("data").path("documents");
            if (!outer.isArray() || outer.isEmpty()) {
                return Collections.emptyList();
            }
            JsonNode inner = outer.get(0);
            if (!inner.isArray() || inner.isEmpty()) {
                return Collections.emptyList();
            }
            List<VdbDO> vs = new ArrayList<>();
            for (JsonNode node : inner) {
                vs.add(toVdbDo(node));
            }
            return vs;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("parse vearch response failed", e);
        }
    }


    private String vearchPost(String urlPath, Map<String, Object> bodyObj) {
        String json;
        try {
            json = objectMapper.writeValueAsString(bodyObj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Request body serialization failed", e);
        }
        log.info(cut(json.toString(), 500));
        Request req = new Request.Builder()
                .url(vearchConfig.getVearchProperties().getUrl() + urlPath)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        log.info(cut(req.toString(), 500));
        try (Response resp = httpClient.newCall(req).execute()) {
            String respBody = resp.body() == null ? null : resp.body().string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("vearch request failed, code=" + resp.code() + ", body=" + respBody);
            }
            if (respBody == null || respBody.isBlank()) {
                throw new RuntimeException("vearch returned empty body");
            }
            return respBody;
        } catch (IOException e) {
            throw new RuntimeException("vearch io error", e);
        }
    }

    private VdbDO toVdbDo(JsonNode d) {
        // Vector field processing
        float[] vec = null;
        JsonNode arr = d.path("vector");
        if (arr.isArray() && arr.size() > 0) {
            vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = (float) arr.get(i).asDouble();
            }
        }

        return VdbDO.builder()
                .id(d.path("_id").asText())
                .userId(d.path("user_id").asText())
                .sessionId(d.path("session_id").asText())
                .msgTurn(d.path("msg_turn").asInt())
                .createTime(d.path("create_time").asLong())
                .text(d.path("text").asText())
                .indexText(d.path("index_text").asText())
                .vector(vec)
                .build();
    }

    private static String cut(String text, int maxTokens) {
        return text.length() <= maxTokens ? text : text.substring(0, maxTokens);
    }
}