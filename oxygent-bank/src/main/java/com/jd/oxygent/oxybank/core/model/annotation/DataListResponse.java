package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;

/**
 * Data list response (generic).
 */
@Data
public class DataListResponse<T> {

    private int total;
    private int page;
    private int pageSize;
    private List<T> items;
}
