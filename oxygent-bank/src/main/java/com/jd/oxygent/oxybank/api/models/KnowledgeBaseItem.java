package com.jd.oxygent.oxybank.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private String kbId = "";

    /**
     * Knowledge base name
     */
    private String kbName;

    /**
     * Knowledge base type
     */
    private String kbType;

    /**
     * Knowledge base description
     */
    private String kbDescription = "";

    /**
     * Knowledge base status
     */
    private String kbStatus = "active";

    /**
     * Knowledge base creation time
     */
    private String createTime = "";

    /**
     * Knowledge base update time
     */
    private String updateTime = "";

    /**
     * Knowledge base creator
     */
    private String kbCreateUser = "";

    /**
     * Knowledge base updater
     */
    private String kbUpdateUser = "";

    /**
     * Knowledge base storage type
     */
    private List<String> kbStoreType = List.of();

    /**
     * Knowledge base extra information
     */
    private Map<String, Object> kbExtraInfo = Map.of();

    /**
     * Knowledge base schema information
     */
    private Map<String, Object> kbSchema = Map.of();

    /**
     * Whether to automatically bind query interfaces for retrieval strategies on restart
     */
    private boolean autoBindQuery = true;
}
