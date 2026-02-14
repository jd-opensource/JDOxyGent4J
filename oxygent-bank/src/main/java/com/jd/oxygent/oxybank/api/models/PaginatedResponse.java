package com.jd.oxygent.oxybank.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Paginated response model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {

    /**
     * Data list
     */
    private List<T> items;

    /**
     * Total number of records
     */
    private int total;

    /**
     * Current page number
     */
    private int page;

    /**
     * Page size
     */
    private int size;

    /**
     * Total number of pages
     */
    private int pages;
}
