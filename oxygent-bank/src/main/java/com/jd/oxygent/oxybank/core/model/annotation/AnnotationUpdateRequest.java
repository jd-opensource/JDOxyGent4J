package com.jd.oxygent.oxybank.core.model.annotation;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Annotation update request.
 */
@Data
public class AnnotationUpdateRequest {

    private Map<String, Object> annotation;
    private String category;
    private List<String> tags;
    private Map<String, Object> scores;
    private String comment;
    private String remark;
    private Map<String, Object> annotationData;
}
