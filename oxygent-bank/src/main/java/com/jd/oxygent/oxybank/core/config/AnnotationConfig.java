package com.jd.oxygent.oxybank.core.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Annotation platform configuration.
 * Knowledge base structured field description see ServiceConfig javadoc.
 */
@Data
@Component
public class AnnotationConfig {

    private String esIndexPrefix = "qa_annotation";

    private boolean kbEnabled = true;

    private String kbId = "";

    private boolean kbAutoIngest = false;

    private int kbTimeout = 30;

    private int kbRetryTimes = 3;

    private int kbRetryInterval = 5;

    private int batchSize = 100;

    private String defaultDataType = "custom";

    private int defaultPriority = 4;

    public AnnotationConfig() {
        // Load from env ANNOTATION_* when needed
    }

    public void validate() {
        if (batchSize < 0) {
            throw new IllegalArgumentException("Must be a positive integer");
        }
        if (kbTimeout < 0 || kbRetryTimes < 0 || kbRetryInterval < 0) {
            throw new IllegalArgumentException("Must be a positive integer");
        }
        List<String> valid_types = List.of("e2e", "agent", "llm", "tool", "custom");
        if (!valid_types.contains(defaultDataType)) {
            throw new IllegalArgumentException(
                    "Invalid data type, must be one of: " + String.join(", ", valid_types)
            );
        }
        if (defaultPriority < 0 || defaultPriority > 4) {
            throw new IllegalArgumentException("Priority must be between 0 and 4");
        }
        if (kbEnabled && (kbId == null || kbId.trim().isEmpty())) {
            String msg = "KB injection functionality is enabled (ANNOTATION_KB_ENABLED=true), "
                    + "but knowledge base ID is not configured. "
                    + "Please configure in .env file: ANNOTATION_KB_ID=<your-knowledge-base-id>";
            throw new IllegalArgumentException(msg);
        }
    }
}
