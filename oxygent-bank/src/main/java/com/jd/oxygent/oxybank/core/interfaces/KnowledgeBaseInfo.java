package com.jd.oxygent.oxybank.core.interfaces;

import com.jd.oxygent.oxybank.core.model.KBSchema;
import lombok.Data;

@Data
public class KnowledgeBaseInfo {
    private String kbName;
    private boolean autoBindQuery = true;
    private KBSchema kbSchema;
}
