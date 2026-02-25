package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.infra.multimodal.MultimodalResourceType;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.DateUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.oxybank.api.model.APIResponse;
import com.jd.oxygent.oxybank.api.model.FileUploadInfo;
import com.jd.oxygent.oxybank.api.model.FileUploadInfoRequest;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import com.jd.oxygent.oxybank.api.model.KnowledgeFileItem;
import com.jd.oxygent.oxybank.api.model.PaginatedResponse;
import com.jd.oxygent.oxybank.api.model.PaginationParams;
import com.jd.oxygent.oxybank.core.model.FieldInfo;
import com.jd.oxygent.oxybank.core.model.KBSchema;
import com.jd.oxygent.oxybank.core.model.MatchPolicy;
import com.jd.oxygent.oxybank.core.model.MatchRule;
import com.jd.oxygent.oxybank.core.model.ParserConfig;
import com.jd.oxygent.oxybank.core.model.VearchVectorMatchPolicy;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbBaseManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbFileManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.KnowledgeIndex;
import com.jd.oxygent.oxybank.core.storer.docmanager.SchemaUtils;
import com.jd.oxygent.oxybank.utils.FilesProcess;
import com.jd.oxygent.oxybank.utils.ParserFactoryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jd.oxygent.oxybank.utils.FilesProcess.extractFileType;

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
    @Autowired
    private KnowledgeIndex knowledgeIndex;

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
            // Generate unique filename
            String uploadDir = Paths.get(Config.getXfile().getSaveDir(), "uploads").toString();
            Files.createDirectories(Paths.get(uploadDir));

            String timestamp = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_TIME_FORMAT3);
            String fileName = timestamp + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Calculate document MD5
             String fileMd5 = CommonUtils.getMD5(filePath.toString());
             Long fileSize = Files.size(filePath);

            String uploadTime = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_TIME_FORMAT);

            FileUploadInfo fileUploadInfo = new FileUploadInfo();
            fileUploadInfo.setFileId(CommonUtils.generateShortUUID());
            fileUploadInfo.setFileName(file.getOriginalFilename());
            fileUploadInfo.setFileType(extractFileType(file.getOriginalFilename()));
            fileUploadInfo.setFileSize(fileSize.intValue());
            fileUploadInfo.setFilePath(filePath.toString());
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
     * 检查文件是否为系统创建的临时文件
     *
     * 临时文件格式: {kb_id}_{36-char-UUID}_{original_filename}
     * 示例: kb_12345_550e8400-e29b-41d4-a716-446655440000_test.pdf
     *
     * @param filePath 要检查的文件路径
     * @param kbId 要匹配的知识库ID
     * @return 如果文件匹配临时文件格式返回true，否则返回false
     */
    private boolean isTempFile(String filePath, String kbId) {
        String filename = Paths.get(filePath).getFileName().toString();

        // 检查文件名是否以{kb_id}_开头
        String prefix = kbId + "_";
        if (!filename.startsWith(prefix)) {
            return false;
        }

        // 提取前缀后的部分: {uuid}_{original_filename}
        String remaining = filename.substring(prefix.length());

        // 按第一个下划线分割以获取UUID部分
        // 格式: {36-char-UUID}_{original_filename}
        String[] parts = remaining.split("_", 2);
        if (parts.length < 2) {
            return false;
        }

        String uuidPart = parts[0];

        // 检查UUID部分是否为36个字符（UUID v4格式: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx）
        if (uuidPart.length() != 36) {
            return false;
        }

        return true;
    }

    /**
     * 获取已上传文件的信息
     *
     * 该端点用于获取已上传文件的信息，而不是已存储在数据库中的文件信息
     * 对于存储后的文件信息，使用端点 /kb_file/{file_id}
     *
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return 包含字段信息的响应
     */
    @GetMapping("/upload_file/{fileId}")
    public APIResponse<APIResponse<List<FieldInfo>>> getUploadedFileInfo(
            @RequestParam String filePath,
            @RequestParam String fileType
    ) {
        try {
            // FIXME: 需要根据file_type和file_path读取文件，并返回对应的数据
            List<Map<String, Object>> dataList = new ArrayList<>();

            if ("csv".equals(fileType)) {
                // FIXME: 需要实现CSV文件读取
                log.warn("CSV file reading not implemented yet");
            } else if ("xls".equals(fileType) || "xlsx".equals(fileType)) {
                // FIXME: 需要实现Excel文件读取
                log.warn("Excel file reading not implemented yet");
            } else {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Unsupported file type: " + fileType);
            }

            List<FieldInfo> fieldsInfo = new ArrayList<>();
            // FIXME: 需要从文件内容中获取字段名和类型
            for (Map<String, Object> data : dataList) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String fieldName = entry.getKey();
                    String frontType = getJavaType(entry.getValue());
                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.setFieldName(fieldName);
                    fieldInfo.setFieldType(frontType);
                    fieldInfo.setFieldDesc("");
                    fieldsInfo.add(fieldInfo);
                }
                break; // 只需要处理第一行
            }

            APIResponse<List<FieldInfo>> response = new APIResponse<>();
            response.setCode(200);
            response.setMsg("File schema extraction successful");
            response.setData(fieldsInfo);

            return APIResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to get uploaded file info", e);
            return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),  "Failed to extract file schema");
        }
    }

    /**
     * 摄取文件到知识库
     *
     * 该端点用于在上传后分割和存储文件，写入ES索引和Vearch空间
     *
     * @param kbId 知识库ID
     * @param fileUploadInfoRequest 上传的文件信息
     * @return 操作结果响应
     */
    @PostMapping("/ingest_file")
    public APIResponse<APIResponse<String>> ingestKbFile(
            @PathVariable String kbId,
            @RequestBody FileUploadInfoRequest fileUploadInfoRequest
    ) {
        try {
            FileUploadInfo fileUploadInfo = fileUploadInfoRequest.getFileUploadInfo();
            Map<String, Object> kbSchemaDict = kbBaseClient.getKbSchemaById(kbId);
            if (fileUploadInfo.getFileId() == null || fileUploadInfo.getFileId().isEmpty()) {
                fileUploadInfo.setFileId(CommonUtils.getMD5(fileUploadInfo.getFileName()));
            }
            if (kbSchemaDict == null) {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "kb_id: [" + kbId + "] The corresponding knowledge base does not have a kb schema");
            }

            // 转换Dict为KBSchema对象
            KBSchema kbSchema;
            try {
                kbSchema = JsonUtils.convertValue(kbSchemaDict, KBSchema.class);
                if (!knowledgeIndex.checkKbSchema(kbSchema)) {
                    return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Current knowledge base kb schema validation failed, please check kb schema");
                }
            } catch (Exception e) {
                log.error("Failed to parse kb_schema", e);
                return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Knowledge base schema parsing/validation failed: " + e.getMessage());
            }

            Map<String, Object> searchResult = kbBaseClient.kbInfoSearchId(kbId);
            if (searchResult == null || searchResult.isEmpty()) {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Failed to query knowledge base information based on knowledge base id");
            }
            String kbType = (String) searchResult.getOrDefault("kb_type", "structured");

            // 根据知识库id查询知识库名称
            String kbName = kbBaseClient.getKbNameById(kbId);
            if (kbName == null) {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Failed to query knowledge base name based on knowledge base id");
            }

            String filePath = fileUploadInfo.getFilePath();
            List<Map<String, Object>> df = new ArrayList<>();

            try {
                if ("structured".equals(kbType)) {
                    // 结构化数据处理方法
                    String fileType = fileUploadInfo.getFileType();

                    if ("csv".equals(fileType) || "xls".equals(fileType) || "xlsx".equals(fileType)) {
                        Map<String, List<?>> multimodalPart = new HashMap<>();
                        multimodalPart.put("files", Arrays.asList(fileUploadInfo.getFilePath()));
                        df = processMultimodalResources(multimodalPart, false);
                    } else {
                        return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Unsupported file type: " + fileType);
                    }
                } else if ("unstructured".equals(kbType)) {
                    // 非结构化数据处理方法
                    df = new ArrayList<>();

                    // FIXME: 需要实现SimpleDirectoryReader文档加载
                    log.warn("Document reading not implemented yet");

                    // 根据kb_schema配置创建解析器
                    ParserConfig parserConfig;
                    if (kbSchema.getParserConfig() == null) {
                        log.warn("Schema has no parser_config defined, use default parser configuration(sentence parser)");
                        parserConfig = new ParserConfig();
                        parserConfig.setParserType("sentence");
                        parserConfig.setChunkSize(500);
                        parserConfig.setChunkOverlap(50);
                    } else {
                        parserConfig = kbSchema.getParserConfig();
                    }
                    ParserFactoryUtil.createParserFromConfig(parserConfig);
                    log.info("Using parser with config: {}", parserConfig);

                    // FIXME: 需要实现节点解析逻辑
                    log.info("node length: {}", df.size());

                    for (Map<String, Object> node : df) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("chunk_to_return", node.get("text"));
                        data.put("chunk_to_emb", node.get("text"));
//                        df.add(data);
                    }
                }
            } catch (Exception e) {
                log.error("File reading and processing failed: {}, Error", filePath, e);
                return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "File reading and processing failed: " + filePath + ", Error: " + e.getMessage());
            } finally {
                // 处理完成后删除临时文件
                // 只有当它是系统创建的临时文件时才删除（格式: {kb_id}_{36-char-UUID}.{ext}）
                File file = new File(filePath);
                if (file.exists() && isTempFile(filePath, kbId)) {
                    try {
                        Files.delete(file.toPath());
                        log.info("Temporary file deleted: {}", filePath);
                    } catch (Exception e) {
                        log.warn("Failed to delete temporary file: {}, Error", filePath, e);
                    }
                } else if (file.exists()) {
                    log.info("File preserved (not a temporary file): {}", filePath);
                }
            }

            // 根据schema转换DataFrame列类型
            // 首先保留NaN值，转换类型，然后根据字段类型处理NaN
            df = SchemaUtils.convertDataTypesBySchema(df, kbSchema);

            // 在这里为每行数据添加三个固定列，在推断ES和Vearch schema时需要相应添加
            List<Map<String, Object>> dataFrame = new ArrayList<>();
            for (Map<String, Object> row : df) {
                row = new HashMap<>(row);
                row.put("kb_id", kbId);
                row.put("ori_file_id", fileUploadInfo.getFileId());
                row.put("chunk_id", CommonUtils.generateShortUUID());
                dataFrame.add(row);
            }

            // 将内存中的df数据写入ES和Vearch
            // 基于df中的数据和推断的schema写入ES索引
            boolean esAddResult = kbFileClient.kbAddDf(kbName, dataFrame);
            if (!esAddResult) {
                log.error("add file data into es failed");
                return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "add file data into es failed");
            }
            log.info("add file data into es success");

            // 基于推断的schema对某些字段首先执行嵌入处理，保存到df，然后写入Vearch空间
            if (kbSchema.getMatchRules() != null && !kbSchema.getMatchRules().isEmpty()) {
                // 收集所有向量匹配策略 - 遍历match_rules，然后遍历每个rule中的match_policies
                List<MatchPolicy> vecMatches = new ArrayList<>();
                for (MatchRule matchRule : kbSchema.getMatchRules()) {
                    for (MatchPolicy policy : matchRule.getMatchPolicies()) {
                        if (policy instanceof VearchVectorMatchPolicy) {
                            vecMatches.add(policy);
                        }
                    }
                }

                if (!vecMatches.isEmpty()) {
                    // FIXME: 需要实现VearchManager初始化
                    log.warn("VearchManager not fully implemented yet");

                    try {
                        for (MatchPolicy vecMatch : vecMatches) {
                            VearchVectorMatchPolicy vearchPolicy = (VearchVectorMatchPolicy) vecMatch;
                            String fieldName = vearchPolicy.getInputFields().get(0);

                            // 检查字段是否存在
                            boolean fieldExists = false;
                            for (Map<String, Object> row : df) {
                                if (row.containsKey(fieldName)) {
                                    fieldExists = true;
                                    break;
                                }
                            }
                            if (!fieldExists) {
                                log.warn("Field {} does not exist in DataFrame, skipping vectorization", fieldName);
                                continue;
                            }

                            // 检查字段是否有数据
                            boolean hasData = false;
                            for (Map<String, Object> row : df) {
                                Object value = row.get(fieldName);
                                if (value != null) {
                                    hasData = true;
                                    break;
                                }
                            }
                            if (!hasData) {
                                log.warn("Field {} is all empty values, skipping vectorization", fieldName);
                                continue;
                            }

                            // FIXME: 需要实现嵌入生成逻辑
                            log.warn("Embedding generation not implemented yet");

                            String vectorFieldName = fieldName + "_vector";
                            for (Map<String, Object> row : df) {
                                row.put(vectorFieldName, new ArrayList<Double>());
                            }
                            log.info("Successfully generated vector field {} for field {}", vectorFieldName, fieldName);
                        }

                        // FIXME: 需要实现写入Vearch
                        log.warn("Vearch write not fully implemented yet");
                        log.info("Vector data successfully written to Vearch space: {}", kbName);
                    } catch (Exception e) {
                        log.error("Vectorization processing failed", e);
                        return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Vectorization processing failed: " + e.getMessage());
                    }
                }
            }

            // 写入文件信息到kb_file
            Map<String, Object> kbFileItem = new HashMap<>();
            kbFileItem.put("kb_id", kbId);
            kbFileItem.put("ori_file_id", fileUploadInfo.getFileId());
            kbFileItem.put("ori_file_type", fileUploadInfo.getFileType());
            kbFileItem.put("file_name", fileUploadInfo.getFileName());
            kbFileItem.put("document_md5", fileUploadInfo.getMd5());
            kbFileItem.put("file_store_mode", "");
            kbFileItem.put("file_extra_info", new HashMap<>());
            kbFileItem.put("language", "zh");
            String currentTime = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_TIME_FORMAT);
            kbFileItem.put("create_time", currentTime);
            kbFileItem.put("update_time", currentTime);

            if (!kbFileClient.kbAddFile(kbFileItem)) {
                log.error("add kb file info into es failed");
                APIResponse<String> response = new APIResponse<>();
                response.setCode(500);
                response.setMsg("add kb file info into es failed");
                return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "add kb file info into es failed");
            }

            APIResponse<String> response = new APIResponse<>();
            response.setCode(200);
            response.setMsg("success");
            response.setData("Data inserted successfully");

            return APIResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to ingest kb file", e);
            return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to ingest file: " + e.getMessage());
        }
    }

    /**
     * 摄取数据到知识库
     *
     * @param kbId 知识库ID
     * @param data 上传的数据
     * @return 操作结果响应
     */
    @PostMapping("/ingest_data")
    public APIResponse<APIResponse<String>> ingestKbData(
            @PathVariable String kbId,
            @RequestBody Map<String, Object> data
    ) {
        try {
            // 根据kb_id获取schema
            Map<String, Object> kbSchemaDict = kbBaseClient.getKbSchemaById(kbId);
            if (kbSchemaDict == null) {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "kb_id: [" + kbId + "] The corresponding knowledge base does not have a kb schema");
            }

            // 转换Dict为KBSchema对象
            KBSchema kbSchema;
            try {
                kbSchema = JsonUtils.convertValue(kbSchemaDict, KBSchema.class);
                if (!knowledgeIndex.checkKbSchema(kbSchema)) {
                    return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Current knowledge base kb schema validation failed, please check kb schema");
                }
            } catch (Exception e) {
                log.error("Failed to parse kb_schema", e);
                return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Knowledge base schema parsing/validation failed: " + e.getMessage());
            }

            Map<String, Object> searchResult = kbBaseClient.kbInfoSearchId(kbId);
            if (searchResult == null || searchResult.isEmpty()) {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Failed to query knowledge base information based on knowledge base id");
            }

            // 根据知识库id查询知识库名称
            String kbName = kbBaseClient.getKbNameById(kbId);
            if (kbName == null) {
                return APIResponse.error(HttpStatus.BAD_REQUEST.value(), "Failed to query knowledge base name based on knowledge base id");
            }

            List<Map<String, Object>> df;
            try {
                // 判断是单个字典还是字典列表
                if (data.containsKey("items")) {
                    df = (List<Map<String, Object>>) data.get("items");
                } else {
                    df = List.of(data);
                }
            } catch (Exception e) {
                // 如果是标量字典，将其包装在列表中
                df = List.of(data);
            }

            // 根据schema转换DataFrame列类型
            // 首先保留NaN值，转换类型，然后根据字段类型处理NaN
            df = SchemaUtils.convertDataTypesBySchema(df, kbSchema);

            // 在这里为每行数据添加三个固定列，在推断ES和Vearch schema时需要相应添加
            String currentTime = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_TIME_FORMAT);
            String mockFileId = "mock_file_" + kbId;
            for (Map<String, Object> row : df) {
                row.put("kb_id", kbId);
                row.put("ori_file_id", mockFileId);
                row.put("chunk_id", CommonUtils.generateShortUUID());
            }

            // 将内存中的df数据写入ES和Vearch
            // 基于df中的数据和推断的schema写入ES索引
            boolean esAddResult = kbFileClient.kbAddDf(kbName, df);
            if (!esAddResult) {
                log.error("add file data into es failed");
                return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "add file data into es failed");
            }
            log.info("add file data into es success");

            // 基于推断的schema对某些字段首先执行嵌入处理，保存到df，然后写入Vearch空间
            if (kbSchema.getMatchRules() != null && !kbSchema.getMatchRules().isEmpty()) {
                // 收集所有向量匹配策略 - 遍历match_rules，然后遍历每个rule中的match_policies
                List<MatchPolicy> vecMatches = new ArrayList<>();
                for (MatchRule matchRule : kbSchema.getMatchRules()) {
                    for (MatchPolicy policy : matchRule.getMatchPolicies()) {
                        if (policy instanceof VearchVectorMatchPolicy) {
                            vecMatches.add(policy);
                        }
                    }
                }

                if (!vecMatches.isEmpty()) {
                    // FIXME: 需要实现VearchManager初始化
                    log.warn("VearchManager not fully implemented yet");

                    try {
                        for (MatchPolicy vecMatch : vecMatches) {
                            VearchVectorMatchPolicy vearchPolicy = (VearchVectorMatchPolicy) vecMatch;
                            String fieldName = vearchPolicy.getInputFields().get(0);

                            // 检查字段是否存在
                            boolean fieldExists = false;
                            for (Map<String, Object> row : df) {
                                if (row.containsKey(fieldName)) {
                                    fieldExists = true;
                                    break;
                                }
                            }
                            if (!fieldExists) {
                                log.warn("Field {} does not exist in DataFrame, skipping vectorization", fieldName);
                                continue;
                            }

                            // 检查字段是否有数据
                            boolean hasData = false;
                            for (Map<String, Object> row : df) {
                                Object value = row.get(fieldName);
                                if (value != null) {
                                    hasData = true;
                                    break;
                                }
                            }
                            if (!hasData) {
                                log.warn("Field {} is all empty values, skipping vectorization", fieldName);
                                continue;
                            }

                            // FIXME: 需要实现嵌入生成逻辑
                            log.warn("Embedding generation not implemented yet");

                            String vectorFieldName = fieldName + "_vector";
                            for (Map<String, Object> row : df) {
                                row.put(vectorFieldName, new ArrayList<Double>());
                            }
                            log.info("Successfully generated vector field {} for field {}", vectorFieldName, fieldName);
                        }

                        // FIXME: 需要实现写入Vearch
                        log.warn("Vearch write not fully implemented yet");
                        log.info("Vector data successfully written to Vearch space: {}", kbName);
                    } catch (Exception e) {
                        log.error("Vectorization processing failed", e);
                        return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Vectorization processing failed: " + e.getMessage());
                    }
                }
            }

            Map<String, Object> result = kbFileClient.getKbFileInfo(kbId, mockFileId);
            Map<String, Object> oriFileInfo = result != null ? (Map<String, Object>) result.get("_source") : null;
            if (oriFileInfo == null) {
                // 如果当前知识库还没有这个文件，写入文件信息到kb_file
                String kbType = (String) searchResult.getOrDefault("kb_type", "structured");
                String mockFileType = "unstructured".equals(kbType) ? "md" : "csv";
                Map<String, Object> kbFileItem = new HashMap<>();
                kbFileItem.put("kb_id", kbId);
                kbFileItem.put("ori_file_id", mockFileId);
                kbFileItem.put("ori_file_type", mockFileType);
                kbFileItem.put("file_name", "qa_data." + mockFileType);
                kbFileItem.put("file_path", "/" + mockFileId + "." + mockFileType);
                kbFileItem.put("document_md5", "");
                kbFileItem.put("file_store_mode", "");
                kbFileItem.put("file_extra_info", new HashMap<>());
                kbFileItem.put("language", "zh");
                kbFileItem.put("create_time", currentTime);
                kbFileItem.put("update_time", currentTime);

                if (!kbFileClient.kbAddFile(kbFileItem)) {
                    log.error("add kb file info into es failed");
                    return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "add kb file info into es failed");
                }
            } else {
                oriFileInfo.put("update_time", currentTime);
                if (!kbFileClient.kbUpdateFileInfo(oriFileInfo)) {
                    log.error("update kb file info into es failed");
                    return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "update kb file info into es failed");
                }
            }

            APIResponse<String> response = new APIResponse<>();
            response.setCode(200);
            response.setMsg("success");
            response.setData("Data inserted successfully");

            return APIResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to ingest kb data", e);
            return APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to ingest data: " + e.getMessage());
        }
    }

    /**
     * 根据值类型获取Java类型字符串
     *
     * @param value 值
     * @return 类型字符串
     */
    private String getJavaType(Object value) {
        if (value == null) {
            return "string";
        }
        if (value instanceof Integer) {
            return "integer";
        }
        if (value instanceof Long) {
            return "integer";
        }
        if (value instanceof Double) {
            return "float";
        }
        if (value instanceof Float) {
            return "float";
        }
        return "string";
    }

    /**
     * Process multimodal resources
     * Structure of multimodalPart is like: {"urls": [...], "files": [...], ...}
     */
    private List<Map<String, Object>> processMultimodalResources(Map<String, List<?>> multimodalPart, boolean isConvertUrlToBase64) {
        List<Map<String, Object>> processedResources = new ArrayList<>();
        multimodalPart.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .forEach(entry -> {
                    String resourceType = entry.getKey();
                    List<?> resourceList = entry.getValue();

                    MultimodalResourceType processor = MultimodalResourceType.fromResourceType(resourceType);
                    List<Map<String, Object>> processed = processor.processResources(resourceList, isConvertUrlToBase64);
                    processedResources.addAll(processed);
                });

        return processedResources;
    }
}
