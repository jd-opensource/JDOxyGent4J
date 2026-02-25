package com.jd.oxygent.oxybank.core.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 结构化知识库字段信息
 */
@Data
@Slf4j
public class FieldInfo {
    private String fieldName;
    private String fieldType;
    private String fieldDesc;
}
