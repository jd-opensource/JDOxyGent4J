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
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillMetadata;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillRegistry;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Skill script execution tool class providing functionality to run scripts bundled with skills.
 *
 * <p>This tool enables execution of scripts located in a skill's scripts/ directory.
 * It resolves the skill path via SkillRegistry and ensures scripts are executed safely
 * within the skill's directory structure.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>On-demand script execution from skill packages</li>
 *   <li>Support for Java, Python, Bash, and Zsh scripts</li>
 *   <li>Automatic path resolution and security validation</li>
 *   <li>Configurable timeout and output tailing</li>
 *   <li>Custom environment variable support</li>
 * </ul>
 *
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li>Only scripts inside &lt;skill&gt;/scripts are allowed</li>
 *   <li>Path traversal attacks are prevented</li>
 *   <li>Script execution is sandboxed to skill directory</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * SkillTools skillTools = new SkillTools();
 *
 * // Run a Python script from a skill
 * String result = skillTools.runSkillScript(
 *     oxyRequest,
 *     "skill-creator",
 *     "init_skill.java",
 *     Arrays.asList("--path", "./my-skill"),
 *     60,
 *     80,
 *     null
 * );
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see SkillRegistry Skill management registry
 * @since 1.0.0
 */
@Slf4j
public class SkillTools extends FunctionHub {

    private static final Set<String> ALLOWED_SCRIPT_EXTENSIONS = Set.of(".class",".java", ".py", ".sh", ".bash", ".zsh");

    /**
     * Constructor to initialize Skill tools.
     */
    public SkillTools() {
        super("skill_tools");
        this.setDesc("Tool set providing skill script execution functionality, supports running scripts bundled with skills");
    }

    /**
     * Tail text to last N lines.
     *
     * @param text Input text
     * @param tail Number of lines to keep from the end
     * @return Tailed text or original text if tail is invalid
     */
    private String tailText(String text, int tail) {
        if (text == null || tail <= 0) {
            return text;
        }
        String[] lines = text.split("\n");
        if (lines.length <= tail) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - tail; i < lines.length; i++) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(lines[i]);
        }
        return sb.toString();
    }

    /**
     * Check if current system is Windows.
     *
     * @return true if current system is Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * Run a script bundled under a skill's scripts/ directory.
     *
     * <p>This method resolves the skill path via SkillRegistry and executes
     * the specified script. Only scripts inside &lt;skill&gt;/scripts are allowed
     * for security reasons.</p>
     *
     * <h3>Supported Script Types:</h3>
     * <ul>
     *   <li><b>.java</b>: Java scripts (compiled and executed with javac + java)</li>
     *   <li><b>.py</b>: Python scripts (uses system Python)</li>
     *   <li><b>.sh/.bash</b>: Bash scripts</li>
     *   <li><b>.zsh</b>: Zsh scripts (falls back to bash if zsh not available)</li>
     * </ul>
     *
     * <h3>Path Handling:</h3>
     * <ul>
     *   <li>Relative paths in --path argument are resolved to project root</li>
     *   <li>Script paths must be inside the skill's scripts/ directory</li>
     *   <li>Path traversal attempts are blocked</li>
     * </ul>
     *
     * @param oxyRequest     OxyRequest object for accessing MAS and skill registry
     * @param skillName      Skill name (as discovered by SkillRegistry)
     * @param scriptRelpath  Path relative to the skill's scripts/ directory
     * @param args           Optional arguments to pass to the script
     * @param timeout        Execution timeout in seconds (default: 60)
     * @param tail           Maximum number of output lines to return (default: 80)
     * @param env            Optional environment variables to set
     * @return Script output or error message
     */
    @Tool(
            name = "run_skill_script",
            description = "Run a script bundled under a skill's scripts/ directory, resolving the skill path via SkillRegistry. Only scripts inside <skill>/scripts are allowed.",
            paramMetas = {
                    @ParamMetaAuto(name = "oxy_request", type = "OxyRequest", description = "OxyRequest object for accessing MAS", defaultValue = "null"),
                    @ParamMetaAuto(name = "skill_name", type = "String", description = "Skill name (as discovered by SkillRegistry)", defaultValue = "null"),
                    @ParamMetaAuto(name = "script_relpath", type = "String", description = "Path relative to the skill's scripts/ directory (e.g. 'init_skill.java') If 'scripts/' prefix is provided, it will be stripped.", defaultValue = "null"),
                    @ParamMetaAuto(name = "args", type = "List<String>", description = "Optional arguments to pass to the script", defaultValue = "null"),
                    @ParamMetaAuto(name = "timeout", type = "int", description = "Execution timeout in seconds", defaultValue = "60"),
                    @ParamMetaAuto(name = "tail", type = "int", description = "Maximum number of output lines to return", defaultValue = "80"),
                    @ParamMetaAuto(name = "env", type = "Map<String,String>", description = "Optional environment variables", defaultValue = "null")
            }
    )
    public String runSkillScript(
            OxyRequest oxyRequest,
            String skillName,
            String scriptRelpath,
            List<String> args,
            int timeout,
            int tail,
            Map<String, String> env) {

        SkillRegistry registry = getSkillRegistry(oxyRequest);
        if (registry == null) {
            return "Error: Skill registry not initialized";
        }

        String validationResult = validateParameters(skillName, scriptRelpath);
        if (validationResult != null) {
            return validationResult;
        }

        skillName = skillName.trim();
        scriptRelpath = normalizeScriptPath(scriptRelpath);

        PathResolutionResult pathResult = resolveScriptPath(registry, skillName, scriptRelpath);
        if (!pathResult.isSuccess()) {
            return pathResult.getErrorMessage();
        }

        String ext = getFileExtension(pathResult.getScriptPath().toString()).toLowerCase();
        if (!ALLOWED_SCRIPT_EXTENSIONS.contains(ext)) {
            return "Error: unsupported script type '" + ext + "' (allowed: .java/.py/.sh/.bash/.zsh)";
        }

        List<String> cmd = buildCommand(pathResult, ext, args);
        List<String> finalArgs = processArguments(args);
        processPathArgument(finalArgs);
        cmd.addAll(finalArgs);

        return executeCommand(cmd, pathResult.getBaseDir(), timeout, tail, env);
    }

    /**
     * Get skill registry from OxyRequest.
     *
     * @param oxyRequest OxyRequest object
     * @return SkillRegistry instance or null
     */
    private SkillRegistry getSkillRegistry(OxyRequest oxyRequest) {
        if (oxyRequest != null && oxyRequest.getMas() != null) {
            return oxyRequest.getMas().getSkillRegistry();
        }
        return null;
    }

    /**
     * Validate input parameters.
     *
     * @param skillName Skill name
     * @param scriptRelpath Script relative path
     * @return Error message if validation fails, null otherwise
     */
    private String validateParameters(String skillName, String scriptRelpath) {
        if (skillName == null || skillName.trim().isEmpty()) {
            return "Error: skill_name is required";
        }
        if (scriptRelpath == null || scriptRelpath.trim().isEmpty()) {
            return "Error: script_relpath is required";
        }
        return null;
    }

    /**
     * Normalize script path by removing prefixes.
     *
     * @param scriptRelpath Original script path
     * @return Normalized script path
     */
    private String normalizeScriptPath(String scriptRelpath) {
        if (scriptRelpath.startsWith("scripts/") || scriptRelpath.startsWith("scripts\\")) {
            scriptRelpath = scriptRelpath.substring("scripts/".length());
        }
        if (scriptRelpath.startsWith("./")) {
            scriptRelpath = scriptRelpath.substring(2);
        }
        return scriptRelpath;
    }

    /**
     * Result class for path resolution.
     */
    private static class PathResolutionResult {
        private final boolean success;
        private final String errorMessage;
        private final Path skillPath;
        private final Path baseDir;
        private final Path scriptPath;

        PathResolutionResult(boolean success, String errorMessage, Path skillPath, Path baseDir, Path scriptPath) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.skillPath = skillPath;
            this.baseDir = baseDir;
            this.scriptPath = scriptPath;
        }

        static PathResolutionResult failure(String errorMessage) {
            return new PathResolutionResult(false, errorMessage, null, null, null);
        }

        static PathResolutionResult success(Path skillPath, Path baseDir, Path scriptPath) {
            return new PathResolutionResult(true, null, skillPath, baseDir, scriptPath);
        }

        boolean isSuccess() { return success; }
        String getErrorMessage() { return errorMessage; }
        Path getSkillPath() { return skillPath; }
        Path getBaseDir() { return baseDir; }
        Path getScriptPath() { return scriptPath; }
    }

    /**
     * Resolve script path and validate security constraints.
     *
     * @param registry Skill registry
     * @param skillName Skill name
     * @param scriptRelpath Normalized script relative path
     * @return PathResolutionResult with paths or error
     */
    private PathResolutionResult resolveScriptPath(SkillRegistry registry, String skillName, String scriptRelpath) {
        SkillMetadata meta = registry.getSkill(skillName);
        if (meta == null || meta.getSkillPath() == null) {
            return PathResolutionResult.failure("Error: skill '" + skillName + "' not found");
        }

        Path skillPath = meta.getSkillPath();
        Path baseDir = skillPath.getParent();
        Path scriptsDir = baseDir.resolve("scripts").toAbsolutePath().normalize();
        Path targetPath = scriptsDir.resolve(scriptRelpath).toAbsolutePath().normalize();

        if (!targetPath.startsWith(scriptsDir)) {
            return PathResolutionResult.failure("Error: script_relpath must be inside the skill's scripts/ directory");
        }

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            if (scriptRelpath.endsWith(".java")) {
                targetPath = scriptsDir.resolve(scriptRelpath.replace(".java", ".class"));
                if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
                    return PathResolutionResult.success(skillPath, baseDir, targetPath);
                }
            }
            return PathResolutionResult.failure("Error: script not found: " + targetPath);
        }

        return PathResolutionResult.success(skillPath, baseDir, targetPath);
    }

    /**
     * Build command based on script type.
     *
     * @param pathResult Path resolution result
     * @param ext File extension
     * @param args Script arguments
     * @return Command list
     */
    private List<String> buildCommand(PathResolutionResult pathResult, String ext, List<String> args) {
        List<String> cmd = new ArrayList<>();
        Path scriptPath = pathResult.getScriptPath();
        Path scriptsDir = pathResult.getBaseDir().resolve("scripts").toAbsolutePath().normalize();

        if (".py".equals(ext)) {
            cmd.add(getPythonExecutable());
            cmd.add(scriptPath.toString());
        } else if (".sh".equals(ext) || ".bash".equals(ext)) {
            cmd.add("bash");
            cmd.add(scriptPath.toString());
        } else if (".zsh".equals(ext)) {
            String zsh = findExecutable("zsh");
            cmd.add(zsh != null ? zsh : "bash");
            cmd.add(scriptPath.toString());
        } else if (".java".equals(ext)) {
            buildJavaCommand(cmd, scriptPath, scriptsDir);
        } else if (".class".equals(ext)) {
            buildClassCommand(cmd, scriptPath, scriptsDir);
        }

        return cmd;
    }

    /**
     * Build Java compilation and execution command.
     */
    private void buildJavaCommand(List<String> cmd, Path scriptPath, Path scriptsDir) {
        cmd.add(getJavaExecutable("javac"));
        cmd.add(scriptPath.toString());
        cmd.add("&&");
        cmd.add(getJavaExecutable("java"));
        cmd.add("-cp");
        cmd.add(scriptsDir.toString());
        String className = scriptPath.getFileName().toString().replace(".java", "");
        cmd.add(className);
    }

    /**
     * Build Java class execution command.
     */
    private void buildClassCommand(List<String> cmd, Path scriptPath, Path scriptsDir) {
        cmd.add(getJavaExecutable("java"));
        cmd.add("-cp");
        String classPath = scriptsDir.toString();
        int classesIndex = classPath.indexOf("classes");
        if (classesIndex >= 0) {
            classPath = classPath.substring(0, classesIndex + "classes".length());
        }
        cmd.add(classPath);

        String className = scriptPath.toString();
        if (classesIndex >= 0) {
            className = className.substring(classesIndex + "classes".length())
                    .replace(".class", "")
                    .replace("\\", ".")
                    .substring(1);
        }
        cmd.add(className);
    }

    /**
     * Process arguments and filter nulls.
     *
     * @param args Original arguments
     * @return Filtered argument list
     */
    private List<String> processArguments(List<String> args) {
        List<String> finalArgs = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (arg != null) {
                    finalArgs.add(arg.toString());
                }
            }
        }
        return finalArgs;
    }

    /**
     * Process --path argument to resolve relative paths.
     *
     * @param finalArgs Argument list to modify
     */
    private void processPathArgument(List<String> finalArgs) {
        if (finalArgs.contains("--path")) {
            try {
                int pathIndex = finalArgs.indexOf("--path");
                if (pathIndex + 1 < finalArgs.size()) {
                    String pathValue = finalArgs.get(pathIndex + 1);
                    if (pathValue != null && !pathValue.trim().isEmpty() && !Paths.get(pathValue).isAbsolute()) {
                        Path resolvedPath = Paths.get(System.getProperty("user.dir")).resolve(pathValue).toAbsolutePath().normalize();
                        finalArgs.set(pathIndex + 1, resolvedPath.toString());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to process --path argument: {}", e.getMessage());
            }
        }
    }

    /**
     * Execute command and handle output.
     *
     * @param cmd Command to execute
     * @param baseDir Working directory
     * @param timeout Timeout in seconds
     * @param tail Max output lines
     * @param env Environment variables
     * @return Execution result or error message
     */
    private String executeCommand(List<String> cmd, Path baseDir, int timeout, int tail, Map<String, String> env) {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(baseDir.toFile());

        if (env != null && !env.isEmpty()) {
            Map<String, String> processEnv = processBuilder.environment();
            for (Map.Entry<String, String> entry : env.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    processEnv.put(entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            log.info("Running skill script: {} (skill={}, cwd={})", cmd, cmd, baseDir);

            Process process = processBuilder.start();
            Charset charset = getCharset();

            StringBuilder output = readStream(process.getInputStream(), charset);
            StringBuilder errorOutput = readStream(process.getErrorStream(), charset);

            boolean finished = process.waitFor(timeout > 0 ? timeout : 60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Error: script timed out after " + timeout + "s";
            }

            int exitCode = process.exitValue();
            return formatExecutionResult(output.toString(), errorOutput.toString(), exitCode, tail);

        } catch (Exception e) {
            log.warn("Failed to run skill script: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get charset for output based on OS.
     *
     * @return Charset instance
     */
    private Charset getCharset() {
        if (isWindows()) {
            try {
                return Charset.forName("GBK");
            } catch (Exception e) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * Read stream to string builder.
     *
     * @param stream Input stream
     * @param charset Character encoding
     * @return StringBuilder with content
     */
    private StringBuilder readStream(java.io.InputStream stream, Charset charset) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (Exception e) {
            log.debug("Failed to read stream: {}", e.getMessage());
        }
        return output;
    }

    /**
     * Format execution result with output tailing.
     *
     * @param output Standard output
     * @param errorOutput Error output
     * @param exitCode Exit code
     * @param tail Max lines to keep
     * @return Formatted result string
     */
    private String formatExecutionResult(String output, String errorOutput, int exitCode, int tail) {
        String combined = output.trim();
        if (errorOutput.trim().length() > 0) {
            combined = combined + "\n" + errorOutput.trim();
        }

        combined = combined.trim();
        combined = tailText(combined, tail);

        if (exitCode != 0) {
            return combined.isEmpty()
                    ? "Error (exit=" + exitCode + ")"
                    : "Error (exit=" + exitCode + "): " + combined;
        }
        return combined;
    }

    /**
     * Get file extension from filename.
     *
     * @param filename Filename to extract extension from
     * @return File extension including the dot, or empty string if none
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }

    /**
     * Get Python executable path.
     *
     * @return Python executable path or "python" as fallback
     */
    private String getPythonExecutable() {
        String python = findExecutable("python3");
        if (python != null) {
            return python;
        }
        python = findExecutable("python");
        if (python != null) {
            return python;
        }
        return isWindows() ? "python.exe" : "python";
    }

    /**
     * Get Java executable path.
     *
     * @param toolName Java tool name (e.g., "java" or "javac")
     * @return Java executable path or fallback name
     */
    private String getJavaExecutable(String toolName) {
        String java = findExecutable(toolName);
        if (java != null) {
            return java;
        }
        return isWindows() ? toolName + ".exe" : toolName;
    }

    /**
     * Find executable in system PATH.
     *
     * @param name Executable name to find
     * @return Full path to executable or null if not found
     */
    private String findExecutable(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }

        String[] paths = pathEnv.split(File.pathSeparator);
        for (String path : paths) {
            File exe = new File(path, isWindows() ? name + ".exe" : name);
            if (exe.exists() && exe.canExecute()) {
                return exe.getAbsolutePath();
            }
            File exeNoExt = new File(path, name);
            if (exeNoExt.exists() && exeNoExt.canExecute()) {
                return exeNoExt.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Test method demonstrating SkillTools functionality.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("=== Skill Tools Test ===");
        System.out.println("Note: This test requires a properly initialized SkillRegistry.");
        System.out.println("Run this test within the OxyGent framework context.");
    }
}
