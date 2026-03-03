/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.oxy.skills;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.oxy.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill tool for skill invocation.
 *
 * <p>This class provides the SkillTool that agents use to invoke skills. When invoked,
 * it loads the full skill content and injects it into the agent's context along with
 * environment modifications.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>On-demand loading: Full skill content is loaded only when invoked</li>
 *   <li>Context injection: Injects skill instructions into conversation context</li>
 *   <li>Environment modifications: Returns modifications for agent to apply</li>
 *   <li>Policy enforcement: Enforces invocation source restrictions</li>
 * </ul>
 *
 * <h3>Invocation Flow:</h3>
 * <ol>
 *   <li>Validate skill name and parameters</li>
 *   <li>Load full skill content from registry (on-demand)</li>
 *   <li>Check invocation policy based on source</li>
 *   <li>Build context injection with skill instructions</li>
 *   <li>Return environment modifications for agent to apply</li>
 *   <li>Return control to agent with enriched context</li>
 * </ol>
 *
 * <h3>Important Notes:</h3>
 * <p>The tool itself does NOT execute the skill's task. Instead, it loads
 * the skill and modifies the agent's context, allowing the agent to continue
 * execution with the skill's guidance.</p>
 *
 * <h3>Invocation Sources:</h3>
 * <ul>
 *   <li><b>user</b>: Manual invocation via /skill-name command</li>
 *   <li><b>selector</b>: Automatic selection by skill selector</li>
 *   <li><b>model</b>: Direct invocation by LLM (not supported in this runtime)</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillTool extends BaseTool {

    private static final Logger logger = LoggerFactory.getLogger(SkillTool.class);

    private static final String TOOL_NAME = "Skill";
    private static final String TOOL_DESC = "Invoke a skill to get specialized instructions and capabilities";

    private static final String INVOCATION_SOURCE_USER = "user";
    private static final String INVOCATION_SOURCE_SELECTOR = "selector";
    private static final String INVOCATION_SOURCE_MODEL = "model";
    
    // Cache scoped registries by normalized directory tuple
    private static final Map<List<String>, SkillRegistry> SCOPED_REGISTRY_CACHE = new ConcurrentHashMap<>();

    /**
     * Reference to the skill registry for loading skills.
     * This is excluded from serialization.
     */
    @JsonIgnore
    private SkillRegistry skillRegistry;

    /**
     * Constructor - Initialize Skill tool with default settings.
     */
    public SkillTool() {
        super(TOOL_NAME, TOOL_DESC);
        //Skills don't require explicit permission
        this.setPermissionRequired(false);
        //set default input schema for the tool
        this.setInputSchema(new HashMap<>(){{
            Map<String, Object> nameProp = new HashMap<>();
            nameProp.put("type", "string");
            nameProp.put("description", "The name of the skill to invoke");

            Map<String, Object> argumentsProp = new HashMap<>();
            argumentsProp.put("type", "string");
            argumentsProp.put("description", "Optional argument string passed to the skill (used for $ARGUMENTS substitution)");

            Map<String, Object> invocationSourceProp = new HashMap<>();
            invocationSourceProp.put("type", "string");
            invocationSourceProp.put("description", "Invocation source: user | selector | model");

            Map<String, Object> skillDirsProp = new HashMap<>();
            skillDirsProp.put("type", "array");
            Map<String, Object> itemsSchema = new HashMap<>();
            itemsSchema.put("type", "string");
            skillDirsProp.put("items", itemsSchema);
            skillDirsProp.put("description", "Optional absolute skill directories for scoped resolution");

            Map<String, Object> properties = new HashMap<>();
            properties.put("name", nameProp);
            properties.put("arguments", argumentsProp);
            properties.put("invocation_source", invocationSourceProp);
            properties.put("skill_dirs", skillDirsProp);

            this.put("type", "object");
            this.put("properties", properties);
            this.put("required", Collections.singletonList("name"));
        }});
        //Generate LLM-friendly description
        this.setDescForLlm();
    }

    /**
     * Set skill registry reference.
     *
     * <p>This is typically called during MAS initialization.</p>
     *
     * @param registry The skill registry instance.
     */
    public void setRegistry(SkillRegistry registry) {
        this.skillRegistry = registry;
    }

    /**
     * Execute skill invocation.
     *
     * <p>This method loads skill content and returns it for context injection.
     * It does NOT directly execute the skill's task.</p>
     *
     * <h4>Execution Flow:</h4>
     * <ol>
     *   <li>Extract skill name, arguments, and invocation source from request</li>
     *   <li>Get skill registry from request's mas if not set</li>
     *   <li>Check if skill exists in registry</li>
     *   <li>Load full skill content (on-demand)</li>
     *   <li>Enforce invocation policy based on source</li>
     *   <li>Build context injection with skill instructions</li>
     *   <li>Build execution environment modifications</li>
     *   <li>Log skill activation</li>
     * </ol>
     *
     * @param oxyRequest The request containing skill name in arguments.
     * @return OxyResponse with:
     *         <ul>
     *           <li>state: COMPLETED if skill found, FAILED/SKIPPED otherwise</li>
     *           <li>output: Context injection string with skill instructions</li>
     *           <li>extra: Environment modifications and skill metadata</li>
     *         </ul>
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        Object skillNameObj = oxyRequest.getArguments().get("name");
        Object skillArgsObj = oxyRequest.getArguments().get("arguments");
        Object invocationSourceObj = oxyRequest.getArguments().get("invocation_source");
        Object scopedSkillDirsObj = oxyRequest.getArguments().get("skill_dirs");

        String skillName = (skillNameObj instanceof String) ? (String) skillNameObj : null;
        String skillArgs = (skillArgsObj instanceof String) ? (String) skillArgsObj : (skillArgsObj == null) ? "" : skillArgsObj.toString();
        
        String invocationSource = (invocationSourceObj instanceof String) ? (String) invocationSourceObj : INVOCATION_SOURCE_MODEL;
        if (invocationSource == null || invocationSource.trim().isEmpty()) {
            invocationSource = INVOCATION_SOURCE_MODEL;
        }
        invocationSource = invocationSource.trim();

        if (skillName == null || skillName.trim().isEmpty()) {
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Skill name is required. Usage: Skill(name=\"skill-name\")")
                    .build();
        }
        // Get skill registry from request's mas if not set
        SkillRegistry registry = this.skillRegistry;
        
        // Handle scoped skill directories
        if (scopedSkillDirsObj != null) {
            try {
                registry = getScopedRegistry(scopedSkillDirsObj);
            } catch (Exception e) {
                return OxyResponse.builder()
                        .state(OxyState.FAILED)
                        .output("Failed to create scoped registry: " + e.getMessage())
                        .build();
            }
        } else if (registry == null) {
            if (oxyRequest.getMas() != null) {
                registry = oxyRequest.getMas().getSkillRegistry();
            }
        }

        if (registry == null) {
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Skill registry not initialized. Cannot invoke skills.")
                    .build();
        }
        //Check if skill exists
        if (!registry.hasSkill(skillName)) {
            List<String> available = registry.listSkills().stream()
                    .map(SkillMetadata::getName)
                    .collect(Collectors.toList());
            String availableStr = String.join(", ", available);
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Skill '" + skillName + "' not found. Available skills: " + availableStr)
                    .build();
        }
        // Load full skill content (on-demand)
        SkillContent skillContent = registry.loadFullContent(skillName);
        if (skillContent == null) {
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Failed to load content for skill '" + skillName + "'")
                    .build();
        }
        // Enforce invocation policy
        if (INVOCATION_SOURCE_MODEL.equals(invocationSource)) {
            return OxyResponse.builder()
                    .state(OxyState.SKIPPED)
                    .output("Skill tool invocation from the model is not supported in this runtime. " +
                             "Use selector-based activation or manual /skill-name invocation.")
                    .build();
        }

        if (INVOCATION_SOURCE_SELECTOR.equals(invocationSource) && skillContent.isDisableModelInvocation()) {
            return OxyResponse.builder()
                    .state(OxyState.SKIPPED)
                    .output("Skill '" + skillName + "' has disable-model-invocation=true; selector activation blocked.")
                    .build();
        }

        if (INVOCATION_SOURCE_USER.equals(invocationSource) && !skillContent.isUserInvocable()) {
            return OxyResponse.builder()
                    .state(OxyState.SKIPPED)
                    .output("Skill '" + skillName + "' is not user-invocable (user-invocable=false).")
                    .build();
        }
        // Build context injection
        String contextInjection = skillContent.toContextInjection();
        if (skillArgs != null && !skillArgs.trim().isEmpty()) {
            if (contextInjection.contains("$ARGUMENTS")) {
                contextInjection = contextInjection.replace("$ARGUMENTS", skillArgs);
            } else {
                contextInjection = contextInjection + "\n\nARGUMENTS: " + skillArgs;
            }
        }

        Map<String, Object> envMods = skillContent.getEnvironmentModifications();

        logger.info("Skill activated: {}", skillName);

        Map<String, Object> extra = new HashMap<>();
        extra.put("skill_name", skillName);
        extra.put("invocation_source", invocationSource);
        extra.put("environment_modifications", envMods);
        extra.put("context_type", "skill_injection");
        extra.put("skill_version", skillContent.getVersion());
        extra.put("skill_author", skillContent.getAuthor());

        return OxyResponse.builder()
                .state(OxyState.COMPLETED)
                .output(contextInjection)
                .extra(extra)
                .build();
    }

    /**
     * Generate LLM-friendly description.
     *
     * <p>Sets the description that will be shown to the LLM when
     * deciding which tools to use.</p>
     */
    @Override
    public void setDescForLlm() {
        String descForLlm = String.format(
                "Tool: %s\n" +
                " Description: %s\n" +
                " Arguments:\n" +
                " - name: string, The name of the skill to invoke (required)\n" +
                " - arguments: string, Optional argument string passed to the skill\n" +
                " - invocation_source: string, Invocation source (user|selector|model)\n",
                " - skill_dirs: string[], Optional absolute directories for scoped skill resolution\n",
                TOOL_NAME,
                TOOL_DESC
        );
        super.setDescForLlm(descForLlm);
    }

    /**
     * Normalize skill directories.
     *
     * <p>Validates and normalizes skill directory paths for scoped registry creation.</p>
     *
     * @param skillDirsObj Object containing skill directory paths
     * @return List of normalized absolute directory paths
     * @throws IllegalArgumentException if validation fails
     */
    @SuppressWarnings("unchecked")
    private List<String> normalizeSkillDirs(Object skillDirsObj) {
        if (!(skillDirsObj instanceof List)) {
            throw new IllegalArgumentException("skill_dirs must be a list of absolute directory paths");
        }
        
        List<?> rawDirs = (List<?>) skillDirsObj;
        if (rawDirs.isEmpty()) {
            throw new IllegalArgumentException("skill_dirs must not be empty");
        }
        
        List<String> normalized = new ArrayList<>();
        for (Object raw : rawDirs) {
            if (!(raw instanceof String) || ((String) raw).trim().isEmpty()) {
                throw new IllegalArgumentException("skill_dirs contains an empty path");
            }
            
            String pathStr = ((String) raw).trim();
            Path path = Paths.get(pathStr);
            
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException("skill_dirs path must be absolute: " + pathStr);
            }
            
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("skill_dirs path does not exist: " + pathStr);
            }
            
            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException("skill_dirs path is not a directory: " + pathStr);
            }
            
            String resolved = path.toAbsolutePath().normalize().toString();
            if (!normalized.contains(resolved)) {
                normalized.add(resolved);
            }
        }
        
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("skill_dirs must not be empty");
        }
        
        return normalized;
    }
    
    /**
     * Get scoped registry for specific skill directories.
     *
     * <p>Creates or retrieves a cached SkillRegistry instance for the given directories.</p>
     *
     * @param skillDirsObj Object containing skill directory paths
     * @return SkillRegistry instance for the scoped directories
     * @throws Exception if registry creation fails
     */
    private SkillRegistry getScopedRegistry(Object skillDirsObj) throws Exception {
        List<String> normalized = normalizeSkillDirs(skillDirsObj);
        
        // Check cache first
        SkillRegistry cached = SCOPED_REGISTRY_CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }
        
        // Create new scoped registry
        SkillRegistry registry = new SkillRegistry(normalized);
        // Note: In a real implementation, you would need to modify SkillRegistry
        // to accept skill directories and auto-discovery settings
        // For now, we'll use the existing registry pattern
        
        SCOPED_REGISTRY_CACHE.put(new ArrayList<>(normalized), registry);
        return registry;
    }
}