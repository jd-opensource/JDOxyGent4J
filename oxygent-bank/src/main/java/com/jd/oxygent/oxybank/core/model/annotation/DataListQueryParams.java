package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;

/**
 * Data list query parameters.
 */
@Data
public class DataListQueryParams {

    private String status;
    private Integer priority;
    private String dataType;
    private String caller;
    private String callee;
    private String category;
    private List<String> tags;
    private String createdAfter;
    private String createdBefore;
    private String searchText;
    private String traceId;
    private String groupId;
    private int page = 1;
    private int pageSize = 10;
    private String sortBy = "created_at";
    private String sortOrder = "desc";
}
