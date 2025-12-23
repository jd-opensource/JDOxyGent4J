package com.jd.oxygent.infra.databases.es;

import com.jd.oxygent.core.oxygent.infra.databases.BaseDB;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

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
    private void validateIndexName(String indexName, String methodName) {
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
    private List<Map<String, Object>> executeSearchRequest(SearchRequest searchRequest, String methodName) {
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

            List<Map<String, Object>> searchResults;
            if (body.get("query") instanceof Map && ((Map<?, ?>) body.get("query")).get("term") instanceof Map) {
                Map<String, Object> termQueryMap = (Map<String, Object>) ((Map) body.get("query")).get("term");
                searchResults = termQueryByPage(indexName, termQueryMap, 1, 10, null, null);
            } else {
                searchResults = termQueryBySearchRequest(indexName, body);
            }

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

    public List<Map<String, Object>> termQueryByPage(String indexName, Map<String, Object> termQueryMap,
                                                     int page, int pageSize, Object[] searchAfter,
                                                     Map<String, SortOrder> sortMap) {
        log.info("RemoteEs.termQueryByPage request params: indexName={}, termQueryMap={}, page={}, pageSize={}",
                indexName, termQueryMap, page, pageSize);

        validateIndexName(indexName, "termQueryByPage");
        if (pageSize <= 0) {
            log.error("RemoteEs.termQueryByPage invalid parameter: pageSize={}", pageSize);
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        // Build search request
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // Add query conditions
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (termQueryMap != null) {
            for (Map.Entry<String, Object> entry : termQueryMap.entrySet()) {
                boolQuery.must(QueryBuilders.termQuery(entry.getKey(), entry.getValue()));
            }
        }
        sourceBuilder.query(boolQuery);
        sourceBuilder.size(pageSize);

        // Add range conditions
        if (searchAfter != null) {
            sourceBuilder.searchAfter(searchAfter);
        }
        // Add sorting
        if (!CollectionUtils.isEmpty(sortMap)) {
            for (Map.Entry<String, SortOrder> entry : sortMap.entrySet()) {
                sourceBuilder.sort(entry.getKey(), entry.getValue());
            }
        }

        searchRequest.source(sourceBuilder);
        return executeSearchRequest(searchRequest, "RemoteEs.termQueryByPage");
    }


    public List<Map<String, Object>> termQueryBySearchRequest(String indexName, Map<String, Object> body) {
        log.info("RemoteEs.termQueryBySearchRequest request params: indexName={}, body={}", indexName, body);

        validateIndexName(indexName, "termQueryBySearchRequest");

        // Extract query parameters
        QueryParams queryParams = extractQueryParams(body);

        // Build query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("trace_id", queryParams.traceIdValue))
                .must(QueryBuilders.termQuery("session_name", queryParams.sessionNameValue));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .size(queryParams.size)
                .sort("create_time", SortOrder.DESC);

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(sourceBuilder);

        return executeSearchRequest(searchRequest, "RemoteEs.termQueryBySearchRequest");
    }

    /**
     * Extract query parameters.
     */
    private QueryParams extractQueryParams(Map<String, Object> body) {
        QueryParams params = new QueryParams();
        params.traceIdValue = new ArrayList<>();
        params.sessionNameValue = "";
        params.size = 10; // default value

        Object queryObj = body.get("query");
        if (!(queryObj instanceof Map)) {
            return params;
        }

        Map<?, ?> queryMap = (Map<?, ?>) queryObj;
        Object boolObj = queryMap.get("bool");
        if (!(boolObj instanceof Map)) {
            return params;
        }

        Map<?, ?> boolMap = (Map<?, ?>) boolObj;
        Object mustObj = boolMap.get("must");
        if (!(mustObj instanceof List) || ((List<?>) mustObj).isEmpty()) {
            return params;
        }

        List<?> mustList = (List<?>) mustObj;

        // Extract trace_id from the first 'must' condition
        if (mustList.size() > 0 && mustList.get(0) instanceof Map) {
            Map<?, ?> firstMust = (Map<?, ?>) mustList.get(0);
            Object termsObj = firstMust.get("terms");
            if (termsObj instanceof Map) {
                Map<?, ?> termsMap = (Map<?, ?>) termsObj;
                Object traceIdObj = termsMap.get("trace_id");
                if (traceIdObj instanceof List) {
                    params.traceIdValue = (List<String>) traceIdObj;
                }
            }
        }

        // Extract session_name from the second 'must' condition
        if (mustList.size() > 1 && mustList.get(1) instanceof Map) {
            Map<?, ?> secondMust = (Map<?, ?>) mustList.get(1);
            Object termObj = secondMust.get("term");
            if (termObj instanceof Map) {
                Map<?, ?> termMap = (Map<?, ?>) termObj;
                Object sessionNameObj = termMap.get("session_name");
                if (sessionNameObj instanceof String) {
                    params.sessionNameValue = (String) sessionNameObj;
                }
            }
        }

        // Extract size parameter
        Object sizeObj = body.get("size");
        if (sizeObj instanceof Integer) {
            params.size = (Integer) sizeObj;
        }

        return params;
    }

    /**
     * Query parameter wrapper class.
     */
    private static class QueryParams {
        List<String> traceIdValue;
        String sessionNameValue;
        int size;
    }

}
