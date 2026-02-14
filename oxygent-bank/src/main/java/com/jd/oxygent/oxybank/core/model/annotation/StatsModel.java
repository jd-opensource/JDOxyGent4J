package com.jd.oxygent.oxybank.core.model.annotation;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Statistics analysis models.
 */
public class StatsModel {

    @Data
    public static class OverallStatsResponse {

        /**
         * Total data count
         */
        private int totalCount;

        /**
         * Pending count
         */
        private int pendingCount;

        /**
         * Annotated count
         */
        private int annotatedCount;

        /**
         * Approved count
         */
        private int approvedCount;

        /**
         * Rejected count
         */
        private int rejectedCount;

        /**
         * KB ingested count
         */
        private int kbIngestedCount;

        /**
         * KB pending count
         */
        private int kbPendingCount;

        /**
         * KB failed count
         */
        private int kbFailedCount;

        /**
         * Statistics by priority {priority: count}
         */
        private Map<String, Integer> priorityStats;

        /**
         * Statistics by data type {type: count}
         */
        private Map<String, Integer> typeStats;
    }

    @Data
    public static class PendingP0Item {

        private String dataId;

        private String question;

        private String answer;

        private String caller;

        private String callee;

        private String sourceTraceId;

        private String createdAt;
    }

    @Data
    public static class PendingP0Response {

        private int total;

        private List<PendingP0Item> items;
    }

    @Data
    public static class TypeStatsItem {

        private String dataType;

        private int totalCount;

        private int pendingCount;

        private int approvedCount;

        private int rejectedCount;
    }

    @Data
    public static class TypeStatsResponse {

        private List<TypeStatsItem> items;
    }

    @Data
    public static class GroupSummaryItem {

        private String groupId;

        private int totalCount;

        private Map<String, Integer> statusCounts;
    }

    @Data
    public static class GroupsSummaryResponse {

        private List<GroupSummaryItem> items;
    }
}