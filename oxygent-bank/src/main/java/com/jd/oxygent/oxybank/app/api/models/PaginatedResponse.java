package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;
import java.util.List;

/**
 * Paginated response model
 * Converted from app/api/models.py
 */
@Data
public class PaginatedResponse<T> {
    private List<T> items; // Data list
    private int total; // Total number of records
    private int page; // Current page number
    private int size; // Page size
    private int pages; // Total number of pages
}
