package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.oxybank.api.model.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bank Manager By Bank Router Controller
 * <p>
 * Provides user profile management and bank listing functionality using Oxygent framework
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bank_manager")
public class BankManagerByBankController {

    // User profile dictionary
    private static final Map<String, String> USER_PROFILE_DICT = Map.of(
            "001", "Arlen, a student, likes music",
            "002", "Tom, a programmer, likes sports"
    );

    // FIXME: Initialize Oxygent MAS and BankRouter
    // private static final MAS mas;
    // private static final BankRouter router;

    /**
     * A tool for querying user profile
     *
     * @param request User profile retrieve request
     * @return User profile information
     */
    @PostMapping("/user_profile_retrieve")
    public APIResponse<String> userProfileRetrieve(@RequestBody UserProfileRetrieveRequest request) {
        try {
            String portrait = USER_PROFILE_DICT.getOrDefault(request.getUser_pin(), "Nothing");
            return APIResponse.success("The current user profile is: " + portrait);
        } catch (Exception e) {
            log.error("Failed to retrieve user profile", e);
            return APIResponse.error(500, "Failed to retrieve user profile");
        }
    }

    /**
     * A tool for updating user profile
     *
     * @param request User profile deposit request
     * @return Update result
     */
    @PostMapping("/user_profile_deposit")
    public APIResponse<String> userProfileDeposit(@RequestBody UserProfileDepositRequest request) {
        try {
            log.info("User profile deposit: agent_pin={}, user_pin={}, content={}",
                    request.getAgent_pin(), request.getUser_pin(), request.getContent());

            // FIXME: Implement actual profile update logic using MAS
            // Output output = await router.mas.call(
            //     callee="bank_manager",
            //     arguments={
            //         "query": "Please update the user profile.",
            //         "chat": content,
            //         "profile": USER_PROFILE_DICT.getOrDefault(request.getUser_pin(), "Nothing"),
            //     },
            // );
            // USER_PROFILE_DICT.put(request.getUser_pin(), output);

            // Mock response for now
            String output = "Profile updated: " + request.getContent();
            USER_PROFILE_DICT.put(request.getUser_pin(), output);

            return APIResponse.success("updated user_profile");
        } catch (Exception e) {
            log.error("Failed to deposit user profile", e);
            return APIResponse.error(500, "Failed to deposit user profile");
        }
    }

    /**
     * List all banks
     *
     * @return List of available banks
     */
    @GetMapping("/list_banks")
    public APIResponse<List<BankInfo>> listBanks() {
        try {
            List<BankInfo> banks = new ArrayList<>();

            // Bank 1: user_profile_retrieve
            Map<String, Object> retrieveInputSchema = new HashMap<>();
            retrieveInputSchema.put("query", Map.of("description", "query", "type", "string"));
            retrieveInputSchema.put("agent_pin", Map.of("description", "SystemArg.agent_pin", "type", "string"));
            retrieveInputSchema.put("user_pin", Map.of("description", "SystemArg.user_pin", "type", "string"));

            banks.add(new BankInfo(
                    "user_profile_retrieve",
                    "/user_profile_retrieve",
                    List.of("POST"),
                    "A tool for querying user profile",
                    retrieveInputSchema
            ));

            // Bank 2: user_profile_deposit
            Map<String, Object> depositInputSchema = new HashMap<>();
            depositInputSchema.put("content", Map.of("description", "content", "type", "string"));
            depositInputSchema.put("agent_pin", Map.of("description", "SystemArg.agent_pin", "type", "string"));
            depositInputSchema.put("user_pin", Map.of("description", "SystemArg.user_pin", "type", "string"));

            banks.add(new BankInfo(
                    "user_profile_deposit",
                    "/user_profile_deposit",
                    List.of("POST"),
                    "A tool for updating user profile",
                    depositInputSchema
            ));

            return APIResponse.success("Query successful", banks);
        } catch (Exception e) {
            log.error("Failed to list banks", e);
            return APIResponse.error(500, "Failed to list banks");
        }
    }

    /**
     * Retrieval request
     */
    public static class UserProfileRetrieveRequest {
        private String query;
        private String user_pin;
        private String agent_pin;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getUser_pin() {
            return user_pin;
        }

        public void setUser_pin(String user_pin) {
            this.user_pin = user_pin;
        }

        public String getAgent_pin() {
            return agent_pin;
        }

        public void setAgent_pin(String agent_pin) {
            this.agent_pin = agent_pin;
        }
    }

    /**
     * User profile deposit request
     */
    public static class UserProfileDepositRequest {
        private String content;
        private String user_pin;
        private String agent_pin;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getUser_pin() {
            return user_pin;
        }

        public void setUser_pin(String user_pin) {
            this.user_pin = user_pin;
        }

        public String getAgent_pin() {
            return agent_pin;
        }

        public void setAgent_pin(String agent_pin) {
            this.agent_pin = agent_pin;
        }
    }

    /**
     * Bank information
     */
    public static class BankInfo {
        private String name;
        private String endpoint;
        private List<String> methods;
        private String description;
        private Map<String, Object> inputSchema;

        public BankInfo(String name, String endpoint, List<String> methods, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.endpoint = endpoint;
            this.methods = methods;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        public String getName() {
            return name;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public List<String> getMethods() {
            return methods;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Object> getInputSchema() {
            return inputSchema;
        }
    }
}
