package com.jd.oxygent.core.oxygent.preset_skills.skill_creator.scripts;
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

import lombok.Data;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick validation script for skills - minimal version
 *
 * <p>Validates the structure and content of a skill directory.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *     QuickValidate.java &lt;skill_directory&gt;
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuickValidate {

    private static final Set<String> ALLOWED_PROPERTIES = new HashSet<>(Arrays.asList(
            "name", "description", "license", "allowed-tools", "metadata"
    ));

    /**
     * Basic validation of a skill
     *
     * @param skillPath path to the skill directory
     * @return ValidationResult containing validity and message
     */
    static ValidationResult validateSkill(String skillPath) {
        Path skillDir = Paths.get(skillPath);

        // Check SKILL.md exists
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            return new ValidationResult(false, "SKILL.md not found");
        }

        // Read and validate frontmatter
        String content;
        try {
            content = Files.readString(skillMd, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ValidationResult(false, "Cannot read SKILL.md: " + e.getMessage());
        }

        if (!content.startsWith("---")) {
            return new ValidationResult(false, "No YAML frontmatter found");
        }

        // Extract frontmatter using regex
        Pattern frontmatterPattern = Pattern.compile("^---\\R(.*?)\\R---", Pattern.DOTALL);
        Matcher matcher = frontmatterPattern.matcher(content);
        if (!matcher.find()) {
            return new ValidationResult(false, "Invalid frontmatter format");
        }

        String frontmatterText = matcher.group(1);

        // Parse YAML frontmatter (simplified parser)
        Yaml yaml = new Yaml();
        Map<String, Object> frontmatter;
        try {
            frontmatter = yaml.load(frontmatterText);
            if (frontmatter == null) {
                return new ValidationResult(false, "Frontmatter must be a YAML dictionary");
            }
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid YAML in frontmatter: " + e.getMessage());
        }

        // Check for unexpected properties (excluding nested keys under metadata)
        Set<String> unexpectedKeys = new HashSet<>(frontmatter.keySet());
        unexpectedKeys.removeAll(ALLOWED_PROPERTIES);
        if (!unexpectedKeys.isEmpty()) {
            String unexpectedList = String.join(", ", unexpectedKeys.stream().sorted().toArray(String[]::new));
            String allowedList = String.join(", ", ALLOWED_PROPERTIES.stream().sorted().toArray(String[]::new));
            return new ValidationResult(false, 
                String.format("Unexpected key(s) in SKILL.md frontmatter: %s. Allowed properties are: %s",  unexpectedList, allowedList));
        }

        // Check required fields
        if (!frontmatter.containsKey("name")) {
            return new ValidationResult(false, "Missing 'name' in frontmatter");
        }
        if (!frontmatter.containsKey("description")) {
            return new ValidationResult(false, "Missing 'description' in frontmatter");
        }

        // Extract name for validation
        Object nameObj = frontmatter.get("name");
        if (!(nameObj instanceof String)) {
            return new ValidationResult(false, 
                String.format("Name must be a string, got %s", nameObj != null ? nameObj.getClass().getSimpleName() : "null"));
        }
        String name = ((String) nameObj).trim();
        if (!name.isEmpty()) {
            // Check naming convention (hyphen-case: lowercase with hyphens)
            if (!name.matches("^[a-z0-9-]+$")) {
                return new ValidationResult(false,  String.format("Name '%s' should be hyphen-case (lowercase letters, digits, and hyphens only)", name));
            }
            if (name.startsWith("-") || name.endsWith("-") || name.contains("--")) {
                return new ValidationResult(false,  String.format("Name '%s' cannot start/end with hyphen or contain consecutive hyphens", name));
            }
            // Check name length (max 64 characters per spec)
            if (name.length() > 64) {
                return new ValidationResult(false,  String.format("Name is too long (%d characters). Maximum is 64 characters.", name.length()));
            }
        }

        // Extract and validate description
        Object descObj = frontmatter.get("description");
        if (!(descObj instanceof String)) {
            return new ValidationResult(false,String.format("Description must be a string, got %s", descObj != null ? descObj.getClass().getSimpleName() : "null"));
        }
        String description = ((String) descObj).trim();
        if (!description.isEmpty()) {
            // Check for angle brackets
            if (description.contains("<") || description.contains(">")) {
                return new ValidationResult(false, "Description cannot contain angle brackets (< or >)");
            }
            // Check description length (max 1024 characters per spec)
            if (description.length() > 1024) {
                return new ValidationResult(false, String.format("Description is too long (%d characters). Maximum is 1024 characters.",  description.length()));
            }
        }

        return new ValidationResult(true, "Skill is valid!");
    }

    /**
     * Result class for validation.
     */
    @Data
    static class ValidationResult {
        private final boolean valid;
        private final String message;
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: QuickValidate.java <skill_directory>");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
            System.exit(1);
        }

        ValidationResult result = validateSkill(args[0]);
        System.out.println(result.getMessage());
        System.exit(result.isValid() ? 0 : 1);
    }
}
