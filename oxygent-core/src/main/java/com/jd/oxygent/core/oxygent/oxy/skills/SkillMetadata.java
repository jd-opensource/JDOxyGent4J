package com.jd.oxygent.core.oxygent.oxy.skills;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

/**
 * Lightweight skill metadata for indexing.
 *
 * This class contains only the essential information about a skill that is
 * needed for discovery and matching. It is loaded at startup and injected
 * into the agent's system prompt to enable LLM-based skill selection.
 *
 * The full skill content is loaded on-demand when the skill is invoked.
 *
 * @author OxyGent Team
 * @version 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillMetadata {

    /**
     * Unique skill identifier used to invoke the skill.
     */
    private String name;

    /**
     * Short description for LLM semantic matching.
     */
    private String description;

    /**
     * Path to the SKILL.md file for on-demand loading.
     */
    private Path skillPath;

    /**
     * Optional semantic version string.
     */
    private String version;

    /**
     * Optional author information.
     */
    private String author;

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
     * Format for system prompt injection.
     *
     * Returns a formatted string suitable for inclusion in the agent's
     * system prompt, showing the skill name and description.
     *
     * @return A formatted markdown string with skill name and description.
     */
    public String toPromptEntry() {
        return "- **" + this.name + "**: " + this.description;
    }

    /**
     * Convert to dictionary for serialization.
     *
     * @return A dictionary representation of the metadata, excluding the
     *         skill_path which is not needed for serialization.
     */
    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("name", this.name);
        dict.put("description", this.description);
        dict.put("version", this.version);
        dict.put("author", this.author);
        dict.put("disable_model_invocation", this.disableModelInvocation);
        dict.put("user_invocable", this.userInvocable);
        dict.put("argument_hint", this.argumentHint);
        return dict;
    }

    /**
     * Create SkillMetadata from parsed frontmatter.
     *
     * @param frontmatter Parsed YAML frontmatter from SKILL.md.
     * @param skillPath Path to the SKILL.md file.
     * @return A SkillMetadata instance.
     * @throws IllegalArgumentException If required fields (name, description) are missing.
     */
    public static SkillMetadata fromFrontmatter(Map<String, Object> frontmatter, Path skillPath) {
        
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

        return SkillMetadata.builder()
                .name((String) frontmatter.get("name"))
                .description((String) frontmatter.get("description"))
                .skillPath(skillPath)
                .version((String) frontmatter.get("version"))
                .author((String) frontmatter.get("author"))
                .disableModelInvocation(disableModelInvocation)
                .userInvocable(userInvocable)
                .argumentHint(argumentHint)
                .build();
    }

    /**
     * String representation for debugging.
     *
     * @return String representation of the skill metadata.
     */
    @Override
    public String toString() {
        String versionStr = this.version != null ? " v" + this.version : "";
        return "SkillMetadata(name='" + this.name + "'" + versionStr + ")";
    }
}