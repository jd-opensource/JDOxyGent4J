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

package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillMetadata;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillRegistry;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillSelector;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.tools.ShellTools;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SkillAgent: ReActAgent with first-class skills support.
 *
 * <p>Skill activation sources:</p>
 * <ul>
 *   <li>Manual: user query starts with `/<skill-name> ...`</li>
 *   <li>Selector: optional extra LLM call over metadata only (name + description)</li>
 * </ul>
 *
 * <p>This agent never relies on the LLM to call the Skill tool.</p>
 *
 * <p>Main features:</p>
 * <ul>
 *   <li>Skill catalog injection into prompts</li>
 *   <li>Manual skill activation via slash commands</li>
 *   <li>Automatic skill selection using LLM</li>
 *   <li>Shell tools auto-registration</li>
 *   <li>Skill listing and help functionality</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * SkillAgent skillAgent = SkillAgent.builder()
 *     .enableSkillCatalog(true)
 *     .skillCatalogMaxEntries(50)
 *     .enableSelector(true)
 *     .selectorMaxCandidates(30)
 *     .selectorMinConfidence(0.6)
 *     .enableShellTools(true)
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SkillAgent extends ReActAgent {

    // ========== Skill Catalog Configuration ==========

    /**
     * Whether to inject the available skills (metadata only) into the prompt.
     */
    @Builder.Default
    protected boolean enableSkillCatalog = true;

    /**
     * Max number of skill metadata entries injected into the prompt.
     */
    @Builder.Default
    protected int skillCatalogMaxEntries = 50;

    // ========== Skill Selector Configuration ==========

    /**
     * Whether to run a selector LLM call to auto-activate at most one skill.
     */
    @Builder.Default
    protected boolean enableSelector = true;

    /**
     * Max candidate skills passed to selector.
     */
    @Builder.Default
    protected int selectorMaxCandidates = 30;

    /**
     * Min confidence required to auto-activate a selected skill.
     */
    @Builder.Default
    protected double selectorMinConfidence = 0.6;

    /**
     * Optional LLM model to use for selection. Defaults to llm_model.
     */
    protected String selectorLlmModel;

    // ========== Shell Tools Configuration ==========

    /**
     * Whether to auto-register and enable preset shell_tools (run_shell_command).
     * This makes Codex-style skills (e.g. agent-browser) practical by default.
     */
    @Builder.Default
    protected boolean enableShellTools = true;

    // ========== Scoped Registry Configuration ==========

    /**
     * Optional absolute skill directories for this agent only.
     * When set, this agent uses only these directories instead of the MAS global registry.
     */
    protected List<String> skillDirs;

    // ========== Internal State ==========

    /**
     * Per-agent scoped registry cache (built lazily/at init when skill_dirs is provided).
     */
    private transient SkillRegistry scopedSkillRegistry;
    
    /**
     * Normalized skill directories for scoped registry.
     */
    private transient List<String> normalizedSkillDirs;

    // ========== Constants ==========

    /**
     * Regular expression pattern for manual skill invocation.
     * Matches: /skill-name [optional arguments]
     */
    private static final Pattern MANUAL_SKILL_PATTERN = Pattern.compile(
            "^/([a-zA-Z0-9][a-zA-Z0-9_\\-]{0,127})(?:\\s+(.*))?$"
    );

    /**
     * Exact skill list query phrases (English and Chinese).
     */
    private static final String[] SKILL_LIST_QUERIES = {
            "list skills", "show skills", "skill list", "skills list",
            "what skills do you have", "what skills do u have",
            "what skill do you have", "what skill do u have",
            "which skills do you have", "which skills do u have",
            "available skills", "skills available",
            "你有什么技能", "你有哪些技能", "技能列表"
    };

    // ========== Internal State ==========

    /**
     * Cache for skill selection results during request processing.
     */
    private final Map<String, Object> skillCache = new ConcurrentHashMap<>();

    @Override
    public void init() {
        // Validate/build scoped registry early so invalid config fails fast at startup.
        ensureScopedRegistry();
        ensureShellTools();
        super.init();
    }

    /**
     * Ensure shell tools are registered and enabled.
     * Auto-registers shell_tools if enabled and not already present.
     */
    private void ensureShellTools() {
        if (!enableShellTools) {
            return;
        }

        if (getExceptTools() != null && getExceptTools().contains("shell_tools")) {
            return;
        }

        if (getMas() == null || getMas().getOxyNameToOxy() == null|| getMas().getOxyNameToOxy().isEmpty()) {
            return;
        }

        // Add shell_tools to tools list if not present
        if (!this.getTools().contains("shell_tools")) {
            this.getTools().add("shell_tools");
        }

        try {
            // Register shell_tools if not already registered
            if (!getMas().getOxyNameToOxy().containsKey("shell_tools")) {
                ShellTools shellTools = new ShellTools();
                shellTools.setMas(getMas());
                getMas().addOxy(shellTools);
            }

            // Initialize shell_tools if needed
            if (!getMas().getOxyNameToOxy().containsKey("run_shell_command")) {
                BaseOxy hub = getMas().getOxyNameToOxy().get("shell_tools");
                if (hub != null && hub instanceof ShellTools) {
                    hub.init();
                    log.debug("Shell tools initialized");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to auto-enable shell_tools: {}", e.getMessage(), e);
        }
    }

    /**
     * Parse manual skill invocation from user query.
     *
     * @param query User query string
     * @return Tuple of (skill_name, arguments) or null if not a manual invocation
     */
    private String[] parseManualInvocation(String query) {
        String q = (query == null) ? "" : query.trim();
        if (!q.startsWith("/") || q.startsWith("//")) {
            return null;
        }

        Matcher matcher = MANUAL_SKILL_PATTERN.matcher(q);
        if (!matcher.matches()) {
            return null;
        }

        String skillName = matcher.group(1);
        String args = (matcher.group(2) == null) ? "" : matcher.group(2).trim();
        return new String[]{skillName, args};
    }

    /**
     * Check if query is a skill list/help request.
     *
     * @param query User query string
     * @return true if this is a skill listing query
     */
    private boolean isSkillListQuery(String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase();
        q = q.replaceAll("\\s+", " ").trim();
        q = q.replaceAll("^[\\s\\t\\r\\n\"'`.,;:!?()\\[\\]{}]+|[\\s\\t\\r\\n\"'`.,;:!?()\\[\\]{}]+$", "");

        // Check exact matches
        for (String exactQuery : SKILL_LIST_QUERIES) {
            if (q.equals(exactQuery)) {
                return true;
            }
        }

        // Lightweight pattern matching for short questions
        if (q.length() <= 64) {
            if (q.matches("^(what|which)\\s+skills?\\b.*(do\\s+)?(you|u)\\s+have\\b.*")) {
                return true;
            }
            if (q.matches("^what\\s+skills?\\b.*\\bavailable\\b.*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Build skill catalog prompt section.
     *
     * @param oxyRequest Current request context
     * @return Formatted skill catalog prompt text
     */
    private String buildSkillCatalogPrompt(OxyRequest oxyRequest) {
        SkillRegistry registry = getEffectiveRegistry(oxyRequest);
        if (registry == null) {
            return "";
        }
        
        // Ensure metadata is loaded
        if (registry.listSkills().isEmpty()) {
            try {
                registry.discoverAll();
            } catch (Exception e) {
                log.warn("Failed to discover skills: {}", e.getMessage());
                return "";
            }
        }

        List<SkillMetadata> skills = registry.listSkills();
        if (skills.isEmpty()) {
            return "";
        }
        
        // Sort skills by name
        skills.sort(Comparator.comparing(SkillMetadata::getName));

        int maxN = Math.max(0, skillCatalogMaxEntries);
        List<SkillMetadata> shown = (maxN == 0) ? skills : skills.subList(0, Math.min(maxN, skills.size()));
        int hiddenCount = Math.max(0, skills.size() - shown.size());

        List<String> lines = new ArrayList<>();
        lines.add("## Available Skills (metadata only)");
        lines.add("");
        lines.add("Invoke manually with: /<skill-name> [task-or-arguments]");
        lines.add("(Skill activation is system-driven; do not call the Skill tool directly.)");
        lines.add("");

        // Add tip about script runner if available
        try {
            if (getPermittedToolNameList() != null && getPermittedToolNameList().contains("run_skill_script")) {
                lines.add("Tip: If an activated skill asks you to run a bundled script under its scripts/ directory,");
                lines.add("use the tool `run_skill_script` (skill_name + script_relpath + args) instead of manual path guessing.");
                lines.add("");
            }
        } catch (Exception e) {
            // Ignore errors in tip generation
        }

        // Add skill listings
        for (SkillMetadata skill : shown) {
            List<String> flags = new ArrayList<>();
            if (skill.isDisableModelInvocation()) {
                flags.add("manual-only");
            }
            if (!skill.isUserInvocable()) {
                flags.add("not-user-invocable");
            }
            
            String suffix = flags.isEmpty() ? "" : " (" + String.join(", ", flags) + ")";
            lines.add("- " + skill.getName() + ": " + skill.getDescription() + suffix);
            
            String argHint = skill.getArgumentHint();
            if (argHint != null && !argHint.trim().isEmpty()) {
                lines.add("  args: " + argHint.trim());
            }
        }

        if (hiddenCount > 0) {
            lines.add("- ... (" + hiddenCount + " more)");
        }

        return String.join("\n", lines);
    }

    /**
     * Activate a skill by calling the Skill tool.
     *
     * @param oxyRequest       Current request context
     * @param skillName        Name of skill to activate
     * @param skillArgs        Arguments for the skill
     * @param invocationSource Source of invocation ("user" or "selector")
     */
    private void activateSkill(OxyRequest oxyRequest, String skillName, String skillArgs, String invocationSource) {
        if (!oxyRequest.hasOxy("Skill")) {
            log.warn("Skill tool not registered; cannot activate skills");
            return;
        }
        
        List<String> skillDirs = getEffectiveSkillDirs();

        try {
            Map<String, Object> toolArgs = new HashMap<>();
            toolArgs.put("name", skillName);
            toolArgs.put("arguments", skillArgs != null ? skillArgs : "");
            toolArgs.put("invocation_source", invocationSource);
            toolArgs.put("skill_dirs", skillDirs);

            OxyResponse toolResp = oxyRequest.call(
                    Map.of(
                            "callee", "Skill",
                            "arguments", toolArgs,
                            "is_send_message", false,
                            "is_save_history", false
                    )
            );

            if (toolResp.getState() != OxyState.COMPLETED) {
                return;
            }

            Object injection = toolResp.getOutput();
            if (injection instanceof String && !((String) injection).trim().isEmpty()) {
                // Append to additional_prompt
                String currentPrompt = (String) oxyRequest.getArguments().getOrDefault("additional_prompt", "");
                String newPrompt = appendPrompt(currentPrompt, (String) injection);
                oxyRequest.getArguments().put("additional_prompt", newPrompt);
                
                // Store activation info
                oxyRequest.getArguments().put("_skill_activation", toolResp.getExtra());
            }
        } catch (Exception e) {
            log.error("Failed to activate skill {}: {}", skillName, e.getMessage(), e);
        }
    }

    /**
     * Append prompt text with proper formatting.
     *
     * @param base  Base prompt text
     * @param extra Additional text to append
     * @return Combined prompt text
     */
    private String appendPrompt(String base, String extra) {
        String baseTrimmed = (base == null) ? "" : base.trim();
        String extraTrimmed = (extra == null) ? "" : extra.trim();

        if (extraTrimmed.isEmpty()) {
            return baseTrimmed;
        }
        if (baseTrimmed.isEmpty()) {
            return extraTrimmed;
        }
        return baseTrimmed + "\n\n" + extraTrimmed;
    }

    /**
     * Select skill automatically using LLM-based selection.
     *
     * @param oxyRequest Current request context
     * @param query      User query
     * @param selectorSkills List of candidate skills
     * @return SkillSelection result
     */
    private SkillSelector.SkillSelection selectSkillWithSelector(OxyRequest oxyRequest,String query,List<SkillMetadata> selectorSkills) {
        
        String modelToUse = (selectorLlmModel != null && !selectorLlmModel.isEmpty()) ? selectorLlmModel : getLlmModel();
        
        try {
            CompletableFuture<SkillSelector.SkillSelection> future = SkillSelector.selectSkill(
                oxyRequest,
                modelToUse,
                selectorSkills,
                query,
                selectorMaxCandidates,
                selectorMinConfidence
            );
            
            // In Java, we need to handle the CompletableFuture synchronously
            // In production, you might want to make this properly async
            return future.get(); // This blocks - consider using async processing
            
        } catch (Exception e) {
            log.warn("Failed to perform skill selection with LLM: {}", e.getMessage());
            return SkillSelector.SkillSelection.selectorLlmFailed(OxyState.FAILED);
        }
    }

    /**
     * Normalize skill directories configuration.
     * 
     * @return Normalized directory list or null if not configured
     */
    private List<String> normalizeSkillDirs() {
        List<String> rawDirs = this.skillDirs;
        if (rawDirs == null || rawDirs.isEmpty()) {
            return null;
        }
        
        List<String> normalized = new ArrayList<>();
        for (String raw : rawDirs) {
            if (raw == null || raw.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("SkillAgent[%s] skillDirs contains an empty path", getName())
                );
            }
            
            Path path = Paths.get(raw.trim());
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(
                    String.format("SkillAgent[%s] skillDirs path must be absolute: %s", getName(), raw)
                );
            }
            
            if (!Files.exists(path)) {
                throw new IllegalArgumentException(
                    String.format("SkillAgent[%s] skillDirs path does not exist: %s", getName(), raw)
                );
            }
            
            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException(
                    String.format("SkillAgent[%s] skillDirs path is not a directory: %s", getName(), raw)
                );
            }
            
            String resolved = path.toAbsolutePath().normalize().toString();
            if (!normalized.contains(resolved)) {
                normalized.add(resolved);
            }
        }
        
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Ensure scoped registry is built and cached.
     */
    private void ensureScopedRegistry() {
        List<String> normalized = normalizeSkillDirs();
        if (normalized == null) {
            this.scopedSkillRegistry = null;
            this.normalizedSkillDirs = null;
            return;
        }

        if (this.scopedSkillRegistry != null && 
            this.normalizedSkillDirs != null && 
            this.normalizedSkillDirs.containsAll(normalized)) {
            return;
        }

        // Build new scoped registry
        this.scopedSkillRegistry = new SkillRegistry(normalized, true);
        this.normalizedSkillDirs = new ArrayList<>(normalized);
    }

    /**
     * Get effective registry for this agent.
     * 
     * @param oxyRequest Current request context
     * @return Effective skill registry
     */
    private SkillRegistry getEffectiveRegistry(OxyRequest oxyRequest) {
        // Use scoped registry if configured
        this.ensureScopedRegistry();
        if (this.scopedSkillRegistry != null) {
            return this.scopedSkillRegistry;
        }
        
        // Fall back to request MAS registry
        if (oxyRequest != null && oxyRequest.getMas() != null) {
            SkillRegistry registry = oxyRequest.getMas().getSkillRegistry();
            if (registry != null) {
                return registry;
            }
        }

        // Fall back to agent MAS registry
        if (getMas() != null) {
            return getMas().getSkillRegistry();
        }
        
        return null;
    }

    /**
     * Get effective skill directories for this agent.
     * 
     * @return List of skill directories or null
     */
    private List<String> getEffectiveSkillDirs() {
        if (this.scopedSkillRegistry == null) {
            return null;
        }
        return this.normalizedSkillDirs != null ? new ArrayList<>(this.normalizedSkillDirs) : null;
    }

    @Override
    protected OxyRequest beforeExecute(OxyRequest oxyRequest) {
        oxyRequest = super.beforeExecute(oxyRequest);

        SkillRegistry registry = getEffectiveRegistry(oxyRequest);
        if (registry == null) {
            return oxyRequest;
        }

        // Inject skill catalog if enabled
        if (enableSkillCatalog) {
            String catalogPrompt = buildSkillCatalogPrompt(oxyRequest);
            String currentPrompt = (String) oxyRequest.getArguments().getOrDefault("additional_prompt", "");
            String newPrompt = appendPrompt(currentPrompt, catalogPrompt);
            oxyRequest.getArguments().put("additional_prompt", newPrompt);
        }

        String rawQuery = (String) oxyRequest.getArguments().getOrDefault("query", "");

        // 0) Skill list/help queries: answer deterministically from metadata.
        if (isSkillListQuery(rawQuery)) {
            if (registry.listSkills().isEmpty()) {
                try {
                    registry.discoverAll();
                } catch (Exception e) {
                    log.warn("Failed to discover skills for help: {}", e.getMessage());
                }
            }
            
            String helpText = "";
            try {
                helpText = registry.generateUserHelpSection();
            } catch (Exception e) {
                log.warn("Failed to generate skill help: {}", e.getMessage());
                helpText = "";
            }
            
            if (helpText != null && !helpText.trim().isEmpty()) {
                oxyRequest.getArguments().put("_skill_help_output", helpText);
                return oxyRequest;
            }
        }

        // 1) Manual activation overrides selector.
        String[] manualInvocation = parseManualInvocation(rawQuery);
        if (manualInvocation != null) {
            String skillName = manualInvocation[0];
            String args = manualInvocation[1];
            
            if (!registry.hasSkill(skillName)) {
                List<String> available = registry.listSkills().stream()
                        .map(SkillMetadata::getName)
                        .sorted()
                        .collect(Collectors.toList());
                String availableStr = available.isEmpty() ? "(none)" : String.join(", ", available);
                
                String errorMsg = String.format(
                    "Skill '%s' not found in this agent scope. Available skills: %s",
                    skillName, availableStr
                );
                oxyRequest.getArguments().put("_skill_help_output", errorMsg);
                return oxyRequest;
            }
            
            activateSkill(oxyRequest, skillName, args, "user");
            
            // Update query to reflect skill activation
            String newQuery = args.isEmpty() ?  String.format("Use the activated skill '%s'.", skillName) : args;
            oxyRequest.getArguments().put("query", newQuery);

            return oxyRequest;
        }

        // 2) Selector activation (metadata only).
        if (enableSelector) {
            try {
                List<SkillMetadata> skills = registry.listSkills();
                if (!skills.isEmpty()) {
                    List<SkillMetadata> selectorSkills = skills.stream()
                            .filter(skill -> !skill.isDisableModelInvocation())
                            .collect(Collectors.toList());
                    
                    if (!selectorSkills.isEmpty()) {
                        SkillSelector.SkillSelection selectedSkillObj = selectSkillWithSelector(oxyRequest, rawQuery,selectorSkills);
                        String selectedSkill = selectedSkillObj.getSelectedSkill();
                        if (selectedSkill != null) {
                            oxyRequest.getArguments().put("_skill_selection", Map.of(
                                    "selected_skill", selectedSkill,
                                    "confidence", selectedSkillObj.getConfidence(), // confidence
                                        "reason", selectedSkillObj.getReason()
                            ));
                            activateSkill(oxyRequest, selectedSkill, "", "selector");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to perform skill selection: {}", e.getMessage());
            }
        }

        return oxyRequest;
    }

    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        // Handle skill help output
        Object helpText = oxyRequest.getArguments().get("_skill_help_output");
        if (helpText instanceof String && !((String) helpText).trim().isEmpty()) {
            return OxyResponse.builder()
                    .state(OxyState.COMPLETED)
                    .output(helpText)
                    .build();
        }
        
        return super._execute(oxyRequest);
    }

    @Override
    protected OxyResponse afterExecute(OxyResponse oxyResponse) {
        OxyRequest oxyRequest = oxyResponse.getOxyRequest();
        if (oxyRequest != null) {
            // Transfer skill activation info to response
            if (oxyRequest.getArguments().containsKey("_skill_activation")) {
                if(oxyRequest.getArguments().containsKey("_skill_activation")){
                    oxyResponse.getExtra().put("skill_activation",oxyRequest.getArguments().get("_skill_activation"));
                }
            }
            if (oxyRequest.getArguments().containsKey("_skill_selection")) {
                if(oxyRequest.getArguments().containsKey("_skill_selection")){
                    oxyResponse.getExtra().put("skill_selection",oxyRequest.getArguments().get("_skill_selection"));
                }
            }
        }
        return super.afterExecute(oxyResponse);
    }

}