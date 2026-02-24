package com.jd.oxygent.oxybank.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Knowledge base item model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeBaseItem {

    /**
     * Knowledge base ID
     */
    @JsonProperty("kb_id")
    private String kbId = "";

    /**
     * Knowledge base name
     */
    @JsonProperty("kb_name")
    private String kbName;

    /**
     * Knowledge base type
     */
    @JsonProperty("kb_type")
    private String kbType;

    /**
     * Knowledge base description
     */
    @JsonProperty("kb_description")
    private String kbDescription = "";

    /**
     * Knowledge base status
     */
    @JsonProperty("kb_status")
    private String kbStatus = "active";

    /**
     * Knowledge base creation time
     */
    @JsonProperty("create_time")
    private String createTime = "";

    /**
     * Knowledge base update time
     */
    @JsonProperty("update_time")
    private String updateTime = "";

    /**
     * Knowledge base creator
     */
    @JsonProperty("kb_create_user")
    private String kbCreateUser = "";

    /**
     * Knowledge base updater
     */
    @JsonProperty("kb_update_user")
    private String kbUpdateUser = "";

    /**
     * Knowledge base storage type
     */
    @JsonProperty("kb_store_type")
    private List<String> kbStoreType = List.of();

    /**
     * Knowledge base extra information
     */
    @JsonProperty("kb_extra_info")
    private Map<String, Object> kbExtraInfo = Map.of();

    /**
     * Knowledge base schema information
     */
    @JsonProperty("kb_schema")
    private Map<String, Object> kbSchema = Map.of();

    /**
     * Whether to automatically bind query interfaces for retrieval strategies on restart
     */
    @JsonProperty("auto_bind_query")
    private boolean autoBindQuery = true;
}
