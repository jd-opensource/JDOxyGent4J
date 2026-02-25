package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

import java.util.List;

/**
 * 匹配策略基类
 * 使用mode字段作为判别器，支持自动反序列化到特定策略类型
 */
@Data
public class MatchPolicy {
    private String mode;
    private List<String> inputFields;
}
