package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

/**
 * Vearch向量匹配策略
 * 使用Vearch向量数据库进行向量相似度匹配
 * 输入参数类型为List<String>，但目前限制为一个输入字段
 */
@Data
public class VearchVectorMatchPolicy extends MatchPolicy {
    private static final String MODE = "vearch_vector";
    private String embeddingModel;
}
