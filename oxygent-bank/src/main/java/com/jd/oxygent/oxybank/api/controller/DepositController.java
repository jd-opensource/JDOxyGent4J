package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.oxybank.api.models.APIResponse;
import com.jd.oxygent.oxybank.core.model.annotation.DepositModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data deposit API endpoints
 * <p>
 * Store QA pair to annotation platform with type inference
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/deposit")
public class DepositController {

    // FIXME: Initialize annotation service
    // private final AnnotationService annotationService;

    /**
     * Deposit single QA pair
     * <p>
     * Features:
     * - Auto hash-based deduplication
     * - Auto type inference (e2e/agent/llm/tool/custom)
     * - Auto priority inference (0-4)
     * - Generate unique data_id
     * - Save to Elasticsearch
     * <p>
     * Deduplication:
     * - Hash based on question + answer
     * - Check cache first, then ES
     * - Return existing data_id if duplicate
     *
     * @param request Deposit request
     * @return APIResponse containing deposit result
     */
    @PostMapping("")
    public APIResponse<DepositModel.DepositResponse> depositData(@RequestBody DepositModel.DepositRequest request) {
        try {
            // FIXME: Implement service.depositData method
            // DepositResponse result = annotationService.depositData(request);

            // Mock response for now
            DepositModel.DepositResponse result = new DepositModel.DepositResponse();
            result.setDataId("mock_data_id_" + System.currentTimeMillis());

            return APIResponse.success("Data deposited successfully", result);
        } catch (IllegalArgumentException e) {
            log.warn("Deposit failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Deposit failed", e);
            return APIResponse.error(500, "Deposit failed");
        }
    }

    /**
     * Batch deposit QA pairs
     * <p>
     * Features:
     * - Auto generate batch_id
     * - Process each item
     * - Count success/duplicate/failed
     * - Return detailed results
     * <p>
     * Notes:
     * - Continue processing even if some items fail
     * - Duplicates are marked in results, not treated as errors
     *
     * @param request Batch deposit request
     * @return APIResponse containing batch deposit result
     */
    @PostMapping("/batch")
    public APIResponse<DepositModel.DepositBatchResponse> depositBatch(@RequestBody DepositModel.DepositBatchRequest request) {
        try {
            // Validate data count
            int dataCount = request.getDataList().size();
            if (dataCount == 0) {
                return APIResponse.error(400, "Data list cannot be empty");
            }

            if (dataCount > 1000) {
                return APIResponse.error(400, "Batch deposit max 1000 items, got " + dataCount);
            }

            // FIXME: Implement service.depositBatch method
            // DepositBatchResponse result = annotationService.depositBatch(request);

            // Mock response for now
            DepositModel.DepositBatchResponse result = new DepositModel.DepositBatchResponse();
            result.setBatchId("mock_batch_id_" + System.currentTimeMillis());
            result.setSuccessCount(dataCount);
            result.setDuplicateCount(0);
            result.setFailedCount(0);

            return APIResponse.success(
                    "Batch completed: " + result.getSuccessCount() + " success, " +
                    result.getDuplicateCount() + " duplicate, " + result.getFailedCount() + " failed",
                    result
            );
        } catch (IllegalArgumentException e) {
            log.warn("Batch deposit failed (invalid params)", e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Batch deposit failed", e);
            return APIResponse.error(500, "Batch deposit failed");
        }
    }
}
