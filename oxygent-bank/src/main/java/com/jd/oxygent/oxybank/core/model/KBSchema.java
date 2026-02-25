package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

import java.util.List;

/**
 * 知识库Schema
 */
@Data
public class KBSchema {
    private List<FieldInfo> fields;
    private List<MatchRule> matchRules;
    private ParserConfig parserConfig;
}
