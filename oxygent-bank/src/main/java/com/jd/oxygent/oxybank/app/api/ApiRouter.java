package com.jd.oxygent.oxybank.app.api;

import com.jd.oxygent.oxybank.app.api.dynamic.QueryEndpoint;
import com.jd.oxygent.oxybank.app.api.endpoints.KnowledgeBase;
import com.jd.oxygent.oxybank.app.api.endpoints.KnowledgeChunk;
import com.jd.oxygent.oxybank.app.api.endpoints.KnowledgeFile;
import com.jd.oxygent.oxybank.app.api.endpoints.annotation.Data;
import com.jd.oxygent.oxybank.app.api.endpoints.annotation.Deposit;
import com.jd.oxygent.oxybank.app.api.endpoints.annotation.Kb;
import com.jd.oxygent.oxybank.app.api.endpoints.annotation.Stats;
import lombok.extern.slf4j.Slf4j;

/**
 * API Router configuration
 * Converted from app/api/router.py
 */
@Slf4j
public class ApiRouter {
    
    /**
     * Initialize and configure all API routes
     */
    public void initializeRoutes() {
        log.info("Initializing API routes...");
        
        // Knowledge base management routes
        initializeKnowledgeBaseRoutes();
        
        // Dynamic query endpoint routes
        initializeDynamicRoutes();
        
        // Annotation platform routes
        initializeAnnotationRoutes();

        log.info("API routes initialized successfully");
    }
    
    /**
     * Initialize knowledge base management routes
     */
    private void initializeKnowledgeBaseRoutes() {
        log.info("Initializing knowledge base management routes...");
        
        // Initialize knowledge_base route
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        log.info("Knowledge base routes initialized");
        
        // Initialize knowledge_file route
        KnowledgeFile knowledgeFile = new KnowledgeFile();
        log.info("Knowledge file routes initialized");
        
        // Initialize knowledge_chunk route
        KnowledgeChunk knowledgeChunk = new KnowledgeChunk();
        log.info("Knowledge chunk routes initialized");
    }
    
    /**
     * Initialize dynamic query endpoint routes
     */
    private void initializeDynamicRoutes() {
        log.info("Initializing dynamic query endpoint routes...");
        
        // Initialize query_endpoint route
        QueryEndpoint queryEndpoint = new QueryEndpoint();
        log.info("Dynamic query endpoint routes initialized");
    }
    
    /**
     * Initialize annotation platform routes
     */
    private void initializeAnnotationRoutes() {
        log.info("Initializing annotation platform routes...");
        
        // Initialize deposit route
        Deposit deposit = new Deposit();
        log.info("Deposit routes initialized");
        
        // Initialize data route
        Data data = new Data();
        log.info("Data routes initialized");
        
        // Initialize kb route
        Kb kb = new Kb();
        log.info("KB routes initialized");
        
        // Initialize stats route
        Stats stats = new Stats();
        log.info("Stats routes initialized");
    }
}
