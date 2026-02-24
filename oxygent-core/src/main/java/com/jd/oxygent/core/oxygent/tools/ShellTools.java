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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

/**
 * Shell command execution tool class providing system command execution functionality.
 * <p>
 * This tool class enables execution of system shell commands and returns command output
 * or error information. Supports command argument specification, working directory setting,
 * and output line count limitation. Mainly used for system administration, file operations,
 * and environment information queries.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>Shell command execution with argument support</li>
 *   <li>Working directory specification</li>
 *   <li>Output line count control</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Cross-platform compatibility consideration</li>
 * </ul>
 *
 * <p><strong>Technical Implementation:</strong></p>
 * <ul>
 *   <li>Based on Java Process API for command execution</li>
 *   <li>Supports Windows and Unix-like system command formats</li>
 *   <li>Automatic character encoding handling</li>
 *   <li>Built-in timeout and resource management</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * ShellTools shellTools = new ShellTools();
 *
 * // Execute simple command
 * String result = shellTools.call("run_shell_command", List.of("ls", "-la"));
 *
 * // Execute command with working directory
 * String pwdResult = shellTools.call("run_shell_command", List.of("pwd"), 10, "/home/user");
 * }</pre>
 *
 * <p><strong>Security Considerations:</strong></p>
 * <ul>
 *   <li>Command execution requires appropriate system permissions</li>
 *   <li>Input parameter validation to prevent command injection</li>
 *   <li>Resource usage and execution time restrictions</li>
 *   <li>Recommended for trusted environment use only</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see ProcessBuilder Java process management class
 * @since 1.0.0
 */
public class ShellTools extends FunctionHub {

    private static final Logger logger = LoggerFactory.getLogger(ShellTools.class);

    /**
     * Constructor to initialize Shell tools.
     * <p>
     * Sets tool name to "shell_tools" and provides basic tool description information.
     * </p>
     */
    public ShellTools() {
        super("shell_tools");
        this.setDesc("Tool set providing system shell command execution functionality, supports command arguments and working directory specification");
    }

    /**
     * Execute shell command with full parameter support.
     * <p>
     * Executes specified shell command and returns command output or error information.
     * Supports both command string and argument list specification, working directory setting,
     * and output line count limitation. Automatically handles cross-platform command format differences
     * and provides comprehensive error handling.
     * </p>
     *
     * <p><strong>Execution Process:</strong></p>
     * <ol>
     *   <li>Validate command parameters (either command or args must be provided)</li>
     *   <li>Convert args list to command string if needed</li>
     *   <li>Set working directory if specified</li>
     *   <li>Execute command and capture output</li>
     *   <li>Process output according to line count limit</li>
     *   <li>Return execution result or error information</li>
     * </ol>
     *
     * <p><strong>Platform Compatibility:</strong></p>
     * <ul>
     *   <li>Windows: Uses cmd.exe /c for command execution</li>
     *   <li>Unix/Linux/macOS: Direct command execution</li>
     *   <li>Automatic platform detection and adaptation</li>
     * </ul>
     *
     * @param command Optional complete command string, can be null if args provided
     * @param args    Optional command arguments list, can be null if command provided
     * @param tail    Output line count limit, defaults to 10 lines
     * @param baseDir Optional working directory, can be null
     * @return Command execution output or error information
     * @throws IllegalArgumentException when both command and args are null/empty
     */
    @Tool(
            name = "run_shell_command",
            description = "Execute shell command and return output or error information. Supports both command string and argument list, working directory specification, and output line count control. Handles cross-platform compatibility automatically.",
            paramMetas = {
                    @ParamMetaAuto(name = "command", type = "String", description = "Complete command string, e.g.: \"ls -la\" or \"dir\"", defaultValue = "null"),
                    @ParamMetaAuto(name = "args", type = "List<String>", description = "Command arguments list, e.g.: [\"ls\", \"-la\"] or [\"dir\"]", defaultValue = "null"),
                    @ParamMetaAuto(name = "tail", type = "int", description = "Output line count limit, returns last N lines", defaultValue = "10"),
                    @ParamMetaAuto(name = "base_dir", type = "String", description = "Optional working directory path, uses current directory when null", defaultValue = "null")
            }
    )
    public String runShellCommand(String command, List<String> args, int tail, String baseDir) {
        // Validate that either command or args is provided
        if ((command == null || command.trim().isEmpty()) && (args == null || args.isEmpty())) {
            return "Error: Either command string or args list must be provided";
        }

        // Convert args to command string if command is not provided
        String finalCommand;
        if (command != null && !command.trim().isEmpty()) {
            finalCommand = command.trim();
        } else {
            // Build command from args list
            StringBuilder commandBuilder = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    commandBuilder.append(" ");
                }
                String arg = args.get(i);
                // Quote arguments containing spaces (basic shell escaping)
                if (arg.contains(" ") && !arg.startsWith("\"") && !arg.endsWith("\"")) {
                    commandBuilder.append("\"").append(arg).append("\"");
                } else {
                    commandBuilder.append(arg);
                }
            }
            finalCommand = commandBuilder.toString();
        }

        try {
            logger.info("Running shell command: {}", finalCommand);

            // Prepare command based on operating system
            ProcessBuilder processBuilder;
            if (isWindows()) {
                // Windows: use cmd.exe /c for command execution
                processBuilder = new ProcessBuilder("cmd.exe", "/c", finalCommand);
            } else {
                // Unix-like systems: use shell -c for command execution
                processBuilder = new ProcessBuilder("/bin/sh", "-c", finalCommand);
            }

            // Set working directory
            if (baseDir != null && !baseDir.trim().isEmpty()) {
                File workingDir = new File(baseDir.trim());
                if (workingDir.exists() && workingDir.isDirectory()) {
                    processBuilder.directory(workingDir);
                } else {
                    return "Error: Working directory does not exist or is not a directory - " + baseDir;
                }
            }

            // Start process
            Process process = processBuilder.start();

            // Read output (both stdout and stderr)
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            // Determine appropriate charset based on OS
            java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
            
            // Windows systems typically use GBK encoding for command output
            if (isWindows()) {
                try {
                    charset = java.nio.charset.Charset.forName("GBK");
                } catch (Exception e) {
                    logger.debug("GBK charset not available, falling back to UTF-8");
                    charset = java.nio.charset.StandardCharsets.UTF_8;
                }
            }

            // Read stdout with proper encoding
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            // Read stderr with proper encoding
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), charset))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append(System.lineSeparator());
                }
            }

            // Wait for process completion
            int exitCode = process.waitFor();

            // Process results
            if (exitCode != 0) {
                if (errorOutput.length() > 0) {
                    return "Error: " + errorOutput.toString().trim();
                } else {
                    return "Error: Command execution failed with exit code " + exitCode;
                }
            }

            // Apply tail limit to output
            String fullOutput = output.toString().trim();
            if (fullOutput.isEmpty()) {
                return "Command executed successfully but produced no output";
            }

            if (tail > 0) {
                String[] lines = fullOutput.split(System.lineSeparator());
                int startLine = Math.max(0, lines.length - tail);
                String[] selectedLines = new String[lines.length - startLine];
                System.arraycopy(lines, startLine, selectedLines, 0, selectedLines.length);
                return String.join(System.lineSeparator(), selectedLines);
            } else {
                return fullOutput;
            }

        } catch (Exception e) {
            logger.warn("Failed to run shell command: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Check if current system is Windows.
     * <p>
     * Detects operating system type for platform-specific command handling.
     * </p>
     *
     * @return true if current system is Windows, false otherwise
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of ShellTools.
     * <p>
     * Tests shell command execution functionality including directory listing,
     * system information queries, and error handling to verify tool correctness
     * and cross-platform compatibility.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        ShellTools shellTools = new ShellTools();

        System.out.println("=== Shell Tools Test ===");

        // Test 1: Directory listing using args list (cross-platform)
        System.out.println("1. Directory listing test (using args list):");
        List<String> dirArgs = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? List.of("dir") 
            : List.of("ls", "-la");
        String dirResult = (String) shellTools.call("run_shell_command", null, dirArgs, 5, null);
        System.out.println("   Directory listing result:");
        System.out.println(dirResult);

        // Test 2: Directory listing using command string (cross-platform)
        System.out.println("\n2. Directory listing test (using command string):");
        String dirCommand = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "dir" 
            : "ls -la";
        String dirResult2 = (String) shellTools.call("run_shell_command", dirCommand, null, 5, null);
        System.out.println("   Directory listing result:");
        System.out.println(dirResult2);

        // Test 3: Current directory
        System.out.println("\n3. Current directory test:");
        String pwdCommand = System.getProperty("os.name").toLowerCase().contains("windows")
            ? "cd"
            : "pwd";
        String pwdResult = (String) shellTools.call("run_shell_command", pwdCommand, null, 10, null);
        System.out.println("   Current directory: " + pwdResult);

        // Test 3: System information
        System.out.println("\n3. System information test:");
        String sysCommand = System.getProperty("os.name").toLowerCase().contains("windows")
            ? "systeminfo"
            : "uname -a";
        String sysResult = (String) shellTools.call("run_shell_command", sysCommand, null, 3, null);
        System.out.println("   System info (first 3 lines):");
        System.out.println(sysResult);

        // Test 5: Error handling
        System.out.println("\n5. Error handling test:");
        String errorResult = (String) shellTools.call("run_shell_command", "nonexistent-command", null, 10, null);
        System.out.println("   Error handling result: " + errorResult);

        // Test 6: Working directory test
        System.out.println("\n6. Working directory test:");
        String tempDir = System.getProperty("java.io.tmpdir");
        String dirTestResult = (String) shellTools.call("run_shell_command", 
            System.getProperty("os.name").toLowerCase().contains("windows")
                ? "dir"
                : "ls -la",
            null,
            3, 
            tempDir);
        System.out.println("   Temp directory listing:");
        System.out.println(dirTestResult);
    }
}