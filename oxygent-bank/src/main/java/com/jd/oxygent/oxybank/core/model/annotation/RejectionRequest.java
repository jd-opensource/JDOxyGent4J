package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

/**
 * Rejection request.
 */
@Data
public class RejectionRequest {

    private String reason;
    private String rejectCategory;
}
