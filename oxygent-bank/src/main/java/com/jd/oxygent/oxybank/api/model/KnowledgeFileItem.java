package com.jd.oxygent.oxybank.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private String oriFileId;

    /**
     * Knowledge base ID
     */
    private String kbId;

    /**
     * File content MD5 value
     */
    private String documentMd5;

    /**
     * File type
     */
    private String oriFileType;

    /**
     * File name
     */
    private String fileName;

    /**
     * File storage mode
     */
    private String fileStoreMode;

    /**
     * File extra information
     */
    private Map<String, Object> fileExtraInfo = Map.of();

    /**
     * File language
     */
    private String language;

    /**
     * Knowledge base creation time
     */
    private String createTime = "";

    /**
     * Knowledge base update time
     */
    private String updateTime = "";
}
