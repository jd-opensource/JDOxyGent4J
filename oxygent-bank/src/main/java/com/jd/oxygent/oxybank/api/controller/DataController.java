package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.oxybank.api.model.APIResponse;
import com.jd.oxygent.oxybank.core.model.annotation.AnnotationUpdateRequest;
import com.jd.oxygent.oxybank.core.model.annotation.DataListQueryParams;
import com.jd.oxygent.oxybank.core.model.annotation.DataListResponse;
import com.jd.oxygent.oxybank.core.model.annotation.QADataItem;
import com.jd.oxygent.oxybank.core.service.AnnotationService;
import com.jd.oxygent.oxybank.core.storer.docmanager.AnnotationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data management API endpoints
 * <p>
 * Query annotation data list with filtering, pagination and sorting
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/data")
public class DataController {

    @Autowired
    private AnnotationService annotationService;

    /**
     * Get data list
     * <p>
     * Supported filters:
     * - status: Data status (pending/annotated/approved/rejected, etc.)
     * - priority: Priority (0-4)
     * - data_type: Data type (e2e/agent/llm/tool/custom)
     * - caller: Caller
     * - callee: Callee
     * - category: Data category
     * - tags: Tags (comma separated)
     * - created_after/before: Time range
     * - search: Full-text search (search question or answer)
     * - trace_id: Source trace ID (fuzzy match)
     * - groupId: Source group ID (fuzzy match)
     * <p>
     * Supported sorting:
     * - created_at, updated_at, priority, etc.
     * - asc (ascending) or desc (descending)
     *
     * @param params Data list query parameters
     * @return APIResponse containing data list
     */
    @GetMapping("")
    public APIResponse<DataListResponse<QADataItem>> getDataList(DataListQueryParams params) {
        try {
            DataListResponse<QADataItem> dataListResponse = annotationService.getDataList(params);
            return APIResponse.success("Query successful", dataListResponse);
        } catch (Exception e) {
            log.error("Data list query failed", e);
            return APIResponse.error(500, "Query failed");
        }
    }

    /**
     * Get data details
     * <p>
     * Returns complete QA data for specified data_id, including:
     * - Question, answer
     * - Source tracking info
     * - Annotation results
     * - KB ingestion status
     * - etc.
     *
     * @param dataId Data ID
     * @return APIResponse containing QA data item
     */
    @GetMapping("/{data_id}")
    public APIResponse<QADataItem> getDataById(@PathVariable(name = "data_id") String dataId) {
        try {
            QADataItem data = annotationService.getById(dataId);
            return APIResponse.success("Query successful", data);
        } catch (Exception e) {
            log.error("Get data details failed", e);
            return APIResponse.error(500, "Query failed");
        }
    }

    /**
     * Update annotation
     * <p>
     * Updatable fields:
     * - category: Data category
     * - tags: Data tag list
     * - scores: Score info (dict)
     * - comment: Annotation comment
     * - remark: Note info
     * - annotation_data: Other custom annotation data
     * <p>
     * Note:
     * - After updating annotation, data status will automatically become annotated
     *
     * @param dataId Data ID
     * @param request Annotation update request
     * @return APIResponse containing update result
     */
    @PutMapping("/{data_id}/annotate")
    public APIResponse<Map<String, String>> updateAnnotation(
            @PathVariable(name = "data_id") String dataId,
            @RequestBody AnnotationUpdateRequest request) {
        try {
            boolean success = annotationService.updateAnnotation(dataId, request);
            if (!success) {
                return APIResponse.error(500, "Annotation update failed");
            }

            return APIResponse.success("Annotation updated successfully", Map.of("data_id", dataId));
        } catch (IllegalArgumentException e) {
            log.warn("Annotation update failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Annotation update failed", e);
            return APIResponse.error(500, "Update failed");
        }
    }

    /**
     * Approve data
     * <p>
     * Features:
     * - Update data status to approved
     * - Record approval reason and time
     * - If kb_auto_ingest enabled, auto-ingest to KB
     * <p>
     * Status flow:
     * - annotated/approved → (optional) kb_ingested
     *
     * @param dataId Data ID
     * @param action Approval action
     * @param reason Approval reason
     * @return APIResponse containing approval result
     */
    @PostMapping("/{data_id}/approve")
    public APIResponse<Map<String, String>> approveData(
            @PathVariable(name = "data_id") String dataId,
            @RequestParam String action,
            @RequestParam(required = false) String reason) {
        try {
            boolean success = annotationService.approveData(dataId, reason);
            if (!success) {
                return APIResponse.error(500, "Approval failed");
            }

            return APIResponse.success("Data approved", Map.of("data_id", dataId, "status", "approved"));
        } catch (IllegalArgumentException e) {
            log.warn("Approval failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Approval failed", e);
            return APIResponse.error(500, "Approval failed");
        }
    }

    /**
     * Reject data
     * <p>
     * Features:
     * - Update data status to rejected
     * - Record rejection reason and category
     * - Record rejection time
     * <p>
     * Status flow:
     * - annotated/approved → rejected
     *
     * @param dataId Data ID
     * @param reason Rejection reason
     * @param rejectCategory Rejection category
     * @return APIResponse containing rejection result
     */
    @PostMapping("/{data_id}/reject")
    public APIResponse<Map<String, String>> rejectData(
            @PathVariable(name = "data_id") String dataId,
            @RequestParam String reason,
            @RequestParam(required = false) String rejectCategory) {
        try {
            boolean success = annotationService.rejectData(dataId, reason, rejectCategory);

            if (!success) {
                return APIResponse.error(500, "Rejection failed");
            }

            return APIResponse.success("Data rejected", Map.of(
                    "data_id", dataId,
                    "status", "rejected",
                    "reason", reason
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Rejection failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Rejection failed", e);
            return APIResponse.error(500, "Rejection failed");
        }
    }

    /**
     * Query by trace_id
     * <p>
     * Returns all QA data related to specified trace_id
     *
     * @param traceId Trace ID
     * @return APIResponse containing data list
     */
    @GetMapping("/trace/{trace_id}")
    public APIResponse<List<QADataItem>> getByTraceId(@PathVariable(name = "trace_id") String traceId) {
        try {
            List<QADataItem> items = annotationService.getByTraceId(traceId);

            return APIResponse.success("Query successful, " + items.size() + " items in total", items);
        } catch (Exception e) {
            log.error("trace_id query failed", e);
            return APIResponse.error(500, "Query failed");
        }
    }

    /**
     * Query by group ID
     * <p>
     * Returns all QA data related to specified group ID
     *
     * @param groupId Group ID
     * @return APIResponse containing data list
     */
    @GetMapping("/group/{group_id}")
    public APIResponse<List<QADataItem>> getByGroupId(@PathVariable(name = "group_id") String groupId) {
        try {
            List<QADataItem> items = annotationService.getByGroupId(groupId);

            return APIResponse.success("Query successful, " + items.size() + " items in total", items);
        } catch (Exception e) {
            log.error("groupId query failed", e);
            return APIResponse.error(500, "Query failed");
        }
    }

    /**
     * Group summary statistics
     * <p>
     * Returns statistics for each group, including:
     * - groupId: Group ID
     * - totalCount: Total count
     * - statusCounts: Count by status
     *
     * @return APIResponse containing group summary
     */
    @GetMapping("/groups/summary")
    public APIResponse<List<Map<String, Object>>> getGroupsSummary() {
        try {
            List<Map<String, Object>> summary = annotationService.getGroupsSummary();

            return APIResponse.success("Query successful", summary);
        } catch (Exception e) {
            log.error("Group summary query failed", e);
            return APIResponse.error(500, "Query failed");
        }
    }
}
