package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.*;

/**
 * User profile bank tools providing user profile retrieval and update functionality.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class UserProfileBankTools {

    private static final Map<String, String> USER_PROFILE_DICT;

    static {
        // Initialize with the exact same data as Python version
        Map<String, String> tempDict = new HashMap<>();
        tempDict.put("001", "Arlen, a student, likes music");
        tempDict.put("002", "Tom, a programmer, likes sports");
        USER_PROFILE_DICT = Collections.unmodifiableMap(tempDict);
    }

    /**
     * A tool for querying user profile
     *
     * @param query query (not used in Python version but kept for compatibility)
     * @param userPin SystemArg.agent_pin
     * @param agentPin SystemArg.user_pin
     * @return The current user profile information
     */
    @MCPTool(name = "user_profile_retrieve",
            description = "A tool for querying user profile")
    public Map<String, Object> userProfileRetrieve(
            @ToolParam(description = "query")
            String query,
            @ToolParam(description = "SystemArg.agent_pin")
            String userPin,
            @ToolParam(description = "SystemArg.user_pin")
            String agentPin) {

        Map<String, Object> response = new LinkedHashMap<>();

        // Get portrait from dictionary
        String portrait = USER_PROFILE_DICT.getOrDefault(userPin, "Nothing");

        // Build the exact same response as Python version
        response.put("message", String.format("The current user profile is: %s", portrait));

        return response;
    }

    /**
     * A tool for updating user profile
     *
     * @param content content to update
     * @param userPin SystemArg.agent_pin
     * @param agentPin SystemArg.user_pin
     * @return Update status message
     */
    @MCPTool(name = "user_profile_deposit",
            description = "A tool for updating user profile")
    public Map<String, Object> userProfileDeposit(
            @ToolParam(description = "content")
            String content,
            @ToolParam(description = "SystemArg.agent_pin")
            String userPin,
            @ToolParam(description = "SystemArg.user_pin")
            String agentPin) {

        Map<String, Object> response = new LinkedHashMap<>();

        // Print to console (simulating Python's print statement)
        System.out.printf("%s %s %s%n", agentPin, userPin, content);

        // Note: Python version doesn't actually update the dictionary, just returns a message
        // We'll keep the same behavior
        response.put("message", "updated user_profile");

        return response;
    }

    /**
     * Main method to start the MCP server
     *
     * @param args Command line arguments
     */
    @EnableMcpServer(mode = "web",transport = "sse")
    public static void main(String[] args) {
        McpServer.start();
    }
}