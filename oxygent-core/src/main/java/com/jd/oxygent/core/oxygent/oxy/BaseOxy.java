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

package com.jd.oxygent.core.oxygent.oxy;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jd.oxygent.core.oxygent.utils.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static com.jd.oxygent.core.oxygent.utils.CommonUtils.getFormatTime;

/**
 * Abstract base class for all agents and tools in the OxyGent system
 * <p>
 * This class defines core execution lifecycle, permission management, message processing, and data persistence patterns.
 * It provides unified interfaces for local and remote execution with comprehensive logging and error handling capabilities.
 * <p>
 * Main features include:
 * - Execution lifecycle management (pre-processing, execution, post-processing)
 * - Permission management and tool list maintenance
 * - Message sending and receiving processing
 * - Data persistence to Elasticsearch
 * - Retry mechanism and error handling
 * - Interceptor and hook function support
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BaseOxy {

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final String DEFAULT_DESCRIPTION = "No description available";
    private static final String DEFAULT_ARGS_DESCRIPTION = "No arguments description available";

    @JsonProperty("name")
    private String name;

    @JsonProperty("desc")
    @Builder.Default
    private String desc = "";

    @JsonProperty("category")
    @Builder.Default
    private String category = "tool";

    @JsonProperty("class_name")
    protected String className;

    @JsonProperty("input_schema")
    protected Map<String, Object> inputSchema;

    @JsonProperty("desc_for_llm")
    @Builder.Default
    private String descForLlm = "";

    @JsonProperty("is_entrance")
    @Builder.Default
    private boolean isEntrance = false;

    @JsonProperty("is_master")
    @Builder.Default
    private boolean isMaster = false;

    @JsonProperty("is_permission_required")
    @Builder.Default
    protected boolean isPermissionRequired = false;

    @JsonProperty("is_save_data")
    @Builder.Default
    private boolean isSaveData = true;

    @JsonProperty("permitted_tool_name_list")
    @Builder.Default
    private List<String> permittedToolNameList = new ArrayList<>();

    @JsonProperty("extra_permitted_tool_name_list")
    @Builder.Default
    private List<String> extraPermittedToolNameList = new ArrayList<>();

    @JsonProperty("is_send_tool_call")
    @Builder.Default
    private boolean isSendToolCall = Config.getMessage().isSendToolCall();

    @JsonProperty("is_send_observation")
    @Builder.Default
    private boolean isSendObservation = Config.getMessage().isSendObservation();

    @JsonProperty("is_send_answer")
    @Builder.Default
    private boolean isSendAnswer = Config.getMessage().isSendAnswer();

    @JsonProperty("is_detailed_tool_call")
    @Builder.Default
    private boolean isDetailedToolCall = Config.getMessage().isDetailedToolCall();

    @JsonProperty("is_detailed_observation")
    @Builder.Default
    private boolean isDetailedObservation = Config.getMessage().isDetailedObservation();

    @JsonIgnore
    @Builder.Default
    private Function<OxyRequest, OxyRequest> funcProcessInput = Function.identity();

    @JsonIgnore
    @Builder.Default
    private Function<OxyResponse, OxyResponse> funcProcessOutput = Function.identity();

    @JsonIgnore
    @Builder.Default
    private Function<OxyRequest, OxyRequest> funcFormatInput = Function.identity();

    @JsonIgnore
    @Builder.Default
    private Function<OxyResponse, OxyResponse> funcFormatOutput = Function.identity();

    @JsonIgnore
    protected Function<OxyRequest, OxyResponse> _funcExecute;

    @JsonIgnore
    private Function<OxyRequest, String> funcInterceptor;

    @JsonIgnore
    protected Mas mas;

    @JsonProperty("friendly_error_text")
    private String friendlyErrorText;

    @JsonProperty("semaphore")
    @Builder.Default
    private int semaphoreCount = 16;

    @Getter
    @JsonIgnore
    private Semaphore semaphore;

    @JsonProperty("timeout")
    @Builder.Default
    private double timeout = 3600.0;

    @JsonProperty("retries")
    @Builder.Default
    private int retries = 2;

    @JsonProperty("delay")
    @Builder.Default
    private double delay = 1.0;

    /**
     * Add permitted tool to the tool list
     *
     * @param toolName Tool name to add, cannot be null or empty string
     * @throws IllegalArgumentException if toolName is null or empty string
     */
    public void addPermittedTool(String toolName) {
        if (permittedToolNameList.contains(toolName)) {
            log.debug("Tool {} already exists in permitted tool list.", toolName);
        } else {
            permittedToolNameList.add(toolName);
            log.debug("Added tool {} to permitted tool list.", toolName);
        }
    }

    /**
     * Batch add permitted tools to the tool list
     *
     * @param toolNames List of tool names to add, cannot be null
     * @throws IllegalArgumentException if toolNames is null or contains null/empty string elements
     */
    public void addPermittedTools(List<String> toolNames) {
        for (String toolName : toolNames) {
            addPermittedTool(toolName);
        }
    }

    /**
     * Sets the description for LLM based on the input schema.
     * This method generates a formatted description that includes tool name,
     * description, and argument details.
     */
    protected void setDescForLlm() {
        List<String> argsDesc = new ArrayList<>();

        if (inputSchema != null && inputSchema.containsKey("properties")) {
            Object propertiesObj = inputSchema.get("properties");
            if (propertiesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>) propertiesObj;
                Map<String, Object> orderedProperties = properties instanceof LinkedHashMap
                        ? properties
                        : new LinkedHashMap<>(properties);

                for (Map.Entry<String, Object> entry : orderedProperties.entrySet()) {
                    String paramName = entry.getKey();
                    Object paramInfoObj = entry.getValue();

                    if (!(paramInfoObj instanceof Map)) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramInfo = (Map<String, Object>) paramInfoObj;
                    String description = (String) paramInfo.get("description");
                    if ("SystemArg".equals(description)) {
                        log.debug("Skipping system parameter: {}", paramName);
                        continue;
                    }

                    String argDesc = String.format("- %s: %s, %s",
                            paramName,
                            paramInfo.getOrDefault("type", "string"),
                            paramInfo.getOrDefault("description", DEFAULT_ARGS_DESCRIPTION));
                    Object requiredObj = inputSchema.get("required");
                    if (requiredObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> required = (List<String>) requiredObj;
                        if (required.contains(paramName)) {
                            argDesc += " (required)";
                        }
                    }

                    argsDesc.add(argDesc);
                    log.debug("Added parameter description: {}", argDesc);
                }
            } else {
                log.warn("Properties object is not a Map for tool {}: {}",
                        this.getName(), propertiesObj != null ? propertiesObj.getClass().getSimpleName() : "null");
            }
        } else {
            log.debug("No properties found in inputSchema for tool {}", this.getName());
        }

        this.descForLlm = String.format(
                "Tool: %s%nDescription: %s%nArguments:%n%s",
                this.getName() != null ? this.getName() : "Unknown",
                this.getDesc() != null ? this.getDesc() : "No description",
                argsDesc.isEmpty() ? "No arguments" : String.join("\n", argsDesc));

        log.debug("Generated descForLlm for tool {}: {}", this.getName(), this.descForLlm);
    }

    public void init() {
        if (this.getClassName() == null || this.getClassName().isEmpty()) {
            this.className = this.getClass().getSimpleName();
        }
        if (this.getInputSchema() == null) {
            this.inputSchema = defaultInputSchema();
        } else {
            this.inputSchema = this.getInputSchema();
        }
        if (this.semaphore == null) {
            this.semaphore = new Semaphore(this.semaphoreCount, true);
        }

        setDescForLlm();
    }

    /**
     * Pre-processes the OxyRequest before execution.
     * Initializes node ID, sets callee information, and updates call stacks.
     *
     * @param oxyRequest the request to pre-process, must not be null
     * @return the pre-processed request
     * @throws IllegalArgumentException if oxyRequest is null
     */
    protected OxyRequest preProcess(OxyRequest oxyRequest) {
        if (oxyRequest.getNodeId() == null || oxyRequest.getNodeId().isEmpty()) {
            oxyRequest.setNodeId(CommonUtils.generateShortUUID());
        }

        oxyRequest.setCallee(this.getName());
        oxyRequest.setCalleeCategory(this.getCategory());
        List<String> callStack = oxyRequest.getCallStack();
        if (callStack == null) {
            callStack = new ArrayList<>();
            oxyRequest.setCallStack(callStack);
        }
        callStack.add(this.getName());
        List<String> nodeIdStack = oxyRequest.getNodeIdStack();
        if (nodeIdStack == null) {
            nodeIdStack = new ArrayList<>();
            oxyRequest.setNodeIdStack(nodeIdStack);
        }
        if (!nodeIdStack.contains(oxyRequest.getNodeId())) {
            nodeIdStack.add(oxyRequest.getNodeId());
        }
        if (funcProcessInput != null) {
            oxyRequest = funcProcessInput.apply(oxyRequest);
        }

        return oxyRequest;
    }

    protected void preLog(OxyRequest oxyRequest) {
        String query = isDetailedToolCall && oxyRequest.getArguments() != null ?
                oxyRequest.getArguments().getOrDefault("query", "...").toString() : "...";

        log.info(LogUtils.ANSI_ORANGE + "{} {} {} : {}" + LogUtils.ANSI_RESET_ALL, oxyRequest.getCurrentTraceId(), oxyRequest.getNodeId(), String.join(" >>> ", oxyRequest.getCallStack()), query);
    }

    /**
     * Request interceptor for handling restart scenarios by loading cached data from Elasticsearch.
     * <p>
     * This method checks if the current request should load data for restart scenarios.
     * It queries Elasticsearch for cached responses based on trace_id and input_md5,
     * and returns the cached response if appropriate conditions are met.
     * </p>
     *
     * @param oxyRequest the request to intercept and potentially handle with cached data
     * @return cached OxyResponse if restart conditions are met, null otherwise
     * @throws IllegalArgumentException if oxyRequest is null
     */
    public OxyResponse requestInterceptor(OxyRequest oxyRequest) {
        if (oxyRequest == null) {
            throw new IllegalArgumentException("OxyRequest cannot be null");
        }
        if (oxyRequest.getReferenceTraceId() == null ||
                !oxyRequest.isLoadDataForRestart() ||
                mas == null ||
                mas.getEsClient() == null ||
                (!"llm".equals(this.getCategory()) && !"tool".equals(this.getCategory()))) {
            return null;
        }

        try {
            Map<String, Object> query = Map.of(
                    "query", Map.of(
                            "bool", Map.of(
                                    "must", List.of(
                                            Map.of("term", Map.of("trace_id", oxyRequest.getReferenceTraceId())),
                                            Map.of("term", Map.of("input_md5", oxyRequest.getInputMd5()))
                                    )
                            )
                    ),
                    "size", 1
            );
            Map<String, Object> esResponse = mas.getEsClient().search(
                    Config.getAppName() + "_node",
                    query
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> hitsWrapper = (Map<String, Object>) esResponse.get("hits");
            if (hitsWrapper == null) {
                log.warn("{} : load null from ES (no hits wrapper). trace_id={} node_id={}",
                        String.join(" === ", oxyRequest.getCallStack()),
                        oxyRequest.getCurrentTraceId(),
                        oxyRequest.getNodeId());
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsWrapper.get("hits");
            if (hits == null || hits.isEmpty()) {
                log.warn("{} : load null from ES. trace_id={} node_id={}",
                        String.join(" === ", oxyRequest.getCallStack()),
                        oxyRequest.getCurrentTraceId(),
                        oxyRequest.getNodeId());
                return null;
            }

            Map<String, Object> firstHit = hits.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) firstHit.get("_source");

            if (source == null) {
                log.warn("ES hit source is null for trace_id={}", oxyRequest.getCurrentTraceId());
                return null;
            }

            Object currentNodeOrderObj = source.get("update_time");
            String currentNodeOrder = currentNodeOrderObj != null ? currentNodeOrderObj.toString() : null;
            String restartNodeOrder = oxyRequest.getRestartNodeOrder();
            String restartNodeOutput = null;

            if (currentNodeOrder != null && restartNodeOrder != null) {
                if (currentNodeOrder.compareTo(restartNodeOrder) < 0) {
                    restartNodeOutput = source.getOrDefault("output", "").toString();

                    log.info("{} Load from ES: {} trace_id={} node_id={}",
                            String.join(" <<< ", oxyRequest.getCallStack()),
                            restartNodeOutput,
                            oxyRequest.getCurrentTraceId(),
                            oxyRequest.getNodeId());

                } else if (oxyRequest.getRestartNodeOutput() != null &&
                        currentNodeOrder.equals(restartNodeOrder)) {
                    restartNodeOutput = oxyRequest.getRestartNodeOutput();

                    log.info("{} Wrote by user: {} trace_id={} node_id={}",
                            String.join(" <<< ", oxyRequest.getCallStack()),
                            restartNodeOutput,
                            oxyRequest.getCurrentTraceId(),
                            oxyRequest.getNodeId());
                }

                if (restartNodeOutput != null) {
                    OxyResponse oxyResponse = new OxyResponse();
                    Object stateObj = source.get("state");
                    if (stateObj != null) {
                        oxyResponse.setState(OxyState.valueOf(stateObj.toString()));
                    }
                    oxyResponse.setOutput(restartNodeOutput);
                    Object extraObj = source.get("extra");
                    if (extraObj != null) {
                        Map<String, Object> extraMap;
                        if (extraObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> castMap = (Map<String, Object>) extraObj;
                            extraMap = castMap;
                        } else if (extraObj instanceof String) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> parsedMap = (Map<String, Object>) JsonUtils.parseObject((String) extraObj, Map.class);
                                extraMap = parsedMap;
                            } catch (Exception e) {
                                log.warn("Failed to parse JSON string: {}", e.getMessage());
                                extraMap = new HashMap<>();
                            }
                        } else {
                            log.warn("Unexpected JSON object type: {}", extraObj.getClass().getSimpleName());
                            extraMap = new HashMap<>();
                        }
                        oxyResponse.setExtra(extraMap);
                    }

                    oxyResponse.setOxyRequest(oxyRequest);
                    return formatOutput(oxyResponse);
                }
            }

        } catch (Exception e) {
            log.error("Error occurred during request interception for trace_id={} node_id={}: {}",
                    oxyRequest.getCurrentTraceId(),
                    oxyRequest.getNodeId(),
                    e.getMessage(), e);
        }

        return null;
    }

    protected void preSaveData(OxyRequest oxyRequest) {
        if (!isSaveData) {
            return;
        }
        if (mas != null && mas.getEsClient() != null) {
            String calleeName = oxyRequest.getCallee();
            String calleeCat = oxyRequest.getCalleeCategory();

            Map<String, Object> body = new HashMap<>();
            body.put("node_id", oxyRequest.getNodeId());
            body.put("node_type", calleeCat);
            body.put("trace_id", oxyRequest.getCurrentTraceId());
            body.put("group_id", oxyRequest.getGroupId());
            body.put("request_id", oxyRequest.getRequestId());
            body.put("caller", oxyRequest.getCaller());
            body.put("callee", calleeName);
            body.put("parallel_id", oxyRequest.getParallelId());
            body.put("father_node_id", oxyRequest.getFatherNodeId());
            body.put("call_stack", oxyRequest.getCallStack());
            body.put("node_id_stack", oxyRequest.getNodeIdStack());
            body.put("pre_node_ids", oxyRequest.getPreNodeIds());
            body.put("shared_data", JsonUtils.toJSONString(oxyRequest.getSharedData()));
            body.put("create_time", getFormatTime());

            mas.getEsClient().index(
                    Config.getAppName() + "_node",
                    oxyRequest.getNodeId(),
                    body
            );
        } else {
            log.info("Node {} data unsaved", oxyRequest.getCallee());
        }
    }

    protected OxyRequest formatInput(OxyRequest oxyRequest) {
        return this.funcFormatInput.apply(oxyRequest);
    }

    protected void preSendMessage(OxyRequest oxyRequest) {
        if (isSendToolCall) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "tool_call");

            Map<String, Object> content = new HashMap<>();
            content.put("node_id", oxyRequest.getNodeId());
            content.put("caller", oxyRequest.getCaller());
            content.put("callee", oxyRequest.getCallee());
            content.put("caller_category", oxyRequest.getCallerCategory());
            content.put("callee_category", oxyRequest.getCalleeCategory());
            content.put("call_stack", oxyRequest.getCallStack());
            content.put("arguments", oxyRequest.getArguments());
            content.put("request_id", oxyRequest.getRequestId());
            content.put("current_trace_id", oxyRequest.getCurrentTraceId());
            content.put("from_trace_id", oxyRequest.getFromTraceId());
            content.put("group_id", oxyRequest.getGroupId());
            content.put("shared_data", oxyRequest.getSharedData());
            message.put("content", content);
            oxyRequest.sendMessage(message);
        }
    }

    protected OxyRequest beforeExecute(OxyRequest oxyRequest) {
        return oxyRequest;
    }

    /**
     * Abstract method that must be implemented by subclasses to define
     * the core execution logic.
     *
     * @param oxyRequest the request to execute
     * @return the execution response
     */
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        throw new UnsupportedOperationException(
                "Method 'execute' must be overridden in subclass: " + this.getClass().getSimpleName()
        );
    }

    protected OxyResponse afterExecute(OxyResponse oxyResponse) {
        return oxyResponse;
    }

    protected OxyResponse postProcess(OxyResponse oxyResponse) {
        return funcProcessOutput.apply(oxyResponse);
    }

    protected void postLog(OxyResponse oxyResponse) {
        String obs = isDetailedObservation && oxyResponse.getOutput() != null ? oxyResponse.getOutput().toString() : "...";
        OxyRequest oxyRequest = oxyResponse.getOxyRequest();

        log.info(LogUtils.ANSI_CYAN + "{} {} {} : {}" + LogUtils.ANSI_RESET_ALL, oxyRequest.getCurrentTraceId(), oxyRequest.getNodeId(), String.join(" <<< ", oxyRequest.getCallStack()), obs);
    }

    public void postSaveData(OxyResponse oxyResponse) {
        if (!isSaveData) {
            return;
        }
        OxyRequest oxyRequest = oxyResponse.getOxyRequest();

        Map<String, Object> oxyInput = new HashMap<>();
        try {
            oxyInput.put("class_attr", ClassModelDumpUtils.getClassAttrForSave(BaseOxy.class, "className", this));
        } catch (IllegalAccessException e) {
            log.error("{} : {}", String.join(" <<< ", oxyRequest.getCallStack()), "Failed to extract class_attr property of this instance");
        }
        oxyInput.put("arguments", oxyRequest.getArguments());

        String calleeName = oxyRequest.getCallee();
        String calleeCat = oxyRequest.getCalleeCategory();

        if (mas != null && mas.getEsClient() != null) {
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("node_id", oxyRequest.getNodeId());
            updateParams.put("node_type", calleeCat);
            updateParams.put("trace_id", oxyRequest.getCurrentTraceId());
            updateParams.put("group_id", oxyRequest.getGroupId());
            updateParams.put("request_id", oxyRequest.getRequestId());
            updateParams.put("caller", oxyRequest.getCaller());
            updateParams.put("callee", calleeName);
            updateParams.put("shared_data", JsonUtils.toJSONString(oxyRequest.getSharedData()));
            updateParams.put("input", JsonUtils.toJSONString(oxyInput));
            updateParams.put("input_md5", oxyRequest.getInputMd5());
            updateParams.put("output", JsonUtils.toJSONString(oxyResponse.getOutput()));
            updateParams.put("state", oxyResponse.getState());
            updateParams.put("extra", oxyResponse.getExtra() == null ? "{}" : JsonUtils.toJSONString(oxyResponse.getExtra()));
            updateParams.put("update_time", getFormatTime());

            mas.getEsClient().update(
                    Config.getAppName() + "_node",
                    oxyRequest.getNodeId(),
                    updateParams
            );
        } else {
            log.warn("Node {} data unsaved.", oxyRequest.getCallee());
        }
    }

    protected OxyResponse formatOutput(OxyResponse oxyResponse) {
        oxyResponse = (OxyResponse) funcFormatOutput.apply(oxyResponse);
        if (oxyResponse.getState() == OxyState.FAILED && friendlyErrorText != null) {
            oxyResponse.setOutput(friendlyErrorText);
        }
        return oxyResponse;
    }

    protected void postSendMessage(OxyResponse oxyResponse) {
        OxyRequest oxyRequest = oxyResponse.getOxyRequest();
        if (isSendObservation) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "observation");
            Map<String, Object> content = new HashMap<>();
            content.put("node_id", oxyRequest.getNodeId());
            content.put("caller", oxyRequest.getCaller());
            content.put("callee", oxyRequest.getCallee());
            content.put("caller_category", oxyRequest.getCallerCategory());
            content.put("callee_category", oxyRequest.getCalleeCategory());
            content.put("call_stack", oxyRequest.getCallStack());
            content.put("output", oxyResponse.getOutput());
            content.put("current_trace_id", oxyRequest.getCurrentTraceId());
            content.put("request_id", oxyRequest.getRequestId());
            content.put("from_trace_id", oxyRequest.getFromTraceId());
            content.put("group_id", oxyRequest.getGroupId());
            message.put("content", content);

            oxyRequest.sendMessage(message);
        }
        if (isSendAnswer && "user".equals(oxyRequest.getCallerCategory())) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "answer");
            message.put("content", oxyResponse.getOutput());
            message.put("caller", oxyRequest.getCaller());
            message.put("caller_category", oxyRequest.getCallerCategory());
            message.put("callee", oxyRequest.getCallee());
            message.put("callee_category", oxyRequest.getCalleeCategory());
            message.put("current_trace_id", oxyRequest.getCurrentTraceId());
            message.put("request_id", oxyRequest.getRequestId());
            oxyRequest.sendMessage(message);
        }
    }

    /**
     * Execute the complete lifecycle of an Oxy operation.
     * <p>
     * This method orchestrates the entire execution pipeline including:
     * - Pre-processing, logging and data saving
     * - Input formatting and pre-send message handling
     * - Validation and permission checks
     * - Before execution hooks
     * - Execution with retry logic
     * - After execution hooks
     * - Post-processing, logging and data saving
     * - Output formatting and post-send message handling
     *
     * @param oxyRequest the request to execute, must not be null
     * @return the execution response
     * @throws IllegalArgumentException if oxyRequest is null
     * @throws RuntimeException         if execution fails
     */
    public OxyResponse execute(OxyRequest oxyRequest) {
        if (this.semaphore != null) {
            try {
                this.semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while acquiring semaphore for: " + this.getName(), e);
            }
        }

        try {
            oxyRequest = preProcess(oxyRequest);
            preLog(oxyRequest);

            Map<String, Object> keyToMd5 = oxyRequest.getArguments();
            ObjectMapper snakeCaseMapper = new ObjectMapper();
            snakeCaseMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            snakeCaseMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            try {
                oxyRequest.setInputMd5(CommonUtils.getMD5(snakeCaseMapper.writeValueAsString(keyToMd5)));
            } catch (Exception e) {
                log.warn("MD5 calculation failed", e);
                oxyRequest.setInputMd5(CommonUtils.getMD5("{}"));
            }

            Object result = requestInterceptor(oxyRequest);
            if (result instanceof OxyResponse) {
                return (OxyResponse) result;
            }
            handlePreSaveDataAsync(oxyRequest);
            formatInput(oxyRequest);
            preSendMessage(oxyRequest);
            oxyRequest = beforeExecute(oxyRequest);
            OxyResponse oxyResponse = executeWithRetrySync(oxyRequest);
            oxyResponse.setOxyRequest(oxyRequest);
            oxyResponse = afterExecute(oxyResponse);
            oxyResponse = postProcess(oxyResponse);
            postLog(oxyResponse);
            handlePostSaveDataAsync(oxyResponse);

            oxyResponse = formatOutput(oxyResponse);
            postSendMessage(oxyResponse);

            return oxyResponse;
        } catch (Exception e) {
            log.error("Error executing oxy {}: {}", this.getName(), e.getMessage(), e);
            throw new RuntimeException("Execution failed for oxy: " + this.getName(), e);
        } finally {
            if (this.semaphore != null) {
                this.semaphore.release();
            }
        }
    }

    /**
     * Handles pre-save data operations asynchronously.
     *
     * @param oxyRequest the request to save data for
     */
    private void handlePreSaveDataAsync(OxyRequest oxyRequest) {
        if (mas != null) {
            final OxyRequest finalOxyRequest = oxyRequest;
            CompletableFuture<Void> future = ChainedEventProcessor.processEvent(ChainedEventProcessor.createEvent(oxyRequest.getNodeId(), () -> {
                try {
                    preSaveData(finalOxyRequest);
                } catch (Exception e) {
                    log.warn("Error in preSaveDataAsync: {}", e.getMessage());
                }
            }));
            if (null == future) {
                return;
            }

            future.whenComplete((result, throwable) -> {
                if (this.getMas() != null && this.getMas().getBackgroundTasks() != null) {
                    this.getMas().getBackgroundTasks().remove(future);
                }
            });

            if (this.getMas().getBackgroundTasks() != null) {
                this.getMas().getBackgroundTasks().add(future);
            }
        } else {
            log.debug("MAS not available, skipping pre-save data. trace_id={}, node_id={}",
                    oxyRequest.getCurrentTraceId(), oxyRequest.getNodeId());
        }
    }

    /**
     * Handles post-save data operations asynchronously.
     *
     * @param oxyResponse the response to save data for
     */
    private void handlePostSaveDataAsync(OxyResponse oxyResponse) {
        if (getMas() != null) {
            final OxyResponse finalOxyResponse = oxyResponse;
            CompletableFuture<Void> future = ChainedEventProcessor.processEvent(ChainedEventProcessor.updateEvent(oxyResponse.getOxyRequest().getNodeId(), () -> {
                try {
                    postSaveData(finalOxyResponse);
                } catch (Exception e) {
                    log.warn("Error in postSaveDataAsync: {}", e.getMessage());
                }
            }));
            if (null == future) {
                return;
            }

            future.whenComplete((result, throwable) -> {
                if (this.getMas() != null && this.getMas().getBackgroundTasks() != null) {
                    this.getMas().getBackgroundTasks().remove(future);
                }
            });

            if (this.getMas().getBackgroundTasks() != null) {
                this.getMas().getBackgroundTasks().add(future);
            }
        } else {
            log.debug("MAS not available, skipping post-save data. trace_id={}, node_id={}",
                    oxyResponse.getOxyRequest().getCurrentTraceId(),
                    oxyResponse.getOxyRequest().getNodeId());
        }
    }

    /**
     * Executes the request with retry logic.
     *
     * @param oxyRequest the request to execute
     * @return the execution response
     * @throws RuntimeException if all retry attempts fail
     */
    private OxyResponse executeWithRetrySync(OxyRequest oxyRequest) {
        int attempt = 0;
        while (attempt < retries) {
            try {
                OxyResponse response;
                if (this.funcInterceptor != null) {
                    String errorMessage = this.funcInterceptor.apply(oxyRequest);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        response = new OxyResponse();
                        response.setState(OxyState.SKIPPED);
                        response.setOutput(errorMessage);
                        break;
                    }
                }

                if (this._funcExecute != null) {
                    response = this._funcExecute.apply(oxyRequest);
                } else {
                    response = _execute(oxyRequest);
                }
                response.setOxyRequest(oxyRequest);
                return response;
            } catch (Exception throwable) {
                log.warn("Error executing oxy {}: {}. Attempt {} of {}.", this.getName(), throwable.getMessage(), attempt + 1, this.getRetries());
                if (attempt < this.getRetries() - 1) {
                    try {
                        Thread.sleep((long) (this.delay * 1000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Max retries reached. Failed. {}", throwable.getMessage());
                    OxyResponse errorResponse = new OxyResponse();
                    errorResponse.setState(OxyState.FAILED);
                    errorResponse.setOutput(String.format("Error executing oxy %s: %s", this.getName(), throwable.getMessage()));
                    errorResponse.setOxyRequest(oxyRequest);
                    return errorResponse;
                }
            }
            attempt++;
        }
        OxyResponse errorResponse = new OxyResponse();
        errorResponse.setState(OxyState.FAILED);
        errorResponse.setOutput("Unknown error");
        errorResponse.setOxyRequest(oxyRequest);
        return errorResponse;
    }

    private static Map<String, Object> defaultInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        Map<String, Object> queryProps = new HashMap<>();
        queryProps.put("description", "Query question");
        Map<String, Object> properties = new HashMap<>();
        properties.put("query", queryProps);
        List<String> required = Arrays.asList("query");
        schema.put("properties", properties);
        schema.put("required", required);

        return schema;
    }
}