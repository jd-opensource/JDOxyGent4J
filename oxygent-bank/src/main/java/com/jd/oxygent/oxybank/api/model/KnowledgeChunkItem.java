package com.jd.oxygent.oxybank.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Knowledge base document chunk item model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeChunkItem {

    /**
     * Knowledge base ID
     */
    private String kbId;

    /**
     * Original file ID
     */
    private String oriFileId;

    /**
     * Document chunk ID
     */
    private String chunkId;

    /**
     * Document chunk text content
     */
    private String chunk_text;

    /**
     * Document chunk extra data
     */
    private Map<String, Object> chunkExtraData = Map.of();

    /**
     * Document chunk language
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
