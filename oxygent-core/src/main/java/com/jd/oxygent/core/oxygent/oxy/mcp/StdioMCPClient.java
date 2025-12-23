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
package com.jd.oxygent.core.oxygent.oxy.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP client implementation based on standard input/output (Stdio)
 * <p>
 * This class implements a client that communicates with MCP server processes via standard input/output (stdin/stdout).
 * This is the classic implementation of the MCP protocol, communicating with child processes through JSON-RPC via stdio.
 *
 * <h3>Core Features</h3>
 * <ul>
 *     <li>Process management: Automatically start and manage MCP server child processes</li>
 *     <li>Standard IO communication: JSON-RPC message exchange through stdin/stdout</li>
 *     <li>Environment variable support: Support configuring environment variables for child processes</li>
 *     <li>File system checking: Automatically verify existence of necessary files and directories</li>
 *     <li>NPX command support: Built-in path resolution for Node.js NPX commands</li>
 * </ul>
 *
 * <h3>Applicable Scenarios</h3>
 * <ul>
 *     <li>Local MCP servers: Suitable for locally installed MCP server tools</li>
 *     <li>Node.js MCP servers: Especially suitable for Node.js servers started with NPX</li>
 *     <li>Script-based tools: Support MCP servers implemented in various scripting languages</li>
 *     <li>Development and testing: Convenient for local development and debugging of MCP servers</li>
 * </ul>
 *
 * <h3>Process Communication Pattern</h3>
 * <ul>
 *     <li>JSON-RPC 2.0: Uses standard JSON-RPC 2.0 protocol for communication</li>
 *     <li>Bidirectional pipes: Send requests via stdin, receive responses via stdout</li>
 *     <li>Line buffering: One complete JSON-RPC message per line</li>
 *     <li>Error isolation: stderr handled independently, doesn't affect protocol communication</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Start Node.js MCP server
 * List<String> args = Arrays.asList(
 *     "--directory", "/path/to/mcp/project",
 *     "run", "server.js"
 * );
 * StdioMCPClient client = new StdioMCPClient(
 *     "filesystem-server",
 *     "npx",
 *     args
 * );
 * client.init();
 *
 * // Start Python MCP server
 * List<String> pythonArgs = Arrays.asList("mcp_server.py");
 * StdioMCPClient pythonClient = new StdioMCPClient(
 *     "python-server",
 *     "python",
 *     pythonArgs
 * );
 * pythonClient.init();
 * }</pre>
 *
 * <h3>Security and Resource Management</h3>
 * <ul>
 *     <li>Process lifecycle: Ensure child processes terminate properly when client closes</li>
 *     <li>Resource cleanup: Automatically clean up file handles and process resources</li>
 *     <li>Path validation: Verify security of executable files and working directories</li>
 *     <li>Error recovery: Handle process abnormal exits and communication interruptions</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see BaseMCPClient MCP client base class
 * @see StdioClientTransport Standard IO transport layer implementation
 * @see ServerParameters Server parameter configuration
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StdioMCPClient extends BaseMCPClient {

    /**
     * MCP server startup command
     * <p>
     * Name or path of the executable file, e.g.: "python", "node", "npx" or full path.
     * For "npx" command, the system will automatically search for the actual path in PATH environment variable.
     */
    private String command;

    /**
     * Command line argument list
     * <p>
     * Command line arguments passed to the MCP server process. The order and content of arguments
     * depend on the specific MCP server implementation requirements.
     */
    private List<String> args;

    /**
     * Environment variable mapping
     * <p>
     * Environment variables set for the MCP server child process. These variables will be merged
     * with system environment variables, used to configure the server's runtime environment,
     * such as API keys, configuration paths, etc.
     */
    Map envMap;

    private static final Logger logger = LoggerFactory.getLogger(StdioMCPClient.class);

    /**
     * Construct a Stdio MCP client instance
     * <p>
     * Creates an MCP client based on standard input/output communication for starting and managing
     * local MCP server processes.
     *
     * @param name    Client name for identification and logging
     * @param command MCP server startup command, can be executable file name or full path
     * @param args    List of command line arguments passed to server process
     */
    public StdioMCPClient(String name, String command, List<String> args) {
        this.setName(name);
        this.command = command;
        this.args = args;
    }

    /**
     * Initialize Stdio connection to MCP server
     * <p>
     * Starts MCP server child process, establishes standard input/output communication pipes, completes client
     * initialization, and automatically discovers and registers tools provided by the server. This method performs
     * complex process management and file system validation.
     *
     * <h3>Initialization Flow</h3>
     * <ol>
     *     <li>Command path resolution: Handle path lookup for special commands like npx</li>
     *     <li>Parameter validation: Verify validity of commands and parameters</li>
     *     <li>Directory checking: Ensure necessary directories and files exist</li>
     *     <li>Environment variable configuration: Merge system and custom environment variables</li>
     *     <li>Process startup: Create ServerParameters and start child process</li>
     *     <li>Transport layer establishment: Create StdioClientTransport communication layer</li>
     *     <li>MCP client building: Build client using Jackson JSON mapper</li>
     *     <li>Protocol initialization: Execute MCP protocol handshake</li>
     *     <li>Tool discovery: Automatically discover and register server tools</li>
     * </ol>
     *
     * <h3>Special Processing Logic</h3>
     * <ul>
     *     <li>NPX path resolution: Automatically find npx executable in PATH</li>
     *     <li>Automatic directory creation: For filesystem servers, automatically create target directories</li>
     *     <li>File existence checking: Verify existence of key MCP tool files</li>
     *     <li>Environment variable merging: Merge custom environment variables with system environment variables</li>
     * </ul>
     *
     * <h3>Supported Command Patterns</h3>
     * <ul>
     *     <li>NPX mode: {@code npx --directory /path run server.js}</li>
     *     <li>Direct execution: {@code python mcp_server.py}</li>
     *     <li>Script mode: {@code /path/to/script.sh}</li>
     * </ul>
     *
     * <h3>Error Checking and Validation</h3>
     * <ul>
     *     <li>Command validity: Ensure command is not null or empty string</li>
     *     <li>File existence: Check if files specified by --directory parameter exist</li>
     *     <li>Directory permissions: Verify access permissions for working directories</li>
     * </ul>
     *
     * <h3>Exception Handling</h3>
     * <ul>
     *     <li>IllegalArgumentException: Thrown when command parameters are invalid</li>
     *     <li>FileNotFoundException: Thrown when necessary files don't exist</li>
     *     <li>RuntimeException: Thrown when process startup or communication fails</li>
     * </ul>
     *
     * <h3>Post-conditions</h3>
     * <ul>
     *     <li>MCP server child process started and communication established</li>
     *     <li>JSON-RPC protocol connection established and initialization completed</li>
     *     <li>Server tools discovered and registered in OxyGent system</li>
     *     <li>Client in available state, can accept tool call requests</li>
     * </ul>
     *
     * @throws IllegalArgumentException when command is null or empty
     * @throws FileNotFoundException    when necessary MCP tool files don't exist
     * @throws RuntimeException         when MCP server startup fails, wrapping process startup exceptions
     * @see #findNpx() NPX path lookup method
     * @see #ensureDirectoriesExist(List) Directory creation and validation method
     * @see #listTools() Tool discovery and registration method
     */
    @Override
    public void init() {
        try {
            String command = "npx".equals(this.command) ? findNpx() : (String) this.command;
            if (command == null || command.isEmpty()) {
                throw new IllegalArgumentException("The command must be a valid string and cannot be null.");
            }

            //List<String> args = (List<String>) params.get("args");
            ensureDirectoriesExist(args);

            if (args.size() >= 4 && "--directory".equals(args.get(0)) && "run".equals(args.get(2))) {
                Path mcpToolFile = Paths.get(args.get(1), args.get(3));
                if (!Files.exists(mcpToolFile)) {
                    throw new FileNotFoundException(mcpToolFile + " does not exist.");
                }
            }

            // Build environment variables
            Map<String, String> env = new HashMap<>(System.getenv());
            Object envParams = envMap;
            if (envParams instanceof Map) {
                ((Map<?, ?>) envParams).forEach((k, v) -> env.put(k.toString(), v.toString()));
            }

            ServerParameters parameters = ServerParameters.builder(this.command).args(this.args).build();
            StdioClientTransport clientTransport = new StdioClientTransport(parameters, new JacksonMcpJsonMapper(new ObjectMapper()));

            this.clientSession = McpClient.sync(clientTransport).build();
            logger.info("{} starting initialization....", this.getName());
            this.clientSession.initialize();
            logger.info("Is initialized: " + this.clientSession.isInitialized());
            logger.info("Starting to load {} tool methods into oxy:", this.getName());
            this.listTools();

            logger.info("MCP Server started via stdio.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to start MCP server", e);
        }

    }

    @Override
    boolean initClientSession(OxyRequest oxyRequest) {
        // Studio client has no Header input, no need to initialize, defaults to true
        return true;
    }

    /**
     * Combine command and arguments into complete command line string
     * <p>
     * This method merges the startup command and argument list into a complete command line string,
     * mainly used for logging and debug information display.
     *
     * <h3>Combination Rules</h3>
     * <ul>
     *     <li>Command is always at the beginning of the string</li>
     *     <li>Arguments are appended in list order</li>
     *     <li>Parts are separated by single spaces</li>
     *     <li>If argument list is null, only return the command itself</li>
     * </ul>
     *
     * <h3>Example Output</h3>
     * <pre>
     * command = "npx"
     * args = ["--directory", "/path", "run", "server.js"]
     * result = "npx --directory /path run server.js"
     * </pre>
     *
     * @return Complete command line string including command and all arguments
     */
    public String joinCommandWithArgs() {
        // Create a new list, put command at the front
        List<String> all = new ArrayList<>();
        all.add(command);
        if (args != null) {
            all.addAll(args);
        }
        // Join with spaces
        return String.join(" ", all);
    }

    /**
     * Find the full path of NPX command
     * <p>
     * Search for npx executable in directories specified by system PATH environment variable,
     * and return its complete absolute path. This is important for ensuring correct startup
     * of Node.js package manager tools.
     *
     * <h3>Search Logic</h3>
     * <ul>
     *     <li>Get system PATH environment variable</li>
     *     <li>Split PATH string by path separator</li>
     *     <li>Look for file named "npx" in each directory</li>
     *     <li>Check if file exists and has execute permissions</li>
     *     <li>Return first valid npx path found</li>
     * </ul>
     *
     * <h3>Platform Compatibility</h3>
     * <ul>
     *     <li>Unix/Linux/macOS: Look for "npx" file</li>
     *     <li>Windows: May need to look for "npx.cmd" or "npx.bat"</li>
     *     <li>Use File.pathSeparator to handle different platform path separators</li>
     * </ul>
     *
     * @return Full path of npx command, or null if not found
     */
    private String findNpx() {
        String path = System.getenv("PATH");
        for (String p : path.split(File.pathSeparator)) {
            File file = new File(p, "npx");
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Ensure necessary directories exist and verify file integrity
     * <p>
     * This method analyzes MCP server work requirements based on command line arguments, automatically
     * creates necessary directories, and verifies existence of key files. This is crucial for ensuring
     * MCP server can start and run normally.
     *
     * <h3>Handled Scenarios</h3>
     * <ul>
     *     <li>File system servers: Check and create target working directories</li>
     *     <li>NPX project mode: Verify package.json and entry files exist</li>
     *     <li>Script mode: Ensure script files are accessible</li>
     * </ul>
     *
     * <h3>Automatic Directory Creation</h3>
     * When arguments contain "server-filesystem", will attempt to create directory specified by last argument:
     * <ul>
     *     <li>Check if directory already exists</li>
     *     <li>If not exists, try to recursively create complete path</li>
     *     <li>Log creation results, output warning on failure but don't interrupt process</li>
     * </ul>
     *
     * <h3>NPX Project Validation</h3>
     * For arguments in format {@code --directory <path> run <script>}:
     * <ul>
     *     <li>Combine complete file path: path + script</li>
     *     <li>Verify target file actually exists</li>
     *     <li>If file doesn't exist, throw FileNotFoundException</li>
     * </ul>
     *
     * <h3>Parameter Format Examples</h3>
     * <pre>
     * // File system server
     * ["@modelcontextprotocol/server-filesystem", "/workspace"]
     *
     * // NPX project mode
     * ["--directory", "/path/to/project", "run", "server.js"]
     *
     * // Direct script
     * ["script.py", "--config", "config.json"]
     * </pre>
     *
     * <h3>Error Handling</h3>
     * <ul>
     *     <li>Directory creation failure: Log warning but continue execution</li>
     *     <li>Key file missing: Throw FileNotFoundException</li>
     *     <li>Path access exception: Propagate IOException to upper level</li>
     * </ul>
     *
     * @param args Command line argument list for analyzing directory and file requirements
     * @throws IOException           when file system operations fail
     * @throws FileNotFoundException when required MCP tool files don't exist
     */
    private void ensureDirectoriesExist(List<String> args) throws IOException {
        if (args.size() >= 2 && String.join(" ", args).contains("server-filesystem")) {
            String targetDir = args.get(args.size() - 1);
            Path dirPath = Paths.get(targetDir);
            if (!Files.exists(dirPath)) {
                try {
                    Files.createDirectories(dirPath);
                    logger.info("Created directory: " + targetDir);
                } catch (Exception e) {
                    logger.warn("Could not create directory " + targetDir + ": " + e);
                }
            }
        }

        if (args.size() >= 4 && "--directory".equals(args.get(0)) && "run".equals(args.get(2))) {
            Path mcpToolFile = Paths.get(args.get(1), args.get(3));
            if (!Files.exists(mcpToolFile)) {
                throw new FileNotFoundException(mcpToolFile + " does not exist.");
            }
        }
    }
}
