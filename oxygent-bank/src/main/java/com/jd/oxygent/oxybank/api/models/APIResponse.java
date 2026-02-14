package com.jd.oxygent.oxybank.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Response Model Definitions
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {

    /**
     * Response status code
     */
    private int code = 200;

    /**
     * Response message
     */
    private String msg = "success";

    /**
     * Response data
     */
    private T data;

    public APIResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> APIResponse<T> success(T data) {
        return new APIResponse<>(200, "success", data);
    }

    public static <T> APIResponse<T> success(String msg, T data) {
        return new APIResponse<>(200, msg, data);
    }

    public static <T> APIResponse<T> error(int code, String msg) {
        return new APIResponse<>(code, msg, null);
    }
}
