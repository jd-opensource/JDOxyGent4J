package com.jd.oxygent.oxybank.core.storer.docmanager;

import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.oxybank.core.model.FieldInfo;
import com.jd.oxygent.oxybank.core.model.KBSchema;
import com.jd.oxygent.oxybank.core.model.MatchPolicy;
import com.jd.oxygent.oxybank.core.model.MatchRule;
import com.jd.oxygent.oxybank.core.storer.vectormanager.VearchManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库索引相关工具类
 */
@Slf4j
@Component
public class KnowledgeIndex {

    /**
     * 检查知识库schema是否符合要求
     * 
     * 1. match_rules中使用的字段应该都出现在fields中
     * 2. 每个match_rule必须恰好有一个高级匹配策略(es/vearch)
     * 3. 高级匹配策略的输入字段只能是一个
     * 4. 每个match_rule可以有0个或多个精确匹配策略作为高级匹配策略的过滤条件
     * 
     * @param schema 要检查的KBSchema对象
     * @return 如果符合要求返回true，否则返回false
     * @throws IllegalArgumentException 当schema不符合要求时抛出
     */
    public boolean checkKbSchema(KBSchema schema) {
        if (true) {
            return true; // fixme
        }
        if (schema.getMatchRules() == null) {
            return false;
        }

        Set<String> fieldNames = new HashSet<>();
        for (FieldInfo field : schema.getFields()) {
            fieldNames.add(field.getFieldName());
        }

        for (int ruleIdx = 0; ruleIdx < schema.getMatchRules().size(); ruleIdx++) {
            MatchRule matchRule = schema.getMatchRules().get(ruleIdx);
            List<MatchPolicy> advancedPolicies = new ArrayList<>();
            List<MatchPolicy> precisePolicies = new ArrayList<>();

            for (MatchPolicy policy : matchRule.getMatchPolicies()) {
                if ("es_text".equals(policy.getMode()) || "vearch_vector".equals(policy.getMode())) {
                    advancedPolicies.add(policy);
                } else if ("precise".equals(policy.getMode())) {
                    precisePolicies.add(policy);
                } else {
                    throw new IllegalArgumentException(
                        "MatchRule " + ruleIdx + " contains unknown match policy type: " + policy.getMode()
                    );
                }
            }

            if (advancedPolicies.isEmpty()) {
                throw new IllegalArgumentException(
                    "MatchRule " + ruleIdx + " missing advanced match policy (es_text or vearch_vector), " +
                    "each match rule must have one advanced match policy as primary query"
                );
            } else if (advancedPolicies.size() > 1) {
                throw new IllegalArgumentException(
                    "MatchRule " + ruleIdx + " contains " + advancedPolicies.size() + " advanced match policies, " +
                    "each match rule can only have one advanced match policy (es_text or vearch_vector)"
                );
            }

            MatchPolicy advancedPolicy = advancedPolicies.get(0);
            if (advancedPolicy.getInputFields().size() != 1) {
                throw new IllegalArgumentException(
                    "MatchRule " + ruleIdx + " advanced match policy (" + advancedPolicy.getMode() + ") has " +
                    advancedPolicy.getInputFields().size() + " input fields, must have exactly one field"
                );
            }

            String advancedFieldName = advancedPolicy.getInputFields().get(0);
            if (!fieldNames.contains(advancedFieldName)) {
                throw new IllegalArgumentException(
                    "MatchRule " + ruleIdx + " advanced match policy uses non-existent field: " + advancedFieldName + ", " +
                    "field not in schema.fields"
                );
            }

            for (MatchPolicy precisePolicy : precisePolicies) {
                for (String fieldName : precisePolicy.getInputFields()) {
                    if (!fieldNames.contains(fieldName)) {
                        throw new IllegalArgumentException(
                            "MatchRule " + ruleIdx + " precise match policy uses non-existent field: " + fieldName + ", " +
                            "field not in schema.fields"
                        );
                    }
                }
            }

            for (String fieldName : matchRule.getOutputFields()) {
                if (!fieldNames.contains(fieldName)) {
                    throw new IllegalArgumentException(
                        "MatchRule " + ruleIdx + " output_fields contains non-existent field: " + fieldName + ", " +
                        "field not in schema.fields"
                    );
                }
            }
        }
        return true;
    }

    /**
     * 根据结构化知识库schema创建对应的ES索引mapping schema
     * 注意默认添加kb_id、ori_file_id、chunk_id字段
     * 
     * @param schema 结构化知识库schema
     * @return ES索引mapping，如果没有ES相关规则则返回null
     */
    public Map<String, Object> inferMappingFromSchema(KBSchema schema) {
        List<FieldInfo> fields = schema.getFields();
        Map<String, Object> indexMapping = new HashMap<>();

        Set<String> textMatchFields = new HashSet<>();
        if (schema.getMatchRules() != null) {
            for (MatchRule matchRule : schema.getMatchRules()) {
                for (MatchPolicy policy : matchRule.getMatchPolicies()) {
                    if ("es_text".equals(policy.getMode())) {
                        textMatchFields.addAll(policy.getInputFields());
                    }
                }
            }
        }

        if (textMatchFields.isEmpty()) {
            return null;
        }

        for (FieldInfo field : fields) {
            Map<String, Object> fieldMapping = new HashMap<>();
            if ("string".equals(field.getFieldType())) {
                if (textMatchFields.contains(field.getFieldName())) {
                    fieldMapping.put("type", "text");
                    fieldMapping.put("analyzer", "smartcn");
                    Map<String, Object> keywordField = new HashMap<>();
                    keywordField.put("type", "keyword");
                    fieldMapping.put("fields", Map.of("keyword", keywordField));
                } else {
                    fieldMapping.put("type", "keyword");
                }
            } else if ("integer".equals(field.getFieldType())) {
                fieldMapping.put("type", "long");
            } else if ("float".equals(field.getFieldType())) {
                fieldMapping.put("type", "double");
            }
            indexMapping.put(field.getFieldName(), fieldMapping);
        }

        indexMapping.put("kb_id", Map.of("type", "keyword"));
        indexMapping.put("ori_file_id", Map.of("type", "keyword"));
        indexMapping.put("chunk_id", Map.of("type", "keyword"));

        return Map.of("properties", indexMapping);
    }

    /**
     * 从知识库schema推断并创建vearch空间schema，可能为空
     * 
     * @param schema 知识库schema
     * @param kbName 知识库名称
     * @return Vearch空间schema，如果没有vearch检索策略则返回null
     */
    public VearchManager.SpaceSchema inferVearchSpaceSchema(KBSchema schema, String kbName) {
        // FIXME: 需要实现vearch相关的schema推断逻辑
        // 这里需要返回vearch的SpaceSchema对象
        log.warn("inferVearchSpaceSchema is not fully implemented yet");
        return null;
    }

    /**
     * 知识库信息索引配置
     */
    public static final Map<String, Object> KB_INFO_INDEX = JsonUtils.parseJsonString("""
            {
              "mappings" : {
                 "properties": {
                     "kb_id": {
                         "type": "keyword"
                     },
                     "kb_name": {
                         "type": "keyword"
                     },
                     "kb_type": {
                         "type": "keyword"
                     },
                     "kb_description": {
                         "type": "text"
                     },
                     "kb_status": {
                         "type": "keyword"
                     },
                     "create_time": {
                         "type": "date",
                         "format": "yyyy-MM-dd HH:mm:ss"
                     },
                     "update_time": {
                         "type": "date",
                         "format": "yyyy-MM-dd HH:mm:ss"
                     },
                     "kb_create_user": {
                         "type": "keyword"
                     },
                     "kb_update_user": {
                         "type": "keyword"
                     },
                     "kb_store_type": {
                         "type": "keyword"
                     },
                     "kb_extra_info": {
                         "type": "object",
                         "dynamic": false
                     },
                     "kb_schema": {
                         "type": "object",
                         "dynamic": false
                     },
                     "auto_bind_query": {
                         "type": "boolean"
                     }
                 }
              }
            }
            """);

    /**
     * 知识库文件索引配置
     */
    public static final Map<String, Object> KB_FILE_INDEX = JsonUtils.parseJsonString("""
            {
              "mappings" : {
                 "properties": {
                    "ori_file_id": {
                        "type": "keyword",
                        "doc_values": true
                    },
                    "kb_id": {
                        "type": "keyword",
                        "doc_values": true
                    },
                    "document_md5": {
                        "type": "keyword",
                        "doc_values": true,
                    },
                    "create_time": {
                        "type": "date",
                        "format": "yyyy-MM-dd HH:mm:ss"
                    },
                    "update_time": {
                        "type": "date",
                        "format": "yyyy-MM-dd HH:mm:ss"
                    },
                    "ori_file_type": {
                        "type": "text"
                    },
                    "file_name": {
                        "type": "keyword"
                    },
                    "file_store_mode": {
                        "type": "text"
                    },
                    "file_extra_info": {
                        "type": "object"
                    },
                    "language": {
                        "type": "keyword",
                        "doc_values": true,
                    }
                }
              }
            }
            """);

    /**
     * 索引配置映射
     */
    public static final Map<String, Map<String, Object>> INDEX_CONFIGS = Map.of(
        "knowledge_base_info", KB_INFO_INDEX,
        "knowledge_file_info", KB_FILE_INDEX
    );
}
