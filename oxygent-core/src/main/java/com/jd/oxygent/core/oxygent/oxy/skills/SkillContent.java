package com.jd.oxygent.core.oxygent.oxy.skills;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Full skill content, loaded on-demand when skill is invoked.
 *
 * This class contains all the information needed to activate a skill,
 * including the detailed instructions, execution environment modifications,
 * and any associated resource files.
 *
 * @author OxyGent Team
 * @version 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillContent {

    /**
     * Unique skill identifier.
     */
    private String name;

    /**
     * Short description for LLM semantic matching.
     */
    private String description;

    /**
     * Optional semantic version string.
     */
    private String version;

    /**
     * Optional author information.
     */
    private String author;

    /**
     * Full markdown instructions from SKILL.md body.
     */
    private String instructions;

    /**
     * Tools available when skill is active.
     */
    @Builder.Default
    private List<String> allowedTools = new ArrayList<>();

    /**
     * Preferred LLM model for this skill.
     */
    private String model;

    /**
     * Timeout override for skill execution in seconds.
     */
    private Double timeout;

    /**
     * Loaded resource files: filename -> content.
     */
    @Builder.Default
    private Map<String, String> resources = new HashMap<>();

    /**
     * Path to the SKILL.md file.
     */
    private Path skillPath;

    /**
     * If true, this skill must not be auto-invoked by the system/model.
     */
    @Builder.Default
    private boolean disableModelInvocation = false;

    /**
     * If false, this skill must not be manually invoked by /skill-name.
     */
    @Builder.Default
    private boolean userInvocable = true;

    /**
     * Optional hint for user-provided arguments.
     */
    private String argumentHint;

    /**
     * Format for conversation context injection.
     *
     * Creates a formatted string that can be injected into the agent's
     * conversation context when the skill is activated. This includes
     * the skill activation marker, instructions, and any resources.
     *
     * @return A formatted string ready for context injection.
     */
    public String toContextInjection() {
        StringBuilder sb = new StringBuilder();
        sb.append("[SKILL ACTIVATED: ").append(this.name).append("]\n\n");
        sb.append(this.instructions);

        // Add resources if any
        if (this.resources != null && !this.resources.isEmpty()) {
            sb.append("\n\n## Skill Resources\n\n");
            for (Map.Entry<String, String> entry : this.resources.entrySet()) {
                sb.append("### ").append(entry.getKey()).append("\n\n");
                sb.append(entry.getValue()).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * Convert to dictionary for serialization.
     *
     * @return A dictionary representation of the skill content.
     */
    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("name", this.name);
        dict.put("description", this.description);
        dict.put("version", this.version);
        dict.put("author", this.author);
        dict.put("instructions", this.instructions);
        dict.put("allowed_tools", this.allowedTools);
        dict.put("model", this.model);
        dict.put("timeout", this.timeout);
        
        // Convert resources to list of filenames
        if (this.resources != null) {
            dict.put("resources", new ArrayList<>(this.resources.keySet()));
        } else {
            dict.put("resources", new ArrayList<>());
        }
        
        dict.put("disable_model_invocation", this.disableModelInvocation);
        dict.put("user_invocable", this.userInvocable);
        dict.put("argument_hint", this.argumentHint);
        return dict;
    }

    /**
     * Get execution environment modifications.
     *
     * Returns a dictionary of modifications that should be applied to
     * the agent's execution environment when this skill is active.
     *
     * @return A dictionary with keys like 'allowed_tools', 'model', 'timeout'.
     */
    public Map<String, Object> getEnvironmentModifications() {
        Map<String, Object> mods = new HashMap<>();

        if (this.allowedTools != null && !this.allowedTools.isEmpty()) {
            mods.put("allowed_tools", this.allowedTools);
        }

        if (this.model != null && !this.model.isEmpty()) {
            mods.put("model", this.model);
        }

        if (this.timeout != null) {
            mods.put("timeout", this.timeout);
        }

        return mods;
    }

    /**
     * Create SkillContent from parsed frontmatter and body.
     *
     * @param frontmatter Parsed YAML frontmatter from SKILL.md.
     * @param body The markdown body content from SKILL.md.
     * @param skillPath Path to the SKILL.md file.
     * @param loadedResources Optional dict of loaded resource files.
     * @return A SkillContent instance.
     * @throws IllegalArgumentException If required fields (name, description) are missing.
     */
    public static SkillContent fromFrontmatterAndBody(Map<String, Object> frontmatter,String body, Path skillPath,Map<String, String> loadedResources) {
        
        // Validate required fields
        if (!frontmatter.containsKey("name")) {
            throw new IllegalArgumentException("Skill frontmatter missing required field: name");
        }
        if (!frontmatter.containsKey("description")) {
            throw new IllegalArgumentException("Skill frontmatter missing required field: description");
        }

        // Handle disable_model_invocation flag
        Object disableModelInvocationObj = frontmatter.get("disable-model-invocation");
        if (disableModelInvocationObj == null) {
            disableModelInvocationObj = frontmatter.get("disable_model_invocation");
        }
        boolean disableModelInvocation = disableModelInvocationObj != null && 
            (disableModelInvocationObj instanceof Boolean ? (Boolean) disableModelInvocationObj : 
             Boolean.parseBoolean(disableModelInvocationObj.toString()));

        // Handle user_invocable flag
        Object userInvocableObj = frontmatter.get("user-invocable");
        if (userInvocableObj == null) {
            userInvocableObj = frontmatter.get("user_invocable");
        }
        boolean userInvocable = userInvocableObj == null || 
            (userInvocableObj instanceof Boolean ? (Boolean) userInvocableObj : 
             Boolean.parseBoolean(userInvocableObj.toString()));

        // Handle argument_hint
        Object argumentHintObj = frontmatter.get("argument-hint");
        if (argumentHintObj == null) {
            argumentHintObj = frontmatter.get("argument_hint");
        }
        String argumentHint = argumentHintObj instanceof String ? (String) argumentHintObj : null;

        // Handle allowed-tools (both naming conventions)
        Object allowedToolsRaw = frontmatter.get("allowed-tools");
        if (allowedToolsRaw == null) {
            allowedToolsRaw = frontmatter.get("allowed_tools");
        }

        List<String> allowedTools = new ArrayList<>();
        if (allowedToolsRaw instanceof String) {
            allowedTools.add((String) allowedToolsRaw);
        } else if (allowedToolsRaw instanceof List) {
            for (Object item : (List<?>) allowedToolsRaw) {
                if (item instanceof String) {
                    allowedTools.add((String) item);
                }
            }
        }

        // Handle timeout
        Double timeout = null;
        Object timeoutObj = frontmatter.get("timeout");
        if (timeoutObj != null) {
            if (timeoutObj instanceof Number) {
                timeout = ((Number) timeoutObj).doubleValue();
            } else {
                try {
                    timeout = Double.parseDouble(timeoutObj.toString());
                } catch (NumberFormatException e) {
                    // Ignore invalid timeout values
                }
            }
        }

        return SkillContent.builder()
                .name((String) frontmatter.get("name"))
                .description((String) frontmatter.get("description"))
                .version((String) frontmatter.get("version"))
                .author((String) frontmatter.get("author"))
                .instructions(body)
                .allowedTools(allowedTools)
                .model((String) frontmatter.get("model"))
                .timeout(timeout)
                .resources(loadedResources != null ? loadedResources : new HashMap<>())
                .skillPath(skillPath)
                .disableModelInvocation(disableModelInvocation)
                .userInvocable(userInvocable)
                .argumentHint(argumentHint)
                .build();
    }

    /**
     * String representation for debugging.
     *
     * @return String representation of the skill content.
     */
    @Override
    public String toString() {
        String versionStr = this.version != null ? " v" + this.version : "";
        return "SkillContent(name='" + this.name + "'" + versionStr + ")";
    }
}