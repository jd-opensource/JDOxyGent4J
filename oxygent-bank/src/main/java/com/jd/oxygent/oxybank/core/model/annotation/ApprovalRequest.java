package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

/**
 * Approval request. action: approve-pass, reject-reject.
 */
@Data
public class ApprovalRequest {

    private String action;
    private String reason;
}
