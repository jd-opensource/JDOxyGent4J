package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * QA data item model.
 */
@Data
public class QADataItem {

    private String dataId;
    private String dataHash;
    private String question;
    private String answer;
    private String sourceTraceId;
    private String sourceRequestId;
    private String sourceGroupId;
    private String caller;
    private String callee;
    private String callerType;
    private String calleeType;
    private String dataType;
    private int priority;
    private String category;
    private List<String> tags;
    private String status;
    private Map<String, Object> annotation;
    private Map<String, Object> scores;
    private String rejectReason;
    private String kbStatus;
    private String kbIngestedAt;
    private String kbErrorMessage;
    private Map<String, Object> kbExtra;
    private String batchId;
    private String createdAt;
    private String updatedAt;
    private Map<String, Object> extra;
}
