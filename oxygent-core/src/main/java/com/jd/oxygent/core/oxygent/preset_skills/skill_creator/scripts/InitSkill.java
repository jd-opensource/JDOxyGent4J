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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Skill Initializer - Creates a new skill from template
 *
 * <p>Usage:</p>
 * <pre>
 *     InitSkill.java &lt;skill-name&gt; --path &lt;path&gt;
 * </pre>
 *
 * <p>Examples:</p>
 * <pre>
 *     InitSkill.java my-new-skill --path skills/public
 *     InitSkill.java my-api-helper --path skills/private
 *     InitSkill.java custom-skill --path /custom/location
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class InitSkill {

    private static final String SKILL_TEMPLATE = """
---
name: %s
description: [TODO: Complete and informative explanation of what the skill does and when to use it. Include WHEN to use this skill - specific scenarios, file types, or tasks that trigger it.]
---

# %s

## Overview

[TODO: 1-2 sentences explaining what this skill enables]

## Structuring This Skill

[TODO: Choose the structure that best fits this skill's purpose. Common patterns:

**1. Workflow-Based** (best for sequential processes)
- Works well when there are clear step-by-step procedures
- Example: DOCX skill with "Workflow Decision Tree" → "Reading" → "Creating" → "Editing"
- Structure: ## Overview → ## Workflow Decision Tree → ## Step 1 → ## Step 2...

**2. Task-Based** (best for tool collections)
- Works well when the skill offers different operations/capabilities
- Example: PDF skill with "Quick Start" → "Merge PDFs" → "Split PDFs" → "Extract Text"
- Structure: ## Overview → ## Quick Start → ## Task Category 1 → ## Task Category 2...

**3. Reference/Guidelines** (best for standards or specifications)
- Works well for brand guidelines, coding standards, or requirements
- Example: Brand styling with "Brand Guidelines" → "Colors" → "Typography" → "Features"
- Structure: ## Overview → ## Guidelines → ## Specifications → ## Usage...

**4. Capabilities-Based** (best for integrated systems)
- Works well when the skill provides multiple interrelated features
- Example: Product Management with "Core Capabilities" → numbered capability list
- Structure: ## Overview → ## Core Capabilities → ### 1. Feature → ### 2. Feature...

Patterns can be mixed and matched as needed. Most skills combine patterns (e.g., start with task-based, add workflow for complex operations).

Delete this entire "Structuring This Skill" section when done - it's just guidance.]

## [TODO: Replace with the first main section based on chosen structure]

[TODO: Add content here. See examples in existing skills:
- Java code samples for technical skills
- Decision trees for complex workflows
- Concrete examples with realistic user requests
- References to scripts/templates/references as needed]

## Resources

This skill includes example resource directories that demonstrate how to organize different types of bundled resources:

### scripts/
Executable code (Java/Python/Bash/etc.) that can be run directly to perform specific operations.

**Examples from other skills:**
- PDF skill: `PdfFiller.java`, `PdfToImageConverter.java` - utilities for PDF manipulation
- DOCX skill: `DocumentProcessor.java`, `DocxUtils.java` - Java classes for document processing

**Appropriate for:** Java classes, Python scripts, shell scripts, or any executable code that performs automation, data processing, or specific operations.

**Note:** Scripts may be executed without loading into context, but can still be read by Claude for patching or environment adjustments.

**For Java scripts:**
- Compile: `javac YourScript.java`
- Run: `java YourScript`
- Ensure class name matches filename (e.g., `YourScript.java` contains `public class YourScript`)

### references/
Documentation and reference material intended to be loaded into context to inform Claude's process and thinking.

**Examples from other skills:**
- Product management: `communication.md`, `context_building.md` - detailed workflow guides
- BigQuery: API reference documentation and query examples
- Finance: Schema documentation, company policies

**Appropriate for:** In-depth documentation, API references, database schemas, comprehensive guides, or any detailed information that Claude should reference while working.

### assets/
Files not intended to be loaded into context, but rather used within the output Claude produces.

**Examples from other skills:**
- Brand styling: PowerPoint template files (.pptx), logo files
- Frontend builder: HTML/React boilerplate project directories
- Typography: Font files (.ttf, .woff2)

**Appropriate for:** Templates, boilerplate code, document templates, images, icons, fonts, or any files meant to be copied or used in the final output.

---

**Any unneeded directories can be deleted.** Not every skill requires all three types of resources.
""";

    private static final String EXAMPLE_SCRIPT = """
/**
 * Example helper class for %s
 *
 * This is a placeholder Java class that can be compiled and executed.
 * Replace with actual implementation or delete if not needed.
 *
 * Example real scripts from other skills:
 * - pdf/scripts/PdfFiller.java - Fills PDF form fields
 * - pdf/scripts/PdfToImageConverter.java - Converts PDF pages to images
 *
 * Compile: javac example.java
 * Run: java example
 */
public class example {

    public static void main(String[] args) {
        System.out.println("This is an example script for %s");
        // TODO: Add actual script logic here
        // This could be data processing, file conversion, API calls, etc.
    }
}
""";

    private static final String EXAMPLE_REFERENCE = """
# Reference Documentation for %s

This is a placeholder for detailed reference documentation.
Replace with actual reference content or delete if not needed.

Example real reference docs from other skills:
- product-management/references/communication.md - Comprehensive guide for status updates
- product-management/references/context_building.md - Deep-dive on gathering context
- bigquery/references/ - API references and query examples

## When Reference Docs Are Useful

Reference docs are ideal for:
- Comprehensive API documentation
- Detailed workflow guides
- Complex multi-step processes
- Information too lengthy for main SKILL.md
- Content that's only needed for specific use cases

## Structure Suggestions

### API Reference Example
- Overview
- Authentication
- Endpoints with examples
- Error codes
- Rate limits

### Workflow Guide Example
- Prerequisites
- Step-by-step instructions
- Common patterns
- Troubleshooting
- Best practices
""";

    private static final String EXAMPLE_ASSET = """
# Example Asset File

This placeholder represents where asset files would be stored.
Replace with actual asset files (templates, images, fonts, etc.) or delete if not needed.

Asset files are NOT intended to be loaded into context, but rather used within
the output Claude produces.

Example asset files from other skills:
- Brand guidelines: logo.png, slides_template.pptx
- Frontend builder: hello-world/ directory with HTML/React boilerplate
- Typography: custom-font.ttf, font-family.woff2
- Data: sample_data.csv, test_dataset.json

## Common Asset Types

- Templates: .pptx, .docx, boilerplate directories
- Images: .png, .jpg, .svg, .gif
- Fonts: .ttf, .otf, .woff, .woff2
- Boilerplate code: Project directories, starter files
- Icons: .ico, .svg
- Data files: .csv, .json, .xml, .yaml

Note: This is a text placeholder. Actual assets can be any file type.
""";

    /**
     * Convert hyphenated skill name to Title Case for display.
     *
     * @param skillName skill name in hyphen-case
     * @return Title Case string
     */
    private static String titleCaseSkillName(String skillName) {
        return Arrays.stream(skillName.split("-"))
                     .filter(word -> !word.isEmpty())
                     .map(word -> Character.toUpperCase(word.charAt(0)) +
                                  (word.length() > 1 ? word.substring(1).toLowerCase() : ""))
                     .collect(Collectors.joining(" "));
    }

    /**
     * Initialize a new skill directory with template SKILL.md.
     *
     * @param skillName Name of the skill
     * @param path      Path where the skill directory should be created
     * @return Path to created skill directory, or null if error
     */
    private static Path initSkill(String skillName, String path) {

        // Determine skill directory path

        Path basePath = Paths.get(path).toAbsolutePath().normalize();
        Path skillDir = basePath.resolve(skillName).normalize();
        //Check if directory already exists
        if (Files.exists(skillDir)) {
            System.err.println("❌ Error: Skill directory already exists: " + skillDir);
            return null;
        }
        //Create skill directory
        try {
            Files.createDirectories(skillDir);
            System.out.println("✅ Created skill directory: " + skillDir);
        } catch (IOException e) {
            System.err.println("❌ Error creating directory: " + e.getMessage());
            return null;
        }
        //Create SKILL.md from template
        String skillTitle = titleCaseSkillName(skillName);
        String skillContent = String.format(SKILL_TEMPLATE, skillName, skillTitle);

        Path skillMdPath = skillDir.resolve("SKILL.md");
        try {
            Files.writeString(skillMdPath, skillContent, StandardCharsets.UTF_8);
            System.out.println("✅ Created SKILL.md");
        } catch (IOException e) {
            System.err.println("❌ Error creating SKILL.md: " + e.getMessage());
            return null;
        }
        //Create resource directories with example files
        try {
            //Create scripts/ directory with example script
            Path scriptsDir = skillDir.resolve("scripts");
            Files.createDirectory(scriptsDir);
            Path exampleScript = scriptsDir.resolve("example.java");
            Files.writeString(exampleScript, String.format(EXAMPLE_SCRIPT, skillName, skillName), StandardCharsets.UTF_8);
            setExecutable(exampleScript);
            System.out.println("✅ Created scripts/example.java");
            //Create references/ directory with example reference doc
            Path referencesDir = skillDir.resolve("references");
            Files.createDirectory(referencesDir);
            Path exampleReference = referencesDir.resolve("api_reference.md");
            Files.writeString(exampleReference,String.format(EXAMPLE_REFERENCE, skillTitle),StandardCharsets.UTF_8);
            System.out.println("✅ Created references/api_reference.md");
            //Create assets/ directory with example asset placeholder
            Path assetsDir = skillDir.resolve("assets");
            Files.createDirectory(assetsDir);
            Path exampleAsset = assetsDir.resolve("example_asset.txt");
            Files.writeString(exampleAsset, EXAMPLE_ASSET, StandardCharsets.UTF_8);
            System.out.println("✅ Created assets/example_asset.txt");
        } catch (IOException e) {
            System.err.println("❌ Error creating resource directories: " + e.getMessage());
            return null;
        }
        //Print next steps
        System.out.println("\n✅ Skill '" + skillName + "' initialized successfully at " + skillDir);
        System.out.println("\nNext steps:");
        System.out.println("1. Edit SKILL.md to complete the TODO items and update the description");
        System.out.println("2. Customize or delete the example files in scripts/, references/, and assets/");
        System.out.println("3. Run the validator when ready to check the skill structure");

        return skillDir;
    }

    /**
     * Set executable permission on a file.
     *
     * @param path file path
     */
    private static void setExecutable(Path path) {
        try {
            File file = path.toFile();
            file.setExecutable(true, false);

            if (isUnix()) {
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_READ);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_READ);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(path, perms);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not set executable permissions: " + e.getMessage());
        }
    }

    /**
     * Check if running on Unix-like system.
     *
     * @return true if Unix-like system
     */
    private static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux") || os.contains("mac") || os.contains("unix");
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: InitSkill.java <skill-name> --path <path>");
        System.out.println("\nSkill name requirements:");
        System.out.println("  - Hyphen-case identifier (e.g., 'data-analyzer')");
        System.out.println("  - Lowercase letters, digits, and hyphens only");
        System.out.println("  - Max 40 characters");
        System.out.println("  - Must match directory name exactly");
        System.out.println("\nExamples:");
        System.out.println("  InitSkill.java my-new-skill --path skills/public");
        System.out.println("  InitSkill.java my-api-helper --path skills/private");
        System.out.println("  InitSkill.java custom-skill --path /custom/location");
    }

    public static void main(String[] args) {
        if (args.length < 3 || !args[1].equals("--path")) {
            printUsage();
            System.exit(1);
        }

        String skillName = args[0];
        String path = args[2];

        System.out.println("🚀 Initializing skill: " + skillName);
        System.out.println("   Location: " + path);
        System.out.println();

        Path result = initSkill(skillName, path);

        System.exit(result != null ? 0 : 1);
    }
}
