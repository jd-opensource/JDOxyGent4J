package com.jd.oxygent.core.oxygent.samples.server.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Web response wrapper class
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class WebResponse {
    /**
     * Response code, 200 indicates success
     */
    private int code;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data
     */
    private Object data;
    /**
     * is success
     */
    private Boolean success;

    public WebResponse() {
        this.code = 200;
        this.message = "success";
    }

    public WebResponse(int code, String message, Object data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    /**
     * Create success response
     */
    public static WebResponse success(Object data) {
        return new WebResponse(200, "success", data, true);
    }

    /**
     * Create success response
     */
    public static WebResponse success(Object data, String message) {
        return new WebResponse(200, message, data, true);
    }

    /**
     * Create error response
     */
    public static WebResponse error(int code, String message) {
        return new WebResponse(code, message, null, false);
    }

    /**
     * Create error response
     */
    public static WebResponse error(int code, String message, Object data) {
        return new WebResponse(code, message, data, false);
    }

    /**
     * Convert to Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        if (data != null) {
            map.put("data", data);
        }
        map.put("success", success);
        return map;
    }
}