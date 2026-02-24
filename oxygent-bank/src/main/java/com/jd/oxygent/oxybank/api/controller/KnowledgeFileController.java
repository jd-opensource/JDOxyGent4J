package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.DateUtils;
import com.jd.oxygent.oxybank.api.model.APIResponse;
import com.jd.oxygent.oxybank.api.model.FileUploadInfo;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import com.jd.oxygent.oxybank.api.model.KnowledgeFileItem;
import com.jd.oxygent.oxybank.api.model.PaginatedResponse;
import com.jd.oxygent.oxybank.api.model.PaginationParams;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbBaseManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbFileManager;
import com.jd.oxygent.oxybank.utils.FilesProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Knowledge Base File Controller
 * <p>
 * Knowledge Base File Management API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kb_base/{kbId}")
public class KnowledgeFileController {

    @Autowired
    private ElasticsearchKbBaseManager kbBaseClient;
    @Autowired
    private ElasticsearchKbFileManager kbFileClient;

    /**
     * Get all files in the knowledge base
     * <p>
     * Returns information about all files in the specified knowledge base, including:
     * - oriFileId: Unique identifier of the file in the knowledge base
     * - kbId: Unique identifier of the knowledge base
     * - documentMd5: MD5 hash value of the file content
     * - oriFileType: File type/extension
     * - filePath: Absolute path of the file
     * - fileStoreMode: File storage mode
     * - fileExtraInfo: Additional information of the file
     * - language: Language identifier of the file
     *
     * @param kbId      Knowledge base ID
     * @param pagination Pagination parameters
     * @return APIResponse containing paginated file list
     */
    @GetMapping("/kb_file")
    public APIResponse<PaginatedResponse<KnowledgeFileItem>> getKbFiles(
            @PathVariable String kbId,
            @ModelAttribute PaginationParams pagination) {
        try {
            // Implement get_kb_files method
             Map<String, Object> result = kbFileClient.getKbFiles(
                 kbId,
                 pagination.getPage(),
                 pagination.getSize()
             );
            return APIResponse.success("Successfully retrieved file list",
                    new PaginatedResponse(
                            (List<KnowledgeBaseItem>) result.get("items"),
                            Integer.parseInt(result.get("total").toString()),
                            Integer.parseInt(result.get("page").toString()),
                            Integer.parseInt(result.get("size").toString()),
                            Integer.parseInt(result.get("pages").toString())
                    ));
        } catch (IllegalArgumentException e) {
            log.warn("Get files failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get files", e);
            return APIResponse.error(500, "Failed to get files");
        }
    }

    /**
     * Upload a single file to a preset directory
     * <p>
     * Upload a single file and return the file's MD5 information
     *
     * @param kbId      Knowledge base ID
     * @param file       File to upload
     * @return APIResponse containing file upload info
     */
    @PostMapping("/upload_file")
    public APIResponse<FileUploadInfo> uploadKbFile(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file) {
        try {
            // File name validation, cannot be empty
            if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
                return APIResponse.error(400, "File name cannot be empty");
            }

            // File type validation, check if type is supported
             if (!FilesProcess.isSupportedFile(file.getOriginalFilename(), Set.of(
                     "txt",
                     "md",
                     "markdown",
                     "rst",
                     "csv",
                     "xlsx",
                     "xls",
                     "pdf",
                     "docx",
                     "doc"
             ))) {
                 return APIResponse.error(400, "Unsupported file type: " + extractFileType(file.getOriginalFilename()));
             }

            // Query knowledge base related information
             Map<String, Object> kbSearchResult = kbBaseClient.kbInfoSearchId(kbId);
             if (kbSearchResult == null) {
                 return APIResponse.error(400, "Knowledge base ID does not exist: " + kbId);
             }
            Path path = Paths.get(Config.getXfile().getSaveDir(), CommonUtils.generateShortUUID());
            // Calculate document MD5
             String fileMd5 = CommonUtils.getMD5(path.toString());
             Long fileSize = Files.size(path);

            String uploadTime = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_TIME_FORMAT);

            FileUploadInfo fileUploadInfo = new FileUploadInfo();
            fileUploadInfo.setFileId(CommonUtils.generateShortUUID());
            fileUploadInfo.setFileName(file.getOriginalFilename());
            fileUploadInfo.setFileType(extractFileType(file.getOriginalFilename()));
            fileUploadInfo.setFileSize(fileSize.intValue());
            fileUploadInfo.setFilePath(path.toString());
            fileUploadInfo.setMd5(fileMd5);
            fileUploadInfo.setUploadTime(uploadTime);

            return APIResponse.success("File uploaded successfully", fileUploadInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Upload file failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            return APIResponse.error(500, "Failed to upload file");
        }
    }

    /**
     * Extract file type from filename
     *
     * @param filename File name
     * @return File type
     */
    private String extractFileType(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}
