package com.jd.oxygent.oxybank.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Knowledge base file item model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeFileItem {

    /**
     * File ID
     */
    @JsonProperty("ori_file_id")
    private String oriFileId;

    /**
     * Knowledge base ID
     */
    @JsonProperty("kb_id")
    private String kbId;

    /**
     * File content MD5 value
     */
    @JsonProperty("document_md5")
    private String documentMd5;

    /**
     * File type
     */@JsonProperty("ori_file_type")
    private String oriFileType;

    /**
     * File name
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * File storage mode
     */
    @JsonProperty("file_store_mode")
    private String fileStoreMode;

    /**
     * File extra information
     */
    @JsonProperty("file_extra_info")
    private Map<String, Object> fileExtraInfo = Map.of();

    /**
     * File language
     */
    private String language;

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
}
