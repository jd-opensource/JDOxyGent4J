package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;

/**
 * Groups summary response.
 */
@Data
public class GroupsSummaryResponse {

    private List<GroupSummaryItem> items;
}
