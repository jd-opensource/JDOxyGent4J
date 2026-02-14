package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

/**
 * Pending P0 item.
 */
@Data
public class PendingP0Item {

    private String dataId;
    private String question;
    private String answer;
    private String caller;
    private String callee;
    private String sourceTraceId;
    private String createdAt;
}
