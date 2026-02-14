package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;
import java.util.Map;

/**
 * Knowledge base file item model
 * Converted from app/api/models.py
 */
@Data
public class KnowledgeFileItem {
    private String oriFileId; // File ID, example: file_123456
    private String kbId; // Knowledge base ID, example: d1686c75272e7ab78643367eb438751c
    private String document_md5; // File content MD5 value, example: e10adc3949ba59abbe56e057f20f883e
    private String oriFileType; // File type, example: pdf
    private String fileName; // File name, example: document.pdf
    private String fileStoreMode; // File storage mode, example: unstructured
    private Map<String, Object> fileExtraInfo = Map.of(); // File extra information
    private String language; // File language, example: zh
    private String createTime = ""; // Knowledge base creation time, example: 2025-01-01 10:00:00
    private String updateTime = ""; // Knowledge base update time, example: 2025-01-01 10:00:00
}
