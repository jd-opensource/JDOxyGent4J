package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Knowledge base item model
 * Converted from app/api/models.py
 */
@Data
public class KnowledgeBaseItem {
    private String kbId = ""; // Knowledge base ID, example: d1686c75272e7ab78643367eb438751c
    private String kbName; // Knowledge base name, example: kb_test1
    private String kbType; // Knowledge base type, example: unstructured
    private String kbDescription = ""; // Knowledge base description, example: this is kb kb_test1, file_path is xxx, kb_id is c1caa264-ce70-4544-8ffa-108dac1d6f64
    private String kbStatus = "active"; // Knowledge base status, example: active
    private String createTime = ""; // Knowledge base creation time, example: 2025-01-01 10:00:00
    private String updateTime = ""; // Knowledge base update time, example: 2025-01-01 10:00:00
    private String kbCreateUser = ""; // Knowledge base creator, example: bob
    private String kbUpdateUser = ""; // Knowledge base updater, example: bob
    private List<String> kbStoreType = List.of(); // Knowledge base storage type, example: [elasticsearch]
    private Map<String, Object> kbExtraInfo = Map.of(); // Knowledge base extra information
    private Map<String, Object> kbSchema = Map.of(); // Knowledge base schema information
    private boolean autoBindQuery = true; // Whether to automatically bind query interfaces for retrieval strategies on restart
}
