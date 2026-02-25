package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

import java.util.List;

/**
 * 匹配规则，每个匹配规则至少包含一个匹配策略
 */
@Data
public class MatchRule {
    private List<MatchPolicy> matchPolicies;
    private List<String> outputFields;
}
