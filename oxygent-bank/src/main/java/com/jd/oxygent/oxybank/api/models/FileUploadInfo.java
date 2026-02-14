package com.jd.oxygent.oxybank.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * File upload response model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadInfo {

    /**
     * File ID
     */
    private String fileId = "";

    /**
     * File name
     */
    private String file_name;

    /**
     * File type
     */
    private String fileType;

    /**
     * File size (bytes)
     */
    private int fileSize;

    /**
     * File storage path
     */
    private String filePath;

    /**
     * File MD5 value
     */
    private String md5 = "";

    /**
     * Upload time
     */
    private String uploadTime = "";
}
