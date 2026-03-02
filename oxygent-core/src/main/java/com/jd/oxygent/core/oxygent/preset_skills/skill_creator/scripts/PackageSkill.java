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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Skill Packager - Creates a distributable .skill file of a skill folder
 *
 * <p>Usage:</p>
 * <pre>
 *     PackageSkill.java &lt;path/to/skill-folder&gt; [output-directory]
 * </pre>
 *
 * <p>Example:</p>
 * <pre>
 *     PackageSkill.java skills/public/my-skill
 *     PackageSkill.java skills/public/my-skill ./dist
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class PackageSkill {

    /**
     * Package a skill folder into a .skill file.
     *
     * @param skillPath Path to the skill folder
     * @param outputDir Optional output directory for the .skill file (defaults to current directory)
     * @return Path to the created .skill file, or null if error
     */
    private static Path packageSkill(Path skillPath, String outputDir) {
        final Path normalizedSkillPath = skillPath.toAbsolutePath().normalize();
        //Validate skill folder exists
        if (!Files.exists(skillPath)) {
            System.err.println("❌ Error: Skill folder not found: " + skillPath);
            return null;
        }

        if (!Files.isDirectory(skillPath)) {
            System.err.println("❌ Error: Path is not a directory: " + skillPath);
            return null;
        }
        //Validate SKILL.md exists
        Path skillMd = skillPath.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            System.err.println("❌ Error: SKILL.md not found in " + skillPath);
            return null;
        }
        //Run validation before packaging
        System.out.println("🔍 Validating skill...");
        QuickValidate.ValidationResult validationResult = QuickValidate.validateSkill(skillPath.toString());
        if (!validationResult.isValid()) {
            System.err.println("❌ Validation failed: Skill structure is invalid");
            System.err.println("   Please fix the validation errors before packaging.");
            return null;
        }
        System.out.println("✅ "+validationResult.getMessage()+"\n");
        //Determine output location
        Path outputPath;
        if (outputDir != null && !outputDir.isEmpty()) {
            outputPath = Paths.get(outputDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                System.err.println("❌ Error creating output directory: " + e.getMessage());
                return null;
            }
        } else {
            outputPath = Paths.get(System.getProperty("user.dir"));
        }

        String skillName = skillPath.getFileName().toString();
        Path skillFile = outputPath.resolve(skillName + ".skill");
        //Create the .skill file (zip format)
        try {
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(skillFile.toFile()));

            Files.walk(skillPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Path relativePath = skillPath.getParent().relativize(file);
                            ZipEntry entry = new ZipEntry(relativePath.toString().replace('\\', '/'));
                            zipOut.putNextEntry(entry);
                            Files.copy(file, zipOut);
                            zipOut.closeEntry();
                            System.out.println("  Added: " + relativePath);
                        } catch (IOException e) {
                            System.err.println("❌ Error adding file to zip: " + file + " - " + e.getMessage());
                        }
                    });

            zipOut.close();

            System.out.println("\n✅ Successfully packaged skill to: " + skillFile);
            return skillFile;

        } catch (IOException e) {
            System.err.println("❌ Error creating .skill file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: PackageSkill.java <path/to/skill-folder> [output-directory]");
        System.out.println("\nExample:");
        System.out.println("  PackageSkill.java skills/public/my-skill");
        System.out.println("  PackageSkill.java skills/public/my-skill ./dist");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String skillPath = args[0];
        String outputDir = args.length > 1 ? args[1] : null;

        System.out.println("📦 Packaging skill: " + skillPath);
        if (outputDir != null) {
            System.out.println("   Output directory: " + outputDir);
        }
        System.out.println();

        Path result = packageSkill(Paths.get(skillPath), outputDir);

        System.exit(result != null ? 0 : 1);
    }
}