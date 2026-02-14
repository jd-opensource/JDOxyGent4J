package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

/**
 * QA data summary model.
 */
@Data
public class QADataSummary {

    private String dataId;
    private String question;
    private String answer;
    private String dataType;
    private int priority;
    private String status;
    private String caller;
    private String callee;
    private String createdAt;
}
