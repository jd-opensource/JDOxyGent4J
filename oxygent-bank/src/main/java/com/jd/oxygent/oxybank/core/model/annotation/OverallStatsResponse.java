package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.Map;

/**
 * Overall statistics response.
 */
@Data
public class OverallStatsResponse {

    private int totalCount;
    private int pendingCount;
    private int annotatedCount;
    private int approvedCount;
    private int rejectedCount;
    private int kbIngestedCount;
    private int kbPendingCount;
    private int kbFailedCount;
    private Map<String, Integer> priorityStats;
    private Map<String, Integer> typeStats;
}
