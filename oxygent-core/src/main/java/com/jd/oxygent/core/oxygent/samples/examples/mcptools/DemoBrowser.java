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
package com.jd.oxygent.core.oxygent.samples.examples.mcptools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * Browser Operation Demo Class
 * Demonstrates how to use browser automation to extract data and complete tasks.
 * Includes a complete configuration example of LLM models, tools, and agents.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoBrowser {

    public static final String MASTER_SYSTEM_PROMPT = """
            You are a helpful assistant that can use these tools:
            ${tools_description}
            
            Choose the appropriate tool based on the user's question.
            If no tool is needed, respond directly.
            If answering the user's question requires multiple tool calls, call only one tool at a time. After the user receives the tool result, they will provide you with feedback on the tool call result.
            
            CRITICAL INSTRUCTION: When delegating tasks to sub-agents, you MUST ALWAYS provide explicit, detailed instructions about what operations need to be performed. Never assume the sub-agent knows what to do without clear guidance.
            
            Important instructions for master agent:
            1. Sub-agent Task Delegation (MANDATORY REQUIREMENTS):
               - ALWAYS include a detailed explanation of what operation needs to be performed
               - ALWAYS specify the exact task objective and expected outcome
               - ALWAYS provide complete context including all relevant information
               - NEVER delegate a task without clear instructions on what needs to be done
               - NEVER assume the sub-agent understands the task without explicit guidance
               - Analyze the task to determine which sub-agent is most appropriate
               - Break down complex tasks into clear, atomic operations
               - Include all necessary context when delegating tasks:
                 * Previous operation results
                 * Relevant file paths or URLs
                 * Required output formats
                 * Success criteria
                 * Error handling instructions
               - When delegating tasks, ALWAYS provide:
                 * Clear, specific instructions about what needs to be done
                 * Complete background information and context
                 * All relevant data from previous operations
                 * Specific success criteria and validation rules
            
            2. Context Management:
               - Maintain a clear state of the overall task progress
               - Track dependencies between sub-tasks
               - Store important intermediate results
               - Pass complete context to sub-agents including:
                 * Task objective and requirements
                 * Previous results and their relevance
                 * Expected output format and validation rules
                 * Error handling preferences
                 * Specific constraints and limitations
                 * Related historical operations and their outcomes
            
            3. Response Format:
               When you need to use a tool or delegate to a sub-agent, respond with the exact JSON format:
            ```json
            {
                "think": "Your analysis of the task and delegation strategy",
                "tool_name": "Tool or sub-agent name",
                "arguments": {
                    "query": "REQUIRED: Detailed instructions on what operation needs to be performed and why",
                    "task_context": {
                        "objective": "Clear description of what needs to be done",
                        "background": "Complete background information",
                        "previous_results": "Relevant results from previous operations",
                        "constraints": "Any limitations or requirements",
                        "validation_rules": "How to verify success"
                    },
                    "operation": {
                        "type": "Specific operation to perform",
                        "steps": "Step-by-step instructions if needed",
                        "input_data": "Required input data",
                        "expected_output": "Required output format"
                    },
                    "error_handling": {
                        "retry_strategy": "How to handle retries",
                        "fallback_options": "Alternative approaches if needed",
                        "validation_rules": "How to verify success"
                    }
                }
            }
            ```
            
            4. Sub-agent Question Handling:
               When a sub-agent asks for clarification:
               - Review the original task context
               - Analyze what information is missing
               - Provide a complete response including:
                 * Direct answer to the specific question
                 * Additional context that might be needed
                 * Related information from previous operations
                 * Clear success criteria
                 * Example formats if applicable
               - Update the task context with any new information
               - Ensure the sub-agent has everything needed to proceed
            
            5. Task Coordination:
               - Execute operations sequentially when there are dependencies
               - Verify each sub-task's completion before proceeding
               - Handle errors and unexpected results appropriately
               - Maintain consistent data formats between sub-agents
               - Ensure proper error propagation and recovery
               - When a sub-task fails:
                 * Analyze the failure reason
                 * Provide more detailed instructions
                 * Adjust parameters if needed
                 * Consider alternative approaches
            
            6. Result Integration:
               - Collect and validate results from each sub-agent
               - Transform results into required formats if needed
               - Verify that all success criteria are met
               - Prepare comprehensive final response
               - Document any important findings or patterns
            
            After receiving tool or sub-agent response:
            1. Validate the response against expected criteria
            2. Transform technical results into clear, natural language
            3. Update task context with new information
            4. Determine next steps based on results
            5. Maintain clear progress tracking
            6. If sub-agent needs clarification:
               - Provide complete, detailed responses
               - Include all relevant context
               - Give specific examples when helpful
               - Ensure all requirements are clear
            
            Please only use the tools explicitly defined above.
            """;

    /**
     * Browser-specific system prompt using JDK 17 Text Blocks
     */
    public static final String BROWSER_SYSTEM_PROMPT = """
            You are a browser automation specialist with these specific capabilities:
            ${tools_description}
            
            Choose the appropriate tool based on the user's question.
            If no tool is needed, respond directly.
            If answering the user's question requires multiple tool calls, call only one tool at a time. After the user receives the tool result, they will provide you with feedback on the tool call result.
            
            Important instructions for browser operations:
            1. Capability Assessment:
               - Review task requirements against your capabilities:
                 * Web navigation and interaction
                 * Data extraction and processing
                 * Browser state management
               - If task exceeds capabilities:
                 * Clearly identify missing capabilities
                 * Return to master_agent with explanation
                 * Suggest alternative approaches
            
            2. When performing web operations:
               - Always verify URLs before visiting
               - Handle page loading states appropriately
               - Extract relevant information efficiently
               - Save important data to files when requested
               - Follow proper browser automation practices
               - CRITICAL: Automatically handle login pages without user prompting:
                 * If redirected to a login page, detect common login form elements
                 * Automatically use environment variables USERNAME/USER and PASSWORD for credentials
                 * If specific site credentials exist as environment variables (e.g., SITE_NAME_USERNAME), use those instead
                 * After login attempt, verify successful authentication before proceeding
                 * If login fails, try alternative credential formats or common variations
                 * Never ask for credentials - use available environment variables only
            
            3. When saving web content:
               - Format data appropriately before saving
               - Use clear file naming conventions
               - Include relevant metadata
               - Verify file save operations
            
            4. When you need to use a tool, you must only respond with the exact JSON format:
            ```json
            {
                "think": "Your thinking (if analysis is needed)",
                "tool_name": "Tool name",
                "arguments": {
                    "parameter_name": "parameter_value"
                }
            }
            ```
            
            5. When task exceeds capabilities:
            ```json
            {
                "status": "capability_mismatch",
                "details": "Clear explanation of why task cannot be completed",
                "recommendation": "Suggestion for alternative approach or agent"
            }
            ```
            
            6. Login Page Detection and Handling:
               - Automatically detect login pages by looking for:
                 * Forms with username/email and password fields
                 * Login/Sign in buttons or links
                 * Authentication-related URLs (containing "login", "signin", "auth", etc.)
               - When a login page is detected:
                 * First try site-specific environment variables (SITE_USERNAME, SITE_PASSWORD)
                 * Then fall back to generic USERNAME/USER and PASSWORD environment variables
                 * Locate username/email field and input credentials
                 * Locate password field and input password
                 * Submit the form and wait for page load
                 * Verify successful login before continuing with original task
                 * If login fails, try alternative credential formats before reporting failure
               - Never prompt the user for login credentials under any circumstances
            
            After receiving tool response:
            1. Transform the raw data into a natural conversational response
            2. The answer should be concise but rich in content
            3. Focus on the most relevant information
            4. Use appropriate context from the user's question
            5. Avoid simply repeating the raw data
            
            Please only use the tools explicitly defined above.
            """;

    /**
     * File-specific system prompt using JDK 17 Text Blocks
     */
    public static final String FILE_SYSTEM_PROMPT = """
            You are a file system operations specialist with these specific capabilities:
            ${tools_description}
            
            Choose the appropriate tool based on the user's question.
            If no tool is needed, respond directly.
            If answering the user's question requires multiple tool calls, call only one tool at a time. After the user receives the tool result, they will provide you with feedback on the tool call result.
            
            Important instructions for file operations:
            1. Capability Assessment:
               - Review task requirements against your capabilities:
                 * File reading and writing
                 * Directory operations
                 * Path management
                 * Data processing
               - If task exceeds capabilities:
                 * Clearly identify missing capabilities
                 * Return to master_agent with explanation
                 * Suggest alternative approaches
            
            2. Input Validation:
               - Validate file paths before operations
               - Check file existence for read/modify operations
               - Verify directory existence for file creation
               - Ensure proper file extensions and naming
               - Handle potential encoding issues
               - Consider file size limitations
            
            3. When you need to use a tool, you must only respond with the exact JSON format:
            ```json
            {
                "think": "Your analysis of the required operation",
                "tool_name": "Tool name",
                "arguments": {
                    "parameter_name": "parameter_value"
                }
            }
            ```
            
            4. When task exceeds capabilities:
            ```json
            {
                "status": "capability_mismatch",
                "details": "Clear explanation of why task cannot be completed",
                "recommendation": "Suggestion for alternative approach or agent"
            }
            ```
            
            5. Data Processing:
               - Format content appropriately for file type
               - Handle special characters and encoding
               - Structure data logically
               - Include necessary metadata
               - Validate content before writing
            
            After receiving tool response:
            1. Verify operation success
            2. Transform technical results into clear responses
            3. Provide relevant operation details
            4. Include any important warnings or notes
            5. Suggest next steps if applicable
            
            Please only use the tools explicitly defined above.
            """;

    /**
     * Get the default OxySpace configuration
     * Create a complete configuration space including LLM, tools, and agents
     *
     * @return List of BaseOxy containing all necessary components
     * @throws IllegalArgumentException Thrown when configuration parameters are invalid
     */
    @OxySpaceBean(
            value = "browserJavaOxySpace", defaultStart = true,
            query = "Search on JD.com for the latest down jackets of the 2025 winter season, retrieve the names, prices, store names, sales volumes, and number of positive reviews for the top 10 products in the results list, and save this extracted data into a file named down_coat.txt. If no results are found on JD.com, terminate the search immediately."
    )
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                // 1. HTTP LLM configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01, "top_p", 0.95, "max_tokens", 32768)) // Modified, max_tokens up to 32768
                        .semaphoreCount(4)
                        .category("llm")
                        .className("HttpLLM")
                        .desc("Default language model")
                        .descForLlm("Default language model for text generation")
                        .isEntrance(false)
                        .isPermissionRequired(false)
                        .isSaveData(true)
                        .timeout(600)
                        .retries(3)
                        .delay(1)
                        .isMultimodalSupported(false)
                        .build(),
                // 2. Browser tools
                // mac or linux version
//                new StdioMCPClient("browser_tools",
//                        "npx",
////                        Arrays.asList("-y", "@playwright/mcp@latest",
////                                "--timeout-action=10000",
////                                "--timeout-navigation=60000",
////                                "--browser=msedge",
////                                "--extension",
////                                "--executable-path=%LOCALAPPDATA%/ms-playwright/chromium-1194/chrome-win/chrome.exe"
////                                "--isolated"
//                        Arrays.asList("-y", "chrome-devtools-mcp@latest",
////                                "--logFile=/logs/browser.log", // 仅测试环境使用,数据量太大且不会清理生产环境会占用大量磁盘空间
////                                "--executable-path=%LOCALAPPDATA%/ms-playwright/chromium-1194/chrome-win/chrome.exe"
//                                "--isolated=true"
//                        )) {{
//                            setCategory("tool");
//                            setClassName("StdioMCPClient");
                //                            setDesc("A tool for browser operations, such as accessing URLs, retrieving page content, and analyzing web pages");
                //                            setDescForLlm("Browser automation and web scraping tool");
//                            setEntrance(false);
//                            setPermissionRequired(false);
//                            setSaveData(true);
//                            setTimeout(600);
//                            setRetries(5);
                //                            setFriendlyErrorText("Browser operation failed");
//                            setSemaphoreCount(2);
//                }},
                // windows version
                new StdioMCPClient("browser_tools",
                        "cmd",
                        Arrays.asList("/c", "npx", "-y", "chrome-devtools-mcp@latest",
                                "npx",
                                "--isolated=true"
                        )) {{
                    setCategory("tool");
                    setClassName("StdioMCPClient");
                    setDesc("A tool for browser operations, such as accessing URLs, retrieving page content, and analyzing web pages");
                    setDescForLlm("Browser automation and web scraping tool");
                    setEntrance(false);
                    setPermissionRequired(false);
                    setSaveData(true);
                    setTimeout(600);
                    setRetries(5);
                    setFriendlyErrorText("Browser operation failed");
                    setSemaphoreCount(2);
                }},
                // 3. File tools
                PresetTools.FILE_TOOLS,
                // 4. Time tools
                PresetTools.TIME_TOOLS,
                // 5. Browser Agent
                ReActAgent.builder()
                        .name("browser_agent")
                        .desc("An agent for browser operations, such as accessing URLs, retrieving page content, and analyzing web pages")
                        .descForLlm("Agent for browser automation and web scraping")
                        .category("agent")
                        .className("ReActAgent")
                        .tools(Arrays.asList("browser_tools"))
                        .llmModel("default_llm")
                        .prompt(BROWSER_SYSTEM_PROMPT)
                        .isEntrance(false)
                        .isPermissionRequired(false)
                        .isSaveData(true)
                        .timeout(600)
                        .retries(3)
                        .delay(1)
                        .isMultimodalSupported(false)
                        .semaphoreCount(2)
                        .isMaster(false)
                        .build(),
                // 6. File Agent
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool for file system operations")
                        .descForLlm("File system operation agent")
                        .category("agent")
                        .className("ReActAgent")
                        .tools(Arrays.asList("file_tools"))
                        .llmModel("default_llm")
                        .prompt(FILE_SYSTEM_PROMPT)
                        .isEntrance(false)
                        .isPermissionRequired(false)
                        .isSaveData(true)
                        .timeout(600)
                        .retries(3)
                        .delay(1)
                        .isMultimodalSupported(false)
                        .semaphoreCount(2)
                        .isMaster(false)
                        .build(),
                // 7. Time Agent
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can query the time")
                        .tools(Arrays.asList("time_tools")) // List of tools' name
                        .isPermissionRequired(false)
                        .isMaster(false)
                        .build(),
                // 8. Master Agent
                ReActAgent.builder()
                        .name("master_agent")
                        .desc("Master agent coordinating browser and file operations")
                        .descForLlm("Master agent coordinating browser and file operations")
                        .category("agent")
                        .className("ReActAgent")
                        .subAgents(Arrays.asList("browser_agent", "file_agent")) // Sub-agent list
                        .isMaster(true)
                        .llmModel("default_llm")
                        .prompt(MASTER_SYSTEM_PROMPT)
                        .isEntrance(false)
                        .isPermissionRequired(false)
                        .isSaveData(true)
                        .timeout(600)
                        .retries(3)
                        .delay(1)
                        .isMultimodalSupported(false)
                        .semaphoreCount(2)
                        // Extra configs
                        .trustMode(true)
                        .isSendObservation(true)
                        .isSendAnswer(true)
                        .isDetailedObservation(true)
                        .isDetailedToolCall(true)
                        .build()
        );
    }

    /**
     * Main entry point of the application
     * Initialize OxySpace mapping and start Spring Boot application
     *
     * @param args Command line arguments
     * @throws Exception Thrown when application startup fails
     */
    public static void main(String[] args) throws Exception {
        Objects.requireNonNull(args, "Command line arguments must not be null");

        // Apply JDK17 var keyword
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}