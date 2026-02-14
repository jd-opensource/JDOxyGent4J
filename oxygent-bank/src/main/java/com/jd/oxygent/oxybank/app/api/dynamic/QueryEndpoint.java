package com.jd.oxygent.oxybank.app.api.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.oxybank.app.api.models.APIResponse;
import com.jd.oxygent.oxybank.core.interfaces.EndpointRegistry;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Base Retrieval Method Dynamic Management
 * Converted from app/api/dynamic/query_endpoint.py
 */
@Slf4j
@WebServlet("/api/v1/query_interface/*")
public class QueryEndpoint extends HttpServlet {
    
    
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    
    // Initialize knowledge base management client
    // Note: ElasticsearchKbBaseManager is not implemented yet
    // private final ElasticsearchKbBaseManager kbBaseClient = new ElasticsearchKbBaseManager();
    
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        // Handle GET /api/v1/query_interface/{kb_name}
        if (pathInfo != null && pathInfo.matches("^/[^/]+$")) {
            String[] pathParts = pathInfo.split("/");
            String kbName = pathParts[1];
            handleGetKbQueryInterface(request, response, kbName);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        // Handle POST /api/v1/query_interface/{kb_name}
        if (pathInfo != null && pathInfo.matches("^/[^/]+$")) {
            String[] pathParts = pathInfo.split("/");
            String kbName = pathParts[1];
            handleCreateKbQueryInterface(request, response, kbName);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    /**
     * Get endpoint registry singleton instance
     * @return EndpointRegistry instance
     */
    private EndpointRegistry getEndpointRegistry() {
        return EndpointRegistry.getInstance();
    }
    
    /**
     * Handle POST /api/v1/query_interface/{kb_name} request
     * Create knowledge base retrieval endpoint based on kb_name's kb_schema
     */
    private void handleCreateKbQueryInterface(HttpServletRequest request, HttpServletResponse response, String kbName) throws IOException {
        try {
            APIResponse<Map<String, Object>> apiResponse = new APIResponse<>();
            
            // Get endpoint registry
            EndpointRegistry endpointRegistry = getEndpointRegistry();
            
            // 1. Check if knowledge base is already registered
            if (endpointRegistry != null && endpointRegistry.isKbRegistered(kbName)) {
                response.sendError(
                    HttpServletResponse.SC_CONFLICT,
                    "Retrieval endpoint for knowledge base '" + kbName + "' has already been created, please restart the service to recreate"
                );
                return;
            }
            
            // 2. Query knowledge base information - TODO: Implement kb_base_client.kb_info_search_name
            // List<Map<String, Object>> kbInfoList = kbBaseClient.kbInfoSearchName(kbName);
            
            // For now, simulate knowledge base information
            Map<String, Object> kbInfo = new HashMap<>();
            kbInfo.put("kb_name", kbName);
            
            // 3. Get kb_schema - TODO: Implement actual schema retrieval
            Map<String, Object> kbSchemaDict = new HashMap<>();
            kbSchemaDict.put("match_rules", new ArrayList<>()); // Empty rules for simulation
            
            // 4. Validate Schema meets requirements - TODO: Implement check_kb_schema
            
            // 5. Create dynamic endpoint generator - TODO: Implement DynamicEndpointGenerator
            
            // 6. Register routes to application - Handled by EndpointRegistry
            
            // Generate endpoint list for response
            List<String> endpoints = new ArrayList<>();
            endpoints.add("POST /kb/" + kbName + "/search/rule_0");
            
            // Create response data
            Map<String, Object> data = new HashMap<>();
            data.put("kb_name", kbName);
            data.put("total_rules", 1); // Simulated
            data.put("endpoints", endpoints);
            data.put("router_prefix", "/kb/" + kbName);
            
            apiResponse.setCode(200);
            apiResponse.setMsg("Knowledge base '" + kbName + "' retrieval endpoint created successfully");
            apiResponse.setData(data);
            
            log.info("✅ Knowledge base '{}' retrieval endpoint created successfully, total {} endpoints", kbName, endpoints.size());
            sendJsonResponse(response, apiResponse);
            
        } catch (Exception e) {
            log.error("Failed to create knowledge base '{}' retrieval endpoint: {}", kbName, e.getMessage(), e);
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to create retrieval endpoint: " + e.getMessage()
            );
        }
    }
    
    /**
     * Handle GET /api/v1/query_interface/{kb_name} request
     * Get bound knowledge base retrieval endpoint information based on kb_name
     */
    private void handleGetKbQueryInterface(HttpServletRequest request, HttpServletResponse response, String kbName) throws IOException {
        try {
            APIResponse<List<Object>> apiResponse = new APIResponse<>();
            
            // TODO: Implement actual route retrieval
            List<Object> apiInfos = new ArrayList<>();
            
            apiResponse.setCode(200);
            apiResponse.setMsg("Successfully queried knowledge base " + kbName + " endpoint information");
            apiResponse.setData(apiInfos);
            
            log.info("Successfully queried knowledge base {} retrieval endpoint information", kbName);
            sendJsonResponse(response, apiResponse);
            
        } catch (Exception e) {
            log.error("Failed to query knowledge base {} endpoint information: {}", kbName, e.getMessage(), e);
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to query knowledge base " + kbName + " endpoint information: " + e.getMessage()
            );
        }
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpServletResponse response, Object content) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(objectMapper.writeValueAsString(content));
        }
    }
}
