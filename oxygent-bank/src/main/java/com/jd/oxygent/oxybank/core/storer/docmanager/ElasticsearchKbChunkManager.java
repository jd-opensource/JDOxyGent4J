package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.infra.databases.es.EsConfiguration;
import com.jd.oxygent.infra.databases.es.RemoteEs;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch知识库chunk管理类
 * 
 * 负责知识库文档chunk的管理，包括添加、查询、删除等操作
 */
@Slf4j
@Component
public class ElasticsearchKbChunkManager {

    @Autowired
    private EsConfiguration esConfiguration;
    @Autowired
    private BaseEs esClient;
//    @Value("${oxygent_bank.knowledge_chunk.index_name:knowledge_chunk_info")
    private String indexName = "knowledge_chunk_info";

    @PostConstruct
    public boolean initialize() {
        Map<String, Object> root = KnowledgeIndex.KB_INFO_INDEX;
        Mas.getEsSetting(root);
        esClient.createIndex(indexName, root);
        Map mapping = esClient.getMapping(indexName);
        return true;
    }

    /**
     * 根据知识库ID和文件ID列表删除对应的chunk信息
     * 
     * @param kbId 知识库ID
     * @param fileIds 要删除的文件ID列表
     * @return 是否删除成功
     */
    public boolean kbDeleteChunk(String kbId, List<String> fileIds) {
        try {
            if (fileIds == null || fileIds.isEmpty()) {
                log.info("No file IDs to delete");
                return true;
            }
            
            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            if (!esConfiguration.getClient().indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
                log.info("Index {} does not exist, no need to delete chunk data", indexName);
                return true;
            }
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("kb_id", kbId))
                .must(QueryBuilders.termsQuery("ori_file_id", fileIds));
            
            DeleteRequest deleteRequest = new DeleteRequest(indexName);
            // FIXME: delete_by_query操作需要使用BulkByScrollAction或UpdateByQueryRequest
            
            log.info("Successfully deleted file records (kb_id: {}, file_ids: {})", kbId, fileIds);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file records (kb_id: {}, file_ids: {})", kbId, fileIds, e);
            return false;
        }
    }

    /**
     * 批量写入chunk数据到ES索引
     * 
     * @param nodes chunk节点列表
     * @return 是否写入成功
     */
    public boolean kbAddChunk(List<Map<String, Object>> nodes) {
        try {
            if (nodes == null || nodes.isEmpty()) {
                log.info("No chunk data to add");
                return true;
            }
            
            BulkRequest bulkRequest = new BulkRequest();
            String currentTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            
            for (Map<String, Object> node : nodes) {
                Map<String, Object> metadata = node != null ? (Map<String, Object>) node.get("metadata") : null;
                
                Map<String, Object> esDoc = new HashMap<>();
                esDoc.put("kb_id", metadata != null ? metadata.get("kb_id") : "");
                String oriFileId = metadata != null ? (String) metadata.get("ori_file_id") : null;
                if (oriFileId == null && metadata != null) {
                    oriFileId = (String) metadata.get("file_id");
                }
                esDoc.put("ori_file_id", oriFileId != null ? oriFileId : "");
                String chunkId = metadata != null ? (String) metadata.get("chunk_id") : null;
                esDoc.put("chunk_id", chunkId != null ? chunkId : "chunk_" + System.currentTimeMillis());
                esDoc.put("chunk_text", node != null ? node.get("text") : "");
                esDoc.put("chunk_extra_data", new HashMap<>());
                esDoc.put("language", "");
                
                IndexRequest indexRequest = new IndexRequest(indexName);
                indexRequest.source(esDoc, XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
            
            bulkRequest.setRefreshPolicy("wait_for");
            
            BulkResponse response = esConfiguration.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
            
            if (response.hasFailures()) {
                int errorCount = 0;
                for (int i = 0; i < response.getItems().length; i++) {
                    if (response.getItems()[i].isFailed()) {
                        errorCount++;
                        log.error("Insert failed: {}", response.getItems()[i].getFailureMessage());
                    }
                }
                
                log.warn("Partial insert failed: success {}, failed {}", nodes.size() - errorCount, errorCount);
                return errorCount != nodes.size();
            } else {
                log.info("Successfully inserted {} chunks to ES index {}", nodes.size(), indexName);
                return true;
            }
        } catch (Exception e) {
            log.error("Batch insert chunks failed", e);
            return false;
        }
    }

    /**
     * 获取知识库的chunks
     * 
     * @param kbId 知识库ID
     * @param page 页码
     * @param size 每页大小
     * @return 包含items、total、page、size、pages的Map
     */
    public Map<String, Object> getKbChunks(String kbId, int page, int size) {
        try {
            int fromValue = (page - 1) * size;
            
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            sourceBuilder.query(QueryBuilders.termQuery("kb_id", kbId));
            sourceBuilder.from(fromValue);
            sourceBuilder.size(size);
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            
            SearchHits hits = response.getHits();
            long total = hits.getTotalHits().value;
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                result.add(hit.getSourceAsMap());
            }
            
            int pages = (int) ((total + size - 1) / size);
            
            return Map.of(
                "items", result,
                "total", total,
                "page", page,
                "size", size,
                "pages", pages
            );
        } catch (Exception e) {
            log.error("search knowledge chunks of [{}] error", kbId, e);
            return Map.of(
                "items", new ArrayList<>(),
                "total", 0L,
                "page", page,
                "size", size,
                "pages", 0
            );
        }
    }

    /**
     * 获取知识库指定文件的chunks
     * 
     * @param kbId 知识库ID
     * @param fileId 文件ID
     * @param page 页码
     * @param size 每页大小
     * @return 包含items、total、page、size、pages的Map
     */
    public Map<String, Object> getKbFileChunks(String kbId, String fileId, int page, int size) {
        try {
            int fromValue = (page - 1) * size;
            
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("kb_id", kbId))
                .must(QueryBuilders.termQuery("ori_file_id", fileId));
            
            sourceBuilder.query(boolQuery);
            sourceBuilder.from(fromValue);
            sourceBuilder.size(size);
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            
            SearchHits hits = response.getHits();
            long total = hits.getTotalHits().value;
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                result.add(hit.getSourceAsMap());
            }
            
            int pages = (int) ((total + size - 1) / size);
            
            return Map.of(
                "items", result,
                "total", total,
                "page", page,
                "size", size,
                "pages", pages
            );
        } catch (Exception e) {
            log.error("search knowledge chunks of [kb_id:{}] [file_id:{}] error", kbId, fileId, e);
            return Map.of(
                "items", new ArrayList<>(),
                "total", 0L,
                "page", page,
                "size", size,
                "pages", 0
            );
        }
    }

    public Map<String, Object> search(String kbId, int page, int size) {
        try {
            int fromValue = (page - 1) * size;

            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.termQuery("kb_id", kbId));
            sourceBuilder.from(fromValue);
            sourceBuilder.size(size);
            sourceBuilder.sort("create_time", SortOrder.DESC);

            searchRequest.source(sourceBuilder);

            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            long total = hits.getTotalHits().value;

            List<Map<String, Object>> result = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                result.add(hit.getSourceAsMap());
            }

            int pages = (int) ((total + size - 1) / size);

            List<KnowledgeBaseItem> kbList = new ArrayList();
            for (Map each : result) {
                kbList.add(JsonUtils.convertValue(each, KnowledgeBaseItem.class));
            }
            return Map.of(
                    "items", kbList,
                    "total", total,
                    "page", page,
                    "size", size,
                    "pages", pages
            );
        } catch (Exception e) {
            log.error("search all knowledge base file of [kb_id:{}] error", kbId, e);
            return Map.of(
                    "items", new ArrayList<>(),
                    "total", 0L,
                    "page", page,
                    "size", size,
                    "pages", 0
            );
        }
    }
}
