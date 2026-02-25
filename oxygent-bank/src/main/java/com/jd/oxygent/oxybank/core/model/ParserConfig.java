package com.jd.oxygent.oxybank.core.model;

import lombok.Data;

/**
 * 解析器配置模型，用于可配置的文本分割
 */
@Data
public class ParserConfig {
    private String parserType = "sentence";
    private int chunkSize = 500;
    private int chunkOverlap = 50;
    private String separator = " ";
    private String splitterType = "sentence";
    private boolean includeMetadata = true;
    private boolean includePrevNextRel = true;
}
