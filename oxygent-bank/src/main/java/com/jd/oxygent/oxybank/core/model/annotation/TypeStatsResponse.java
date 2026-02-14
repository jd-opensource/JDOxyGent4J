package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;

/**
 * Type statistics response.
 */
@Data
public class TypeStatsResponse {

    private List<TypeStatsItem> items;
}
