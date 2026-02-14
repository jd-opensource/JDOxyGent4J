package com.jd.oxygent.infra.databases.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jd.oxygent.core.oxygent.infra.databases.BaseDB;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Remote Elasticsearch implementation.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@ConditionalOnProperty(name = "oxygent.database.es", havingValue = "es")
@Service
@Slf4j
public class RemoteEs extends BaseDB implements BaseEs {

    @Autowired
    private EsConfiguration esConfiguration;
    private static final String ES_UPDATE_FORMAT = "ctx._source.%s = params.%s;";

    /**
     * Unified parameter validation methods.
     */
    public void validateIndexName(String indexName, String methodName) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(indexName)) {
            log.warn("{} invalid parameter: indexName must not be empty", methodName);
            throw new IllegalArgumentException("Index name must not be empty");
        }
    }

    private void validateDocId(String docId, String methodName) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(docId)) {
            log.warn("{} invalid parameter: docId must not be empty", methodName);
            throw new IllegalArgumentException("Document ID must not be empty");
        }
    }

    private void validateBody(Map<String, Object> body, String methodName) {
        if (body == null) {
            log.warn("{} invalid parameter: body must not be null", methodName);
            throw new IllegalArgumentException("Request body must not be empty");
        }
    }

    /**
     * Unified exception handling method.
     */
    private Map<String, Object> handleException(Exception e, String methodName) {
        log.error("{} exception", methodName, e);
        Map<String, Object> result = new HashMap<>();
        result.put("error", e.getMessage());
        return result;
    }

    /**
     * Process search results.
     */
    private List<Map<String, Object>> processSearchResults(SearchHits hits) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        long totalHits = hits.getTotalHits().value;

        if (totalHits == 0) {
            log.info("No matching documents found");
            return resultList;
        }

        for (SearchHit hit : hits.getHits()) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("id", hit.getId());
            resultMap.put("score", hit.getScore());
            resultMap.put("source", JsonUtils.parseObject(hit.getSourceAsString(), Map.class));
            resultMap.put("total", totalHits);
            resultList.add(resultMap);
        }
        return resultList;
    }

    /**
     * Execute a search request.
     */
    public List<Map<String, Object>> executeSearchRequest(SearchRequest searchRequest, String methodName) {
        try {
            log.debug("{} request parameters", methodName);
            SearchResponse searchResponse = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            log.debug("{} query completed, hit {} records", methodName, searchResponse.getHits().getTotalHits().value);

            SearchHits hits = searchResponse.getHits();
            return processSearchResults(hits);
        } catch (Exception e) {
            log.error("{} exception", methodName, e);
            throw new RuntimeException(methodName + " exception", e);
        }
    }

    @Override
    public Map<String, Object> createIndex(String indexName, Map<String, Object> body) {
        try {
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName) || body == null) {
                log.warn("Invalid parameters for createIndex, indexName={}, body={}", indexName, body);
                return Map.of("error", "Invalid parameters");
            }
            return Map.of("acknowledged", createIndexInternal(indexName, body));
        } catch (Exception e) {
            return handleException(e, "createIndex");
        }
    }

    @Override
    public Map<String, Object> index(String indexName, String docId, Map<String, Object> body) {
        try {
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName) ||
                    org.apache.commons.lang3.StringUtils.isEmpty(docId) || body == null) {
                log.warn("Invalid parameters for index, indexName={}, docId={}, body={}", indexName, docId, body);
                return Map.of("error", "Invalid parameters");
            }
            String[] parts = indexName.split("_");
            String lastPart = parts[parts.length - 1];
            if ("message".equals(lastPart)) {
                body.remove("body"); // es cannot use this field
            }
            String json = JsonUtils.writeValueAsString(body);
            Boolean b = insertIndex(indexName, docId, json);

            return Map.of(
                    "_id", docId,
                    "result", "created " + b
            );
        } catch (Exception e) {
            return handleException(e, "index");
        }
    }

    @Override
    public Map<String, Object> update(String indexName, String docId, Map<String, Object> body) {
        try {
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName) ||
                    org.apache.commons.lang3.StringUtils.isEmpty(docId) || body == null) {
                log.warn("Invalid parameters for update, indexName={}, docId={}, body={}", indexName, docId, body);
                return Map.of("error", "Invalid parameters");
            }

            Boolean b = updateData(indexName, docId, body);
            return Map.of(
                    "_id", docId,
                    "result", "updated " + b
            );
        } catch (Exception e) {
            return handleException(e, "update");
        }
    }

    @Override
    public Boolean exists(String indexName, String docId) {
        try {
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName) || org.apache.commons.lang3.StringUtils.isEmpty(docId)) {
                log.warn("Invalid parameters for exists, indexName={}, docId={}", indexName, docId);
                return false;
            }
            GetRequest request = new GetRequest(indexName, docId);
            GetResponse response = esConfiguration.getClient().get(request, RequestOptions.DEFAULT);
            return response.isExists();
        } catch (Exception e) {
            log.error("exists exception", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> search(String indexName, Map<String, Object> body) {
        try {
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName) || body == null) {
                log.warn("Invalid parameters for search, indexName={}, body={}", indexName, body);
                return Map.of("error", "Invalid parameters");
            }
            String json = JsonUtils.toJSONString(body);
            log.info("RemoteEs.search request params: indexName={}, body={}", indexName, json);
            validateIndexName(indexName, "search");

            JsonNode rootNode = JsonUtils.readTree(json);

            JsonNode queryNode = rootNode.get("query");
            String queryJson = (queryNode != null) ? queryNode.toString() : "{}";
            QueryBuilder queryBuilder = QueryBuilders.wrapperQuery(queryJson);

            List<SortBuilder<?>> sortBuilders = new ArrayList<>();
            JsonNode sortNode = rootNode.get("sort");
            if (sortNode != null && sortNode.isArray()) {
                for (JsonNode sortItem : sortNode) {
                    Iterator<String> fieldNames = sortItem.fieldNames();
                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        JsonNode options = sortItem.get(fieldName);

                        SortOrder order = SortOrder.ASC;
                        if (options.has("order")) {
                            String orderStr = options.get("order").asText().toLowerCase();
                            order = "desc".equals(orderStr) ? SortOrder.DESC : SortOrder.ASC;
                        }
                        SortBuilder<?> sortBuilder = SortBuilders.fieldSort(fieldName).order(order);
                        sortBuilders.add(sortBuilder);
                    }
                }
            }
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(queryBuilder);
            for (SortBuilder<?> sortBuilder : sortBuilders) {
                sourceBuilder.sort(sortBuilder);
            }
            JsonNode sourceNode = rootNode.get("_source");
            if (sourceNode instanceof ArrayNode arrayNode) {
                List<String> result = new ArrayList<>();
                for (int i = 0; i < arrayNode.size(); i++) {
                    JsonNode element = arrayNode.get(i);
                    if (StringUtils.isNotBlank(element.asText())) {
                        result.add(element.asText());
                    }
                }
                sourceBuilder.fetchSource(result.toArray(new String[0]), null);
            }
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(sourceBuilder);
            List<Map<String, Object>> searchResults = executeSearchRequest(searchRequest, "RemoteEs.search");

            List<Map<String, Object>> limitedDocs = new ArrayList<>();
            if (searchResults != null) {
                searchResults.forEach(searchResult -> limitedDocs.add(Map.of("_source", searchResult.get("source"))));
            }
            return Map.of("hits", Map.of("hits", limitedDocs));
        } catch (Exception e) {
            return handleException(e, "search");
        }
    }

    @Override
    public void close() {
        try {
            esConfiguration.getClient().close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close ES client", e);
        }
    }

    @Override
    public Map<String, Object> deleteIndex(String indexName) {
        try {
            // Parameter validation
            validateIndexName(indexName, "delete");
            // Execute bulk operation
            AcknowledgedResponse acknowledgedResponse = esConfiguration.getClient().indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
            if (acknowledgedResponse.isAcknowledged()) {
                log.info("Index created successfully: {}", indexName);
                return Map.of("acknowledged", true);
            } else {
                log.warn("Index creation not acknowledged: {}", indexName);
                return Map.of("acknowledged", false);
            }
        } catch (Exception e) {
            log.error("Unknown exception occurred during bulk delete operation: {}", e.getMessage(), e);
            return handleException(e, "delete");
        }
    }

    public Boolean createIndexInternal(String indexName, Map<String, Object> body) {
        try {
            // Parameter validation
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName)) {
                log.error("Failed to create index: index name must not be empty");
                return false;
            }

            // Index existence check request
            GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(indexName);

            // Execute request
            boolean exists = esConfiguration.getClient().indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if (exists) {
                log.info("Index already exists: {}", indexName);
                return true;
            }

            CreateIndexRequest request = new CreateIndexRequest(indexName);

            // Set shard configuration (defaults: shards=5, replicas=1)
            int shards = 5;
            int replicas = 1;
            if (body != null) {
                if (body.get("settings") instanceof Map) {
                    Map<String, Object> settings = (Map<String, Object>) body.get("settings");
                    if (settings.get("number_of_shards") instanceof Integer) {
                        shards = (Integer) settings.get("number_of_shards");
                    }
                    if (settings.get("number_of_replicas") instanceof Integer) {
                        replicas = (Integer) settings.get("number_of_replicas");
                    }
                }
            }

            request.settings(Settings.builder()
                    .put("index.number_of_shards", shards)
                    .put("index.number_of_replicas", replicas));

            // Add mapping definition
            if (body != null && body.get("mappings") != null) {
                String mappingJson = JsonUtils.writeValueAsString(body.get("mappings"));
                request.mapping("_doc", mappingJson, XContentType.JSON);
            }

            CreateIndexResponse response = esConfiguration.getClient().indices()
                    .create(request, RequestOptions.DEFAULT);

            if (response.isAcknowledged()) {
                log.info("Index created successfully: {}", indexName);
                return true;
            } else {
                log.warn("Index creation not acknowledged: {}", indexName);
                return false;
            }

        } catch (Exception e) {
            log.error("Unknown exception, failed to create index: {}", indexName, e);
            return false;
        }
    }

    public Boolean insertIndex(String indexName, String docId, String jsonDoc) {
        BulkRequest request = new BulkRequest();

        try {
            // Parameter validation
            validateIndexName(indexName, "insertIndex");
            validateDocId(docId, "insertIndex");
            if (org.apache.commons.lang3.StringUtils.isBlank(jsonDoc)) {
                throw new IllegalArgumentException("JSON document must not be empty");
            }

            IndexRequest indexRequest = new IndexRequest(indexName)
                    .id(docId)
                    .source(jsonDoc, XContentType.JSON);
            request.add(indexRequest);

            // Execute bulk index operation
            BulkResponse bulkResponse = esConfiguration.getClient().bulk(request, RequestOptions.DEFAULT);

            // Check bulk operation result
            if (bulkResponse.hasFailures()) {
                log.error("Bulk index operation contains failures: {}", bulkResponse.buildFailureMessage());

                // Log each failed document in detail
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        log.error("Document indexing failed - ID: {}, reason: {}",
                                failure.getId(), failure.getMessage());
                    }
                }
                return false;
            }
            log.info("Successfully indexed {} documents to RemoteEs docId:{} index:{}", bulkResponse.getItems().length, docId, indexName);
            return true;

        } catch (Exception e) {
            log.error("Unknown exception occurred during bulk index operation", e);
            throw new RuntimeException("Bulk indexing failed", e);
        }
    }


    public Boolean updateData(String indexName, String docId, Map<String, Object> fields) {
        BulkRequest request = new BulkRequest();
        try {
            // Parameter validation
            validateIndexName(indexName, "updateData");
            validateDocId(docId, "updateData");
            if (fields == null || fields.isEmpty()) {
                log.warn("Update fields are empty; skipping update");
                return true;
            }

            // Build update request
            request.add(new UpdateRequest(indexName, docId).doc(fields));

            // Execute bulk operation
            BulkResponse bulkResponse = esConfiguration.getClient().bulk(request, RequestOptions.DEFAULT);

            // Check bulk operation result
            if (bulkResponse.hasFailures()) {
                log.error("Bulk index operation contains failures: {}", bulkResponse.buildFailureMessage());

                // Log each failed document in detail
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        log.error("Document indexing failed - ID: {}, reason: {}, status: {}",
                                failure.getId(), failure.getMessage(), failure.getStatus());
                    }
                }
                return false;
            }

            log.info("Successfully indexed {} documents into index [{}]", bulkResponse.getItems().length, indexName);
            return true;

        } catch (Exception e) {
            log.error("Unknown exception occurred during bulk update operation: {}", e.getMessage(), e);
            return false;
        } finally {
            // Cleanup resources (if needed)
            if (request != null) {
                log.debug("Bulk update completed; request contains {} operations", request.numberOfActions());
            }
        }
    }

    @Override
    public Map<String, Object> delete(String indexName, String docId) {
        BulkRequest request = new BulkRequest();
        try {
            // Parameter validation
            validateIndexName(indexName, "delete");
            validateDocId(docId, "delete");
            request.add(new DeleteRequest(indexName, docId));
            // Execute bulk operation
            BulkResponse bulkResponse = esConfiguration.getClient().bulk(request, RequestOptions.DEFAULT);
            // Check bulk operation result
            if (bulkResponse.hasFailures()) {
                log.error("Bulk index operation contains failures: {}", bulkResponse.buildFailureMessage());
                StringBuilder errorMessage = new StringBuilder();
                // Log each failed document in detail
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        errorMessage.append(String.format("Document indexing failed - ID: %s, reason: %s, status: %s", failure.getId(), failure.getMessage(), failure.getStatus()));
                    }
                }
                log.error(errorMessage.toString());
                return Map.of(
                        "_id", docId,
                        "result", errorMessage.toString()
                );
            }
            log.info("Successfully deleted {} documents from index [{}]", bulkResponse.getItems().length, indexName);
            return Map.of(
                    "_id", docId,
                    "result", "deleted"
            );

        } catch (Exception e) {
            log.error("Unknown exception occurred during bulk delete operation: {}", e.getMessage(), e);
            return handleException(e, "delete");
        } finally {
            // Cleanup resources (if needed)
            if (request != null) {
                log.debug("Bulk delete completed; request contains {} operations", request.numberOfActions());
            }
        }
    }

    @Override
    public Map<String, Object> refreshIndex(String indexName) {
        try {
            if (org.apache.commons.lang3.StringUtils.isEmpty(indexName)) {
                log.warn("Invalid parameters for createIndex, indexName={}", indexName);
                return Map.of("error", "Invalid parameters");
            }
            return Map.of("acknowledged", esConfiguration.getClient().indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT));
        } catch (Exception e) {
            return handleException(e, "createIndex");
        }
    }

    @Override
    public int getHitsTotal(Map<String, Object> response) {
        if (response == null) {
            return 0;
        }
        Map<String, Object> hits = (Map<String, Object>) response.getOrDefault("hits", Map.of());
        Object total = hits.get("total");
        if (total != null) {
            if (total instanceof Map) {
                return (Integer) ((Map) total).getOrDefault("value", 0);
            } else {
                return (Integer) total;
            }
        }
        List<?> hitsList = (List<?>) hits.getOrDefault("hits", List.of());
        return hitsList.size();
    }
}
