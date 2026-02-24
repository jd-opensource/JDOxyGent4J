package com.jd.oxygent.oxybank.core.storer.docmanager;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态端点生成器
 * 
 * 基于KBSchema中的match_rules动态生成检索端点
 * 
 * 规则约束:
 * 1. 每个检索规则只能有一个高级搜索策略(es_text或vearch_vector)作为主查询
 * 2. 精确搜索策略(precise)可以出现多次，作为过滤条件
 * 
 * 查询模板设计:
 * - 策略确定时预构建查询模板结构
 * - 运行时只填充参数值，避免重复构建DSL
 * - 统一查询接口支持多种搜索引擎(ES、Vearch等)
 */

/**
 * 通用搜索结果
 */
@Data
class SearchResult {
    private List<Map<String, Object>> items;
    private long total;
    private double tookMs;
}

/**
 * ============================================================================
 * 查询模板抽象基类
 * ============================================================================
 */

/**
 * 查询模板抽象基类
 * 
 * 提供统一查询接口支持多种搜索引擎(ES、Vearch等)
 * 子类必须实现execute方法
 */
@Slf4j
abstract class BaseQueryTemplate {
    protected List<String> advancedSearchFields;
    protected List<String> preciseFilterFields;
    protected List<String> outputFields;

    /**
     * 初始化查询模板
     * 
     * @param advancedSearchFields 高级搜索字段列表
     * @param preciseFilterFields 精确过滤字段列表
     * @param outputFields 输出字段列表
     */
    public BaseQueryTemplate(
        List<String> advancedSearchFields,
        List<String> preciseFilterFields,
        List<String> outputFields
    ) {
        this.advancedSearchFields = advancedSearchFields;
        this.preciseFilterFields = preciseFilterFields;
        this.outputFields = outputFields;
    }

    /**
     * 执行查询(抽象方法，必须由子类实现)
     * 
     * @param requestData 请求参数字典
     * @param topK 返回结果数量
     * @param kwargs 其他参数(如kbName等)
     * @return 搜索结果
     */
    public abstract SearchResult execute(
        Map<String, Object> requestData,
        int topK,
        Map<String, Object> kwargs
    );
}

/**
 * ES查询模板
 * 
 * 封装Elasticsearch全文搜索逻辑
 */
@Slf4j
class ESQueryTemplate extends BaseQueryTemplate {
    private final RestHighLevelClient esClient;

    /**
     * 初始化ES查询模板
     * 
     * @param advancedSearchFields 高级搜索字段列表
     * @param preciseFilterFields 精确过滤字段列表
     * @param outputFields 输出字段列表
     * @param esClient Elasticsearch客户端实例
     */
    public ESQueryTemplate(
        List<String> advancedSearchFields,
        List<String> preciseFilterFields,
        List<String> outputFields,
        RestHighLevelClient esClient
    ) {
        super(advancedSearchFields, preciseFilterFields, outputFields);
        this.esClient = esClient;
    }

    /**
     * 构建ES查询DSL
     * 
     * @param requestData 请求参数字典
     * @param topK 返回结果数量
     * @return ES查询DSL
     */
    public Map<String, Object> buildQuery(Map<String, Object> requestData, int topK) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", topK);
        query.put("_source", outputFields);
        
        Map<String, Object> boolQuery = new HashMap<>();
        query.put("query", Map.of("bool", boolQuery));

        Map<String, Object> mustClause = buildMustClause(requestData);
        if (mustClause != null) {
            boolQuery.put("must", List.of(mustClause));
        }

        List<Map<String, Object>> filterClauses = buildFilterClauses(requestData);
        if (!filterClauses.isEmpty()) {
            boolQuery.put("filter", filterClauses);
        }

        if (boolQuery.isEmpty()) {
            query.put("query", Map.of("match_all", new HashMap<>()));
        }

        return query;
    }

    /**
     * 构建must子句(高级搜索)
     * 
     * @param requestData 请求参数字典
     * @return must子句字典
     */
    private Map<String, Object> buildMustClause(Map<String, Object> requestData) {
        String value = (String) requestData.get(advancedSearchFields.get(0));

        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Advanced search strategy field cannot be empty when executing query strategy");
        }

        return Map.of("match", Map.of(advancedSearchFields.get(0), value));
    }

    /**
     * 构建filter子句(精确过滤)
     * 
     * @param requestData 请求参数字典
     * @return filter子句列表
     */
    private List<Map<String, Object>> buildFilterClauses(Map<String, Object> requestData) {
        List<Map<String, Object>> clauses = new ArrayList<>();

        for (String field : preciseFilterFields) {
            String filterFieldName = "filter_" + field;
            Object value = requestData.get(filterFieldName);

            if (value == null) {
                continue;
            }

            clauses.add(Map.of("term", Map.of(field, String.valueOf(value))));
        }

        return clauses;
    }

    @Override
    public SearchResult execute(
        Map<String, Object> requestData,
        int topK,
        Map<String, Object> kwargs
    ) {
        String kbName = (String) kwargs.get("kbName");
        
        Map<String, Object> esQuery = buildQuery(requestData, topK);

        log.debug("ES Query: {}", esQuery);

        try {
            SearchRequest searchRequest = new SearchRequest(kbName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            
            Map<String, Object> query = (Map<String, Object>) esQuery.get("query");
            if (query.containsKey("bool")) {
                Map<String, Object> boolMap = (Map<String, Object>) query.get("bool");
                if (boolMap.containsKey("must")) {
                    List<Map<String, Object>> mustList = (List<Map<String, Object>>) boolMap.get("must");
                    for (Map<String, Object> must : mustList) {
                        if (must.containsKey("match")) {
                            Map<String, Object> match = (Map<String, Object>) must.get("match");
                            for (Map.Entry<String, Object> entry : match.entrySet()) {
                                boolQuery.must(QueryBuilders.matchQuery(entry.getKey(), entry.getValue()));
                            }
                        }
                    }
                }
                if (boolMap.containsKey("filter")) {
                    List<Map<String, Object>> filterList = (List<Map<String, Object>>) boolMap.get("filter");
                    for (Map<String, Object> filter : filterList) {
                        if (filter.containsKey("term")) {
                            Map<String, Object> term = (Map<String, Object>) filter.get("term");
                            for (Map.Entry<String, Object> entry : term.entrySet()) {
                                boolQuery.filter(QueryBuilders.termQuery(entry.getKey(), entry.getValue()));
                            }
                        }
                    }
                }
            } else if (query.containsKey("match_all")) {
                boolQuery.must(QueryBuilders.matchAllQuery());
            }
            
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(topK);
//            sourceBuilder.fetchSource(outputFields.toArray(new String[0])); fixme
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            
            SearchHits hits = response.getHits();
            long total = hits.getTotalHits().value;
            long tookMs = response.getTook().getMillis();
            
            List<Map<String, Object>> items = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                Map<String, Object> item = new HashMap<>(hit.getSourceAsMap());
                item.put("_score", hit.getScore());
                items.add(item);
            }
            
            log.info("ES search completed, returned {} results, total {}", items.size(), total);
            
            SearchResult result = new SearchResult();
            result.setItems(items);
            result.setTotal(total);
            result.setTookMs(tookMs);
            return result;
        } catch (Exception e) {
            log.error("ES search failed", e);
            throw new RuntimeException("Search service exception: " + e.getMessage(), e);
        }
    }
}

/**
 * Vearch查询模板
 * 
 * 封装Vearch向量搜索逻辑
 */
@Slf4j
class VearchQueryTemplate extends BaseQueryTemplate {
    private final Object vearchClient;
    private final Object embeddingModel;

    /**
     * 初始化Vearch查询模板
     * 
     * @param advancedSearchFields 高级搜索字段列表
     * @param preciseFilterFields 精确过滤字段列表
     * @param outputFields 输出字段列表
     * @param vearchClient Vearch客户端实例
     * @param embeddingModel 嵌入模型实例
     */
    public VearchQueryTemplate(
        List<String> advancedSearchFields,
        List<String> preciseFilterFields,
        List<String> outputFields,
        Object vearchClient,
        Object embeddingModel
    ) {
        super(advancedSearchFields, preciseFilterFields, outputFields);
        this.vearchClient = vearchClient;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public SearchResult execute(
        Map<String, Object> requestData,
        int topK,
        Map<String, Object> kwargs
    ) {
        String kbName = (String) kwargs.get("kbName");
        String queryText = (String) requestData.get(advancedSearchFields.get(0));

        if (queryText == null || queryText.isEmpty()) {
            throw new IllegalArgumentException("Advanced search strategy field cannot be empty when executing query strategy");
        }

        log.info("Query text: {}", queryText);

        // FIXME: 需要实现实际的向量生成和Vearch搜索逻辑
        log.warn("VearchQueryTemplate.execute is not fully implemented yet");
        
        SearchResult result = new SearchResult();
        result.setItems(new ArrayList<>());
        result.setTotal(0);
        result.setTookMs(0);
        return result;
    }
}

/**
 * ============================================================================
 * 动态端点生成器
 * ============================================================================
 */

/**
 * 动态生成并注册检索端点
 */
@Slf4j
@Component
public class RuleQueryInfer {

    @Autowired
    private ElasticsearchKbBaseManager kbBaseManager;
    @Autowired
    private ElasticsearchKbFileManager kbFileManager;
    @Autowired
    private KnowledgeIndex knowledgeIndex;

    /**
     * 生成所有端点
     * 
     * @param kbName 知识库名称
     * @param kbSchema 知识库schema
     * @return 端点映射
     */
    public Map<String, Object> generateAllEndpoints(String kbName, KBSchema kbSchema) {
        Map<String, Object> endpoints = new HashMap<>();
        
        // FIXME: 需要实现动态端点生成逻辑
        log.warn("generateAllEndpoints is not fully implemented yet");
        
        return endpoints;
    }

    /**
     * 分离高级搜索策略和精确搜索策略
     * 
     * @param matchRule 匹配规则
     * @return 包含高级搜索策略和精确搜索策略列表的Map
     */
    public Map<String, Object> separatePolicies(MatchRule matchRule) {
        MatchPolicy advancedPolicy = null;
        List<MatchPolicy> precisePolicies = new ArrayList<>();

        for (MatchPolicy policy : matchRule.getMatchPolicies()) {
            if ("es_text".equals(policy.getMode()) || "vearch_vector".equals(policy.getMode())) {
                advancedPolicy = policy;
            } else if ("precise".equals(policy.getMode())) {
                precisePolicies.add(policy);
            }
        }

        return Map.of(
            "advancedPolicy", advancedPolicy,
            "precisePolicies", precisePolicies
        );
    }
}
