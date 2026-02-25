package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

/**
 * ES全文搜索策略
 * 使用Elasticsearch进行全文搜索的配置策略
 */
@Data
public class ESTextMatchPolicy extends MatchPolicy {
    private static final String MODE = "es_text";
}
