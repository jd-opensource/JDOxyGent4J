package com.jd.oxygent.oxybank.core.model.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data deposit request and response models.
 */
public class DepositModel {

    @Data
    @NoArgsConstructor
    public static class DepositRequest {

        /**
         * Question content
         */
        private String question;

        /**
         * Answer content
         */
        private String answer;

        /**
         * Original trace id
         */
        private String sourceTraceId;

        /**
         * Original request id
         */
        private String sourceRequestId;

        /**
         * Session group ID
         */
        private String sourceGroupId;

        /**
         * Caller
         */
        private String caller = "";

        /**
         * Callee
         */
        private String callee = "";

        /**
         * Caller type
         */
        private String callerType;

        /**
         * Callee type
         */
        private String calleeType;

        /**
         * Data type (auto-inferred if not provided)
         */
        private String dataType;

        /**
         * Priority (0-4)
         */
        private Integer priority;

        /**
         * Data category
         */
        private String category;

        /**
         * Data tags
         */
        private List<String> tags = new ArrayList<>();

        /**
         * Extra data
         */
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    public static class DepositBatchRequest {

        /**
         * Data list
         */
        private List<DepositRequest> dataList;

        /**
         * Batch ID (auto-generated if not provided)
         */
        private String batchId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositResponse {

        /**
         * Data ID
         */
        private String dataId;

        /**
         * Data hash
         */
        private String dataHash;

        /**
         * Data status
         */
        private String status;

        /**
         * Is duplicate data
         */
        private boolean isDuplicate;

        /**
         * Response message
         */
        private String message;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepositBatchResponse {

        /**
         * Batch ID
         */
        private String batchId;

        /**
         * Total count
         */
        private int total;

        /**
         * Success count
         */
        private int successCount;

        /**
         * Duplicate count
         */
        private int duplicateCount;

        /**
         * Failed count
         */
        private int failedCount;

        /**
         * Detailed results
         */
        private List<DepositResponse> results;
    }
}