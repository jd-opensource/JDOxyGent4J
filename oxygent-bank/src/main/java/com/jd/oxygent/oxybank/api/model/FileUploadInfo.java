package com.jd.oxygent.oxybank.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("file_id")
    private String fileId = "";

    /**
     * File name
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * File type
     */
    @JsonProperty("file_type")
    private String fileType;

    /**
     * File size (bytes)
     */
    @JsonProperty("file_size")
    private int fileSize;

    /**
     * File storage path
     */
    @JsonProperty("file_path")
    private String filePath;

    /**
     * File MD5 value
     */
    private String md5 = "";

    /**
     * Upload time
     */
    @JsonProperty("upload_time")
    private String uploadTime = "";
}
