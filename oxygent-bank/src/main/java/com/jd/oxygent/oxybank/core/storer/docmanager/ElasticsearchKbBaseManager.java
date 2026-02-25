package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.infra.databases.es.EsConfiguration;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch知识库基础管理类
 * <p>
 * 负责知识库基本信息的管理，包括创建、查询、更新、删除等操作
 */
@Slf4j
@Component
public class ElasticsearchKbBaseManager {

    @Autowired
    private EsConfiguration esConfiguration;
    @Autowired
    private BaseEs esClient;
    //    @Value("${oxygent_bank.knowledge_base.index_name:knowledge_base_info")
    private String indexName = "knowledge_base_info";

    @PostConstruct
    public boolean initialize() {
        Map<String, Object> root = KnowledgeIndex.KB_INFO_INDEX;
        Mas.getEsSetting(root);
        esClient.createIndex(indexName, root);
        Map mapping = esClient.getMapping(indexName);
        return true;
    }

    /**
     * 检查知识库是否存在
     *
     * @param kbName 知识库名称
     * @return 如果存在返回true，否则返回false
     */
    public boolean kbExists(String kbName) {
        try {
            KnowledgeBaseItem hits = kbInfoSearchName(kbName);
            return hits != null;
        } catch (Exception e) {
            log.error("Query index {} failed", indexName, e);
            return false;
        }
    }

    /**
     * 根据知识库名称搜索知识库信息
     * <p>
     * 示例输出:
     * {
     * "kb_id": "ac741bbe-2603-4d97-8712-a4e8c63b5e85",
     * "kb_name": "kb_1",
     * "kb_description": "this is the first knowledge base",
     * "kb_type": "unstructured",
     * "kb_extra_info": {}
     * }
     *
     * @param kbName 知识库名称
     * @return 知识库信息列表
     */
    public KnowledgeBaseItem kbInfoSearchName(String kbName) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.termQuery("kb_name", kbName));
            sourceBuilder.fetchSource(true);

            searchRequest.source(sourceBuilder);

            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);

            List<Map<String, Object>> result = new ArrayList<>();
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits.getHits()) {
                result.add(hit.getSourceAsMap());
            }
            return JsonUtils.convertValue(result.get(0), KnowledgeBaseItem.class);
        } catch (Exception e) {
            log.error("Query index {} failed", indexName, e);
            return null;
        }
    }

    /**
     * 根据知识库ID搜索知识库信息
     *
     * @param kbId 知识库ID
     * @return 知识库信息列表
     */
    public Map<String, Object> kbInfoSearchId(String kbId) {
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
            return result.get(0);
        } catch (Exception e) {
            log.error("Query index {} failed", indexName, e);
            return null;
        }
    }

    /**
     * 根据知识库ID获取知识库schema
     *
     * @param kbId 知识库ID
     * @return 知识库schema
     */
    public Map<String, Object> getKbSchemaById(String kbId) {
        Map<String, Object> kbInfo = kbInfoSearchId(kbId);
        if (kbInfo == null || kbInfo.isEmpty()) {
            return null;
        }
        // FIXME: 需要验证返回类型是否为Map<String, Object>
        return (Map<String, Object>) kbInfo.get("kb_schema");
    }

    /**
     * 添加知识库信息
     *
     * @param kbInfo 知识库信息
     * @return 如果添加成功返回true，否则返回false
     */
    public boolean kbAdd(Map<String, Object> kbInfo) {
        try {
            IndexRequest indexRequest = new IndexRequest(indexName);
            indexRequest.source(kbInfo);
            indexRequest.setRefreshPolicy("wait_for");

            IndexResponse response = esConfiguration.getClient().index(indexRequest, RequestOptions.DEFAULT);

            if ("created".equals(response.getResult().getLowercase())) {
                log.info("Knowledge base {} created successfully", kbInfo.get("kb_id"));
                return true;
            } else {
                log.error("Failed to create knowledge base information: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception creating knowledge base information", e);
            return false;
        }
    }

    /**
     * 获取所有知识库信息
     *
     * @param page 页码
     * @param size 每页大小
     * @return 包含items、total、page、size、pages的Map
     */
    public Map<String, Object> kbList(int page, int size) {
        try {
            int fromValue = (page - 1) * size;

            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.matchAllQuery());
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
            log.error("search all knowledge base error", e);
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
     * 获取所有知识库信息（无分页）
     *
     * @return 所有知识库信息列表
     */
    public List<Map<String, Object>> kbListAll() {
        try {
            List<Map<String, Object>> allKbs = new ArrayList<>();

            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.matchAllQuery());
            sourceBuilder.size(1000);

            searchRequest.source(sourceBuilder);
            searchRequest.scroll(TimeValue.timeValueMinutes(2));

            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            String scrollId = response.getScrollId();
            SearchHits hits = response.getHits();

            for (SearchHit hit : hits.getHits()) {
                allKbs.add(hit.getSourceAsMap());
            }

            while (hits.getHits().length > 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(2));

                response = esConfiguration.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = response.getScrollId();
                hits = response.getHits();

                for (SearchHit hit : hits.getHits()) {
                    allKbs.add(hit.getSourceAsMap());
                }
            }

            if (scrollId != null) {
                try {
                    ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                    clearScrollRequest.addScrollId(scrollId);
                    esConfiguration.getClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
                } catch (Exception e) {
                    // 忽略清理scroll的异常
                }
            }

            log.info("Successfully retrieved {} knowledge base information", allKbs.size());
            return allKbs;
        } catch (Exception e) {
            log.error("Failed to get all knowledge base information", e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新知识库信息
     *
     * @param kbName       知识库名称
     * @param updateFields 要更新的字段
     * @return 如果更新成功返回true，否则返回false
     */
    public boolean kbUpdate(String kbName, Map<String, Object> updateFields) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.termQuery("kb_name", kbName));
            sourceBuilder.size(1);

            searchRequest.source(sourceBuilder);

            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();

            if (hits.getHits().length == 0) {
                log.warn("Knowledge base {} does not exist", kbName);
                return false;
            }

            String docId = hits.getAt(0).getId();

            if (updateFields.containsKey("kb_schema")) {
                Map<String, Object> script = new HashMap<>();
                Map<String, String> scriptSource = Map.of("source", "ctx._source.remove('kb_schema')", "lang", "painless");
                script.put("script", scriptSource);

                UpdateRequest deleteSchemaRequest = new UpdateRequest(indexName, docId);
//                deleteSchemaRequest.script(scriptSource);

                try {
                    esConfiguration.getClient().update(deleteSchemaRequest, RequestOptions.DEFAULT);
                } catch (Exception e) {
                    log.warn("Exception deleting old kb_schema field (field may not exist)", e);
                }
            }

            UpdateRequest updateRequest = new UpdateRequest(indexName, docId);
            updateRequest.doc(updateFields);
            updateRequest.setRefreshPolicy("wait_for");

            UpdateResponse updateResponse = esConfiguration.getClient().update(updateRequest, RequestOptions.DEFAULT);

            if ("updated".equals(updateResponse.getResult().getLowercase()) ||
                    "noop".equals(updateResponse.getResult().getLowercase())) {
                log.info("Knowledge base {} updated successfully", kbName);
                return true;
            } else {
                log.error("Failed to update knowledge base information: {}", updateResponse);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception updating knowledge base information", e);
            return false;
        }
    }

    /**
     * 删除知识库信息
     *
     * @param kbName 知识库名称
     * @return 如果删除成功返回true，否则返回false
     */
    public boolean kbDelete(String kbName) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.termQuery("kb_name", kbName));
            sourceBuilder.size(1);

            searchRequest.source(sourceBuilder);

            SearchResponse response = esConfiguration.getClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();

            if (hits.getHits().length == 0) {
                log.warn("Knowledge base {} does not exist", kbName);
                return false;
            }

            String docId = hits.getAt(0).getId();

            DeleteRequest deleteRequest = new DeleteRequest(indexName, docId);
            deleteRequest.setRefreshPolicy("wait_for");

            DeleteResponse deleteResponse = esConfiguration.getClient().delete(deleteRequest, RequestOptions.DEFAULT);

            if ("deleted".equals(deleteResponse.getResult().getLowercase())) {
                log.info("Knowledge base {} deleted successfully", kbName);
                return true;
            } else {
                log.error("Failed to delete knowledge base information: {}", deleteResponse);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception deleting knowledge base information", e);
            return false;
        }
    }

    /**
     * 根据知识库ID获取知识库名称
     *
     * @param kbId 知识库ID
     * @return 知识库名称，如果不存在返回null
     */
    public String getKbNameById(String kbId) {
        Map<String, Object> kbInfo = kbInfoSearchId(kbId);
        if (kbInfo != null && !kbInfo.isEmpty()) {
            return (String) kbInfo.get("kb_name");
        }
        return null;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
