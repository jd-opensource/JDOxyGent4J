package com.jd.oxygent.oxybank.core.interfaces;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated ElasticsearchKbBaseManager class
 */
@Component
public class ElasticsearchKbBaseManager {

    /**
     * Query all knowledge bases
     * @return List of knowledge base info
     */
    public List<KnowledgeBaseInfo> listAllKbs() {
        return new ArrayList<>();
    }
}
