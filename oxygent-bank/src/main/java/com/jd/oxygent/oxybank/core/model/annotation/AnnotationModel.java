package com.jd.oxygent.oxybank.core.model.annotation;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Annotation and approval request models.
 */
public class AnnotationModel {

    @Data
    public static class AnnotationUpdateRequest {

        /**
         * Main annotation data (content, question, score, comment)
         */
        private Map<String, Object> annotation;

        /**
         * Data category
         */
        private String category;

        /**
         * Data tags
         */
        private List<String> tags;

        /**
         * Score info
         */
        private Map<String, Object> scores;

        /**
         * Annotation comment
         */
        private String comment;

        /**
         * Remark info
         */
        private String remark;

        /**
         * Other annotation data
         */
        private Map<String, Object> annotationData;
    }

    @Data
    public static class ApprovalRequest {

        /**
         * Approval action: approve-pass, reject-reject
         * pattern: ^(approve|reject)$
         */
        private String action;

        /**
         * Approval reason/rejection reason
         */
        private String reason;
    }

    @Data
    public static class RejectionRequest {

        /**
         * Rejection reason
         */
        private String reason;

        /**
         * Rejection category
         */
        private String rejectCategory;
    }
}