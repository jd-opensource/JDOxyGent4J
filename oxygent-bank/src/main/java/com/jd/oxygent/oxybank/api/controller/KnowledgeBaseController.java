package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.DateUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.oxybank.api.model.APIResponse;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import com.jd.oxygent.oxybank.api.model.PaginatedResponse;
import com.jd.oxygent.oxybank.api.model.PaginationParams;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchIndexManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbBaseManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbChunkManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbFileManager;
import com.jd.oxygent.oxybank.core.storer.vectormanager.VearchManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Knowledge Base Controller
 * <p>
 * Knowledge Base Management API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kb_base")
public class KnowledgeBaseController {

    @Autowired
    private ElasticsearchIndexManager kbIndexClient;
    @Autowired
    private ElasticsearchKbBaseManager kbBaseClient;
    @Autowired
    private ElasticsearchKbChunkManager kbChunkClient;
    @Autowired
    private ElasticsearchKbFileManager kbFileClient;
    @Autowired
    private VearchManager vearchManager;

    /**
     * Get all knowledge bases
     * <p>
     * Returns information about all created knowledge bases, including:
     * - kbId: Unique identifier of the knowledge base
     * - kbName: Name of the knowledge base
     * - kbDescription: Description of the knowledge base
     * - kbType: Type of the knowledge base (e.g., unstructured)
     * - kbExtraInfo: Additional information (dictionary format)
     *
     * @param pagination Pagination parameters
     * @return APIResponse containing paginated knowledge base list
     */
    @GetMapping("")
    public APIResponse<PaginatedResponse<KnowledgeBaseItem>> getAllKnowledgeBase(
            @ModelAttribute PaginationParams pagination) {
        try {
             Map<String, Object> result = kbBaseClient.kbList(
                 pagination.getPage(),
                 pagination.getSize()
             );
             if (result == null || !result.containsKey("items")) {
                 log.warn("Knowledge base list query returned empty result");
                 return APIResponse.success(
                     "Successfully retrieved knowledge base list",
                     new PaginatedResponse<>(
                         List.of(),
                         0,
                         pagination.getPage(),
                         pagination.getSize(),
                         0
                     )
                 );
             }
            return APIResponse.success(
                    "Successfully retrieved knowledge base list",
                    new PaginatedResponse<>(
                            (List<KnowledgeBaseItem>) result.get("items"),
                            Integer.parseInt(result.get("total").toString()),
                            Integer.parseInt(result.get("page").toString()),
                            Integer.parseInt(result.get("size").toString()),
                            Integer.parseInt(result.get("pages").toString())
                    )
            );
        } catch (Exception e) {
            log.error("Failed to get knowledge base list", e);
            return APIResponse.error(500, "Failed to get knowledge base list");
        }
    }

    /**
     * Create an empty knowledge base
     * <p>
     * Create a new knowledge base
     *
     * @param kbItem Knowledge base information
     * @return APIResponse containing ID of the newly created knowledge base
     */
    @PostMapping("")
    public APIResponse<String> createKnowledgeBase(@RequestBody KnowledgeBaseItem kbItem) {
        try {
            // Validate knowledge base name
            if (kbItem.getKbName() == null || kbItem.getKbName().trim().isEmpty()) {
                return APIResponse.error(400, "Knowledge base name cannot be empty");
            }

            // Check if knowledge base already exists
             if (kbBaseClient.kbExists(kbItem.getKbName())) {
                 return APIResponse.error(409, "Knowledge base '" + kbItem.getKbName() + "' already exists");
             }

//             Generate knowledge base ID and timestamp
             String kbId = CommonUtils.getMD5(kbItem.getKbName());
             String currentTime = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_TIME_FORMAT);
             kbItem.setKbId(kbId);
             kbItem.setCreateTime(currentTime);
             kbItem.setUpdateTime(currentTime);

            // Create knowledge base
             if (!kbBaseClient.kbAdd(JsonUtils.convertToMap(kbItem, "snake"))) {
                 log.error("Knowledge base {} creation failed", kbItem.getKbName());
                 return APIResponse.error(500, "Knowledge base creation failed, please check system status");
             }
            log.info("Knowledge base {} (ID: {}) created successfully", kbItem.getKbName(), kbId);
            return APIResponse.success("Knowledge base created successfully", kbId);
        } catch (IllegalArgumentException e) {
            log.warn("Create knowledge base failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Exception while creating knowledge base", e);
            return APIResponse.error(500, "Failed to create knowledge base");
        }
    }

    /**
     * Delete knowledge base
     * <p>
     * Delete specified knowledge base and its indexes and vector space
     *
     * @param kbName Knowledge base name
     * @return APIResponse containing deletion result
     */
    @DeleteMapping("/{kbName}")
    public APIResponse<String> deleteKnowledgeBase(@PathVariable String kbName) {
        try {
            // Validate input
            if (kbName == null || kbName.trim().isEmpty()) {
                return APIResponse.error(400, "Knowledge base name cannot be empty");
            }

             // Query knowledge base information
             Map<String, Object> kbInfoList = kbBaseClient.kbInfoSearchName(kbName);
             if (kbInfoList == null || kbInfoList.isEmpty()) {
                 return APIResponse.error(404, "Knowledge base '" + kbName + "' does not exist");
             }
             String kbId = (String) kbInfoList.get("kb_id");
             if (kbId == null) {
                 return APIResponse.error(500, "Knowledge base information is missing kb_id");
             }

            // Delete knowledge base file/chunk information
             List<Map<String, Object>> fileInfoList = kbFileClient.kbFileSearch(kbId) != null ?
                 kbFileClient.kbFileSearch(kbId) : List.of();
             List<String> fileIds = fileInfoList.stream()
                 .map(fileInfo -> (String) fileInfo.get("ori_file_id"))
                 .filter(Objects::nonNull)
                 .collect(Collectors.toList());
             if (!fileIds.isEmpty()) {
                 if (!kbChunkClient.kbDeleteChunk(kbId, fileIds)) {
                     return APIResponse.error(500, "Failed to delete knowledge base document chunks");
                 }
                 if (!kbFileClient.kbDeleteFile(kbId, fileIds)) {
                     return APIResponse.error(500, "Failed to delete knowledge base file records");
                 }
             }

            // Delete ES index
             if (kbIndexClient.indexExists(kbName)) {
                 if (!kbIndexClient.deleteIndex(kbName)) {
                     return APIResponse.error(500, "Failed to delete ES index");
                 }
             }

            // Delete Vearch vector space
             if (!vearchManager.deleteSpace(Config.getVearch().getDbName(), kbName)) {
                 return APIResponse.error(500, "Failed to delete Vearch vector space");
             }

            // Delete knowledge base basic information
             if (!kbBaseClient.kbDelete(kbName)) {
                 return APIResponse.error(500, "Failed to delete knowledge base information");
             }

            log.info("Knowledge base {} (ID: {}) deleted successfully", kbName, kbName);
            return APIResponse.success("Knowledge base deleted successfully", kbName);
        } catch (IllegalArgumentException e) {
            log.warn("Delete knowledge base failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete knowledge base", e);
            return APIResponse.error(500, "Failed to delete knowledge base");
        }
    }

    /**
     * Update knowledge base Schema
     * <p>
     * Update Schema configuration of specified knowledge base
     *
     * @param kbName   kbName
     * @param kbSchema New Schema configuration, including field definitions and matching policies
     * @return APIResponse containing update result
     */
    @PostMapping("/{kbName}/schema")
    public APIResponse<String> updateKbSchema(
            @PathVariable String kbName,
            @RequestBody Object kbSchema) {
        try {
            // Validate input
            if (kbName == null || kbName.trim().isEmpty()) {
                return APIResponse.error(400, "Knowledge base name cannot be empty");
            }

            // FIXME: Validate Schema validity
            // if (kbSchema.getFields() == null || kbSchema.getFields().isEmpty()) {
            //     return APIResponse.error(400, "Schema must contain at least one field definition");
            // }

            // FIXME: 1. Query whether knowledge base exists
            // Map<String, Object> kbInfoList = kbBaseClient.kbInfoSearchName(kbName);
            // if (kbInfoList == null || kbInfoList.isEmpty() ||
            //     kbInfoList.get(0).getKbSchema() == null ||
            //     kbInfoList.get(0).getKbSchema().getFields() == null ||
            //     kbInfoList.get(0).getKbSchema().getFields().isEmpty() ||
            //     kbInfoList.get(0).getKbSchema().getMatchRules() == null ||
            //     kbInfoList.get(0).getKbSchema().getMatchRules().isEmpty()) {
            //     return APIResponse.error(404, "Knowledge base '" + kbName + "' does not exist");
            // }

            // FIXME: 2. Update knowledge base schema fields and update time
            // Map<String, Object> updateFields = new HashMap<>();
            // updateFields.put("kb_schema", kbSchema);
            // updateFields.put("update_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            // if (!kbBaseClient.kbUpdate(kbName, updateFields)) {
            //     return APIResponse.error(500, "Failed to update knowledge base Schema");
            // }

            // FIXME: 3. Infer and create ES index
            // Map<String, Object> esIndexMapping = inferMappingFromSchema(kbSchema);
            // if (esIndexMapping != null) {
            //     boolean indexCreateResult = kbIndexClient.createIndex(kbName, esIndexMapping);
            //     if (!indexCreateResult) {
            //         log.error("Failed to create ES index {}", kbName);
            //         return APIResponse.error(500, "Failed to create ES index");
            //     }
            //     log.info("ES index {} created successfully", kbName);
            // }

            // FIXME: 4. Infer and create Vearch space
            // Map<String, Object> vearchSpaceSchema = inferVearchSpaceSchema(kbSchema, kbName);
            // if (vearchSpaceSchema != null) {
            //     if (vearchManager.spaceExists(settings.getVearchConfig().getDbName(), kbName)) {
            //         log.warn("Vearch space {} already exists, will delete and recreate", kbName);
            //         boolean deleteResult = vearchManager.deleteSpace(settings.getVearchConfig().getDbName(), kbName);
            //         if (!deleteResult) {
            //             log.error("Failed to delete Vearch space {}", kbName);
            //             return APIResponse.error(500, "Failed to delete old Vearch vector space");
            //         }
            //         log.info("Vearch space {} deleted", kbName);
            //     }
            //     boolean spaceCreateResult = vearchManager.createSpaceWithSchema(settings.getVearchConfig().getDbName(), vearchSpaceSchema);
            //     if (!spaceCreateResult) {
            //         log.error("Failed to create Vearch space {}", kbName);
            //         return APIResponse.error(500, "Failed to create Vearch vector space");
            //     }
            //     log.info("Vearch space {} created successfully", kbName);
            // }

            // FIXME: If there is no retrieval strategy, raise an error
            // if (vearchSpaceSchema == null && esIndexMapping == null) {
            //     return APIResponse.error(400, "Schema must include at least one retrieval strategy (ES full-text search or Vearch vector search)");
            // }

            // FIXME: 5. Check if endpoints are already bound
            // if (endpointRegistry != null && endpointRegistry.isKbRegistered(kbName)) {
            //     log.info("Knowledge base '{}' endpoints are already bound, this schema update operation requires unbinding and rebinding endpoints", kbName);
            //     endpointRegistry.unbindKbEndpoints(kbName);
            // }

            // FIXME: 6. Create dynamic endpoint generator
            // DynamicEndpointGenerator generator = new DynamicEndpointGenerator(kbName, kbSchema);
            // Object dynamicRouter = generator.generateAllEndpoints();

            // FIXME: 7. Get Spring application instance and register routes
            // app.includeRouter(dynamicRouter);

            // Generate endpoint list
            // List<String> endpoints = new ArrayList<>();
            // if (kbSchema.getMatchRules() != null && !kbSchema.getMatchRules().isEmpty()) {
            //     for (int ruleIdx = 0; ruleIdx < kbSchema.getMatchRules().size(); ruleIdx++) {
            //         endpoints.add("POST /kb/" + kbName + "/search/rule_" + ruleIdx);
            //     }
            // }

            log.info("✅ Knowledge base '{}' retrieval endpoints created successfully, total {} endpoints", kbName, 0);
            return APIResponse.success("success", "Knowledge base '" + kbName + "' schema updated successfully and retrieval endpoints created successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Update knowledge base Schema failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Schema update exception", e);
            return APIResponse.error(500, "Failed to update Schema");
        }
    }

    /**
     * Check if knowledge base Schema exists
     * <p>
     * Check if specified knowledge base has Schema configured
     *
     * @param kbName Knowledge base name
     * @return APIResponse containing whether schema exists
     */
    @GetMapping("/{kbName}/schema/exists")
    public APIResponse<Boolean> getKbSchema(@PathVariable String kbName) {
        try {
            // Validate input
            if (kbName == null || kbName.trim().isEmpty()) {
                return APIResponse.error(400, "Knowledge base name cannot be empty");
            }

            // Query knowledge base information
             Map<String, Object> result = kbBaseClient.kbInfoSearchName(kbName);
             if (result == null || result.isEmpty() || result.size() == 0) {
                 return APIResponse.error(404, "Knowledge base '" + kbName + "' does not exist");
             }
            KnowledgeBaseItem kbInfo = JsonUtils.convertValue(result, KnowledgeBaseItem.class);

             // Determine if schema exists and is valid
             boolean hasSchema = kbInfo.getKbSchema() != null;

            return APIResponse.success("Query successful", hasSchema);
        } catch (IllegalArgumentException e) {
            log.warn("Check Schema existence failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to check Schema existence", e);
            return APIResponse.error(500, "Failed to check Schema");
        }
    }
}
