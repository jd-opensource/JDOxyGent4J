package com.jd.oxygent.oxybank.app.api.models;

import lombok.Data;

/**
 * API Response Model Definitions
 * Converted from app/api/models.py
 */
@Data
public class APIResponse<T> {
    private int code = 200; // Response status code
    private String msg = "success"; // Response message
    private T data; // Response data
}
