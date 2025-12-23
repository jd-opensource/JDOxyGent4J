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

import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.config.Prompts;
import com.jd.oxygent.core.oxygent.schemas.LLM.LLMResponse;
import com.jd.oxygent.core.oxygent.schemas.LLM.LLMState;
import com.jd.oxygent.core.oxygent.schemas.observation.ExecResult;
import com.jd.oxygent.core.oxygent.schemas.observation.ObservationData;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * ReAct Agent - Agent based on Reasoning-Acting paradigm
 *
 * <p>ReActAgent implements the reasoning-acting paradigm proposed in the ReAct (Reasoning and Acting) paper,
 * which is an advanced agent capable of multi-step reasoning and tool usage. It solves complex problems through
 * iterative "think-act-observe" cycles.</p>
 *
 * <p>Core Features:</p>
 * <ul>
 *     <li>Multi-step Reasoning: Support complex logical reasoning and problem decomposition</li>
 *     <li>Tool Invocation: Dynamically select and invoke appropriate tools</li>
 *     <li>Observational Learning: Make subsequent decisions based on tool execution results</li>
 *     <li>Memory Management: Intelligent short-term and long-term memory management</li>
 *     <li>Reflection Mechanism: Support reflection and improvement on response quality</li>
 *     <li>Trust Mode: Support fast mode that directly returns tool results</li>
 * </ul>
 *
 * <p>ReAct Cycle Process:</p>
 * <ol>
 *     <li>Thought: Analyze current problem and available information</li>
 *     <li>Action: Select and execute appropriate tools or provide answers</li>
 *     <li>Observation: Analyze tool execution results</li>
 *     <li>Repeat: Conduct next round of reasoning based on observation results</li>
 *     <li>Conclusion: Provide final answer at appropriate timing</li>
 * </ol>
 *
 * <p>Memory Management Strategy:</p>
 * <ul>
 *     <li>Short-term Memory: Recent conversation history and context</li>
 *     <li>ReAct Memory: Intermediate steps of current reasoning process</li>
 *     <li>Weighted Management: Weighted sorting of memories based on importance</li>
 *     <li>Token Limitation: Intelligent truncation to fit model context length</li>
 * </ul>
 *
 * <p>Applicable Scenarios:</p>
 * <ul>
 *     <li>Complex Problem Solving: Complex tasks requiring multi-step reasoning</li>
 *     <li>Tool Chain Orchestration: Tasks requiring combination of multiple tools</li>
 *     <li>Research Analysis: Collecting and analyzing multi-source information</li>
 *     <li>Decision Support: Decision making based on data analysis</li>
 *     <li>Automation Processes: Complex automated workflows</li>
 * </ul>
 *
 * <p>Configuration Example:</p>
 * <pre>{@code
 * ReActAgent reactAgent = ReActAgent.builder()
 *     .name("Research Assistant")
 *     .maxReactRounds(10)
 *     .memoryMaxTokens(20000)
 *     .trustMode(false)
 *     .isDiscardReactMemory(true)
 *     .tools(List.of("web_search", "calculator", "file_reader"))
 *     .build();
 *
 * // Set custom reflection function
 * reactAgent.setFuncReflexion((response, request) -> {
 *     if (response.length() < 10) {
 *         return "Answer is too brief, please provide more detailed analysis";
 *     }
 *     return null;
 * });
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Slf4j
@NoArgsConstructor
public class ReActAgent extends LocalAgent {

    private static final Logger logger = Logger.getLogger(ReActAgent.class.getName());

    /**
     * Default system prompt
     * Used for system-level instructions in normal ReAct mode
     */
    @Builder.Default
    protected String systemPrompt = Prompts.SYSTEM_PROMPT;

    /**
     * Retrieval mode system prompt
     * Used for system-level instructions when tool retrieval functionality is enabled
     */
    @Builder.Default
    protected String systemPromptRetrieval = Prompts.SYSTEM_PROMPT_RETRIEVAL;

    // ========== ReAct Execution Configuration ==========

    /**
     * Maximum ReAct reasoning rounds
     *
     * <p>Limits the maximum number of iterations in the ReAct loop to prevent infinite loops.
     * When the maximum rounds are reached, the agent will generate a final answer based on the collected information.</p>
     */
    @Builder.Default
    protected int maxReactRounds = 16;

    /**
     * Whether to discard detailed ReAct memory
     *
     * <p>Controls whether to retain detailed steps of the ReAct reasoning process in history:</p>
     * <ul>
     *     <li>true: Only retain final Q&amp;A pairs, saving storage space</li>
     *     <li>false: Retain complete reasoning process, supporting advanced memory management</li>
     * </ul>
     */
    @Builder.Default
    protected boolean isDiscardReactMemory = true;

    // ========== Memory Management Configuration ==========

    /**
     * Maximum token count for memory management
     *
     * <p>Threshold for controlling context length. When memory content exceeds this limit,
     * intelligent truncation will be performed based on importance.</p>
     */
    @Builder.Default
    protected int memoryMaxTokens = 24800;

    /**
     * Short-term memory weight
     *
     * <p>The importance weight of short-term memory (recent conversations) in memory management.
     * The higher the value, the higher the priority for short-term memory to be retained under token limits.</p>
     */
    @Builder.Default
    protected int weightShortMemory = 5;

    /**
     * ReAct memory weight
     *
     * <p>The importance weight of ReAct reasoning process memory in memory management.
     * Usually lower than short-term memory weight, as reasoning process is relatively less important.</p>
     */
    @Builder.Default
    protected int weightReactMemory = 1;

    // ========== Advanced Feature Configuration ==========

    /**
     * Trust mode switch
     *
     * <p>When enabled, the agent can directly return tool execution results without further reasoning.
     * Suitable for scenarios with high trust in tool results, can improve response speed.</p>
     */
    @Builder.Default
    protected boolean trustMode = false;

    // ========== Functional Interface Configuration ==========

    /**
     * Memory order mapping function
     *
     * <p>Used to map the position of memory in the list to importance scores.
     * Default implementation is identity function (x -> x), can be customized to other mapping rules.</p>
     *
     * <p>Example customizations:</p>
     * <ul>
     *     <li>Linear decay: x -> maxScore - x</li>
     *     <li>Exponential decay: x -> (int) Math.pow(0.9, x) * maxScore</li>
     *     <li>Logarithmic growth: x -> (int) Math.log(x + 1) * factor</li>
     * </ul>
     */
    protected java.util.function.Function<Integer, Integer> funcMapMemoryOrder = x -> x;

    /**
     * LLM response parsing function
     *
     * <p>Used to parse raw responses from large language models, identifying tool calls, final answers, or format errors.
     * If not set, built-in default parsing logic will be used.</p>
     */
    protected java.util.function.BiFunction<String, OxyRequest, LLMResponse> funcParseLlmResponse;

    /**
     * Reflection function
     *
     * <p>Function used to evaluate LLM response quality and provide improvement suggestions. When LLM responses
     * do not meet expectations, this function can return improvement suggestions to prompt the agent to regenerate better answers.</p>
     *
     * <p>Function signature:</p>
     * <ul>
     *     <li>Input: Response content string and current request object</li>
     *     <li>Output: Improvement suggestion string, return null if response is acceptable</li>
     * </ul>
     */
    protected BiFunction<String, OxyRequest, String> funcReflexion;

    @Override
    public void init() {
        super.init();
        // Initialize prompt
        if (this.getPrompt() == null || this.getPrompt().isEmpty()) {
            this.setPrompt(this.isSourcingTools() ? systemPromptRetrieval : systemPrompt);
        }

        // Initialize parsing function (supports dynamic injection with request context)
        if (this.getFuncParseLlmResponse() == null) {
            this.setFuncParseLlmResponse(this::parseLlmResponse);
        }

        if (funcReflexion == null) {
            funcReflexion = this::defaultReflexion;
        }

        // Add retrieve_tools tool (if vector search is configured)
        if (Config.getVearch().isEnabled()) {
            List<String> tools = this.getTools();
            tools.add("retrieve_tools");
            this.setTools(tools);
        }

    }

    /**
     * Default reflexion function that checks if response is empty or invalid.
     *
     * @param response   The agent's response to evaluate
     * @param oxyRequest The current request context
     * @return reflection message for improvement (null if response is acceptable)
     */
    protected String defaultReflexion(String response, OxyRequest oxyRequest) {
        // Check if response is empty
        if (response == null || response.trim().isEmpty()) {
            return "The response should not be empty. Please provide a more detailed and helpful answer.";
        }
        return null;
    }

    /**
     * Estimate token count of text
     * Uses more accurate estimation method, considering mixed Chinese-English text
     */
    protected int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Basic estimation: English words ~1 token, Chinese characters ~1.5 tokens
        int chineseChars = 0;
        int englishWords = 0;
        int otherChars = 0;

        String[] words = text.split("\\s+");
        for (String word : words) {
            boolean hasEnglish = false;
            for (char c : word.toCharArray()) {
                if (c >= 0x4e00 && c <= 0x9fff) {
                    chineseChars++;
                } else if (Character.isLetter(c)) {
                    hasEnglish = true;
                } else {
                    otherChars++;
                }
            }
            if (hasEnglish) {
                englishWords++;
            }
        }

        // Estimation formula: Chinese characters*1.5 + English words*1 + Other characters*0.5
        return (int) Math.ceil(chineseChars * 1.5 + englishWords + otherChars * 0.5);
    }

    /**
     * Generic method for calling LLM
     */
    protected OxyResponse callLlm(OxyRequest oxyRequest, Memory memory) {
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("callee", llmModel);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("messages", memory);
        kwargs.put("arguments", arguments);
        return oxyRequest.call(kwargs);
    }

    /**
     * Build message context
     */
    protected Memory buildMessageContext(OxyRequest oxyRequest, Memory reactMemory) {
        Memory tempMemory = new Memory();
        tempMemory.addMessage(Message.systemMessage(this.buildInstruction(oxyRequest.getArguments())));
        tempMemory.addMessages(Message.dictListToMessages(oxyRequest.getShortMemory(false)));
        tempMemory.addMessage(Message.userMessage(oxyRequest.getQueryObject(false)));
        tempMemory.addMessages(reactMemory.getMessages());
        return tempMemory;
    }

    /**
     * Handle tool calls
     */
    protected ObservationData executeToolCalls(OxyRequest oxyRequest, Object toolCallOutput) {
        List<Map<String, Object>> toolCallList;
        if (toolCallOutput instanceof Map) {
            toolCallList = List.of((Map<String, Object>) toolCallOutput);
        } else if (toolCallOutput instanceof List) {
            toolCallList = (List<Map<String, Object>>) toolCallOutput;
        } else {
            throw new RuntimeException("Invalid tool call output type: " + toolCallOutput.getClass());
        }

        String parallelId = generateShortUUID();
        ObservationData observation = new ObservationData();

        // Synchronously execute all tool calls (sequential execution, not parallel)
        for (Map<String, Object> toolCallDict : toolCallList) {
            try {
                Map<String, Object> toolKwargs = new HashMap<>();
                toolKwargs.put("callee", toolCallDict.get("tool_name"));
                toolKwargs.put("arguments", toolCallDict.get("arguments"));
                toolKwargs.put("parallel_id", parallelId);

                OxyResponse toolResponse = oxyRequest.call(toolKwargs);
                observation.addExecResult(new ExecResult(
                        toolCallDict.getOrDefault("tool_name", "").toString(),
                        toolResponse
                ));
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolCallDict.get("tool_name"), e);
                OxyResponse errorResponse = OxyResponse.builder()
                        .state(OxyState.FAILED)
                        .output("Tool execution failed: " + e.getMessage())
                        .build();
                observation.addExecResult(new ExecResult(
                        toolCallDict.getOrDefault("tool_name", "").toString(),
                        errorResponse
                ));
            }
        }

        return observation;
    }

    /**
     * Check if trust mode should be enabled
     */
    protected boolean shouldUseTrustMode(Object llmOutput) {
        if (trustMode) {
            return true;
        }

        if (llmOutput instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outMap = (Map<String, Object>) llmOutput;
            return outMap.containsKey("trust_mode") &&
                    Integer.valueOf(1).equals(outMap.get("trust_mode"));
        }

        return false;
    }

    /**
     * Get historical short-term memory (synchronous Map query version)
     *
     * @param oxyRequest             Current request context, containing trace_id, session and other information
     * @param isGetUserMasterSession Whether to get user-master agent session fragments
     * @return Constructed short-term memory object
     * @throws NullPointerException Thrown when required parameters are null
     */
    @Override
    public Memory getHistory(OxyRequest oxyRequest, boolean isGetUserMasterSession) {
        Objects.requireNonNull(oxyRequest, "Parameter oxyRequest cannot be null");
        Memory shortMemory = new Memory();

        String sessionName = isGetUserMasterSession
                ? String.join("__", oxyRequest.getCallStack().subList(0, 2))
                : oxyRequest.getSessionName();

        List<Map<String, Object>> historys = new ArrayList<>();

        Map<String, Object> query = Map.of(
                "query", Map.of(
                        "bool", Map.of(
                                "must", List.of(
                                        Map.of("terms", Map.of("trace_id", oxyRequest.getRootTraceIds())),
                                        Map.of("term", Map.of("session_name", sessionName))
                                )
                        )
                ),
                "size", this.getShortMemorySize(),
                "sort", List.of(Map.of("create_time", Map.of("order", "desc")))
        );

        Map<String, Object> resp = this.getMas().getEsClient().search(
                Config.getAppName() + "_history",
                query
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> hitsWrapper = resp == null ? null : (Map<String, Object>) resp.get("hits");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hitList = hitsWrapper == null
                ? Collections.emptyList()
                : (List<Map<String, Object>>) hitsWrapper.getOrDefault("hits", Collections.emptyList());

        historys = new ArrayList<>(hitList);
        Collections.reverse(historys);

        if (isDiscardReactMemory) {
            for (Map<String, Object> hit : historys) {
                Map<String, Object> source = hit.get("_source") == null ? hit : (Map<String, Object>) hit.get("_source");
                Map<String, Object> memory = source.get("memory") != null && !"".equals(source.get("memory")) ? JsonUtils.readValue(source.getOrDefault("memory", "").toString(), Map.class, new HashMap()) : new HashMap();
                shortMemory.addMessage(Message.userMessage(memory.getOrDefault("query", "").toString()));
                shortMemory.addMessage(Message.assistantMessage(memory.getOrDefault("answer", "").toString()));
            }
        } else {
            // Advanced mode: Weighted memory management
            List<MemoryEntry> qaList = new ArrayList<>();
            for (int shortI = 0; shortI < historys.size(); shortI++) {
                Map<String, Object> hit = historys.get(shortI);
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                Map<String, Object> memory = source != null && source.get("memory") instanceof Map ? (Map<String, Object>) source.get("memory") : new HashMap();

                qaList.add(new MemoryEntry(
                        memory.getOrDefault("query", "").toString(),
                        memory.getOrDefault("answer", "").toString(),
                        shortI,
                        "short"
                ));

                // Assume react_memory is List<Map<String, String>>, each element has content
                @SuppressWarnings("unchecked")
                List<Map<String, String>> reactMemory = (List<Map<String, String>>) memory.get("react_memory");
                if (reactMemory != null) {
                    for (int i = 0; i < reactMemory.size(); i += 2) {
                        if (i + 1 >= reactMemory.size()) {
                            break;
                        }
                        Map<String, String> q = reactMemory.get(i);
                        Map<String, String> a = reactMemory.get(i + 1);
                        qaList.add(new MemoryEntry(q.get("content"), a.get("content"), shortI, "react"));
                    }
                }
            }

            // Calculate scores
            List<ScoredIndex> scored = new ArrayList<>();
            for (int i = 0; i < qaList.size(); i++) {
                MemoryEntry entry = qaList.get(i);
                int weight = "short".equals(entry.memoryType) ? weightShortMemory : weightReactMemory;
                int score = funcMapMemoryOrder.apply(i + 1) * weight;
                scored.add(new ScoredIndex(i, score));
            }

            // Sort by score in descending order
            scored.sort((a, b) -> Integer.compare(b.score, a.score));

            // Filter by token limit
            Set<Integer> retained = new HashSet<>();
            int tokenCount = 0;
            for (ScoredIndex si : scored) {
                MemoryEntry entry = qaList.get(si.index);
                int tokens = estimateTokenCount(entry.query) + estimateTokenCount(entry.answer);
                if (tokenCount + tokens > memoryMaxTokens) {
                    break;
                }
                tokenCount += tokens;
                retained.add(si.index);
            }

            // Reconstruct memory
            String shortTempMessage = null;
            for (int i = 0; i < qaList.size(); i++) {
                if (!retained.contains(i)) {
                    continue;
                }
                MemoryEntry entry = qaList.get(i);
                if ("short".equals(entry.memoryType)) {
                    if (shortTempMessage != null) {
                        shortMemory.addMessage(Message.assistantMessage(shortTempMessage));
                        shortTempMessage = null;
                    }
                    shortMemory.addMessage(Message.userMessage(entry.query));
                    shortTempMessage = entry.answer;
                } else {
                    if (shortTempMessage == null) {
                        continue;
                    }
                    shortMemory.addMessage(Message.assistantMessage(entry.query));
                    shortMemory.addMessage(Message.userMessage(entry.answer));
                }
            }
            if (shortTempMessage != null) {
                shortMemory.addMessage(Message.assistantMessage(shortTempMessage));
            }
        }

        return shortMemory;
    }

    private LLMResponse parseLlmResponse(String oriResponse, OxyRequest oxyRequest) {
        try {
            // Handle think model format
            if (oriResponse.contains("</think>")) {
                oriResponse = oriResponse.split("</think>", 2)[1].trim();
            }

            String jsonStr = CommonUtils.extractFirstJson(oriResponse);
            Map<String, Object> toolCallDict = JsonUtils.readValue(jsonStr, Map.class);

            if (toolCallDict.containsKey("tool_name")) {
                return new LLMResponse(LLMState.TOOL_CALL, toolCallDict, oriResponse);
            } else if (oxyRequest.getRestartNodeOutput() != null && !"".equals(oxyRequest.getRestartNodeOutput())) {
                return new LLMResponse(LLMState.ANSWER, oriResponse, oriResponse);
            } else {
                return new LLMResponse(LLMState.ERROR_PARSE,
                        "Please answer strictly according to the format. If you want to call a tool, provide tool_name.",
                        oriResponse);
            }
        } catch (Exception e) {
            if (oriResponse.contains("tool_name") && oriResponse.contains("arguments") && oriResponse.contains("{") && oriResponse.contains("}")) {
                return new LLMResponse(LLMState.ERROR_PARSE,
                        "JSON cannot be parsed properly, please provide the answer again.",
                        oriResponse);
            } else {
                // Apply reflexion function if available
                if (oxyRequest != null && funcReflexion != null) {
                    String reflectionMsg = funcReflexion.apply(oriResponse, oxyRequest);
                    if (reflectionMsg != null) {
                        return new LLMResponse(LLMState.ERROR_PARSE, reflectionMsg, oriResponse);
                    }
                }
                return new LLMResponse(LLMState.ANSWER, oriResponse, oriResponse);
            }
        }
    }

    @Override
    // Execute ReAct loop (synchronous)
    public OxyResponse _execute(OxyRequest oxyRequest) {
        Memory reactMemory = new Memory();

        for (int currentRound = 0; currentRound <= maxReactRounds; currentRound++) {
            // Build message context and call LLM
            Memory tempMemory = buildMessageContext(oxyRequest, reactMemory);
            OxyResponse oxyResponse = callLlm(oxyRequest, tempMemory);
            // Use injectable parser to allow dynamic behavior based on Python react_agent design
            LLMResponse llmResponse = this.getFuncParseLlmResponse().apply(oxyResponse.getOutput().toString(), oxyRequest);

            if (llmResponse.getState() == LLMState.ANSWER) {
                return OxyResponse.builder().state(OxyState.COMPLETED).output(llmResponse.getOutput()).extra(Map.of("react_memory", reactMemory.toDictList())).build();
            } else if (llmResponse.getState() == LLMState.TOOL_CALL) {
                // Execute tool calls
                ObservationData observation = executeToolCalls(oxyRequest, llmResponse.getOutput());

                // Check trust mode
                if (shouldUseTrustMode(llmResponse.getOutput())) {
                    return OxyResponse.builder()
                            .state(OxyState.COMPLETED)
                            .output(observation.toStr())
                            .extra(Map.of("react_memory", reactMemory.toDictList()))
                            .build();
                }

                // Add to reactMemory for next round use
                reactMemory.addMessage(Message.assistantMessage(llmResponse.getOriResponse()));
                reactMemory.addMessage(Message.userMessage(observation.toContent(this.isDiscardReactMemory)));

            } else {
                // Parse error, record to reactMemory for correction
                log.warn("Format error, adding to react_memory: " + llmResponse.getOriResponse());
                reactMemory.addMessage(Message.assistantMessage(llmResponse.getOriResponse()));
                reactMemory.addMessage(Message.userMessage(llmResponse.getOutput().toString()));
            }
        }

        // === Exceeded maximum rounds, generate final answer as fallback ===
        StringBuilder toolResults = new StringBuilder();
        int tid = 1;
        for (Message msg : reactMemory.getMessages()) {
            if ("user".equals(msg.getRole())) {
                toolResults.append(tid).append(". ").append(msg.getContent()).append("\n\n");
                tid++;
            }
        }

        String userInput = "User question: " + oxyRequest.getQuery() + "\n---\nTool execution results: " + toolResults.toString();
        Memory finalMemory = new Memory();
        finalMemory.addMessage(Message.systemMessage("Please answer the user's question based on the given tool execution results."));
        finalMemory.addMessage(Message.userMessage(userInput));

        OxyResponse finalLlmResponse = callLlm(oxyRequest, finalMemory);

        return OxyResponse.builder()
                .state(OxyState.COMPLETED).output(finalLlmResponse.getOutput())
                .extra(Map.of("react_memory", reactMemory.toDictList())).build();
    }

    private static class MemoryEntry {
        String query, answer;
        int shortIndex;
        String memoryType;

        MemoryEntry(String q, String a, int idx, String type) {
            this.query = q;
            this.answer = a;
            this.shortIndex = idx;
            this.memoryType = type;
        }
    }

    private static class ScoredIndex {
        int index, score;

        ScoredIndex(int i, int s) {
            index = i;
            score = s;
        }
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

}
