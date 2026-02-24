package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.oxybank.api.model.APIResponse;
import com.jd.oxygent.oxybank.api.model.KnowledgeBaseItem;
import com.jd.oxygent.oxybank.api.model.PaginatedResponse;
import com.jd.oxygent.oxybank.api.model.PaginationParams;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchIndexManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbBaseManager;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbChunkManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Base Chunk Controller
 * <p>
 * Knowledge Base Document Chunk Management API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kb_base/{kbId}")
public class KnowledgeChunkController {

    @Autowired
    private ElasticsearchKbBaseManager kbBaseClient;
    @Autowired
     private ElasticsearchKbChunkManager kbChunkClient;
    @Autowired
     private ElasticsearchIndexManager kbIndexClient;

    /**
     * Get all document chunks for a specified file in the knowledge base
     * <p>
     * Return all document chunk information for a specific file in the specified knowledge base, including:
     * - kbId: Knowledge base unique identifier
     * - oriFileId: Original file ID
     * - chunkId: Document chunk ID
     * - chunkText: Document chunk text content
     * - chunkExtraData: Document chunk extra data
     * - language: Document chunk language
     *
     * @param kbId      Knowledge base ID
     * @param fileId    File ID
     * @param pagination Pagination parameters
     * @return APIResponse containing paginated chunk list
     */
    @GetMapping("/file/{fileId}/chunks")
    public APIResponse<PaginatedResponse<Map<String, Object>>> getKbFileChunks(
            @PathVariable String kbId,
            @PathVariable String fileId,
            @ModelAttribute PaginationParams pagination) {
        try {
            // Validate input
            if (kbId == null || kbId.trim().isEmpty()) {
                return APIResponse.error(400, "Knowledge base ID cannot be empty");
            }

            if (fileId == null || fileId.trim().isEmpty()) {
                return APIResponse.error(400, "File ID cannot be empty");
            }

            // 1. Get kbName by kbId
             String kbName = kbBaseClient.getKbNameById(kbId);
             if (kbName == null) {
                 return APIResponse.error(404, "Knowledge base with ID '" + kbId + "' does not exist");
             }

            // 2. Check if ES index exists
             if (!kbIndexClient.indexExists(kbName)) {
                 return APIResponse.error(404, "ES index for knowledge base '" + kbName + "' does not exist, please create knowledge base Schema and generate index first");
             }

            // Implement get_kb_files method
            Map<String, Object> result = kbChunkClient.search(
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
            log.warn("Get chunks for file {} failed (invalid params)", fileId, e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get chunks for file {}", fileId, e);
            return APIResponse.error(500, "Failed to get file document chunk list");
        }
    }

    /**
     * Get document data for structured knowledge base
     * <p>
     * Query all document data in ES index of a structured knowledge base (knowledge base with Schema)
     *
     * @param kbId      Knowledge base ID
     * @param pagination Pagination parameters
     * @return APIResponse containing paginated document data list
     */
    @GetMapping("/chunks")
    public APIResponse<PaginatedResponse<Map<String, Object>>> getKbChunks(
            @PathVariable String kbId,
            @ModelAttribute PaginationParams pagination) {
        try {
            // Validate input
            if (kbId == null || kbId.trim().isEmpty()) {
                return APIResponse.error(400, "Knowledge base ID cannot be empty");
            }

            // 1. Get kbName by kbId
            // String kbName = kbBaseClient.getKbNameById(kbId);
            // if (kbName == null) {
            //     return APIResponse.error(404, "Knowledge base with ID '" + kbId + "' does not exist");
            // }

            // 2. Check if ES index exists
            // if (!kbIndexClient.indexExists(kbName)) {
            //     return APIResponse.error(404, "ES index for knowledge base '" + kbName + "' does not exist, please create knowledge base Schema and generate index first");
            // }

            // 3. Query all documents in ES index (paginated)
            // int fromValue = (pagination.getPage() - 1) * pagination.getSize();
            // Map<String, Object> query = Map.of(
            //     "query", Map.of("match_all", Map.of()),
            //     "from", fromValue,
            //     "size", pagination.getSize()
            // );
            // Map<String, Object> resp = settings.getEsClient().search(kbName, query);

            // 4. Parse results
            // List<Map<String, Object>> hits = resp.get("hits").get("hits");
            // int total = resp.get("hits").get("total").get("value");

            // Extract document data (_source field)
            // List<Map<String, Object>> items = new ArrayList<>();
            // for (Map<String, Object> doc : hits) {
            //     items.add(doc.get("_source"));
            // }

            // Calculate total pages
            // int pages = (total + pagination.getSize() - 1) / pagination.getSize() if total > 0 else 0;

            // Mock response for now
            List<Map<String, Object>> items = new ArrayList<>();
            int total = 0;
            int pages = 0;

            PaginatedResponse<Map<String, Object>> paginatedResponse = new PaginatedResponse<>();
            paginatedResponse.setItems(items);
            paginatedResponse.setTotal(total);
            paginatedResponse.setPage(pagination.getPage());
            paginatedResponse.setSize(pagination.getSize());
            paginatedResponse.setPages(pages);

            return APIResponse.success("Successfully retrieved structured knowledge base document data", paginatedResponse);
        } catch (IllegalArgumentException e) {
            log.warn("Get structured knowledge base {} document data failed (invalid params)", kbId, e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get structured knowledge base {} document data", kbId, e);
            return APIResponse.error(500, "Failed to get document data");
        }
    }
}
