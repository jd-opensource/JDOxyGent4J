package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deposit request model.
 */
@Data
@NoArgsConstructor
public class DepositRequest {

    private String question;
    private String answer;
    private String sourceTraceId;
    private String sourceRequestId;
    private String sourceGroupId;
    private String caller = "";
    private String callee = "";
    private String callerType;
    private String calleeType;
    private String dataType;
    private Integer priority;
    private String category;
    private List<String> tags = new ArrayList<>();
    private Map<String, Object> extra;
}
