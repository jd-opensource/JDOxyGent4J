package com.jd.oxygent.oxybank.core.model.annotation;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * QA data core models.
 */
public class QADataModel {

    @Data
    public static class QADataItem {

        /**
         * Unique data ID
         */
        private String dataId;

        /**
         * Data hash (for deduplication)
         */
        private String dataHash;

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
        private String caller;

        /**
         * Callee
         */
        private String callee;

        /**
         * Caller type (reserved)
         */
        private String callerType;

        /**
         * Callee type (reserved)
         */
        private String calleeType;

        /**
         * Data type
         */
        private String dataType;

        /**
         * Priority (0-4, P0=0)
         */
        private int priority;

        /**
         * Data category
         */
        private String category;

        /**
         * Data tags
         */
        private List<String> tags;

        /**
         * Data status
         */
        private String status;

        /**
         * Annotation results
         */
        private Map<String, Object> annotation;

        /**
         * Score info
         */
        private Map<String, Object> scores;

        /**
         * Rejection reason
         */
        private String rejectReason;

        /**
         * KB ingestion status
         */
        private String kbStatus;

        /**
         * KB ingestion time
         */
        private String kbIngestedAt;

        /**
         * KB ingestion error message
         */
        private String kbErrorMessage;

        /**
         * KB extra info
         */
        private Map<String, Object> kbExtra;

        /**
         * Batch ID
         */
        private String batchId;

        /**
         * Creation time
         */
        private String createdAt;

        /**
         * Update time
         */
        private String updatedAt;

        /**
         * Extra data
         */
        private Map<String, Object> extra;
    }

    @Data
    public static class QADataSummary {

        private String dataId;

        private String question;

        private String answer;

        private String dataType;

        private int priority;

        private String status;

        private String caller;

        private String callee;

        private String createdAt;
    }
}