package com.jd.oxygent.oxybank.core.interfaces;

/**
 * Simulated KnowledgeBaseInfo class
 */
public class KnowledgeBaseInfo {
    private String kbName;
    private boolean autoBindQuery = true;
    private KBSchema kbSchema;

    public String getKbName() {
        return kbName;
    }

    public boolean isAutoBindQuery() {
        return autoBindQuery;
    }

    public KBSchema getKbSchema() {
        return kbSchema;
    }
}
