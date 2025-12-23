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
package com.jd.oxygent.core.oxygent.oxy.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.BaseTool;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionTool;
import com.jd.oxygent.core.oxygent.oxy.mcp.BaseMCPClient;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;

import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Local Agent - Complete Functional Agent Base Implementation
 *
 * <p>LocalAgent is the most complete agent implementation in the OxyGent system, providing the core capabilities required by agents.
 * It supports advanced features such as tool calling, multimodal processing, memory management, and sub-agent coordination, serving as the foundation for other specialized agents.</p>
 *
 * <p>Core Features:</p>
 * <ul>
 *     <li>Tool Management: Support tool discovery, selection and dynamic invocation</li>
 *     <li>Memory System: Manage short-term and long-term conversation history</li>
 *     <li>Multimodal Support: Process text, images, files and other types of input</li>
 *     <li>Sub-agent Coordination: Support complex task decomposition and collaboration</li>
 *     <li>Intent Understanding: Configurable specialized intent understanding agents</li>
 *     <li>Prompt Templates: Support dynamic parameter replacement</li>
 *     <li>Attachment Processing: Automatically process various types of attachments</li>
 * </ul>
 *
 * <p>Tool Ecosystem:</p>
 * <ul>
 *     <li>MCP Tools: Support Model Context Protocol tools</li>
 *     <li>Function Tools: Support functional tool definitions</li>
 *     <li>Vector Retrieval: Intelligent tool recommendation based on Vearch</li>
 *     <li>Tool Exclusion: Support tool blacklist mechanism</li>
 * </ul>
 *
 * <p>Memory Management:</p>
 * <ul>
 *     <li>Short-term Memory: Maintain recent N rounds of conversation context</li>
 *     <li>Master Session Memory: Maintain main conversation thread with users</li>
 *     <li>ES Persistence: Historical storage based on Elasticsearch</li>
 *     <li>Session Isolation: Support multi-user, multi-session management</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * LocalAgent agent = LocalAgent.builder()
 *     .name("AI Assistant")
 *     .prompt("You are a professional AI assistant, skilled at using various tools to solve problems.")
 *     .llmModel("gpt-4")
 *     .tools(List.of("web_search", "calculator", "file_reader"))
 *     .shortMemorySize(10)
 *     .isMultimodalSupported(true)
 *     .build();
 *
 * // Process multimodal request
 * OxyRequest request = OxyRequest.builder()
 *     .query("Please analyze this image and search for related information")
 *     .queryParts(List.of(
 *         Map.of("type", "text", "text", "Analyze image"),
 *         Map.of("type", "image_url", "image_url", Map.of("url", "https://example.com/image.jpg"))
 *     ))
 *     .build();
 *
 * OxyResponse response = agent.execute(request);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString(callSuper = true, exclude = "prompt")
public class LocalAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgent.class);

    /**
     * Large Language Model identifier
     * Specifies the LLM model name used by the agent, such as "gpt-4", "claude-3-sonnet", etc.
     */
    @Builder.Default
    protected String llmModel = Config.getAgent().getLlmModel();

    /**
     * Main prompt template
     * Defines the agent's role, behavioral guidelines and task guidance, supports ${variable} parameter replacement
     */
    @Builder.Default
    protected String prompt = Config.getAgent().getPrompt();

    /**
     * Additional prompt
     * Extra instructions or context information supplementing the main prompt
     */
    @Builder.Default
    protected String additionalPrompt = "";

    /**
     * Sub-agent list
     * List of other agent names that the current agent can invoke, used for task decomposition and collaboration
     */
    @Builder.Default
    protected List<String> subAgents = new ArrayList<>();

    /**
     * Tool list
     * List of tool names that the agent can use, including MCP tools, Function tools, etc.
     */
    @Builder.Default
    protected List<String> tools = new ArrayList<>();

    /**
     * Excluded tool list
     * List of tool names that are explicitly not allowed to be used, for tool access control
     */
    @Builder.Default
    protected List<String> exceptTools = new ArrayList<>();

    // ========== Tool Retrieval Configuration ==========

    /**
     * Whether to enable tool search
     * When true, the agent can dynamically search and discover new tools
     */
    @Builder.Default
    protected boolean isSourcingTools = false;

    /**
     * Whether to retain sub-agents in toolset
     * When true, sub-agents will also be included in the available tool list
     */
    @Builder.Default
    protected boolean isRetainSubagentInToolset = false;

    /**
     * Tool recommendation count
     * Upper limit of tool count recommended based on vector retrieval
     */
    @Builder.Default
    protected int topKTools = 10;

    /**
     * Whether to still retrieve when tools are scarce
     * Whether to continue using retrieval logic when available tool count is less than topKTools
     */
    @Builder.Default
    protected boolean isRetrieveEvenIfToolsScarce = true;

    // ========== Memory Management Configuration ==========

    /**
     * Short-term memory size
     * Number of recent conversation rounds to retain, used for maintaining conversation context
     */
    @Builder.Default
    protected int shortMemorySize = 10;

    /**
     * Intent understanding agent
     * Agent name specifically used for understanding and rewriting user queries, can be null
     */
    @Builder.Default
    protected String intentUnderstandingAgent = null;

    /**
     * Whether to retain master session short-term memory
     * When true, additionally retain memory of main conversation thread with users
     */
    @Builder.Default
    protected boolean isRetainMasterShortMemory = false;

    // ========== Multimodal and Attachment Configuration ==========

    /**
     * Whether to enable attachment processing
     * When true, automatically process attachments and multimedia content in requests
     */
    @Builder.Default
    protected boolean isAttachmentProcessingEnabled = true;

    /**
     * Whether to support multimodal
     * Indicates whether the agent can handle multimodal inputs such as images, audio, etc.
     */
    @Builder.Default
    protected boolean isMultimodalSupported = false;

    // ========== Team Collaboration Configuration ==========

    /**
     * Team size
     * Number of agent instances to create for parallel processing of complex tasks (not yet implemented)
     */
    @Builder.Default
    protected int teamSize = 1;

    private static final Pattern INSTRUCTION_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    /**
     * Default constructor
     *
     * <p>Initializes basic configuration of LocalAgent and validates the validity of LLM model settings.</p>
     *
     * @throws IllegalStateException if LLM model is not configured or empty
     */
    public LocalAgent() {
        this.llmModel = Config.getAgent().getLlmModel();
        if (this.llmModel == null || this.llmModel.isEmpty()) {
            throw new IllegalStateException("Agent " + this.getName() + " has not set LLM model");
        }
    }

    /**
     * Initialize available tool name list
     *
     * <p>Scans and registers all tools that the agent can use, including:</p>
     * <ul>
     *     <li>Sub-agents: Used as advanced tools</li>
     *     <li>MCP tools: Tools based on Model Context Protocol</li>
     *     <li>Function tools: Functional tool collections</li>
     * </ul>
     *
     * @throws IllegalStateException if referenced agent or tool does not exist
     */
    private void initAvailableToolNameList() {
        // sub-agents
        for (String subAgent : new HashSet<>(subAgents)) {
            if (!this.getMas().getOxyNameToOxy().containsKey(subAgent)) {
                throw new IllegalStateException("Agent [" + subAgent + "] not exists.");
            }
            this.addPermittedTool(subAgent);
        }

        // tools
        for (String oxyName : new HashSet<>(tools)) {
            if (exceptTools.contains(oxyName)) {
                continue;
            }
            if (!this.getMas().getOxyNameToOxy().containsKey(oxyName)) {
                throw new IllegalStateException("Tool [" + oxyName + "] not exists.");
            }
            BaseOxy oxy = this.getMas().getOxyNameToOxy().get(oxyName);
            if (!(oxy instanceof BaseTool)) {
                throw new IllegalStateException("[" + oxyName + "] is not a tool.");
            } else if (oxy instanceof BaseMCPClient) {
                BaseMCPClient client = (BaseMCPClient) oxy;
                for (String toolName : client.getIncludedToolNameList()) {
                    if (exceptTools.contains(toolName)) {
                        continue;
                    }
                    this.addPermittedTool(toolName);
                }
            } else if (oxy instanceof FunctionTool tool) {
                if (exceptTools.contains(tool.getName())) {
                    continue;
                }
                this.addPermittedTool(tool.getName());
            } else if (oxy instanceof FunctionHub hub) {
                for (String toolName : hub.getTools().keySet()) {
                    if (exceptTools.contains(toolName)) {
                        continue;
                    }
                    this.addPermittedTool(toolName);
                }
            } else {
                logger.warn("Unknown tool type: {}", oxy.getClass());
            }
        }
    }

    /**
     * Agent initialization
     *
     * <p>Completes the full initialization process of LocalAgent, including:</p>
     * <ol>
     *     <li>Calling parent class initialization logic</li>
     *     <li>Adding intent understanding agent to sub-agent list</li>
     *     <li>Initializing available tool list</li>
     *     <li>Validating the validity of LLM model</li>
     * </ol>
     *
     * @throws IllegalStateException if LLM model does not exist or configuration is invalid
     */
    public void init() {
        super.init();

        // Add intent understanding agent
        if (this.intentUnderstandingAgent != null && !this.intentUnderstandingAgent.isEmpty()) {
            this.subAgents.add(this.intentUnderstandingAgent);
        }

        // Initialize tool list
        this.initAvailableToolNameList();

        // Validate LLM model existence
        if (!this.getMas().getOxyNameToOxy().containsKey(this.getLlmModel())) {
            throw new IllegalStateException("LLM model [" + this.getLlmModel() + "] does not exist");
        }

        logger.debug("LocalAgent initialization completed: {}, tool count: {}",
                this.getName(), this.getPermittedToolNameList().size());

        // TODO: Team mode functionality to be implemented
        // When teamSize > 1, create multiple agent instances for parallel processing
    }

    /**
     * Get session history
     *
     * <p>Retrieves historical conversation records for specified session from Elasticsearch, supporting two modes:</p>
     * <ul>
     *     <li>Normal mode: Get history records of current session</li>
     *     <li>Master session mode: Get history records of user's main conversation thread</li>
     * </ul>
     *
     * @param oxyRequest             Request object containing session information, cannot be null
     * @param isGetUserMasterSession Whether to get user master session history
     * @return Memory object containing historical conversations, returns empty Memory if no history exists
     * @throws RuntimeException if parsing history record JSON fails
     */
    protected Memory getHistory(OxyRequest oxyRequest, boolean isGetUserMasterSession) {
        Memory shortMemory = new Memory();

        if (oxyRequest.getFromTraceId() != null && !oxyRequest.getFromTraceId().isEmpty()) {
            final String sessionName;
            if (isGetUserMasterSession) {
                List<String> stack = oxyRequest.getCallStack();
                sessionName = String.join("__", stack.subList(0, Math.min(2, stack.size())));
            } else {
                sessionName = oxyRequest.getSessionName();
            }

            List<Map<String, Object>> reversed = new ArrayList<>();
//            if (this.getMas().getEsClient() instanceof LocalEs) {
            Map<String, Object> query = new HashMap<>();
            Map<String, Object> bool = new HashMap<>();
            List<Map<String, Object>> mustList = new ArrayList<>();
            mustList.add(Map.of("terms", Map.of("trace_id", oxyRequest.getRootTraceIds())));
            mustList.add(Map.of("term", Map.of("session_name", sessionName)));
            bool.put("must", mustList);
            query.put("bool", bool);

            Map<String, Object> esBody = new HashMap<>();
            esBody.put("query", query);
            esBody.put("size", this.shortMemorySize);
            esBody.put("sort", List.of(Map.of("create_time", Map.of("order", "desc"))));

            // Ensure complete _source field is retrieved without truncation
            esBody.put("_source", true);

            // Add debug information
            logger.debug("ES query body: {}", esBody);

            // Synchronous ES call (if your client is async, use searchBlocking or block at outer layer)
            Map<String, Object> resp = this.getMas().getEsClient().search(Config.getAppName() + "_history", esBody);

            // Add debug information, check ES returned data
            logger.debug("ES response hits total: {}",
                    resp.get("hits") != null ? ((Map<?, ?>) resp.get("hits")).get("total") : "null");

            Map<String, Object> hitsWrapper = (Map<String, Object>) resp.get("hits");
            List<Map<String, Object>> hitList = (hitsWrapper == null)
                    ? Collections.emptyList()
                    : (List<Map<String, Object>>) hitsWrapper.getOrDefault("hits", Collections.emptyList());

            logger.debug("Retrieved {} history records from ES", hitList.size());

            // Add debug information for each record
            for (int i = 0; i < hitList.size(); i++) {
                Map<String, Object> hit = hitList.get(i);
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                if (source != null) {
                    Object memObj = source.get("memory");
                    if (memObj != null) {
                        String memStr = String.valueOf(memObj);
                        logger.debug("Record {}: memory field length = {}, type = {}",
                                i, memStr.length(), memObj.getClass().getSimpleName());

                        // Check if contains truncation flag
                        if (memStr.contains("...") || memStr.endsWith("，怎么解决")) {
                            logger.warn("Record {} appears to be truncated, ends with: '{}'",
                                    i, memStr.length() > 100 ? memStr.substring(memStr.length() - 100) : memStr);
                        }
                    }
                }
            }

            reversed = new ArrayList<>(hitList);
            Collections.reverse(reversed);

            for (Map<String, Object> h : reversed) {
                Map<String, Object> source = (Map<String, Object>) h.get("_source");
                if (source == null) {
                    continue;
                }
                Object memObj = source.get("memory");
                if (memObj == null) {
                    continue;
                }

                try {
                    Map<String, Object> mem;

                    // If memObj is already Map type, use directly
                    if (memObj instanceof Map) {
                        mem = (Map<String, Object>) memObj;
                    } else {
                        // Try to parse JSON string directly
                        String memJson = String.valueOf(memObj);

                        // Add debug information, check original JSON length
                        logger.debug("Processing memory JSON, length: {}, preview: {}",
                                memJson.length(),
                                memJson.length() > 200 ? memJson.substring(0, 200) + "..." : memJson);

                        try {
                            mem = JsonUtils.readValue(memJson, new TypeReference<>() {
                            });
                        } catch (Exception parseException) {
                            logger.warn("First JSON parse attempt failed: {}", parseException.getMessage());
                            try {
                                com.fasterxml.jackson.databind.JsonNode jsonNode = JsonUtils.readTree(memJson);
                                mem = JsonUtils.convertValue(jsonNode, new TypeReference<>() {
                                });
                                logger.debug("Second JSON parse attempt succeeded using JsonNode");
                            } catch (Exception secondParseException) {
                                logger.warn("Second JSON parse attempt failed: {}, attempting manual cleanup",
                                        secondParseException.getMessage());

                                String cleanedJson = sanitizeJsonString(memJson);
                                logger.debug("Cleaned JSON length: {}, preview: {}",
                                        cleanedJson.length(),
                                        cleanedJson.length() > 200 ? cleanedJson.substring(0, 200) + "..." : cleanedJson);
                                mem = JsonUtils.readValue(cleanedJson, new TypeReference<>() {
                                });
                                logger.debug("Third JSON parse attempt succeeded after manual cleanup");
                            }
                        }
                    }

                    Object q = mem.get("query");
                    Object a = mem.get("answer");

                    // Add debug information, check parsed content length
                    if (a != null) {
                        String answerStr = String.valueOf(a);
                        logger.debug("Parsed answer length: {}, ends with: '{}'",
                                answerStr.length(),
                                answerStr.length() > 50 ? answerStr.substring(Math.max(0, answerStr.length() - 50)) : answerStr);
                        shortMemory.addMessage(Message.assistantMessage(answerStr));
                    }
                    if (q != null) {
                        shortMemory.addMessage(Message.userMessage(String.valueOf(q)));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse memory json after all attempts, skipping this record. Error: {}", e.getMessage());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Full stack trace for memory parsing failure:", e);
                    }
                }
            }
        }
        return shortMemory;
    }

    protected List<String> getLlmToolDescList(OxyRequest oxyRequest, String query) {
        List<String> sortedList = new ArrayList<>(this.getPermittedToolNameList());
        Collections.sort(sortedList);

        List<String> llmToolDescList = new ArrayList<>();

        if (!Config.getVearch().isEnabled()) {
            for (String toolName : sortedList) {
                BaseOxy tool = oxyRequest.getOxy(toolName);
                llmToolDescList.add(tool.getDescForLlm());
            }
            return llmToolDescList;
        }

        if (this.isRetainSubagentInToolset) {
            for (String toolName : sortedList) {
                BaseOxy tool = oxyRequest.getOxy(toolName);
                if (this.getMas().isAgent(toolName)) {
                    llmToolDescList.add(tool.getDescForLlm());
                }
            }
        }

        if (this.isSourcingTools) {
            BaseOxy retrieve = oxyRequest.getOxy("retrieve_tools");
            llmToolDescList.add(retrieve.getDescForLlm());
        } else {
            int toolNumber = sortedList.size();

            if (this.isRetainSubagentInToolset) {
                List<String> pure = new ArrayList<>();
                for (String toolName : sortedList) {
                    if (this.getMas().isAgent(toolName)) {
                        continue;
                    }
                    pure.add(oxyRequest.getOxy(toolName).getDescForLlm());
                }
                toolNumber = pure.size();
            }

            if (this.isRetrieveEvenIfToolsScarce && this.topKTools >= toolNumber) {
                for (String toolName : sortedList) {
                    if ("retrieve_tools".equals(toolName)) {
                        continue;
                    }
                    if (this.isRetainSubagentInToolset && this.getMas().isAgent(toolName)) {
                        continue;
                    }
                    llmToolDescList.add(oxyRequest.getOxy(toolName).getDescForLlm());
                }
            } else {
                OxyResponse oxyResponse = oxyRequest.call(Map.of("query", query));
                if (oxyResponse != null && oxyResponse.getOutput() != null && !oxyResponse.getOutput().toString().isEmpty()) {
                    llmToolDescList.add(oxyResponse.getOutput().toString());
                }
            }
        }
        return llmToolDescList;
    }

    protected OxyRequest preProcess(OxyRequest oxyRequest) {
        OxyRequest req = super.preProcess(oxyRequest);

        if (!req.hasShortMemory()) {
            Memory history = getHistory(req, false);
            req.getShortMemory(false).addAll(history.toDictList());
        }
        if (this.isRetainMasterShortMemory) {
            Memory master = getHistory(req, true);
            req.getShortMemory(true).addAll(master.toDictList());
        }
        return req;
    }

    protected OxyRequest beforeExecute(OxyRequest oxyRequest) {
        OxyRequest req = super.beforeExecute(oxyRequest);
        String rewrittenQuery = req.getQuery();
        if (this.intentUnderstandingAgent != null && !this.intentUnderstandingAgent.isEmpty()) {
            OxyResponse resp = oxyRequest.call(Map.of(
                    "query", req.getQuery(),
                    "short_memory", req.getShortMemory(false)
            ));
            if (resp != null && resp.getOutput() != null) {
                rewrittenQuery = String.valueOf(resp.getOutput());
            }
        }

        List<String> llmToolDescList = getLlmToolDescList(req, rewrittenQuery);
        req.getArguments().put("additional_prompt", this.additionalPrompt);
        req.getArguments().put("tools_description", String.join("\n\n", llmToolDescList));

        return req;
    }

    protected String buildInstruction(Map<String, Object> arguments) {
        // Define regex pattern to match ${variable}
        Matcher matcher = INSTRUCTION_PATTERN.matcher(this.prompt.strip());

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            // Get value from arguments, if not exists use original matched string
            String replacement = arguments.getOrDefault(key, matcher.group(0)).toString();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        return null;
    }

    private static List<String> castStringList(Object o) {
        if (o == null) {
            return List.of();
        }
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }

    private static String coalesce(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    /**
     * Safely clean special characters in JSON string
     *
     * @param jsonString Original JSON string
     * @return Cleaned JSON string
     */
    private static String sanitizeJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        // Use StringBuilder for better performance
        StringBuilder sb = new StringBuilder();
        boolean insideString = false;
        boolean escaped = false;

        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);

            if (escaped) {
                // If previous character is escape character, directly add current character
                sb.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                // Encountered escape character
                sb.append(c);
                escaped = true;
                continue;
            }

            if (c == '"' && !escaped) {
                // Encountered unescaped double quote, toggle string inside/outside state
                insideString = !insideString;
                sb.append(c);
                continue;
            }

            if (insideString) {
                // Inside string, need to escape special characters
                switch (c) {
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    default:
                        // Other control characters (ASCII 0-31) also need escaping
                        if (c < 32) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                        break;
                }
            } else {
                // Outside string, directly add character
                sb.append(c);
            }
        }

        return sb.toString();
    }


}
