package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.oxybank.api.model.APIResponse;
import com.jd.oxygent.oxybank.core.storer.docmanager.ElasticsearchKbBaseManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Base Retrieval Method Dynamic Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/query_interface")
public class QueryController {

    @Autowired
    private ElasticsearchKbBaseManager kbBaseClient;

    /**
     * Get endpoint registry singleton instance
     *
     * @return Endpoint registry instance
     */
    private Object getEndpointRegistry() {
        return null;
    }

    /**
     * Create knowledge base retrieval endpoint based on kb_schema corresponding to kb_name
     * <p>
     * Process:
     * 1. Query knowledge base information based on kb_name
     * 2. Check if knowledge base exists
     * 3. Get and validate kb_schema
     * 4. Use DynamicEndpointGenerator to create retrieval endpoint
     * 5. Dynamically register to Spring application
     * 6. Persist registration information to configuration file
     *
     * @param kbName Knowledge base name
     * @return APIResponse containing generated endpoint information
     */
    @PostMapping("/{kb_name}")
    public APIResponse<Map<String, Object>> createKbQueryInterface(
            @PathVariable(name = "kb_name") String kbName) {
        try {
            // 1. Check if knowledge base is already registered
            Object endpointRegistry = getEndpointRegistry();
            if (endpointRegistry != null) {
                // TODO: call endpointRegistry.isKbRegistered(kbName) when implemented
            }

            // 2. Query knowledge base information
            List<Map<String, Object>> kbInfoList = new ArrayList<>();

            if (kbInfoList == null || kbInfoList.isEmpty()) {
                return APIResponse.error(404, "Knowledge base '" + kbName + "' does not exist");
            }

            Map<String, Object> kbInfo = kbInfoList.get(0);
            Map<String, Object> kbSchemaDict = (Map<String, Object>) kbInfo.get("kb_schema");

            if (kbSchemaDict == null || kbSchemaDict.isEmpty()) {
                return APIResponse.error(400,
                        "Knowledge base '" + kbName + "' has not configured Schema, please configure Schema via POST /api/v1/kb_base/" + kbName + "/schema first");
            }

            // 3–6. KBSchema parsing, validation, DynamicEndpointGenerator, route registration: TODO when implemented
            List<String> endpoints = new ArrayList<>();
            // if (matchRules != null && !matchRules.isEmpty()) {
            //     for (int ruleIdx = 0; ruleIdx < matchRules.size(); ruleIdx++) {
            //         endpoints.add("POST /kb/" + kbName + "/search/rule_" + ruleIdx);
            //     }
            // }

            log.info("✅ Knowledge base '{}' retrieval endpoint created successfully, total {} endpoints", kbName, endpoints.size());
            return APIResponse.success(
                    "Knowledge base '" + kbName + "' retrieval endpoint created successfully",
                    Map.of(
                            "kb_name", kbName,
                            "total_rules", endpoints.size(),
                            "endpoints", endpoints,
                            "router_prefix", "/kb/" + kbName
                    )
            );
        } catch (Exception e) {
            log.error("Failed to create knowledge base '{}' retrieval endpoint", kbName, e);
            return APIResponse.error(500, "Failed to create retrieval endpoint");
        }
    }

    /**
     * Get bound knowledge base retrieval endpoint information based on kb_name
     *
     * @param kbName Knowledge base name
     * @return APIResponse containing endpoint information
     */
    @GetMapping("/{kb_name}")
    public APIResponse<List<Map<String, Object>>> getKbQueryInterface(
            @PathVariable(name = "kb_name") String kbName) {
        try {
            List<Map<String, Object>> routes = new ArrayList<>();

            // TODO: Implement route query logic when dynamic routes are registered
            // RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);
            // for (RequestMappingInfo mapping : mapping.getHandlerMethods().keySet()) {
            //     String path = mapping.getPatternsCondition().getPatterns().iterator().next();
            //     if (path.startsWith("/kb/" + kbName + "/search")) {
            //         routes.add(Map.of("path", path, "method", mapping.getMethods()));
            //     }
            // }

            List<Map<String, Object>> apiInfos = getQueryApiInfo(routes);
            log.info("Successfully queried knowledge base {} retrieval endpoint information", kbName);
            return APIResponse.success("Successfully queried knowledge base " + kbName + " endpoint information", apiInfos);
        } catch (Exception e) {
            log.error("Failed to query knowledge base {} endpoint information", kbName, e);
            return APIResponse.error(500, "Failed to query knowledge base " + kbName + " endpoint information");
        }
    }

    /**
     * Get query API info
     *
     * @param routes Route list
     * @return API info list
     */
    private List<Map<String, Object>> getQueryApiInfo(List<Map<String, Object>> routes) {
        List<Map<String, Object>> apiInfos = new ArrayList<>();
        for (Map<String, Object> route : routes) {
            apiInfos.add(Map.of(
                "path", route.getOrDefault("path", ""),
                "method", route.getOrDefault("method", "POST"),
                "description", "Dynamic query endpoint"
            ));
        }
        return apiInfos;
    }
}
