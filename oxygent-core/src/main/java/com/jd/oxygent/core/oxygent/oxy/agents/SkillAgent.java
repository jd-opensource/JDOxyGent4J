package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.config.Prompts;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillMetadata;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.utils.FileUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight skill-aware agent with direct path-based skill loading.
 * <p>
 * A simplified agent that loads skills directly from specified directory paths
 * without requiring global registry or SkillSource components.
 * <p>
 * This agent does NOT provide:
 * - Skill activation (manual or automatic)
 * - Skill selection via LLM
 * - Runtime skill content loading
 * <p>
 * Architecture:
 * 1. Configuration: skills=["./path/to/skill1", "./path/to/skill2"]
 * 2. Initialization: Discovers skills from each path
 * 3. Prompt Enhancement: Injects skill metadata into system prompt
 * <p>
 * Attributes:
 * skills: List of skill directory paths to load skills from.
 * Each path should point to a directory containing SKILL.md file,
 * or a parent directory containing multiple skill subdirectories.
 * skill_prompt_template: Jinja-like template for skill prompt section.
 * Use {skill_list} placeholder for skill entries.
 * _skills_metadata: Internal cache of discovered skill metadata.
 * Maps skill name -> SkillMetadata.
 * <p>
 * Example:
 * >>> agent = SkillAgent(
 * ...     name="assistant",
 * ...     skills=["./skills/project", "./skills/code"]
 * ... )
 * >>> # Agent will discover skills from both paths
 */
@SuperBuilder
@Slf4j
public class SkillAgent extends ReActAgent {

    /**
     * List of skill directory paths to load skills from Each path can be a skill folder with SKILL.md or a parent directory containing multiple skill subfolders. Supports absolute paths, project root relative paths, classpath resources, and paths within JAR files.
     */
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Builder.Default
    private Map<String, SkillMetadata> skillsMetadata = new HashMap<>();

    /**
     * Build skill list (sorted for consistency)
     */
    @Builder.Default
    private List<String> skillEntries = new ArrayList<>();

    public int getSkillsCount() {
        return skillsMetadata.size();
    }

    public List<String> getSkillNames() {
        return skillsMetadata.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Initialize the agent with skill discovery.
     * <p>
     * Initialization sequence:
     * 1. Discover skills from each specified path
     * 2. Load skill metadata (name, description, argument_hint)
     * 3. Build skill prompt section
     * 4. Inject skill prompt into additional_prompt
     * 5. Call parent init
     * <p>
     * Logs:
     * - INFO: Initialization start and completion
     * - DEBUG: Per-path skill discovery
     * - WARNING: Invalid paths or missing skills
     * - ERROR: Failed path initializations
     */
    @Override
    public void init() {
        log.info("[SkillAgent] Initializing agent '{}' with {} skill path(s)",
                getName(), skills != null ? skills.size() : 0);

        discoverSkills();
        buildSkillPrompt();
        super.init();

        log.info("[SkillAgent] Agent '{}' initialized: {} skills discovered and ready",
                getName(), getSkillsCount());
    }

    /**
     * Discover and load skill metadata directly from paths.
     * <p>
     * This method scans each path for SKILL.md files and loads
     * skill metadata directly, without using global registry.
     * <p>
     * Process:
     * 1. For each path, resolve to absolute path
     * 2. If path contains SKILL.md, load it as single skill
     * 3. If path is directory, search for SKILL.md files (recursive)
     * 4. Collect metadata into _skills_metadata
     * <p>
     * Raises:
     * No exceptions raised; errors are logged and skipped.
     * <p>
     * Note:
     * Skills with duplicate names from different paths will be
     * overwritten by the last path's version.
     */
    private void discoverSkills() {
        if (skills == null || skills.isEmpty()) {
            log.debug("[SkillAgent] Agent '{}': No skill paths configured", getName());
            return;
        }

        log.debug("[SkillAgent] Agent '{}': Discovering skills from {} path(s)",
                getName(), skills.size());

        int failedPaths = 0;
        List<Path> skillFiles = new ArrayList<>();
        for (String skillPathStr : skills) {
            try {
                skillFiles.addAll(FileUtils.findAllPaths(skillPathStr, Set.of("scripts", "references", "assets"), "SKILL.md"));
            } catch (IOException | URISyntaxException e) {
                log.warn("[SkillAgent] Agent '{}': Path does not exist: {}", getName(), skillPathStr);
                failedPaths++;
            }
        }
        for (Path skillFile : skillFiles) {
            int pathSkillCount = 0;
            SkillMetadata metadata = loadMetadataFromFile(skillFile);
            if (metadata != null && metadata.getName() != null) {
                skillsMetadata.put(metadata.getName(), metadata);
                pathSkillCount++;
            }
            if (pathSkillCount > 0) {
                log.debug("[SkillAgent] Agent '{}': Loaded {} skills from '{}'",
                        getName(), pathSkillCount, skillFile.toString());
            } else {
                failedPaths++;
                log.warn("[SkillAgent] Agent '{}': No skills discovered from '{}'",
                        getName(), skillFile.toString());
            }
        }
        log.info("[SkillAgent] Agent '{}': Discovery complete - {}, {} unique skills from {}/{}} path(s) ({} failed)",
                getName(), JsonUtils.toJSONString(getSkillNames()), getSkillsCount(), skills.size() - failedPaths, skills.size(), failedPaths);
    }

    /**
     * Build skill prompt section from discovered skill metadata.
     * <p>
     * Creates a formatted markdown skill list and injects it into the
     * agent's additional_prompt, providing the LLM with awareness of
     * available skills.
     * <p>
     * Process:
     * 1. Return early if no skills discovered
     * 2. Build sorted skill entries with name, description, args, and path
     * 3. Format using skill_prompt_template
     * 4. Append to additional_prompt (or replace if empty)
     * <p>
     * Log:
     * - DEBUG: Prompt size and skill count
     */
    private void buildSkillPrompt() {
        if (skillsMetadata.isEmpty()) {
            log.debug("[SkillAgent] Agent '{}': No skills to add to prompt", getName());
            return;
        }
        for (Map.Entry<String, SkillMetadata> entry : skillsMetadata.entrySet()) {
            String name = entry.getKey();
            SkillMetadata meta = entry.getValue();
            String entryText = "## " + name + "\n" + meta.getDescription();
            if (meta.getSkillPath() != null) {
                entryText += "\nCheck \"" + meta.getSkillPath() + "\" for how to use this skill";
            }
            skillEntries.add(entryText);
        }
    }

    @Override
    protected OxyRequest beforeExecute(OxyRequest oxyRequest) {
        super.beforeExecute(oxyRequest);
        oxyRequest.setArguments("skill_list", String.join("\n\n", skillEntries));
        return oxyRequest;
    }

    /**
     * Get metadata for a specific skill.
     * <p>
     * Example:
     * >>> meta = agent.get_skill_metadata("code-review")
     * >>> if meta:
     * ...     print(f"Description: {meta.description}")
     *
     * @param skillName Name of the skill to retrieve.
     * @return SkillMetadata if found, None otherwise.
     */
    public SkillMetadata getSkillMetadata(String skillName) {
        return skillsMetadata.get(skillName);
    }

    /**
     * List all available skill names.
     * <p>
     * Note:
     * This is an alias for the skill_names property.
     *
     * @return Sorted list of skill names.
     */
    public List<String> listSkills() {
        return getSkillNames();
    }

    /**
     * Load metadata from a SKILL.md file.
     * <p>
     * Parses the frontmatter to extract name and description.
     * Does not read the full markdown body to minimize I/O.
     *
     * @param skillPath Path to the SKILL.md file.
     * @return SkillMetadata if successful, None otherwise.
     */
    private static SkillMetadata loadMetadataFromFile(Path skillPath) {
        try {
            String frontmatterLines = Files.readString(skillPath);
            Map frontmatter = parseSimpleFormatter(Arrays.stream(frontmatterLines.split("\n")).toList());
            if (frontmatter.isEmpty()) {
                log.warn("Empty frontmatter in: {}", skillPath);
                return null;
            }
            return SkillMetadata.fromFrontmatter(frontmatter, skillPath);
        } catch (Exception e) {
            log.error("Error reading frontmatter from: {}", skillPath, e);
            return null;
        }
    }

    /**
     * Parse simple key-value frontmatter (no nested structures).
     * <p>
     * Supports basic 'key: value' format only.
     *
     * @param lines List of frontmatter lines.
     * @return Dictionary of key-value pairs.
     */
    private static Map<String, String> parseSimpleFormatter(List<String> lines) {
        Map<String, String> result = new HashMap<>();
        for (String line : lines) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.contains(":")) {
                int colonIndex = line.indexOf(":");
                String key = line.substring(0, colonIndex).strip();
                String value = line.substring(colonIndex + 1).strip();
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        }
        return result;
    }
}