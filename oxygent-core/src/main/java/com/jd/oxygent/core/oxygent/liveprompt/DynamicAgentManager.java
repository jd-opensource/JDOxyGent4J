package com.jd.oxygent.core.oxygent.liveprompt;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.LocalAgent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
@Slf4j
public class DynamicAgentManager {
    private static DynamicAgentManager instance;

    private final Map<String, String> agentPromptMapping = new HashMap<>();
    private Mas mas;

    private DynamicAgentManager() {
        // Private constructor for singleton pattern
    }

    public static DynamicAgentManager getInstance() {
        synchronized (DynamicAgentManager.class) {
            if (instance == null) {
                instance = new DynamicAgentManager();
            }
            return instance;
        }
    }

    public boolean registerAgentsFromMas(Mas mas) {
        try {
            this.mas = mas;
            Map<String, BaseOxy> oxyNameToOxy = mas.getOxyNameToOxy();
            int registeredCount = 0;
            // Only register agents that use live prompts
            for (Object agentNameObj : oxyNameToOxy.keySet()) {
                String agentName = agentNameObj.toString();
                BaseOxy agentInstance = oxyNameToOxy.get(agentNameObj);

                // Check if this agent uses live prompts by examining its prompt
                if (agentUsesLivePrompts(agentInstance)) {
                    // Use agent's custom promptKey if set, otherwise default to {agentName}_prompt
                    String promptKey = null;
                    if (agentInstance instanceof LocalAgent agent) {
                        promptKey = agent.getPromptKey();
                    } else {
                        promptKey = agentName + "_prompt";
                    }
                    agentPromptMapping.put(agentName, promptKey);
                    registeredCount++;
                    log.info("Registered live prompt agent: {} with key: {}", agentName, promptKey);
                }
            }

            log.debug("Live prompt registration completed: {} agents registered", registeredCount);
            log.debug("Live prompt agents: {}", agentPromptMapping.keySet());
            return registeredCount > 0;

        } catch (Exception e) {
            log.error("Live prompt registration failed: {}", e);
            return false;
        }
    }

    private boolean agentUsesLivePrompts(BaseOxy agentInstance) {
        if (agentInstance instanceof LocalAgent agent) {
            return agent.isUseLivePrompt();
        } else {
            return false;
        }
    }

    public boolean updateAgentPrompt(String agentName) {
        if (mas == null || !agentPromptMapping.containsKey(agentName)) {
            log.warn("Agent not found: {}", agentName);
            return false;
        }

        try {
            Map<String, BaseOxy> oxyNameToOxy = mas.getOxyNameToOxy();
            BaseOxy agentInstance = oxyNameToOxy.get(agentName);
            String promptKey = agentPromptMapping.get(agentName);

            // Get manager (cache may already have latest data from savePrompt)
            PromptManager manager = PromptManager.getInstance();
            log.debug("Hot reload for {} using promptKey: {}", agentName, promptKey);

            // Use agent's reloadPrompt method if available
            if (agentInstance instanceof LocalAgent agent) {
                return agent.reloadPrompt();
            } else {
                // Fallback to old approach for backward compatibility
                try {
                    // Get original prompt as fallback
                    String originalPrompt = "";
                    try {
                        java.lang.reflect.Field promptField = agentInstance.getClass().getDeclaredField("prompt");
                        promptField.setAccessible(true);
                        originalPrompt = (String) promptField.get(agentInstance);
                    } catch (NoSuchFieldException ex) {
                        Method getPromptMethod = agentInstance.getClass().getMethod("getPrompt");
                        originalPrompt = (String) getPromptMethod.invoke(agentInstance);
                    }

                    // Get latest prompt from ES
                    String newPrompt = PromptManager.resolvePromptFromEs(promptKey, originalPrompt, false);

                    // Update agent prompt
                    try {
                        java.lang.reflect.Field promptField = agentInstance.getClass().getDeclaredField("prompt");
                        promptField.setAccessible(true);
                        promptField.set(agentInstance, newPrompt);
                    } catch (NoSuchFieldException ex) {
                        Method setPromptMethod = agentInstance.getClass().getMethod("setPrompt", String.class);
                        setPromptMethod.invoke(agentInstance, newPrompt);
                    }

                    // Re-set description for LLM
                    agentInstance.setDescForLlm();

                    log.info("Updated prompt for: {}", agentName);
                    return true;
                } catch (Exception ex) {
                    log.warn("Agent has no prompt attribute or reload_prompt method: {}", agentName);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Failed to update prompt for {}: {}", agentName, e);
            return false;
        }
    }

    public Map<String, Boolean> updateAllPrompts() {
        Map<String, Boolean> results = new HashMap<>();
        for (String agentName : agentPromptMapping.keySet()) {
            Boolean result = updateAgentPrompt(agentName);
            results.put(agentName, result);
        }

        int successCount = 0;
        for (Boolean success : results.values()) {
            if (success) {
                successCount++;
            }
        }
        int totalCount = results.size();
        log.info("Batch prompt update completed: {}/{}", successCount, totalCount);

        return results;
    }

    public Map<String, Boolean> updatePromptByKey(String promptKey) {
        Map<String, Boolean> results = new HashMap<>();
        for (Map.Entry<String, String> entry : agentPromptMapping.entrySet()) {
            String agentName = entry.getKey();
            String key = entry.getValue();
            if (key.equals(promptKey)) {
                Boolean result = updateAgentPrompt(agentName);
                results.put(agentName, result);
            }
        }

        if (!results.isEmpty()) {
            int successCount = 0;
            for (Boolean success : results.values()) {
                if (success) {
                    successCount++;
                }
            }
            int totalCount = results.size();
            log.info("Prompt key update completed ({}) - {}/{} successful", promptKey, successCount, totalCount);
        }

        return results;
    }

    public Map<String, String> getAgentPromptMapping() {
        return new HashMap<>(agentPromptMapping);
    }

    // Convenient hot-reload functions
    public static boolean hotReloadPrompt(String promptKey) {
        DynamicAgentManager manager = getInstance();
        Map<String, Boolean> results = manager.updatePromptByKey(promptKey);
        return results.values().stream().anyMatch(result -> result);
    }

    public static boolean hotReloadAllPrompts() {
        DynamicAgentManager manager = getInstance();
        Map<String, Boolean> results = manager.updateAllPrompts();
        return results.values().stream().anyMatch(result -> result);
    }

    public static boolean hotReloadAgent(String agentName) {
        DynamicAgentManager manager = getInstance();
        return manager.updateAgentPrompt(agentName);
    }

    // MAS setup functions
    public static void setupDynamicAgents(Mas mas) {
        log.debug("Setting up dynamic agents...");

        DynamicAgentManager manager = getInstance();
        boolean success = manager.registerAgentsFromMas(mas);

        if (success) {
            int registeredCount = manager.getAgentPromptMapping().size();
            log.debug("Dynamic agent manager setup completed: {} agents registered", registeredCount);

            // Print registered agents
            Map<String, String> agentMapping = manager.getAgentPromptMapping();
            for (Map.Entry<String, String> entry : agentMapping.entrySet()) {
                String agentName = entry.getKey();
                String promptKey = entry.getValue();
                log.debug("{} ↔ {}", agentName, promptKey);
            }

            // Auto-save existing agent prompts to database
            autoSaveAgentPromptsToDatabase(mas);
        }
    }

    private static void autoSaveAgentPromptsToDatabase(Mas mas) {
        try {
            // Use global singleton prompt manager (CRITICAL: ensures cache consistency)
            PromptManager manager = PromptManager.getInstance();

            // Get existing prompts from database to avoid duplicates
            Map<String, String> existingKeys = new HashMap<>();
            List<Map<String, Object>> prompts = manager.listPrompts(null, null, null, null);
            for (Map<String, Object> prompt : prompts) {
                String promptKey = (String) prompt.get("prompt_key");
                existingKeys.put(promptKey, promptKey);
            }

            int savedCount = 0;
            int skippedCount = 0;

            DynamicAgentManager dynamicAgentManager = getInstance();
            Map<String, String> agentPromptMapping = dynamicAgentManager.getAgentPromptMapping();
            Map<String, BaseOxy> oxyNameToOxy = mas.getOxyNameToOxy();
            // Save prompts for registered live prompt agents only
            for (String agentName : agentPromptMapping.keySet()) {
                BaseOxy agentInstance = oxyNameToOxy.get(agentName);
                // Use the promptKey that was registered (may be custom or default)
                String promptKey = agentPromptMapping.get(agentName);

                // Skip if prompt already exists in database
                if (existingKeys.containsKey(promptKey)) {
                    skippedCount++;
                    continue;
                }

                // Get agent's current prompt
                String promptContent = "";
                try {
                    java.lang.reflect.Field promptField = agentInstance.getClass().getDeclaredField("prompt");
                    promptField.setAccessible(true);
                    promptContent = (String) promptField.get(agentInstance);
                } catch (NoSuchFieldException ex) {
                    Method getPromptMethod = agentInstance.getClass().getMethod("getPrompt");
                    promptContent = (String) getPromptMethod.invoke(agentInstance);
                }

                if (promptContent.isEmpty()) {
                    // Try to get a default prompt or description
                    try {
                        java.lang.reflect.Field descriptionField = agentInstance.getClass().getDeclaredField("description");
                        descriptionField.setAccessible(true);
                        promptContent = (String) descriptionField.get(agentInstance);
                    } catch (NoSuchFieldException ex) {
                        try {
                            Method getDescriptionMethod = agentInstance.getClass().getMethod("getDescription");
                            promptContent = (String) getDescriptionMethod.invoke(agentInstance);
                        } catch (NoSuchMethodException e) {
                            promptContent = "Default prompt for " + agentName;
                        }
                    }
                }

                // Determine agent type
                String agentType = agentInstance.getClass().getSimpleName();

                // Save prompt to database
                try {
                    boolean success = manager.savePrompt(
                            promptKey,
                            promptContent,
                            "Auto-generated prompt for " + agentName,
                            "agent",
                            agentType,
                            1,
                            true,
                            null,
                            "system_auto_setup"
                    );

                    if (success) {
                        savedCount++;
                        log.info("Auto-saved prompt for {}", agentName);
                    } else {
                        log.warn("Failed to save prompt for {}", agentName);
                    }

                } catch (Exception e) {
                    log.error("Error saving prompt for {}: {}", agentName, e);
                }
            }

            if (savedCount > 0) {
                log.info("Auto-saved {} agent prompts to database", savedCount);
            }
            if (skippedCount > 0) {
                log.info("⏭Skipped {} existing prompts", skippedCount);
            }

        } catch (Exception e) {
            log.error("Failed to auto-save agent prompts: {}", e);
        }
    }
}