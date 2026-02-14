package com.jd.oxygent.oxybank.core.interfaces;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Endpoint Registration Manager (Singleton Pattern)
 * Converted from oxybank/core/interface/endpoint_registry.py
 * 
 * Responsible for managing registration, persistence, and recovery of knowledge base retrieval endpoints
 * Uses singleton pattern to ensure only one instance in the entire application
 */
@Slf4j
@Component
public class EndpointRegistry {
    
    // Singleton instance
    private static EndpointRegistry instance = null;
    private static boolean initialized = false;
    
    // Application instance (in Java, this would be a ServletContext or similar)
    private final Object app;
    
    @Autowired
    private ElasticsearchKbBaseManager kbBaseClient;
    
    // Registered endpoints list (simulates FastAPI routes)
    private final List<EndpointRoute> routes = new ArrayList<>();
    
    /**
     * Private constructor for singleton pattern
     * @param app Application instance
     */
    private EndpointRegistry(Object app) {
        if (app == null) {
            throw new IllegalArgumentException("Must provide app parameter when creating EndpointRegistry for the first time");
        }
        this.app = app;
        this.kbBaseClient = new ElasticsearchKbBaseManager();
        initialized = true;
    }
    
    /**
     * Singleton pattern implementation
     * @param app Application instance (only needed on first creation)
     * @return EndpointRegistry singleton instance
     */
    public static synchronized EndpointRegistry getInstance(Object app) {
        if (instance == null) {
            instance = new EndpointRegistry(app);
        }
        return instance;
    }
    
    /**
     * Get singleton instance without creating it
     * @return EndpointRegistry singleton instance, or null if not initialized
     */
    public static synchronized EndpointRegistry getInstance() {
        return instance;
    }
    
    /**
     * Check if knowledge base is registered
     * @param kbName Knowledge base name
     * @return Whether registered
     */
    public boolean isKbRegistered(String kbName) {
        String prefix = "/kb/" + kbName;
        for (EndpointRoute route : routes) {
            if (route.getPath().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Unbind all endpoints for the specified knowledge base
     * @param kbName Knowledge base name
     * @return Whether unbinding was successful
     */
    public boolean unbindKbEndpoints(String kbName) {
        String prefix = "/kb/" + kbName;
        List<EndpointRoute> routesToRemove = new ArrayList<>();
        
        // Find all routes that need to be removed
        for (EndpointRoute route : routes) {
            boolean shouldRemove = false;
            
            // Check path prefix
            if (route.getPath().startsWith(prefix)) {
                shouldRemove = true;
            }
            
            // Check route name
            if (!shouldRemove && route.getName() != null) {
                if (route.getName().startsWith(kbName + "_search_rule_")) {
                    shouldRemove = true;
                }
            }
            
            if (shouldRemove) {
                routesToRemove.add(route);
            }
        }
        
        if (routesToRemove.isEmpty()) {
            log.warn("⚠️  Knowledge base '{}' endpoints not registered, no need to unbind", kbName);
            return false;
        }
        
        // Remove the routes
        int removedCount = 0;
        for (EndpointRoute route : routesToRemove) {
            if (routes.remove(route)) {
                log.debug("🗑️  Removed route: {} (name: {}) ", route.getPath(), route.getName());
                removedCount++;
            } else {
                log.debug("⚠️  Route {} does not exist in routes list ", route.getPath());
            }
        }
        
        log.info("✅ Knowledge base '{}' {} endpoints unbound ", kbName, removedCount);
        return true;
    }
    
    /**
     * Restore all knowledge bases that need endpoint binding
     */
    public void restoreAllEndpoints() {
        try {
            // Query all knowledge bases from ES
            List<KnowledgeBaseInfo> allKbs = kbBaseClient.listAllKbs();
            
            if (allKbs.isEmpty()) {
                log.info("ℹ️  No knowledge bases to process");
                return;
            }
            
            log.info("🚀 Starting to check auto_bind_query configuration for {} knowledge bases... ", allKbs.size());
            
            int boundCount = 0;
            int skippedCount = 0;
            int unboundCount = 0;
            
            // Iterate through each knowledge base
            for (KnowledgeBaseInfo kbInfo : allKbs) {
                String kbName = kbInfo.getKbName();
                boolean autoBindQuery = kbInfo.isAutoBindQuery();
                KBSchema kbSchema = kbInfo.getKbSchema();
                
                if (kbName == null) {
                    log.warn("⚠️  Knowledge base information missing kb_name, skipping");
                    continue;
                }
                
                // If auto_bind_query is False, check if unbinding is needed
                if (!autoBindQuery) {
                    if (isKbRegistered(kbName)) {
                        // Endpoints are bound but configured as False, need to unbind
                        if (unbindKbEndpoints(kbName)) {
                            unboundCount++;
                        }
                    } else {
                        log.debug("ℹ️  Knowledge base '{}' auto_bind_query is False and endpoints not bound, skipping", kbName);
                        skippedCount++;
                    }
                    continue;
                }
                
                // auto_bind_query=True, need to bind endpoints
                if (isKbRegistered(kbName)) {
                    log.info("ℹ️  Knowledge base '{}' endpoints already bound, skipping", kbName);
                    skippedCount++;
                    continue;
                }
                
                // Check if schema exists
                if (kbSchema == null) {
                    log.warn("⚠️  Knowledge base '{}' Schema information is empty, skipping binding", kbName);
                    skippedCount++;
                    continue;
                }
                
                try {
                    // Use DynamicEndpointGenerator to generate endpoints
                    DynamicEndpointGenerator generator = new DynamicEndpointGenerator(kbName, kbSchema);
                    List<EndpointRoute> generatedRoutes = generator.generateAllEndpoints();
                    
                    // Add to routes list
                    routes.addAll(generatedRoutes);
                    
                    // Log bound endpoint information
                    int rulesCount = kbSchema.getMatchRules() != null ? kbSchema.getMatchRules().size() : 0;
                    log.info("✅ Knowledge base '{}' {} retrieval endpoints bound", kbName, rulesCount);
                    boundCount++;
                } catch (Exception e) {
                    log.error("❌ Failed to bind knowledge base '{}' endpoints: {}", kbName, e.getMessage());
                }
            }
            
            log.info("✅ Endpoint processing complete: {} bound, {} unbound, {} skipped", boundCount, unboundCount, skippedCount);
        } catch (Exception e) {
            log.error("❌ Error occurred while restoring registered endpoints: {}", e.getMessage());
        }
    }
    
    /**
     * Inner class representing an endpoint route
     */
    private static class EndpointRoute {
        private String path;
        private String name;
        private String method;
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
    }

    /**
     * Simulated DynamicEndpointGenerator class
     */
    private static class DynamicEndpointGenerator {
        
        public DynamicEndpointGenerator(String kbName, KBSchema kbSchema) {
            // Constructor implementation
        }
        
        /**
         * Generate all endpoints for the knowledge base
         * @return List of generated endpoints
         */
        public List<EndpointRoute> generateAllEndpoints() {
            return new ArrayList<>();
        }
    }
}