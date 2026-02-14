package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;

/**
 * File upload response model
 * Converted from app/api/models.py
 */
@Data
public class FileUploadInfo {
    private String fileId = ""; // File ID
    private String file_name; // File name
    private String fileType; // File type
    private int fileSize = 0; // File size (bytes)
    private String filePath; // File storage path
    private String md5 = ""; // File MD5 value
    private String uploadTime = ""; // Upload time
}
