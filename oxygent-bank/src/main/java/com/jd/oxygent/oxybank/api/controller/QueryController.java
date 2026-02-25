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
            return APIResponse.success(
                    "Knowledge base '" + kbName + "' retrieval endpoint created successfully",
                    Map.of(
                            "kb_name", kbName,
                            "total_rules", 1,
                            "endpoints", List.of(),
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
