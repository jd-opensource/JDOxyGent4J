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
package com.jd.oxygent.core.oxygent.schemas.oxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.f4b6a3.uuid.UuidCreator;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.jd.oxygent.core.oxygent.utils.CommonUtils.generateShortUUID;

/**
 * Task execution request wrapper class in OxyGent system
 *
 * <p>This class represents a request to execute an oxy (agent/tool) within the MAS (Multi-Agent System).
 * It contains all the information needed to route, execute, and track the request.</p>
 *
 * <p>Main functions include:</p>
 * <ul>
 *     <li>Conversation tracking management - Supports full lifecycle tracking of session threads</li>
 *     <li>Parallel execution support - Supports multi-task concurrent execution and synchronization</li>
 *     <li>Permission and security context - Provides fine-grained permission control</li>
 *     <li>Data sharing and parameter passing - Supports data exchange between agents</li>
 *     <li>Retry and error handling - Built-in fault tolerance mechanisms</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * OxyRequest request = OxyRequest.builder()
 *     .callee("someAgent")
 *     .arguments(Map.of("query", "user question"))
 *     .build();
 * OxyResponse response = request.call(kwargs);
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@ToString(exclude = "mas")
@AllArgsConstructor
@NoArgsConstructor
public class OxyRequest implements Cloneable {

    private static final Logger logger = Logger.getLogger(OxyRequest.class.getName());

    // ==================== Request Identification and Tracking Fields ====================

    /**
     * Request unique identifier, globally unique ID for each request
     */
    @JsonProperty("request_id")
    @Builder.Default
    private String requestId = UuidCreator.getShortSuffixComb().toString();

    /**
     * Group ID, identifies related requests in the same group
     */
    @JsonProperty("group_id")
    @Builder.Default
    private String groupId = generateShortUUID();

    /**
     * Source trace ID, indicating the source chain of the request
     */
    @JsonProperty("from_trace_id")
    @Builder.Default
    private String fromTraceId = null;

    /**
     * Current trace ID, used to track the current execution chain
     */
    @JsonProperty("current_trace_id")
    @Builder.Default
    private String currentTraceId = UuidCreator.getTimeBased().toString();

    /**
     * Reference trace ID, used to associate related execution chains
     */
    @JsonProperty("reference_trace_id")
    @Builder.Default
    private String referenceTraceId = null;

    /**
     * Restart node ID, used for failure recovery scenarios
     */
    @JsonProperty("restart_node_id")
    @Builder.Default
    private String restartNodeId = null;

    /**
     * Restart node output, saves the execution result of the restart node
     */
    @JsonProperty("restart_node_output")
    @Builder.Default
    private String restartNodeOutput = null;

    /**
     * Restart node execution order
     */
    @JsonProperty("restart_node_order")
    @Builder.Default
    private String restartNodeOrder = null;

    /**
     * Whether to load data for restart scenario
     */
    @JsonProperty("is_load_data_for_restart")
    @Builder.Default
    private boolean isLoadDataForRestart = true;

    /**
     * MD5 hash value of input data, used for caching and deduplication
     */
    @JsonProperty("input_md5")
    @Builder.Default
    private String inputMd5 = null;

    /**
     * Root trace ID list, records the root nodes of the entire call chain
     */
    @JsonProperty("root_trace_ids")
    @Builder.Default
    private List<String> rootTraceIds = new CopyOnWriteArrayList<>();

    // ==================== Execution Context Fields ====================

    /**
     * MAS system instance, contains all registered agents and tools
     */
    @Builder.Default
    @JsonIgnore
    private Mas mas = null;

    /**
     * Caller name, identifies the agent or user that initiated the request
     */
    @JsonProperty("caller")
    @Builder.Default
    private String caller = "user";

    /**
     * Callee name, identifies the target agent or tool to execute
     */
    @JsonProperty("callee")
    @Builder.Default
    private String callee = null;

    /**
     * Call stack, records the complete call chain
     */
    @JsonProperty("call_stack")
    @Builder.Default
    private List<String> callStack = new CopyOnWriteArrayList<>(List.of("user"));

    /**
     * Node ID stack, records the hierarchical relationship of execution nodes
     */
    @JsonProperty("node_id_stack")
    @Builder.Default
    private List<String> nodeIdStack = new CopyOnWriteArrayList<>();

    /**
     * Father node ID, identifies the parent node of the current node
     */
    @JsonProperty("father_node_id")
    @Builder.Default
    private String fatherNodeId = "";

    /**
     * Pre-node ID list, records execution dependencies
     */
    @JsonProperty("pre_node_ids")
    @Builder.Default
    private List<String> preNodeIds = new CopyOnWriteArrayList<>();

    /**
     * Latest node ID list, records recently executed nodes
     */
    @JsonProperty("latest_node_ids")
    @Builder.Default
    private List<String> latestNodeIds = new CopyOnWriteArrayList<>();

    /**
     * Caller category, such as: user, agent, tool, etc.
     */
    @JsonProperty("caller_category")
    @Builder.Default
    private String callerCategory = "user";

    /**
     * Callee category, such as: agent, tool, llm, etc.
     */
    @JsonProperty("callee_category")
    @Builder.Default
    private String calleeCategory = null;

    /**
     * Current node ID, identifies the current execution node
     */
    @JsonProperty("node_id")
    @Builder.Default
    private String nodeId = null;

    /**
     * Whether to save history records
     */
    @JsonProperty("is_save_history")
    @Builder.Default
    private boolean isSaveHistory = true;

    /**
     * Parallel execution ID, used to identify parallel execution task groups
     */
    @JsonProperty("parallel_id")
    @Builder.Default
    private String parallelId = "";

    /**
     * Parallel execution dictionary, stores information related to parallel tasks
     */
    @JsonProperty("parallel_dict")
    @Builder.Default
    private Map<String, Object> parallelDict = new ConcurrentHashMap<>();

    // ==================== Data Exchange Fields ====================

    /**
     * Arguments mapping, parameters passed to the target agent or tool
     */
    @JsonProperty("arguments")
    @Builder.Default
    private Map<String, Object> arguments = new ConcurrentHashMap<>();

    /**
     * Shared data mapping, data shared throughout the entire execution chain
     */
    @JsonProperty("shared_data")
    @Builder.Default
    private Map<String, Object> sharedData = new ConcurrentHashMap<>();

    /**
     * Group data mapping, data shared within the same group of requests
     */
    @JsonProperty("group_data")
    @Builder.Default
    private Map<String, Object> groupData = new ConcurrentHashMap<>();

    // ==================== Basic Access Methods ====================

    /**
     * Get session name
     *
     * <p>Used for oxy-python integration, generates session identifier in format "caller__callee"</p>
     *
     * @return session name string
     */
    public String getSessionName() {
        return caller + "__" + callee;
    }

    /**
     * Get oxy instance from MAS container by name
     *
     * @param oxyName oxy name, cannot be null
     * @return corresponding BaseOxy instance, returns null if not exists
     * @throws NullPointerException if oxyName is null
     */
    public BaseOxy getOxy(String oxyName) {
        return this.mas.getOxyNameToOxy().get(oxyName);
    }

    /**
     * Check if oxy with specified name exists in MAS container
     *
     * @param oxyName oxy name, cannot be null
     * @return true if exists, false otherwise
     * @throws NullPointerException if oxyName is null
     */
    public boolean hasOxy(String oxyName) {
        return this.mas.getOxyNameToOxy().containsKey(oxyName);
    }

    /**
     * Create a deep copy of current request and apply specified parameter overrides
     *
     * @param kwargs parameter mapping to override, can be empty
     * @return new OxyRequest instance
     * @throws RuntimeException if deep copy fails
     */
    public OxyRequest cloneWith(Map<String, Object> kwargs) {
        try {
            // Manually deep copy all fields
            OxyRequest copy = new OxyRequest();
            copy.inputMd5 = this.inputMd5;
            copy.rootTraceIds = this.rootTraceIds == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(this.rootTraceIds);
            copy.mas = this.mas;
            copy.caller = this.caller;
            copy.callee = this.callee;
            copy.callStack = this.callStack == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(this.callStack);
            copy.nodeIdStack = this.nodeIdStack == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(this.nodeIdStack);
            copy.fatherNodeId = this.fatherNodeId;
            copy.preNodeIds = this.preNodeIds == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(this.preNodeIds);
            copy.callerCategory = this.callerCategory;
            copy.calleeCategory = this.calleeCategory;
            copy.nodeId = this.nodeId;
            copy.isSaveHistory = this.isSaveHistory;
            copy.parallelDict = this.parallelDict == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(this.parallelDict);
            copy.arguments = this.arguments == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(this.arguments);
            copy.sharedData = this.sharedData == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(this.sharedData);
            copy.groupData = this.groupData == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(this.groupData);
            copy.requestId = this.requestId;
            copy.currentTraceId = this.currentTraceId;
            copy.fromTraceId = this.fromTraceId;
            copy.currentTraceId = this.currentTraceId;
            copy.groupId = this.groupId;
            copy.referenceTraceId = this.referenceTraceId;//Copy attribute values needed for restart
            copy.restartNodeOrder = this.restartNodeOrder;
            copy.restartNodeOutput = this.restartNodeOutput;

            for (Map.Entry<String, Object> mapEntry : this.getArguments().entrySet()) {
                Map<String, Object> map = copy.getArguments();
                if (!map.containsKey(mapEntry.getKey())) {
                    map.put(mapEntry.getKey(), mapEntry.getValue());
                }
                if ("first_query_struct".equals(mapEntry.getKey())) {
                    map.remove("first_query_struct"); // Only used to save query conditions for user's first query, not used subsequently
                }
            }

            for (Map.Entry<String, Object> mapEntry : this.getSharedData().entrySet()) {
                Map<String, Object> map = copy.getSharedData();
                if (!map.containsKey(mapEntry.getKey())) {
                    map.put(mapEntry.getKey(), mapEntry.getValue());
                }
            }
            for (Map.Entry<String, Object> mapEntry : this.getGroupData().entrySet()) {
                Map<String, Object> map = copy.getGroupData();
                if (!map.containsKey(mapEntry.getKey())) {
                    map.put(mapEntry.getKey(), mapEntry.getValue());
                }
            }

            // Override specified fields
            for (Map.Entry<String, Object> entry : kwargs.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                try {
                    Field field = OxyRequest.class.getDeclaredField(JsonUtils.toCamelCase(key));
                    field.setAccessible(true);
                    field.set(copy, value);
                } catch (Exception e) {
                    logger.warning("cloneWith unknown field:" + key);
                }
            }

            return copy;
        } catch (Exception e) {
            throw new RuntimeException("cloneWith deep copy failed", e);
        }
    }

    /**
     * Set query content
     *
     * @param query       query string
     * @param masterLevel whether to set to master level data
     */
    public void setQuery(String query, boolean masterLevel) {
        getDataMap(masterLevel).put("query", query);
    }

    /**
     * Get query content (using default level)
     *
     * @return query string
     */
    public String getQuery() {
        return getQuery(false);
    }

    /**
     * Get query content
     *
     * @param masterLevel whether to get from master level data
     * @return query string, returns empty string if not exists
     */
    public String getQuery(boolean masterLevel) {
        return getDataMap(masterLevel).getOrDefault("query", "").toString();
    }

    /**
     * Get query object (original form)
     *
     * @param masterLevel whether to get from master level data
     * @return query object, returns empty string if not exists
     */
    public Object getQueryObject(boolean masterLevel) {
        return getDataMap(masterLevel).getOrDefault("query", "");
    }

    /**
     * Get multimodal part data (using default level)
     *
     * @return mapping containing urls and files lists
     */
    public Map<String, List<?>> getMultimodalPart() {
        return getMultimodalPart(true);
    }

    /**
     * Get multimodal part data
     *
     * @param masterLevel whether to get from master level data
     * @return mapping containing urls and files lists
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<?>> getMultimodalPart(boolean masterLevel) {
        var urlList = (List<String>) getDataMap(masterLevel).getOrDefault("urls", new ArrayList<>());
        var fileList = (List<String>) getDataMap(masterLevel).getOrDefault("files", new ArrayList<>());
        var multipart = new HashMap<String, List<?>>();
        multipart.put("urls", urlList);
        multipart.put("files", fileList);
        return multipart;
    }

    /**
     * Get query parts list
     *
     * <p>Convert query object to standardized parts list format. Supports the following input types:</p>
     * <ul>
     *     <li>List - return directly</li>
     *     <li>Map - wrap as single-element list and return</li>
     *     <li>Others - convert to text/plain format and return</li>
     * </ul>
     *
     * @return standardized query parts list
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getQueryParts() {
        var queryObject = this.getQueryObject(false);

        if (queryObject instanceof List<?>) {
            return (List<Map<String, Object>>) queryObject;
        }

        if (queryObject instanceof Map<?, ?>) {
            var result = new ArrayList<Map<String, Object>>();
            result.add((Map<String, Object>) queryObject);
            return result;
        }

        // Default case: convert to text/plain format
        var part = new HashMap<String, Object>();
        var content = new HashMap<String, String>();
        content.put("content_type", "text/plain");
        content.put("data", (queryObject != null) ? queryObject.toString() : "");
        part.put("part", content);
        return Collections.singletonList(part);
    }

    // ==================== Short-term Memory Management Methods ====================

    /**
     * Check if short-term memory exists (using default level)
     *
     * @return true if short-term memory exists
     */
    public boolean hasShortMemory() {
        return hasShortMemory(false);
    }

    /**
     * Check if short-term memory exists
     *
     * @param masterLevel whether to check master level memory
     * @return true if short-term memory exists
     */
    public boolean hasShortMemory(boolean masterLevel) {
        return arguments.containsKey(getShortMemoryKey(masterLevel));
    }

    /**
     * Set short-term memory
     *
     * @param shortMemory short-term memory object
     * @param masterLevel whether to set to master level
     */
    public void setShortMemory(Object shortMemory, boolean masterLevel) {
        arguments.put(getShortMemoryKey(masterLevel), shortMemory);
    }

    /**
     * Get short-term memory
     *
     * @param masterLevel whether to get master level memory
     * @return short-term memory list, returns empty list if not exists
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getShortMemory(boolean masterLevel) {
        arguments.putIfAbsent(getShortMemoryKey(masterLevel), new ArrayList<>());//Keep short_memory passed in requests
        return (List<Map<String, Object>>) arguments.get(getShortMemoryKey(masterLevel));
    }

    /**
     * Get data mapping based on level
     *
     * @param masterLevel whether to get master level data
     * @return corresponding data mapping
     */
    private Map<String, Object> getDataMap(boolean masterLevel) {
        return masterLevel ? sharedData : arguments;
    }

    /**
     * Get short-term memory key name based on level
     *
     * @param masterLevel whether master level
     * @return corresponding key name
     */
    private String getShortMemoryKey(boolean masterLevel) {
        return masterLevel ? "master_short_memory" : "short_memory";
    }

    // ==================== Message Sending Methods ====================

    /**
     * Send message to current MAS system
     *
     * <p>Send message to Redis queue for inter-system communication. Messages are routed
     * to corresponding processing queues based on current trace ID.</p>
     *
     * @param message message content to send, cannot be null or empty
     */
    public void sendMessage(Map<String, Object> message) {
        if (this.mas != null && message != null && !message.isEmpty()) {
            var redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + this.getCurrentTraceId();
            this.mas.sendMessage(message, redisKey);
        }
    }

    public void breakTask() {
        this.sendMessage(Map.of("event", "close", "data", "done"));
        CompletableFuture<?> completableFuture = (CompletableFuture<?>) this.mas.getActiveTasks().get(this.getCurrentTraceId());
        if (completableFuture != null) {
            completableFuture.cancel(true);
        }
    }

    /**
     * Oxy execution method with retry mechanism
     *
     * <p>If oxy tool execution fails, it will automatically retry based on configured retry count.
     * There will be delay intervals between each retry, and error response is returned on final failure.</p>
     *
     * @param oxy      oxy tool object, cannot be null
     * @param reqParam request parameter object, uses current instance if null
     * @return execution result of oxy tool
     */
    public OxyResponse retryExecute(BaseOxy oxy, OxyRequest reqParam) {
        if (reqParam == null) {
            reqParam = this;
        }

        OxyRequest oxyRequest = reqParam;

        for (int attempt = 1; attempt <= oxy.getRetries(); attempt++) {
            try {
                return oxy.execute(oxyRequest);
            } catch (Exception e) {
                boolean isFinal = attempt == oxy.getRetries();

                int finalAttempt = attempt;
                logger.info(() -> String.format(
                        "Oxy %s exec failed, attempt %d/%d, trace=%s, node=%s, ex=%s",
                        oxy.getName(), finalAttempt, oxy.getRetries(),
                        oxyRequest.getCurrentTraceId(), oxyRequest.getNodeId(),
                        e.toString()));

                if (isFinal) {
                    logger.log(Level.SEVERE,
                            String.format("Abandoning oxy %s after %d/%d retries",
                                    oxy.getName(), attempt, oxy.getRetries()), e);
                    break;
                }
                try {
                    Thread.sleep((long) oxy.getDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warning("Retry loop interrupted for oxy " + oxy.getName());
                    break;
                }
            }
        }

        return OxyResponse.builder()
                .state(OxyState.FAILED)
                .output("Error executing tool " + oxy.getName())
                .build();
    }


    /**
     * Execute oxy tool and return execution result
     *
     * <p>This is the core execution method, including the following functions:</p>
     * <ul>
     *     <li>Parameter validation and permission checking</li>
     *     <li>Parallel execution support</li>
     *     <li>Pre-processing and post-processing for special tools</li>
     *     <li>Error handling and logging</li>
     * </ul>
     *
     * @param kwargs execution parameters for oxy tool, in key-value pair format
     * @return OxyResponse object containing execution status and output information
     */
    public OxyResponse call(Map<String, Object> kwargs) {

        OxyRequest oxyRequest = this.cloneWith(kwargs);

        oxyRequest.nodeId = generateShortUUID();

        if (oxyRequest.parallelId == null || oxyRequest.parallelId.isEmpty()) {
            oxyRequest.parallelId = generateShortUUID();
        }


        Map<String, Object> parallelInfo = (Map<String, Object>) this.parallelDict.getOrDefault(oxyRequest.parallelId, null);

        if (parallelInfo != null) {
            @SuppressWarnings("unchecked")
            List<String> parallelNodeIds = (List<String>) parallelInfo.get("parallel_node_ids");
            parallelNodeIds.add(oxyRequest.nodeId);
        } else {
            parallelInfo = new HashMap<>();
            parallelInfo.put("pre_node_ids", this.latestNodeIds);
            List<String> parallelNodeIds = new ArrayList<>();
            parallelNodeIds.add(oxyRequest.nodeId);
            parallelInfo.put("parallel_node_ids", parallelNodeIds);
            this.parallelDict.put(oxyRequest.parallelId, parallelInfo);
        }
        oxyRequest.preNodeIds = Optional.ofNullable((List<String>) kwargs.get("pre_node_ids"))
                .orElseGet(() -> Optional.ofNullable(this.parallelDict.get(oxyRequest.parallelId))
                        .map(info -> (Map<String, Object>) info)
                        .map(info -> (List<String>) info.get("pre_node_ids"))
                        .orElse(new ArrayList<>()));

        this.latestNodeIds = Optional.ofNullable(this.parallelDict.get(oxyRequest.parallelId))
                .map(info -> (Map<String, Object>) info)
                .map(info -> (List<String>) info.get("parallel_node_ids"))
                .orElse(new ArrayList<>());


        oxyRequest.fatherNodeId = this.nodeId;
        oxyRequest.caller = this.callee;
        oxyRequest.callerCategory = this.calleeCategory;

        String oxyName = oxyRequest.callee;

        if (!this.hasOxy(oxyName)) {
            logger.severe("oxy " + oxyName + " not exists");
            return OxyResponse.builder()
                    .state(OxyState.SKIPPED)
                    .output(String.format("No permission for tool: %s", oxyName))
                    .build();
        }

        BaseOxy callerOxy = getOxy(oxyRequest.caller);
        BaseOxy oxy = getOxy(oxyName);
        if (!"user".equals(oxyRequest.getCallerCategory())
                && oxy.isPermissionRequired()
                && !(callerOxy.getPermittedToolNameList().contains(oxyName)
                || callerOxy.getExtraPermittedToolNameList().contains(oxyName))) {

            logger.severe(String.format(
                    "No permission for oxy: %s, caller: %s, trace_id=%s, node_id=%s",
                    oxyName,
                    oxyRequest.getCaller(),
                    oxyRequest.getCurrentTraceId(),
                    oxyRequest.getNodeId()));

            return OxyResponse.builder()
                    .state(OxyState.SKIPPED)
                    .output(String.format("No permission for tool: %s", oxyName))
                    .build();
        }

        // Special parameter processing for retrieve_tools
        if ("retrieve_tools".equals(oxyName)) {
            Map<String, Object> args = oxyRequest.getArguments();
            // Ensure parameter mapping is not null
            if (args != null) {
                args.put("app_name", Config.getAppName());
                args.put("agent_name", callerOxy.getName());
                args.put("vearch_client", this.getMas().getVearchClient());
            }
        }
        try {

            double timeoutMs = oxy.getTimeout();
            OxyResponse oxyResponse = oxy.execute(oxyRequest);
            // 3) retrieve_tools response post-processing: convert tool name list to tool descriptions and concatenate
            if ("retrieve_tools".equals(oxyName)) {
                Object out = oxyResponse.getOutput();
                List<String> toolNames;
                if (out instanceof List<?>) {
                    toolNames = ((List<?>) out).stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                } else if (out != null) {
                    // Fallback handling: if output is not a list, try to split by lines
                    toolNames = Arrays.stream(out.toString().split("\\r?\\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                } else {
                    toolNames = Collections.emptyList();
                }

                List<String> llmToolDescList = new ArrayList<>();
                for (String toolName : toolNames) {
                    BaseOxy tool = this.getOxy(toolName);
                    if (tool != null) {
                        llmToolDescList.add(tool.getDescForLlm());
                    }
                }
                oxyResponse.setOutput(String.join("\n\n", llmToolDescList));
            }

            return oxyResponse;

        } catch (Exception e) {

            OxyResponse failed = new OxyResponse();
            failed.setState(OxyState.FAILED);
            failed.setOutput("Error executing tool " + oxy.getName() + ": " + e.getMessage());
            return failed;
        }
    }

    /**
     * Start executing the specified callee and return execution result
     *
     * @return execution response result
     * @throws IllegalStateException if callee does not exist
     */
    public OxyResponse start() {
        Objects.requireNonNull(this.callee, "Callee cannot be null");
        logger.info("Starting execution of: " + callee);
        return this.getOxy(this.callee).execute(this);
    }

    // ==================== Global Data Methods ====================

    /**
     * Check if specified key exists in global data
     *
     * @param key key to check
     * @return true if key exists, false otherwise
     */
    public boolean hasGlobalData(String key) {
        if (this.mas == null || this.mas.getGlobalData() == null) {
            return false;
        }
        return this.mas.getGlobalData().containsKey(key);
    }

    /**
     * Get global data
     *
     * @param key          key to get, returns entire global data Map if null
     * @param defaultValue default value returned when key does not exist
     * @return global data value or default value
     */
    public Object getGlobalData(String key, Object defaultValue) {
        if (this.mas == null || this.mas.getGlobalData() == null) {
            return key == null ? new HashMap<>() : defaultValue;
        }

        if (key == null) {
            return this.mas.getGlobalData();
        }

        return this.mas.getGlobalData().getOrDefault(key, defaultValue);
    }

    /**
     * Get global data (using null as default value)
     *
     * @param key key to get, returns entire global data Map if null
     * @return global data value or null
     */
    public Object getGlobalData(String key) {
        return getGlobalData(key, null);
    }

    /**
     * Get entire global data Map
     *
     * @return global data Map
     */
    public Map<String, Object> getGlobalData() {
        return (Map<String, Object>) getGlobalData(null, new HashMap<>());
    }

    /**
     * Set global data
     *
     * @param key   key to set
     * @param value value to set
     */
    public void setGlobalData(String key, Object value) {
        if (this.mas == null) {
            logger.warning("Cannot set global data: mas is null");
            return;
        }

        if (this.mas.getGlobalData() == null) {
            logger.warning("Cannot set global data: mas.globalData is null");
            return;
        }

        this.mas.getGlobalData().put(key, value);
    }

}
