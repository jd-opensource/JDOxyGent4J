package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;
import java.util.Map;

/**
 * Knowledge base document chunk item model
 * Converted from app/api/models.py
 */
@Data
public class KnowledgeChunkItem {
    private String kbId; // Knowledge base ID, example: d1686c75272e7ab78643367eb438751c
    private String oriFileId; // Original file ID, example: file_123456
    private String chunk_id; // Document chunk ID, example: chunk_789abc
    private String chunk_text; // Document chunk text content, example: This is a segment of the document...
    private Map<String, Object> chunkExtraData = Map.of(); // Document chunk extra data
    private String language; // Document chunk language, example: zh
    private String createTime = ""; // Knowledge base creation time, example: 2025-01-01 10:00:00
    private String updateTime = ""; // Knowledge base update time, example: 2025-01-01 10:00:00
}
