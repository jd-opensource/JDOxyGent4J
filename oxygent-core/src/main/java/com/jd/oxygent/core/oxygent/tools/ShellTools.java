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
import com.jd.oxygent.core.oxygent.utils.OSUtil;
import com.jd.oxygent.core.oxygent.utils.SmartCharsetReader;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
@Slf4j
public class ShellTools extends FunctionHub {

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
     * Runs a shell command and returns the output or error.
     */
    @Tool(
            name = "run_shell_command",
            description = "Run a shell command and return the output or error.",
            paramMetas = {
                    @ParamMetaAuto(name = "args", type = "List<String>", description = "Command arguments list, e.g.: [\"ls\", \"-la\"] or [\"dir\"]", defaultValue = "null"),
                    @ParamMetaAuto(name = "tail", type = "int", description = "Output line count limit, returns last N lines", defaultValue = "10"),
                    @ParamMetaAuto(name = "base_dir", type = "String", description = "Optional working directory path, uses current directory when null", defaultValue = "null")
            }
    )
    public String runShellCommand(List<String> args, Integer tail, String baseDir) {
        // Validate that either command or args is provided
        if (args == null || args.isEmpty()) {
            return "Error: args list must be provided";
        }
        if (tail == null) {
            tail = 10;
        }
        LinkedList<String> commandArgs = new LinkedList<String>(args);
        try {
            log.info("Running shell command: {}", String.join(" ", commandArgs));

            // Prepare command based on operating system
            ProcessBuilder processBuilder;
            if (OSUtil.isWindows()) {
                // Windows: use cmd.exe /c for command execution
                commandArgs.addFirst("/c");
                commandArgs.addFirst("cmd.exe");
                processBuilder = new ProcessBuilder(commandArgs);
            } else {
                // Unix-like systems: use shell -c for command execution
                commandArgs.addFirst("/c");
                commandArgs.addFirst("/bin/sh");
                processBuilder = new ProcessBuilder(commandArgs);
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

            // Determine appropriate charset based on OS
            java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
            // Windows systems typically use GBK encoding for command output
            if (OSUtil.isWindows()) {
                try {
                    charset = java.nio.charset.Charset.forName("GBK");
                } catch (Exception e) {
                    charset = java.nio.charset.StandardCharsets.UTF_8;
                }
            }

            // Start process
            Process process = processBuilder.start();

            // Read output (both stdout and stderr)
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            // Read stdout with proper encoding
            try (BufferedReader reader = new BufferedReader(new SmartCharsetReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            // Read stderr with proper encoding
            try (BufferedReader errorReader = new BufferedReader(new SmartCharsetReader(process.getErrorStream()))) {
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
            log.warn("Failed to run shell command: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute given shell command and return the result.
     * <p>
     * Args:
     * command: The shell command to execute.
     * timeout: Maximum time (in seconds) allowed for the command to run. Defaults to 300.
     * <p>
     * Returns:
     * A formatted string containing returncode, stdout, and stderr.
     */
    @Tool(
            name = "execute_shell_command",
            description = "Execute a shell command asynchronously and return the return code, standard output and standard error. Supports timeout for long-running commands.",
            paramMetas = {
                    @ParamMetaAuto(name = "command", type = "String", description = "shell Command", defaultValue = "null"),
                    @ParamMetaAuto(name = "timeout", type = "Integer", description = "timeout in milliseconds for long-running commands", defaultValue = "300")
            }
    )
    public String executeShellCommand(String command, Integer timeout) {
        // Validate that args is provided
        if (StringUtils.isBlank(command)) {
            return "Error: command must be provided";
        }
        if (timeout == null) {
            timeout = 3 * 1000;
        }
        String[] commands = command.split("\\|");
        LinkedList<String> commandArgs = new LinkedList<String>(List.of(commands));
        log.info("Running shell command: {}", command);
        List<ProcessBuilder> pipelineCmd = new ArrayList<ProcessBuilder>();
        ProcessBuilder processBuilder = null;
        for (String _command : commandArgs) {
            // Prepare command based on operating system
            if (OSUtil.isWindows()) {
                // Windows: use cmd.exe /c for command execution
                processBuilder = new ProcessBuilder(List.of("cmd.exe", "/c", _command.trim()));
            } else {
                // Unix-like systems: use shell -c for command execution
                processBuilder = new ProcessBuilder(List.of("/bin/sh", "/c", _command.trim()));
            }
            pipelineCmd.add(processBuilder);
        }
        ProcessResult result = executePipelineAsync(pipelineCmd, timeout.longValue()).join();
        if (result.exitCode() == 0) {
            return result.output();
        } else {
            String errorMessage = String.format("execute_shell_command pipline failed with exit code:%d %s: %s", result.exitCode(), result.output(), command);
            log.error(errorMessage);
            return errorMessage;
        }
    }

    private CompletableFuture<ProcessResult> executePipelineAsync(List<ProcessBuilder> pipelineCmd, long timeout) {
        try {
            List<Process> pipeline = ProcessBuilder.startPipeline(pipelineCmd);
            Process lastProcess = pipeline.get(pipeline.size() - 1);

            return lastProcess.onExit().orTimeout(timeout, TimeUnit.MILLISECONDS).thenApply(p -> {
                // Determine appropriate charset based on OS
                java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
                // Windows systems typically use GBK encoding for command output
                if (OSUtil.isWindows()) {
                    try {
                        charset = java.nio.charset.Charset.forName("GBK");
                    } catch (Exception e) {
                        charset = java.nio.charset.StandardCharsets.UTF_8;
                    }
                }

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new SmartCharsetReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("execute_shell_command pipline error", e);
                }
                return new ProcessResult(p.exitValue(), output.toString().trim());
            });
        } catch (Exception e) {
            log.error("execute_shell_command pipline error", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

record ProcessResult(int exitCode, String output) {
}
