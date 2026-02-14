package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;

/**
 * Pagination parameter model
 * Converted from app/api/models.py
 */
@Data
public class PaginationParams {
    private int page = 1; // Page number, starting from 1
    private int size = 10; // Page size, options: 10/20/50/100/200
    
    /**
     * Validate pagination parameters
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void validate() throws IllegalArgumentException {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be >= 1");
        }
        
        // Check if size is in allowed values
        int[] allowedSizes = {10, 20, 50, 100, 200};
        boolean isValidSize = false;
        for (int allowedSize : allowedSizes) {
            if (size == allowedSize) {
                isValidSize = true;
                break;
            }
        }
        
        if (!isValidSize) {
            throw new IllegalArgumentException("Size must be one of the following values: 10/20/50/100/200");
        }
    }
}
