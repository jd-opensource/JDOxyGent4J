package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

/**
 * Annotation platform common API response (generic).
 * Named CommonAPIResponse to avoid conflict with api.models.APIResponse.
 */
@Data
public class CommonAPIResponse<T> {

    private int code = 200;
    private String msg = "success";
    private T data;
}
