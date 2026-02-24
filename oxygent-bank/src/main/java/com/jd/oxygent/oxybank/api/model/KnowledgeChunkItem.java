package com.jd.oxygent.oxybank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeChunkItem {

    /**
     * Knowledge base ID
     */
    @JsonProperty("kb_id")
    private String kbId;

    /**
     * Original file ID
     */
    @JsonProperty("ori_file_id")
    private String oriFileId;

    /**
     * Document chunk ID
     */
    @JsonProperty("chunk_id")
    private String chunkId;

    /**
     * Document chunk text content
     */
    @JsonProperty("chunk_text")
    private String chunk_text;

    /**
     * Document chunk extra data
     */
    @JsonProperty("chunk_extra_data")
    private Map<String, Object> chunkExtraData = Map.of();

    /**
     * Document chunk language
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
