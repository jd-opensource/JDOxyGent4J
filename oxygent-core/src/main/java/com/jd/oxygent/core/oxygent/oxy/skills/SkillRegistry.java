package com.jd.oxygent.core.oxygent.oxy.skills;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages skill discovery, metadata indexing, and on-demand loading.
 *
 * The registry implements progressive disclosure for efficient resource usage:
 * - At startup, only skill metadata (name + description) is loaded
 * - Full skill content is loaded on-demand when a skill is invoked
 * - Loaded content is cached for subsequent invocations
 *
 * @author OxyGent Team
 * @version 1.0.0
 */
@Slf4j
public class SkillRegistry {

    // Directories inside a skill that must not be treated as separate skills.
    private static final Set<String> NON_SKILL_SUBDIRS = Set.of("scripts", "references", "assets");

    // Skill directories to search
    private final List<String> skillDirs;

    // Metadata index (lightweight, always in memory)
    private final Map<String, SkillMetadata> metadataIndex = new ConcurrentHashMap<>();

    // Content cache (loaded on-demand)
    private final Map<String, SkillContent> contentCache = new ConcurrentHashMap<>();

    private final Yaml yaml = new Yaml();

    /**
     * Initialize the skill registry.
     *
     * @param skillDirs List of directories to search for skills. Defaults
     *                  to [.oxygent/skills/, ~/.oxygent/skills/].
     * @param autoDiscover If True, automatically discover skills on init.
     */
    public SkillRegistry(List<String> skillDirs, boolean autoDiscover) {
        if (skillDirs != null) {
            this.skillDirs = skillDirs;
        } else {
            // Discovery precedence: later entries override earlier ones on name collision.
            String location = SkillRegistry.class.getResource("").getPath();

            // Handle Windows paths (remove leading slash)
            if (location.startsWith("/") && location.indexOf(":") == 2) {
                location = location.substring(1); // 移除开头的"/"
            }

            Path PACKAGE_OXYGENT_DIR = Paths.get(location).getParent().getParent();
            // Priority (low -> high): preset < personal < project.
            // This follows the Codex/Claude Code convention: project-local skills override personal skills.
            List<String> DEFAULT_SKILL_DIRS = Arrays.asList(
                PACKAGE_OXYGENT_DIR.resolve("preset_skills").toString(),  // Built-in preset skills (lowest priority)
                "~/.oxygent/skills/",  // Personal OxyGent skills
                "~/.claude/skills/",  // Personal Claude/Codex skills
                ".oxygent/skills/",  // Project-local OxyGent skills
                ".claude/skills/"  // Project-local Claude/Codex skills (highest priority)
            );
            this.skillDirs = new ArrayList<>(DEFAULT_SKILL_DIRS);
        }
        if (autoDiscover) {
            discoverAll();
        }
    }

    /**
     * Default constructor with auto-discovery enabled.
     */
    public SkillRegistry() {
        this(null, true);
    }

    /**
     * Constructor with custom skill directories.
     */
    public SkillRegistry(List<String> skillDirs) {
        this(skillDirs, true);
    }

    /**
     * Discover all skills and load metadata ONLY.
     *
     * This method scans all skill directories and loads only
     * the metadata (name + description) from each SKILL.md file.
     * The full content is NOT loaded to minimize startup time and memory.
     *
     * @return A list of discovered skill names.
     */
    public List<String> discoverAll() {
        List<String> discovered = new ArrayList<>();

        for (String skillDir : this.skillDirs) {
            Path path = Paths.get(skillDir).toAbsolutePath().normalize();
            
            // Expand user home directory
            if (skillDir.startsWith("~")) {
                String homeDir = System.getProperty("user.home");
                path = Paths.get(homeDir + skillDir.substring(1)).toAbsolutePath().normalize();
            }

            if (!Files.exists(path)) {
                log.debug("Skill directory does not exist: {}", skillDir);
                continue;
            }

            if (!Files.isDirectory(path)) {
                log.warn("Skill path is not a directory: {}", skillDir);
                continue;
            }

            try {
                final Path finalPath = path;
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equals("SKILL.md"))
                        .forEach(skillFile -> {
                            // Support the standard "skill folder" layout where a skill may contain
                            // scripts/references/assets. Those subdirectories may themselves contain
                            // a file named SKILL.md (e.g., in templates), which must not be discovered
                            // as an independent skill.
                            try {
                                Path relativePath = finalPath.relativize(skillFile);
                                String[] relParts = relativePath.toString().split("[/\\\\]");
            
                                // rel_parts like: (<skill-name>, 'SKILL.md') or
                                // (<group>, <skill-name>, 'SKILL.md') or
                                // (<skill-name>, 'assets', ..., 'SKILL.md')
                                boolean isNonSkillSubdir = false;
                                for (int i = 1; i < relParts.length - 1; i++) {
                                    if (NON_SKILL_SUBDIRS.contains(relParts[i])) {
                                        isNonSkillSubdir = true;
                                        break;
                                    }
                                }
            
                                if (isNonSkillSubdir) {
                                    return;
                                }
            
                                SkillMetadata metadata = loadMetadataOnly(skillFile);
                                if (metadata != null) {
                                    // Check for duplicate skill names
                                    if (this.metadataIndex.containsKey(metadata.getName())) {
                                        Path existingPath = this.metadataIndex.get(metadata.getName()).getSkillPath();
                                        log.warn("Duplicate skill name '{}'. Using {}, overriding {}",metadata.getName(), skillFile, existingPath);
                                    }
                                    this.metadataIndex.put(metadata.getName(), metadata);
                                    discovered.add(metadata.getName());
                                    log.debug("Discovered skill: {} at {}", metadata.getName(), skillFile);
                                }
                            } catch (Exception e) {
                                log.warn("Error processing skill file {}: {}", skillFile, e.getMessage());
                            }
                        });
            } catch (IOException e) {
                log.warn("Error walking directory {}: {}", skillDir, e.getMessage());
            }
        }

        log.info("Discovered {} skills", discovered.size());
        return discovered;
    }

    /**
     * Load ONLY the frontmatter metadata from a SKILL.md file.
     *
     * Parses only the YAML frontmatter to extract name and description.
     * Does not read the full markdown body to minimize I/O and memory.
     *
     * @param skillPath Path to the SKILL.md file.
     * @return SkillMetadata if parsing succeeded, null otherwise.
     */
    private SkillMetadata loadMetadataOnly(Path skillPath) {
        try (BufferedReader reader = Files.newBufferedReader(skillPath)) {
            String firstLine = reader.readLine();
            if (firstLine == null || !firstLine.trim().equals("---")) {
                log.warn("SKILL.md missing frontmatter: {}", skillPath);
                return null;
            }

            StringBuilder frontmatterBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("---")) {
                    break;
                }
                frontmatterBuilder.append(line).append("\n");
            }

            if (line == null || !line.trim().equals("---")) {
                log.warn("Invalid SKILL.md frontmatter format: {}", skillPath);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> frontmatter = yaml.load(frontmatterBuilder.toString());
            if (frontmatter == null || frontmatter.isEmpty()) {
                log.warn("Empty frontmatter in: {}", skillPath);
                return null;
            }

            return SkillMetadata.fromFrontmatter(frontmatter, skillPath);

        } catch (Exception e) {
            log.warn("Failed to load skill metadata from {}: {}", skillPath, e.getMessage());
            return null;
        }
    }

    /**
     * Load full skill content on-demand.
     *
     * This method loads the complete skill content including instructions,
     * environment modifications, and associated resources. The result is
     * cached for subsequent invocations.
     *
     * @param skillName The name of the skill to load.
     * @return SkillContent if found, null otherwise.
     */
    public SkillContent loadFullContent(String skillName) {
        // Check cache first
        if (this.contentCache.containsKey(skillName)) {
            log.debug("Using cached content for skill: {}", skillName);
            return this.contentCache.get(skillName);
        }

        // Get metadata
        SkillMetadata metadata = this.metadataIndex.get(skillName);
        if (metadata == null) {
            log.warn("Skill not found in registry: {}", skillName);
            return null;
        }

        // Load full content
        try {
            SkillContent skillContent = loadContentFromFile(metadata.getSkillPath());
            if (skillContent != null) {
                // Cache for future use
                this.contentCache.put(skillName, skillContent);
                log.debug("Loaded full content for skill: {}", skillName);
            }
            return skillContent;

        } catch (Exception e) {
            log.error("Error loading skill content for {}: {}", skillName, e.getMessage());
            return null;
        }
    }

    /**
     * Load full skill content from a SKILL.md file.
     *
     * Parses the complete SKILL.md file including frontmatter and body,
     * and loads any associated resource files.
     *
     * @param skillPath Path to the SKILL.md file.
     * @return SkillContent if parsing succeeded, null otherwise.
     */
    private SkillContent loadContentFromFile(Path skillPath) {
        try {
            String content = Files.readString(skillPath, StandardCharsets.UTF_8);

            // Parse frontmatter and body
            if (!content.startsWith("---")) {
                return null;
            }

            String[] parts = content.split("---", 3);
            if (parts.length < 3) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> frontmatter = yaml.load(parts[1]);
            String body = parts[2].trim();

            if (frontmatter == null || frontmatter.isEmpty()) {
                return null;
            }

            // Load resources if specified
            Map<String, String> resources = new HashMap<>();
            Object resourcePathsObj = frontmatter.get("resources");

            List<String> resourcePaths = new ArrayList<>();
            if (resourcePathsObj instanceof String) {
                resourcePaths.add((String) resourcePathsObj);
            } else if (resourcePathsObj instanceof List) {
                for (Object item : (List<?>) resourcePathsObj) {
                    if (item instanceof String) {
                        resourcePaths.add((String) item);
                    }
                }
            } else if (resourcePathsObj != null) {
                log.warn("Invalid resources in {}; expected list or string", skillPath);
            }

            if (!resourcePaths.isEmpty()) {
                Path skillDir = skillPath.getParent();
                Path skillDirResolved = skillDir.toAbsolutePath().normalize();
                
                for (String resourcePath : resourcePaths) {
                    if (resourcePath == null) {
                        continue;
                    }
                    
                    Path resourceRel = Paths.get(resourcePath);
                    if (resourceRel.isAbsolute()) {
                        log.warn("Resource path must be relative to skill dir: {}", resourcePath);
                        continue;
                    }
                    
                    Path resourceFile = skillDir.resolve(resourceRel).toAbsolutePath().normalize();
                    try {
                        Path relative = skillDirResolved.relativize(resourceFile);
                        // Check if resource file escapes skill directory
                        if (!resourceFile.startsWith(skillDirResolved)) {
                            log.warn("Resource path escapes skill dir: {}", resourcePath);
                            continue;
                        }
                        
                        if (Files.exists(resourceFile)) {
                            // Support directory resources (e.g. "references/") by
                            // recursively loading text files under the directory.
                            if (Files.isDirectory(resourceFile)) {
                                Map<String, String> loaded = loadResourceDirectory(
                                        skillDir, resourceFile, 50, 200_000);
                                resources.putAll(loaded);
                                continue;
                            }

                            try {
                                resources.put(resourcePath, Files.readString(resourceFile));
                            } catch (Exception e) {
                                log.warn("Failed to load resource {}: {}", resourceFile, e.getMessage());
                            }
                        } else {
                            log.warn("Resource file not found: {}", resourceFile);
                        }
                    } catch (Exception e) {
                        log.warn("Error processing resource {}: {}", resourcePath, e.getMessage());
                    }
                }
            }

            return SkillContent.fromFrontmatterAndBody(frontmatter, body, skillPath, resources);

        } catch (Exception e) {
            log.error("Error loading skill content from {}: {}", skillPath, e.getMessage());
            return null;
        }
    }

    /**
     * Recursively load text resources under a directory.
     *
     * Returned keys are POSIX-style relative paths from baseDir.
     */
    private Map<String, String> loadResourceDirectory(
            Path baseDir,
            Path targetDir,
            int maxFiles,
            int maxBytes) {
        
        Map<String, String> loaded = new HashMap<>();

        // Prefer stable ordering for deterministic prompts.
        try {
            final int[] counters = {0, 0}; // [0] = fileCount, [1] = totalBytes
            Files.walk(targetDir)
                    .filter(Files::isRegularFile)
                    .sorted() // Stable ordering
                    .forEach(p -> {
                        if (counters[0] >= maxFiles) {
                            return;
                        }

                        // Skip common binary extensions by default.
                        String fileName = p.getFileName().toString().toLowerCase();
                        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || 
                            fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                            fileName.endsWith(".pdf") || fileName.endsWith(".zip")) {
                            return;
                        }

                        try {
                            Path rel = baseDir.toAbsolutePath().relativize(p.toAbsolutePath());
                            String key = rel.toString().replace("\\", "/"); // POSIX style
                            
                            String text = Files.readString(p);
                            int textSize = text.getBytes().length;
                            
                            if (counters[1] + textSize > maxBytes) {
                                return;
                            }

                            loaded.put(key, text);
                            counters[1] += textSize;
                            counters[0]++;

                        } catch (Exception e) {
                            // Skip files that can't be read
                        }
                    });
        } catch (IOException e) {
            log.warn("Error walking resource directory {}: {}", targetDir, e.getMessage());
        }

        return loaded;
    }

    /**
     * Generate the skill catalog section for the agent's system prompt.
     *
     * Creates a formatted markdown section listing all available skills
     * with their descriptions. This is injected into the agent's system
     * prompt to enable LLM-based skill selection.
     *
     * @return A formatted string, or empty string if no skills.
     */
    public String generateSystemPromptSection() {
        if (this.metadataIndex.isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>(Arrays.asList(
                "## Available Skills",
                "",
                "Skills are loaded with progressive disclosure:",
                "- Metadata (name + description) is always available",
                "- Full SKILL.md content is loaded only when a skill is activated",
                "",
                "Do NOT invoke the Skill tool unless the user explicitly requests it.",
                "(This system uses a selector-based activator.)",
                ""
        ));

        for (SkillMetadata metadata : this.metadataIndex.values()) {
            if (metadata.isDisableModelInvocation()) {
                continue;
            }
            lines.add(metadata.toPromptEntry());
        }

        return String.join("\n", lines);
    }

    /**
     * Generate a user-facing skill list.
     *
     * This is intended for answering questions like "what skills do you have?"
     * without loading full SKILL.md content.
     */
    public String generateUserHelpSection() {
        if (this.metadataIndex.isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>(Arrays.asList(
                "Available skills (metadata only):",
                ""
        ));

        List<SkillMetadata> sortedMetadata = new ArrayList<>(this.metadataIndex.values());
        sortedMetadata.sort(Comparator.comparing(SkillMetadata::getName));

        for (SkillMetadata metadata : sortedMetadata) {
            List<String> flags = new ArrayList<>();
            if (metadata.isDisableModelInvocation()) {
                flags.add("manual-only");
            }
            if (!metadata.isUserInvocable()) {
                flags.add("not-user-invocable");
            }
            
            String suffix = flags.isEmpty() ? "" : " (" + String.join(", ", flags) + ")";
            lines.add("- " + metadata.getName() + ": " + metadata.getDescription() + suffix);

            String argumentHint = metadata.getArgumentHint();
            if (argumentHint != null && !argumentHint.trim().isEmpty()) {
                lines.add("  args: " + argumentHint.trim());
            }
        }

        lines.addAll(Arrays.asList(
                "",
                "Invoke a skill with: /<skill-name> [arguments]"
        ));

        return String.join("\n", lines);
    }

    /**
     * Return all registered skill metadata.
     *
     * @return A list of SkillMetadata for all discovered skills.
     */
    public List<SkillMetadata> listSkills() {
        return new ArrayList<>(this.metadataIndex.values());
    }

    /**
     * Get metadata for a specific skill.
     *
     * @param skillName The name of the skill.
     * @return SkillMetadata if found, null otherwise.
     */
    public SkillMetadata getSkill(String skillName) {
        return this.metadataIndex.get(skillName);
    }

    /**
     * Check if a skill is registered.
     *
     * @param skillName The name of the skill.
     * @return True if the skill exists in the registry.
     */
    public boolean hasSkill(String skillName) {
        return this.metadataIndex.containsKey(skillName);
    }

    /**
     * Clear the content cache.
     *
     * This forces subsequent invocations to reload content from disk.
     * Useful for development when skill files may change.
     */
    public void clearCache() {
        this.contentCache.clear();
        log.debug("Skill content cache cleared");
    }

    /**
     * Reload all skills from disk.
     *
     * Clears the cache and re-discovers all skills.
     *
     * @return A list of discovered skill names.
     */
    public List<String> reload() {
        clearCache();
        this.metadataIndex.clear();
        return discoverAll();
    }

    /**
     * Return the number of registered skills.
     */
    public int size() {
        return this.metadataIndex.size();
    }

    /**
     * Check if a skill is registered.
     */
    public boolean contains(String skillName) {
        return this.metadataIndex.containsKey(skillName);
    }

    /**
     * Iterate over all skill metadata.
     */
    public Iterable<SkillMetadata> iterSkills() {
        return this.metadataIndex.values();
    }
}