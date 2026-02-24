package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.infra.databases.es.EsConfiguration;
import com.jd.oxygent.oxybank.core.model.annotation.QADataItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotation data manager for Elasticsearch.
 */
@Service
@Slf4j
public class AnnotationManager {

    private String indexName;

    private String indexPrefix = "qa_annotation";

    @Autowired
    EsConfiguration esConfiguration;
    @Autowired
    private BaseEs esClient;

    @PostConstruct
    public boolean initialize() {
        this.indexName = indexPrefix + "_data";
        Map<String, Object> root = JsonUtils.parseJsonString("""
                {
                  "mappings" : {
                     "properties": {
                         "data_id": {
                             "type": "keyword"
                         },
                         "data_hash": {
                             "type": "keyword"
                         },
                         "question": {
                             "type": "text",
                             "fields": {
                                 "keyword": {
                                     "type": "keyword",
                                     "ignore_above": 256
                                 }
                             }
                         },
                         "answer": {
                             "type": "text",
                             "fields": {
                                 "keyword": {
                                     "type": "keyword",
                                     "ignore_above": 256
                                 }
                             }
                         },
                         "source_trace_id": {
                             "type": "keyword"
                         },
                         "source_request_id": {
                             "type": "keyword"
                         },
                         "source_group_id": {
                             "type": "keyword"
                         },
                         "caller": {
                             "type": "keyword"
                         },
                         "callee": {
                             "type": "keyword"
                         },
                         "caller_type": {
                             "type": "keyword"
                         },
                         "callee_type": {
                             "type": "keyword"
                         },
                         "data_type": {
                             "type": "keyword"
                         },
                         "priority": {
                             "type": "integer"
                         },
                         "category": {
                             "type": "keyword"
                         },
                         "tags": {
                             "type": "keyword"
                         },
                         "status": {
                             "type": "keyword"
                         },
                         "annotation": {
                             "type": "object",
                             "enabled": "true"
                         },
                         "scores": {
                             "type": "object",
                             "enabled": "true"
                         },
                         "reject_reason": {
                             "type": "text"
                         },
                         "kb_status": {
                             "type": "keyword"
                         },
                         "kb_ingested_at": {
                             "type": "date",
                             "format": "yyyy-MM-dd HH:mm:ss.SSSSSS||yyyy-MM-dd HH:mm:ss||epoch_millis"
                         },
                         "kb_error_message": {
                             "type": "text"
                         },
                         "kb_extra": {
                             "type": "object",
                             "enabled": "true"
                         },
                         "batch_id": {
                             "type": "keyword"
                         },
                         "extra": {
                             "type": "object",
                             "enabled": "true"
                         },
                         "created_at": {
                             "type": "date",
                             "format": "yyyy-MM-dd HH:mm:ss.SSSSSS||yyyy-MM-dd HH:mm:ss||epoch_millis"
                         },
                         "updated_at": {
                             "type": "date",
                             "format": "yyyy-MM-dd HH:mm:ss.SSSSSS||yyyy-MM-dd HH:mm:ss||epoch_millis"
                         }
                     }
                  }
                }
                """);
        Mas.getEsSetting(root);
        esClient.createIndex(indexName, root);
        Map mapping = esClient.getMapping(indexName);
        return true;
    }

    public Map<String, Object> getByHash(String dataHash) {
        return esClient.search(indexName, Map.of(
                "query", Map.of(
                        "term", Map.of("data_hash", dataHash)
                ),
                "size", 0
        ));
    }

    public boolean create(QADataItem data) {
        Map<String, Object> result = esClient.index(indexName, data.getDataId(), JsonUtils.convertToMap(data, "snake"));
        if (result.get("result") != null) {
            log.debug("Data created successfully: {} {}", data.getDataId());
            return true;
        } else if (result.get("error") != null) {
            log.error("Data updated failed: {} {}", data.getDataId(), result.get("error"));
        }
        return false;
    }

    public boolean update(String dataId, Map<String, Object> updateData) {
        Map<String, Object> result = esClient.update(indexName, dataId, updateData);
        if (result.get("result") != null) {
            log.debug("Data updated successfully: {} {}", dataId);
            return true;
        } else if (result.get("error") != null) {
            log.error("Data updated failed: {} {}", dataId, result.get("error"));
        }
        return false;
    }

    public boolean delete(String dataId) {
        Map<String, Object> result = esClient.delete(indexName, dataId);
        if (result.get("result") != null) {
            log.debug("Data deleted successfully: {} {}", dataId);
            return true;
        } else if (result.get("error") != null) {
            log.error("Data deleted failed: {} {}", dataId, result.get("error"));
        }
        return false;
    }

    public Map<String, Object> getById(String dataId) {
        Map<String, Object> query = Map.of("query", Map.of("term", Map.of("_id", dataId)));
        return esClient.search(indexName, query);
    }

    /**
     * List query
     * <p>
     * List query with filtering, pagination and sorting
     *
     * @param filters    Filter conditions
     * @param pagination Pagination params {page, page_size}
     * @param sorting    Sorting params [{field, order}]
     * @return Query result {total, items}
     */
    public Map<String, Object> listQuery(
            Map<String, Object> filters,
            Map<String, Object> pagination,
            List<Map<String, Object>> sorting) {
        try {
            // Build query
            Map<String, Object> query = new HashMap<>();
            Map<String, Object> queryObj = new HashMap<>();
            Map<String, Object> boolObj = new HashMap<>();
            List<Map<String, Object>> must = new ArrayList<>();

            boolObj.put("must", must);
            queryObj.put("bool", boolObj);
            query.put("query", queryObj);

            // Add filter conditions
            if (filters != null) {
                // Status filter
                if (filters.containsKey("status")) {
                    must.add(Map.of(
                            "term", Map.of("status", filters.get("status"))
                    ));
                }

                // Priority filter
                if (filters.containsKey("priority")) {
                    must.add(Map.of(
                            "term", Map.of("priority", filters.get("priority"))
                    ));
                }

                // Data type filter
                if (filters.containsKey("data_type")) {
                    must.add(Map.of(
                            "term", Map.of("data_type", filters.get("data_type"))
                    ));
                }

                // Caller filter (wildcard for fuzzy match)
                if (filters.containsKey("caller")) {
                    must.add(Map.of(
                            "wildcard", Map.of(
                                    "caller", Map.of(
                                            "value", "*" + filters.get("caller") + "*"
                                    )
                            )
                    ));
                }

                // Callee filter (wildcard for fuzzy match)
                if (filters.containsKey("callee")) {
                    must.add(Map.of(
                            "wildcard", Map.of(
                                    "callee", Map.of(
                                            "value", "*" + filters.get("callee") + "*"
                                    )
                            )
                    ));
                }

                // Category filter
                if (filters.containsKey("category")) {
                    must.add(Map.of(
                            "term", Map.of("category", filters.get("category"))
                    ));
                }

                // Tags filter
                if (filters.containsKey("tags")) {
                    must.add(Map.of(
                            "terms", Map.of("tags", filters.get("tags"))
                    ));
                }

                // Date range filter - created_after
                if (filters.containsKey("created_after")) {
                    must.add(Map.of(
                            "range", Map.of(
                                    "created_at", Map.of(
                                            "gte", filters.get("created_after")
                                    )
                            )
                    ));
                }

                // Date range filter - created_before
                if (filters.containsKey("created_before")) {
                    must.add(Map.of(
                            "range", Map.of(
                                    "created_at", Map.of(
                                            "lte", filters.get("created_before")
                                    )
                            )
                    ));
                }

                // Full-text search (search in question and answer fields)
                if (filters.containsKey("search_text")) {
                    must.add(Map.of(
                            "multi_match", Map.of(
                                    "query", filters.get("search_text"),
                                    "fields", List.of("question", "answer"),
                                    "type", "best_fields"
                            )
                    ));
                }

                // Trace ID filter (wildcard for fuzzy match)
                if (filters.containsKey("trace_id")) {
                    must.add(Map.of(
                            "wildcard", Map.of(
                                    "source_trace_id", Map.of(
                                            "value", "*" + filters.get("trace_id") + "*"
                                    )
                            )
                    ));
                }

                // Group ID filter (wildcard for fuzzy match)
                if (filters.containsKey("group_id")) {
                    must.add(Map.of(
                            "wildcard", Map.of(
                                    "source_group_id", Map.of(
                                            "value", "*" + filters.get("group_id") + "*"
                                    )
                            )
                    ));
                }
            }

            // Use match_all if no filter conditions
            if (must.isEmpty()) {
                query.put("query", Map.of("match_all", Map.of()));
            }

            // Add sorting fixme
//            if (sorting != null && !sorting.isEmpty()) {
//                List<Map<String, Object>> sortList = new ArrayList<>();
//                for (Map<String, Object> sortItem : sorting) {
//                    String field = (String) sortItem.getOrDefault("field", "created_at");
//                    String order = (String) sortItem.getOrDefault("order", "desc");
//                    sortList.add(Map.of(field, Map.of("order", order)));
//                }
//                query.put("sort", sortList);
//            } else {
//                query.put("sort", List.of(Map.of("created_at", Map.of("order", "desc")))); fixme
//            }

            // Add pagination
            if (pagination != null) {
                int page = (int) pagination.getOrDefault("page", 1);
                int pageSize = (int) pagination.getOrDefault("page_size", 10);
                query.put("from", (page - 1) * pageSize);
                query.put("size", pageSize);
            } else {
                query.put("from", 0);
                query.put("size", 10);
            }
            return esClient.search(indexName, query);
        } catch (Exception e) {
            log.error("Build query failed", e);
            throw new RuntimeException("Build query failed", e);
        }
    }

    public Map<String, Object> getByTraceId(String traceId) {
        Map query = JsonUtils.parseJsonString(String.format("""
                {
                    "query": {
                        "term": {"source_trace_id": %s}
                    },
                    "sort": [{"created_at": {"order": "desc"}}],
                    "size": 100
                }
                """, traceId));
        return esClient.search(indexName, query);
    }

    public Map<String, Object> getByGroupId(String groupId) {
        Map query = JsonUtils.parseJsonString(String.format("""
                {
                    "query": {
                        "term": {"source_group_id": %s}
                    },
                    "sort": [{"created_at": {"order": "desc"}}],
                    "size": 100
                }
                """, groupId));
        return esClient.search(indexName, query);
    }

    public Map<String, Object> getGroupsSummary() {
        Map query = JsonUtils.parseJsonString("""
                   {
                    "size": 0,
                    "aggs": {
                        "group_by_source_group_id": {
                            "terms": {
                                "field": "source_group_id",
                                "size": 1000
                            },
                            "aggs": {
                                "by_status": {
                                    "terms": {
                                        "field": "status",
                                        "size": 10
                                    }
                                }
                            }
                        }
                    }
                }
                """);
        return esClient.search(indexName, query);
    }

    public void updateKbStatus(
            String dataId,
            String kbStatus,
            String kbIngestedAt,
            String kbErrorMessage,
            Map<String, Object> kbExtra
    ) {
        Map updateData = Map.of(
                "status", "kb_" + kbStatus,
                "kb_status", kbStatus,
                "kb_ingested_at", kbIngestedAt,
                "kb_error_message", kbErrorMessage,
                "kb_extra", kbExtra
        );
        esClient.update(indexName, dataId, updateData);
    }
}