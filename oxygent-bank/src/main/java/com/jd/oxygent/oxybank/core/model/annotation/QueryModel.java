package com.jd.oxygent.oxybank.core.model.annotation;

import java.util.List;

import lombok.Data;

/**
 * Query filter models.
 */
public class QueryModel {

    @Data
    public static class DataListQueryParams {

        /**
         * Data status
         */
        private String status;

        /**
         * Priority
         */
        private Integer priority;

        /**
         * Data type
         */
        private String dataType;

        /**
         * Caller
         */
        private String caller;

        /**
         * Callee
         */
        private String callee;

        /**
         * Data category
         */
        private String category;

        /**
         * Data tag list
         */
        private List<String> tags;

        /**
         * Creation time start (YYYY-MM-DD HH:MM:SS)
         */
        private String createdAfter;

        /**
         * Creation time end (YYYY-MM-DD HH:MM:SS)
         */
        private String createdBefore;

        /**
         * Full-text search (search question or answer)
         */
        private String searchText;

        /**
         * Source trace ID (fuzzy match)
         */
        private String traceId;

        /**
         * Source group ID (fuzzy match)
         */
        private String groupId;

        /**
         * Page number
         */
        private int page = 1;

        /**
         * Items per page
         */
        private int pageSize = 10;

        /**
         * Sort field
         */
        private String sortBy = "created_at";

        /**
         * Sort direction
         */
        private String sortOrder = "desc";
    }

    @Data
    public static class TraceQueryParams {

        /**
         * Result count limit
         */
        private int limit = 100;
    }

    @Data
    public static class GroupQueryParams {

        /**
         * Result count limit
         */
        private int limit = 100;
    }
}