package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.Map;

/**
 * Group summary item.
 */
@Data
public class GroupSummaryItem {

    private String groupId;
    private int totalCount;
    private Map<String, Integer> statusCounts;
}
