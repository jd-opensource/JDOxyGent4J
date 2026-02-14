package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

/**
 * Type statistics item.
 */
@Data
public class TypeStatsItem {

    private String dataType;
    private int totalCount;
    private int pendingCount;
    private int approvedCount;
    private int rejectedCount;
}
