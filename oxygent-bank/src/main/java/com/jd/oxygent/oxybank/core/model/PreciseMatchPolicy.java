package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

/**
 * 精确匹配策略
 * 根据输入字段名匹配对应的精确匹配规则，生成最终查询语句
 * 确认要查询和返回的字段
 */
@Data
public class PreciseMatchPolicy extends MatchPolicy {
    private static final String MODE = "precise";
}
