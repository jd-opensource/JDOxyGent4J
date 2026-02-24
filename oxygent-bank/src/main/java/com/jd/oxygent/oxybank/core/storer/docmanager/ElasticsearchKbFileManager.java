package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.infra.databases.es.EsConfiguration;
import com.jd.oxygent.infra.databases.es.RemoteEs;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch知识库文件管理类
 * 
 * 负责知识库文件信息的管理，包括添加、查询、更新、删除等操作
 */
@Slf4j
@Component
public class ElasticsearchKbFileManager {

    @Autowired
    private EsConfiguration esConfiguration;
    @Autowired
    private BaseEs esClient;
//    @Value("${oxygent_bank.knowledge_file.index_name:knowledge_file_info")
    private String indexName = "knowledge_file_info";

    @PostConstruct
    public boolean initialize() {
        Map<String, Object> root = KnowledgeIndex.KB_FILE_INDEX;
        Mas.getEsSetting(root);
        esClient.createIndex(indexName, root);
        Map mapping = esClient.getMapping(indexName);
        return true;
    }

    /**
     * 根据知识库ID搜索所有文件ID
     * 
     * @param kbId 知识库ID
     * @return 文件信息列表
     */
    public List<Map<String, Object>> kbFileSearch(String kbId) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            sourceBuilder.query(QueryBuilders.termQuery("kb_id", kbId));
            sourceBuilder.fetchSource(true);
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            
            List<Map<String, Object>> result = new ArrayList<>();
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits.getHits()) {
                result.add(hit.getSourceAsMap());
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to query index {}", indexName, e);
            return null;
        }
    }

    /**
     * 根据知识库ID和文件ID列表删除对应的文件信息
     * 
     * @param kbId 知识库ID
     * @param fileIds 要删除的文件ID列表
     * @return 是否删除成功
     */
    public boolean kbDeleteFile(String kbId, List<String> fileIds) {
        try {
            if (fileIds == null || fileIds.isEmpty()) {
                log.info("No file IDs to delete");
                return true;
            }
            
            GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(indexName);
            if (!esConfiguration.getClient().indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
                log.info("Index {} does not exist, no need to delete file records", indexName);
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
     * 批量添加知识库文件信息
     * 
     * @param kbInfoList 知识库文件信息列表
     * @return 是否添加成功
     */
    public boolean kbAddBatchFiles(List<Map<String, Object>> kbInfoList) {
        try {
            if (kbInfoList == null || kbInfoList.isEmpty()) {
                log.info("No knowledge file information to add");
                return true;
            }
            
            // FIXME: 批量操作需要使用BulkRequest
            for (Map<String, Object> kbFile : kbInfoList) {
                IndexRequest indexRequest = new IndexRequest(indexName);
                indexRequest.source(kbFile, XContentType.JSON);
                indexRequest.setRefreshPolicy("wait_for");
                
                IndexResponse response = esConfiguration.getClient().index(indexRequest, RequestOptions.DEFAULT);
                if (!"created".equals(response.getResult().getLowercase()) && 
                    !"updated".equals(response.getResult().getLowercase())) {
                    return false;
                }
            }
            
            log.info("Successfully indexed {} documents", kbInfoList.size());
            return true;
        } catch (Exception e) {
            log.error("Error adding knowledge base file information", e);
            return false;
        }
    }

    /**
     * 添加文档信息到知识库
     * 
     * @param kbInfo 知识库文件信息
     * @return 是否添加成功
     */
    public boolean kbAddFile(Map<String, Object> kbInfo) {
        try {
            IndexRequest indexRequest = new IndexRequest(indexName);
            indexRequest.source(kbInfo, XContentType.JSON);
            indexRequest.setRefreshPolicy("wait_for");
            
            IndexResponse response = esConfiguration.getClient().index(indexRequest, RequestOptions.DEFAULT);
            
            if ("created".equals(response.getResult().getLowercase()) || 
                "updated".equals(response.getResult().getLowercase())) {
                log.info("Knowledge base file {} created/updated successfully", kbInfo.get("kb_id"));
                return true;
            } else {
                log.error("Knowledge base file information creation/update failed: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Knowledge base file information creation/update error", e);
            return false;
        }
    }

    /**
     * 更新文档信息
     * 
     * @param kbInfo 要更新的文档信息
     * @return 是否更新成功
     */
    public boolean kbUpdateFileInfo(Map<String, Object> kbInfo) {
        String kbId = (String) kbInfo.get("kb_id");
        String oriFileId = (String) kbInfo.get("ori_file_id");
        
        Map<String, Object> result = getKbFileInfo(kbId, oriFileId);
        if (result == null) {
            return false;
        }
        
        String esDocId = (String) result.get("_id");
        
        try {
            IndexRequest indexRequest = new IndexRequest(indexName);
            indexRequest.id(esDocId);
            indexRequest.source(kbInfo, XContentType.JSON);
            indexRequest.setRefreshPolicy("wait_for");
            
            IndexResponse response = esConfiguration.getClient().index(indexRequest, RequestOptions.DEFAULT);
            
            if ("updated".equals(response.getResult().getLowercase()) || 
                "noop".equals(response.getResult().getLowercase())) {
                log.info("Successfully updated document (es_doc_id: {}, kb_id: {}, ori_file_id: {})", 
                    esDocId, kbId, oriFileId);
                return true;
            } else {
                log.error("Failed to update document: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Error updating document (es_doc_id: {})", esDocId, e);
            return false;
        }
    }

    /**
     * 获取文档存储信息
     * 
     * @param kbId 知识库ID
     * @param oriFileId 原始文件ID
     * @return 包含_id和_source的Map
     */
    public Map<String, Object> getKbFileInfo(String kbId, String oriFileId) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("kb_id", kbId))
                .must(QueryBuilders.termQuery("ori_file_id", oriFileId));
            
            sourceBuilder.query(boolQuery);
            sourceBuilder.fetchSource(true);
            sourceBuilder.size(1);
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            
            if (hits.getHits().length > 0) {
                SearchHit hit = hits.getAt(0);
                return Map.of(
                    "_id", hit.getId(),
                    "_source", hit.getSourceAsMap()
                );
            } else {
                log.info("File information not found (kb_id: {}, ori_file_id: {})", kbId, oriFileId);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to query file information (kb_id: {}, ori_file_id: {})", kbId, oriFileId, e);
            return null;
        }
    }

    /**
     * 获取知识库的文件列表
     * 
     * @param kbId 知识库ID
     * @param page 页码
     * @param size 每页大小
     * @return 包含items、total、page、size、pages的Map
     */
    public Map<String, Object> getKbFiles(String kbId, int page, int size) {
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

    /**
     * 检查文件是否已存在于知识库文件列表中
     * 
     * @param kbId 知识库ID
     * @param oriFileId 原始文件ID
     * @return 如果文件存在返回true，否则返回false
     */
    public boolean checkFileExists(String kbId, String oriFileId) {
        try {
            CountRequest countRequest = new CountRequest(indexName);
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("kb_id", kbId))
                .must(QueryBuilders.termQuery("ori_file_id", oriFileId));
            
            countRequest.query(boolQuery);
            
            CountResponse response = esConfiguration.getClient().count(countRequest, RequestOptions.DEFAULT);
            
            return response.getCount() > 0;
        } catch (Exception e) {
            log.error("Failed to check if file exists (kb_id: {}, ori_file_id: {})", kbId, oriFileId, e);
            return false;
        }
    }

    /**
     * 批量添加DataFrame数据到Elasticsearch索引
     * 
     * @param kbName 知识库名称
     * @param df 数据列表
     * @return 是否添加成功
     */
    public boolean kbAddDf(String kbName, List<Map<String, Object>> df) {
        try {
            int batchSize = 100;
            List<Map<String, Object>> actions = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;
            
            for (int index = 0; index < df.size(); index++) {
                Map<String, Object> doc = df.get(index);
                
                Map<String, Object> action = new HashMap<>();
                action.put("_index", kbName);
                action.put("_source", doc);
                actions.add(action);
                
                if (actions.size() >= batchSize) {
                    // FIXME: 批量操作需要使用BulkRequest
                    for (Map<String, Object> item : actions) {
                        IndexRequest indexRequest = new IndexRequest(kbName);
                        indexRequest.source((Map<String, Object>) item.get("_source"), XContentType.JSON);
                        IndexResponse response = esConfiguration.getClient().index(indexRequest, RequestOptions.DEFAULT);
                        if ("created".equals(response.getResult().getLowercase())) {
                            successCount++;
                        } else {
                            failedCount++;
                        }
                    }
                    log.info("bulk add data into {}. {} success, {} failed", kbName, successCount, failedCount);
                    actions.clear();
                }
            }
            
            if (!actions.isEmpty()) {
                for (Map<String, Object> item : actions) {
                    IndexRequest indexRequest = new IndexRequest(kbName);
                    indexRequest.source((Map<String, Object>) item.get("_source"), XContentType.JSON);
                    IndexResponse response = esConfiguration.getClient().index(indexRequest, RequestOptions.DEFAULT);
                    if ("created".equals(response.getResult().getLowercase())) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                }
                log.info("bulk add data into {}. {} success, {} failed", kbName, successCount, failedCount);
            }
            
            return failedCount == 0;
        } catch (Exception e) {
            log.error("batch add data into es index {} exception", kbName, e);
            return false;
        }
    }
}
