package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.infra.databases.es.EsConfiguration;
import com.jd.oxygent.infra.databases.es.RemoteEs;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch Index Manager
 * <p>
 * Demonstrates how to use ES Python client to check, validate, delete, and create indexes
 */
@Slf4j
@Component
public class ElasticsearchIndexManager {

    @Autowired
    private EsConfiguration esConfiguration;
    @Autowired
    private BaseEs esClient;

    /**
     * Check if index exists
     *
     * @param indexName Index name
     * @return Whether index exists
     */
    public boolean indexExists(String indexName) {
        try {
            IndicesClient indicesClient = esConfiguration.getClient().indices();
            return indicesClient.exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("Failed to check if index exists: {}", indexName, e);
            return false;
        }
    }

    /**
     * Get index mapping structure
     *
     * @param indexName Index name
     * @return Index mapping structure
     */
    public GetMappingsResponse getIndexMapping(String indexName) {
        try {
            return esConfiguration.getClient().indices().getMapping(new GetMappingsRequest(), RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("Failed to get index mapping: {}", indexName, e);
            throw new RuntimeException("Index " + indexName + " does not exist");
        }
    }

    /**
     * Validate if index structure matches expectations
     *
     * @param indexName       Index name
     * @param expectedMapping Expected mapping structure
     * @return Whether structure matches
     */
    public boolean validateIndexStructure(String indexName, Map<String, Object> expectedMapping) {
        if (!indexExists(indexName)) {
            return false;
        }

        GetMappingsResponse getMappingsResponse = getIndexMapping(indexName);
        Map<String, MappingMetaData> currentProperties = getMappingsResponse.mappings();
        Map<String, Object> expectedProperties = (Map<String, Object>) expectedMapping.get("properties");

        // Simple comparison: check if all expected fields exist in current mapping and types match
        for (Map.Entry<String, Object> entry : expectedProperties.entrySet()) {
            String field = entry.getKey();
            Map<String, Object> fieldConfig = (Map<String, Object>) entry.getValue();

            if (!currentProperties.containsKey(field)) {
                log.info("Field {} does not exist in current index", field);
                return false;
            }

            Object currentType = currentProperties.get(field);
            String expectedType = (String) fieldConfig.get("type");

            // For object type, also accept when ES returns no type (version compatibility)
            if ("object".equals(expectedType) && currentType == null) {
                continue;
            }

            if (!expectedType.equals(currentType)) {
                log.error("Field {} type mismatch: expected {}, actual {}", field, expectedType, currentType);
                return false;
            }
        }

        return true;
    }

    /**
     * Delete index
     *
     * @param indexName Index name
     * @return Whether deletion was successful
     */
    public boolean deleteIndex(String indexName) {
        try {
            if (indexExists(indexName)) {
                esConfiguration.getClient().indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
                log.info("Index {} deleted", indexName);
                return true;
            } else {
                log.info("Index {} does not exist, no need to delete", indexName);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to delete index {}: {}", indexName, e);
            return false;
        }
    }

    /**
     * Create index
     *
     * @param indexName Index name
     * @param mapping  Index mapping structure
     * @param settings Index settings
     * @return Whether creation was successful
     */
    public boolean createIndex(String indexName, Map<String, Object> mapping, Map<String, Object> settings) {
        try {
            // If index exists, delete it first
            if (indexExists(indexName)) {
                log.info("Index {} already exists, will delete first", indexName);
                deleteIndex(indexName);
            }

            // Build request body for index creation
            Map<String, Object> body = Map.of("mappings", mapping);

            if (settings != null && !settings.isEmpty()) {
                body = new HashMap<>(body);
                body.put("settings", settings);
            }

            // Create index
            Map<String, Object> result = esClient.createIndex(indexName, body);
            log.info("Index {} created successfully", indexName);
            return true;
        } catch (Exception e) {
            log.error("Failed to create index {}: {}", indexName, e);
            return false;
        }
    }

    /**
     * Ensure index exists and structure matches expectations
     *
     * @param indexName       Index name
     * @param expectedMapping Expected mapping structure
     * @param settings       Index settings
     * @return Whether operation was successful
     */
    public boolean ensureIndex(String indexName, Map<String, Object> expectedMapping, Map<String, Object> settings) {
        log.info("Checking index {}...", indexName);

        // Check if index exists
        if (!indexExists(indexName)) {
            log.warn("Index {} does not exist, will create new index", indexName);
            return createIndex(indexName, expectedMapping, settings);
        }

        // Validate index structure
        if (validateIndexStructure(indexName, expectedMapping)) {
            log.info("Index {} exists and structure matches expectations", indexName);
            return true;
        } else {
            log.error("Index {} structure does not match expectations, will delete and recreate", indexName);
            return createIndex(indexName, expectedMapping, settings);
        }
    }
}
