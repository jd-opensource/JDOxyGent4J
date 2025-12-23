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

package com.jd.oxygent.core;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.infra.databases.BaseCache;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.infra.impl.databases.es.LocalEs;
import com.jd.oxygent.core.oxygent.infra.impl.databases.redis.LocalCache;
import com.jd.oxygent.core.oxygent.oxy.BaseFlow;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.BaseTool;
import com.jd.oxygent.core.oxygent.oxy.agents.BaseAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.RemoteAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.BaseLlM;
import com.jd.oxygent.core.oxygent.schemas.contextengineer.ContextEngine;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.utils.ObjectUtils;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.msgpack.core.MessagePack;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OxyGent Multi-Agent System Core Management Class
 * <p>
 * This class is responsible for managing the entire lifecycle of the multi-agent system, including:
 * - System startup and initialization
 * - Component registration and management
 * - Agent organization structure
 * - Resource management and scheduling
 * - Task execution and coordination
 * - Data persistence and caching
 * <p>
 * Core attribute descriptions:
 * - name: MAS instance identifier
 * - oxyNameToOxy: Mapping table from Oxy names to instances (registry)
 * - oxySpace: List of registered Oxy instances
 * - masterAgentName: Master agent name (BaseAgent instance)
 * - activeTasks: Active task management dictionary for SSE and async operations
 * - esClient/redisClient/vearchClient: Elasticsearch, Redis, Vearch database clients
 * - agentOrganization: Agent organization structure
 * - lock: Task execution flow control lock
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Slf4j
public class Mas {

    private String name = "";
    private AgentNode agentOrganization;

    private List<BaseOxy> defaultOxySpace = new ArrayList<>();
    private List<BaseOxy> oxySpace = new ArrayList<>();
    private Map<String, BaseOxy> oxyNameToOxy = new ConcurrentHashMap<>();

    private String masterAgentName = "";
    private String firstQuery = "";
    @Setter(AccessLevel.NONE)
    private ThreadLocal<String> currentTraceId = new ThreadLocal<>();

    private static final MessagePack.PackerConfig packerConfig = new MessagePack.PackerConfig()
            .withBufferSize(655360)
            .withBufferFlushThreshold(327680)
            .withSmallStringOptimizationThreshold(10240)
            .withStr8FormatSupport(true);
    private static final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory(packerConfig));
    private boolean lock = false;
    private String messagePrefix = "oxygent";

    private Map<String, Object> activeTasks = new ConcurrentHashMap<>();
    private Set<Object> backgroundTasks = new HashSet<>();
    private Map<String, Object> eventDict = new ConcurrentHashMap<>();

    private HttpClient httpClient;

    @Autowired
    private BaseEs esClient;
    @Autowired
    private BaseCache redisClient;

    @Autowired
    private ContextEngine contextEngine;

    private Object vearchClient;

    @Autowired
    private ServerProperties serverProperties;

    @JsonProperty("global_data")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> globalData = new HashMap<>();

    /**
     * User's first query marker, used to implement query record saving only once, key is request_id, value is boolean
     */
    @JsonIgnore
    public static Set firstQuerySet = ConcurrentHashMap.newKeySet();

    /**
     * Temporarily store user query parameters, used to solve the problem of missing parameters in response when concatenating streaming answers
     * key is request_id
     * value is body
     */
    @JsonIgnore
    public static Map<Object, Map> queryParamMap = new ConcurrentHashMap();

    /**
     * Default constructor, creates a MAS instance named "app"
     */
    public Mas() {
        this("app", null);
    }

    /**
     * Constructor, creates a MAS instance with specified name and component space
     *
     * @param name     MAS instance name, defaults to "app" if null
     * @param oxySpace Initial Oxy component list, can be null
     */
    public Mas(String name, List<BaseOxy> oxySpace) {
        this.name = name != null ? name : "app";
        this.oxySpace = oxySpace;
    }

    /**
     * Initialize MAS system
     * <p>
     * Execute all necessary setup steps to prepare the MAS system for operation, including:
     * - Display startup banner and environment information
     * - Register all instances in oxy_space and inject them into MAS
     * - Initialize database connections (Elasticsearch, Redis)
     * - Set up agent organization structure
     * - Initialize vector search if configured
     *
     * @throws RuntimeException if errors occur during initialization
     */
    public void init() {
        try {
            showBanner();
            showMasInfo();
            addOxyList(oxySpace);
            initDb();
            initAllOxy();
            initMasterAgentName();
            if (Config.getVearch().isEnabled()) {
                createVearchTable();
            }
            initAgentOrganization();
            showOrg();
            log.info("MAS system initialization completed: {}", this.name);
        } catch (Exception e) {
            log.error("MAS initialization failed: {}", e.getMessage());
            throw new RuntimeException("MAS initialization failed", e);
        }
    }

    /**
     * Batch add Oxy components to the system
     *
     * @param oxySpace Oxy component list, can be null or empty
     * @throws IllegalArgumentException if there are components with duplicate names
     */
    public void addOxyList(List<BaseOxy> oxySpace) {
        if (oxySpace != null && !oxySpace.isEmpty()) {
            for (BaseOxy oxy : oxySpace) {
                if (oxyNameToOxy.containsKey(oxy.getName())) {
                    throw new IllegalArgumentException("oxy [{}] already exists." + oxy.getName());
                }
                oxyNameToOxy.put(oxy.getName(), oxy);
                log.debug("Added oxy [{}]. Current registry: {}", oxy.getName(), oxyNameToOxy);
            }
        }
    }

    /**
     * Add a single Oxy component to the system
     *
     * @param oxy The Oxy component to add, cannot be null
     * @throws IllegalArgumentException if oxy is null, name is null, or a component with the same name already exists
     */
    public void addOxy(BaseOxy oxy) {
        Objects.requireNonNull(oxy, "Oxy component cannot be null");
        Objects.requireNonNull(oxy.getName(), "Oxy component name cannot be null");

        if (oxyNameToOxy.containsKey(oxy.getName())) {
            throw new IllegalArgumentException("Oxy [" + oxy.getName() + "] already exists.");
        }
        oxyNameToOxy.put(oxy.getName(), oxy);
    }

    /**
     * CLI mode execution - corresponds to the cli method in Python version
     */
    public void cli() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Entering CLI mode, type 'exit' to quit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                break;
            }

            try {
                // Process user input
                Map<String, Object> payload = new HashMap<>();
                payload.put("query", input);
                OxyResponse response = chatWithAgent(payload);
                System.out.println(response.getOutput());
            } catch (Exception e) {
                System.err.println("Error processing input: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("Exiting CLI mode");
    }

    /**
     * Get Oxy component by name
     */
    public BaseOxy getOxyByName(String oxyName) {
        return oxyNameToOxy != null ? oxyNameToOxy.get(oxyName) : null;
    }

    /**
     * Get all Oxy component names
     */
    public List<String> getAllOxyNames() {
        return oxyNameToOxy != null ? new ArrayList<>(oxyNameToOxy.keySet()) : new ArrayList<>();
    }

    /**
     * Display startup banner
     */
    private void showBanner() {
        String banner =
                "\n" +
                        "  ____                ____            _   \n" +
                        " / __ \\__  ____  ____/ __ \\___  ____  (_)_ \n" +
                        "/ / / / / / / / / / __/ /_/ / _ \\/ __ \\/ __/\n" +
                        "\\/ /_/ / /_/ / /_/ /_/ /_/ /  __/ / / / /_  \n" +
                        " \\____/\\__, /\\__, /\\____/_/\\___/_/ /_/\\__/  \n" +
                        "       /____//____/                        \n" +
                        "\n" +
                        "OxyGent Multi-Agent System (Java Version)\n" +
                        "Initialization Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                        "========================================\n";

        System.out.println(banner);
        log.info("OxyGent MAS starting...");
    }

    Map<String, Object> createTraceMapping() {
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        properties.put("request_id", Map.of("type", "keyword"));
        properties.put("group_id", Map.of("type", "keyword"));
        properties.put("trace_id", Map.of("type", "keyword"));
        properties.put("from_trace_id", Map.of("type", "keyword"));
        properties.put("root_trace_ids", Map.of("type", "keyword"));
        properties.put("input", Map.of("type", "text"));
        properties.put("callee", Map.of("type", "keyword"));
        properties.put("output", Map.of("type", "text"));
        properties.put("group_data", Map.of("type", "object"));
        properties.put("shared_data", Map.of("type", "object"));
        properties.put("create_time", Map.of(
                "type", "date",
                "format", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        ));

        mapping.put("properties", properties);
        Map<String, Object> root = new HashMap<>();
        root.put("mappings", mapping);
        getEsSetting(root);
        return root;
    }

    Map<String, Object> createNodeMapping() {
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

        properties.put("node_id", Map.of("type", "keyword"));
        properties.put("group_id", Map.of("type", "keyword"));
        properties.put("request_id", Map.of("type", "keyword"));
        properties.put("node_type", Map.of("type", "keyword"));
        properties.put("trace_id", Map.of("type", "keyword"));
        properties.put("caller", Map.of("type", "keyword"));
        properties.put("callee", Map.of("type", "keyword"));
        properties.put("parallel_id", Map.of("type", "keyword"));
        properties.put("father_node_id", Map.of("type", "keyword"));
        properties.put("input", Map.of("type", "text"));
        properties.put("input_md5", Map.of("type", "keyword"));
        properties.put("output", Map.of("type", "text"));
        properties.put("state", Map.of("type", "keyword"));
        properties.put("extra", Map.of("type", "text"));
        properties.put("shared_data", Map.of("type", "text"));
        properties.put("call_stack", Map.of("type", "text"));
        properties.put("node_id_stack", Map.of("type", "text"));
        properties.put("pre_node_ids", Map.of("type", "text"));
        properties.put("create_time", Map.of(
                "type", "date",
                "format", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        ));
        properties.put("update_time", Map.of(
                "type", "date",
                "format", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        ));

        mapping.put("properties", properties);
        getEsSetting(mapping);
        Map<String, Object> root = new HashMap<>();
        root.put("mappings", mapping);
        getEsSetting(root);
        return root;
    }

    Map<String, Object> createHistoryMapping() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("history_id", Map.of("type", "keyword"));
        properties.put("session_name", Map.of("type", "keyword"));
        properties.put("trace_id", Map.of("type", "keyword"));
        properties.put("memory", Map.of("type", "text"));
        properties.put("create_time", Map.of(
                "type", "date",
                "format", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        ));

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("properties", properties);
        Map<String, Object> root = new HashMap<>();
        root.put("mappings", mappings);
        getEsSetting(root);
        return root;
    }

    Map<String, Object> createMessageMapping() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("message_id", Map.of("type", "keyword"));
        properties.put("trace_id", Map.of("type", "keyword"));
        properties.put("message", Map.of("type", "text"));
        properties.put("message_type", Map.of("type", "keyword"));
        properties.put("create_time", Map.of(
                "type", "date",
                "format", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        ));
        properties.put("from_trace_id", Map.of("type", "keyword"));
        properties.put("group_id", Map.of("type", "keyword"));

        Map<String, Object> mappings = new HashMap<>();
        mappings.put("properties", properties);
        // If you need the outer "mappings" wrapper
        Map<String, Object> root = new HashMap<>();
        root.put("mappings", mappings);
        getEsSetting(root);
        return root;
    }

    private void getEsSetting(Map mappings) {
        Map setting = new HashMap();
        if (Config.getEsSettings().getNumberOfShards() != null) {
            setting.put("number_of_shards", Config.getEsSettings().getNumberOfShards());
        }
        if (Config.getEsSettings().getNumberOfReplicas() != null) {
            setting.put("number_of_replicas", Config.getEsSettings().getNumberOfReplicas());
        }
        if (!setting.isEmpty()) {
            mappings.put("settings", setting);
        }
    }

    private void initDb() {
        try {
            if (this.esClient == null) {
                this.esClient = new LocalEs();
                log.info("esClient not injected, using LocalEs");
            }

            if (this.esClient instanceof BaseEs) {
                BaseEs es = this.esClient;
                String indexNameTrace = Config.getAppName() + "_trace";
                es.createIndex(indexNameTrace, createTraceMapping());
                String indexNameNode = Config.getAppName() + "_node";
                es.createIndex(indexNameNode, createNodeMapping());
                String indexNameHistory = Config.getAppName() + "_history";
                es.createIndex(indexNameHistory, createHistoryMapping());
                String indexNameMessage = Config.getAppName() + "_message";
                es.createIndex(indexNameMessage, createMessageMapping());
            }

            if (this.redisClient == null) {
                this.redisClient = new LocalCache();
                log.info("redisClient not injected, using LocalRedis");
            }
            log.debug("init db {} enabled, {} enabled", esClient.getClass().getName(), redisClient.getClass().getName());
        } catch (Exception e) {
            log.error("Database initialization failed", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Initialize agent organization structure
     */
    public void initAgentOrganization() {
        List<AgentNode> rootList = new ArrayList<>();

        if (masterAgentName != null && !masterAgentName.isEmpty()) {
            addTools(rootList, Arrays.asList(masterAgentName), new ArrayList<>());
        } else {
            rootList.add(new AgentNode());
        }

        this.agentOrganization = rootList.isEmpty() ? new AgentNode() : rootList.get(0);
    }

    private void addTools(List<AgentNode> agentOrganization,
                          List<String> agentNames,
                          List<String> path) {
        for (String agentName : agentNames) {
            BaseOxy agent = oxyNameToOxy.get(agentName);
            if (agent == null) {
                continue;
            }

            List<String> tempPath = new ArrayList<>(path);
            tempPath.add(agentName);

            AgentNode node = new AgentNode(agentName, agent.getCategory());
            agentOrganization.add(node);

            if (!isAgent(agentName)) {
                continue;
            }

            List<AgentNode> children = new ArrayList<>();
            node.setChildren(children);

            List<String> toolNameList = new ArrayList<>();
            List<String> permittedToolNameList = agent.getPermittedToolNameList();
            if (permittedToolNameList != null) {
                for (String toolName : permittedToolNameList) {
                    toolNameList.add(toolName);
                }
            }

            addTools(children, toolNameList, tempPath);

            if (agent instanceof RemoteAgent) {
                var remoteOrg = ((RemoteAgent) agent).getOrg();
                if (remoteOrg != null && remoteOrg.containsKey("children")) {
                    List<AgentNode> agentNodes = mapConvertNode((List<Map>) remoteOrg.get("children"));
                    node.setChildren(agentNodes);
                }
            }
        }
    }

    public String getCurrentTraceId() {
        return currentTraceId.get();
    }


    @Data
    public class AgentNode {
        private String name;
        private String type;
        private List<AgentNode> children = new ArrayList<>();

        public AgentNode() {
        }

        public AgentNode(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public List<AgentNode> getChildren() {
            return children;
        }

    }

    private List<AgentNode> mapConvertNode(List<Map> childrenList) {
        List<AgentNode> agentNodes = new ArrayList<>();
        if (childrenList == null || childrenList.isEmpty()) {
            return agentNodes;
        }

        for (Map m : childrenList) {
            AgentNode agentNode = new AgentNode(m.getOrDefault("name", "").toString(), m.getOrDefault("type", "").toString());
            if (m.containsKey("children")) {
                agentNode.setChildren(mapConvertNode((List<Map>) m.get("children")));
            }
            agentNodes.add(agentNode);
        }
        return agentNodes;
    }

    /**
     * Display organization structure
     */
    public void showOrg() {
        log.info("🌐 OxyGent MAS Organization Structure Overview");
        log.info("================================================================");
        printTree(agentOrganization, 0); // 0 indicates initial indentation level
        log.info("================================================================");
    }

    // Recursively print tree structure
    private void printTree(AgentNode node, int level) {
        if (node == null) {
            return;
        }
        String indent = "  ".repeat(level);
        log.info(indent + "- " + node.getName() + " (" + node.getType() + ")");
        for (AgentNode child : node.getChildren()) {
            printTree(child, level + 1);
        }
    }

    /**
     * Batch synchronous initialization of specified types of Oxy objects
     */
    public void batchInitOxy(Class<?>... classTypes) {
        Set<Class<?>> typeSet = new HashSet<>(Arrays.asList(classTypes));

        try {

            for (BaseOxy oxy : oxyNameToOxy.values()) {
                boolean match = typeSet.stream().anyMatch(type -> type.isInstance(oxy));
                if (!match) {
                    continue;
                }

                oxy.setMas(this);
                oxy.init();
            }
        } catch (Exception e) {
            log.error("batchInitOxy error", e);
            throw e;
        }
    }

    /**
     * Initialize all Oxy components
     * Including batch initialization of tools and agents
     */
    private void initAllOxy() {
        try {
            batchInitOxy(BaseLlM.class);
            batchInitOxy(BaseTool.class);
            batchInitOxy(BaseFlow.class);
            batchInitOxy(BaseAgent.class);
            log.info("All Oxy components initialization completed");

        } catch (Exception e) {
            log.error("Oxy components initialization failed: " + e.getMessage());
            throw new RuntimeException("Oxy components initialization failed", e);
        }
    }


    /**
     * Get active task count
     */
    public int getActiveTaskCount() {
        return activeTasks != null ? activeTasks.size() : 0;
    }

    /**
     * Get background task count
     */
    public int getBackgroundTaskCount() {
        return backgroundTasks != null ? backgroundTasks.size() : 0;
    }

    /**
     * Check if system is busy
     */
    public boolean isBusy() {
        return getActiveTaskCount() > 0 || getBackgroundTaskCount() > 0;
    }

    /**
     * Initialize the master agent name.
     * Iterate through all Oxy objects, find the first Agent and set it as masterAgentName;
     * If there is an Agent with isMaster=true, prioritize setting it as masterAgentName.
     */
    public void initMasterAgentName() {
        for (Map.Entry<String, BaseOxy> entry : oxyNameToOxy.entrySet()) {
            String oxyName = entry.getKey();
            BaseOxy oxy = entry.getValue();

            if (!isAgent(oxyName)) {
                continue;
            }
            // If masterAgentName is not set yet, set it to the first agent
            if (masterAgentName == null || masterAgentName.isEmpty()) {
                masterAgentName = oxyName;
            }
            // If current agent is master, set it as master directly and exit
            if (oxy.isMaster()) {
                masterAgentName = oxyName;
                break;
            }
        }
    }

    public void createVearchTable() {
        List<ToolInfo> toolList = new ArrayList<>();

        for (Map.Entry<String, BaseOxy> entry : oxyNameToOxy.entrySet()) {
            String toolName = entry.getKey();
            BaseOxy tool = entry.getValue();

            if (!isAgent(toolName)) {
                continue;
            }

            for (String permittedToolName : tool.getPermittedToolNameList()) {
                BaseOxy permittedTool = oxyNameToOxy.get(permittedToolName);
                if (permittedTool == null) {
                    continue;
                }
                String toolDesc = permittedTool.getDescForLlm();

                if ("retrieve_tools".equals(permittedToolName)) {
                    continue;
                }
                toolList.add(new ToolInfo(this.name, toolName, permittedToolName, toolDesc));
            }
        }
    }

    public static class ToolInfo {
        private String masName;
        private String toolName;
        private String permittedToolName;
        private String toolDesc;

        public ToolInfo(String masName, String toolName, String permittedToolName, String toolDesc) {
            this.masName = masName;
            this.toolName = toolName;
            this.permittedToolName = permittedToolName;
            this.toolDesc = toolDesc;
        }

        @Override
        public String toString() {
            return String.format("ToolInfo{masName='%s', toolName='%s', permittedToolName='%s', toolDesc='%s'}",
                    masName, toolName, permittedToolName, toolDesc);
        }
    }

    public void sendMessage(Map<String, Object> body, String sendMsgKey) {
        try {
            if (Config.getMessage().isShowInTerminal()) {
                log.info("--- Send Message ---: {}", body);
            }
            if (this.redisClient != null && sendMsgKey != null) {
                byte[] messageByte = packMessage(body);
                String base64 = Base64.getEncoder().encodeToString(messageByte);
                redisClient.lpush(sendMsgKey, base64);
            }
            if (Config.getMessage().isStored() && this.esClient != null) {
                String type = body.get("type") != null ? body.getOrDefault("type", "").toString() : body.getOrDefault("message_type", "").toString();
                Map<String, Object> content = body;
                if (body.get("content") instanceof Map contentMap && !"todolist".equals(type)) {
                    content = contentMap;
                }
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("message_id", CommonUtils.generateShortUUID(16));
                messageData.put("trace_id", content.get("current_trace_id"));
                Map _copy = ObjectUtils.deepCopy(body);
                removeAbandonedFields(_copy);
                messageData.put("message", JsonUtils.writeValueAsString(_copy));

                messageData.put("message_type", type);
                messageData.put("caller", content.get("caller"));
                messageData.put("callee", content.get("callee"));
                messageData.put("callee_category", content.get("callee_category"));
                messageData.put("caller_category", content.get("caller_category"));
                messageData.put("create_time", CommonUtils.getFormatTime());
                messageData.put("from_trace_id", content.get("from_trace_id"));
                messageData.put("group_id", content.get("group_id"));
                messageData.put("body", _copy);
                esClient.index(Config.getAppName() + "_message", messageData.getOrDefault("message_id", "").toString(), messageData);
            }
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    public byte[] packMessage(Map<String, Object> message) throws IOException {
        if (message != null) {
            return objectMapper.writeValueAsBytes(message);
        } else {
            return objectMapper.writeValueAsBytes(new ObjectMapper());
        }
    }

    public Map unpackMessage(byte[] bytesMsg) throws IOException {
        return objectMapper.readValue(bytesMsg, Map.class);
    }

    /**
     * Handle chat query and forward to MAS
     *
     * @param payload Request payload
     * @return Response result
     */
    public OxyResponse chatWithAgent(Map<String, Object> payload) throws Exception {
        String redisKey = this.messagePrefix + ":" + this.name + ":" + payload.get("current_trace_id");
        return chatWithAgent(payload, redisKey);
    }

    /**
     * Top‑level helper that forwards a *chat query* into the MAS.
     * <p>
     * The method converts *payload* into an :class:`~schemas.OxyRequest`,
     * ensures reasonable defaults (e.g. *callee* = master agent), and then
     * awaits the resulting :class:`~schemas.OxyResponse`.
     * <p>
     * If *send_msg_key* is supplied, partial outputs are written to the
     * corresponding Redis list so that a connected SSE client can stream
     * them to the browser.
     * <p>
     * Args:
     * payload: Mapping that **must** contain the key ``query``.
     * send_msg_key: Optional Redis key for SSE streaming.
     * <p>
     * Returns:
     * OxyResponse: Fully populated response object.
     */
    public OxyResponse chatWithAgent(Map<String, Object> payload, String sendMsgKey) throws Exception {
        if (!payload.containsKey("query")) {
            throw new IllegalArgumentException("Payload must contain the key 'query'.");
        }

        // Set default values for shared_data and query
        Map<String, Object> sharedData = new HashMap<>();
        if (payload.get("shared_data") != null) {
            sharedData = (Map<String, Object>) payload.get("shared_data");
        }
        if (payload.get("first_query_struct") == null) {
            payload.put("first_query_struct", payload.get("query"));
        }
        Object requestId = JsonUtils.firstNotBlank(payload.get("request_id"), payload.get("req_id"), payload.get("requestId"));
        if (requestId != null) {
            Mas.firstQuerySet.add(requestId);
            Mas.queryParamMap.put(requestId, payload);
        } else {
            log.warn("requestId is null");
        }

        Object query = payload.get("query");
        String queryString = (query instanceof String) ? (String) query
                : (query != null ? JsonUtils.toJSONString(query) : null);
        sharedData.put("query", queryString);
        payload.put("shared_data", sharedData);

        // Check for restart_node_id and reference_trace_id in payload
        if (payload.containsKey("restart_node_id") && payload.get("restart_node_id") != null) {
            Map<String, Object> searchQuery = new HashMap<>();
            searchQuery.put("query", Map.of("term", Map.of("_id", payload.get("restart_node_id"))));
            Map<String, Object> esResponse = esClient.search(Config.getAppName() + "_node", searchQuery);

            if (esResponse.containsKey("hits") && ((Map<String, Object>) esResponse.get("hits")).containsKey("hits")) {
                List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");
                if (!hits.isEmpty()) {
                    Map<String, Object> source = (Map<String, Object>) hits.get(0).get("_source");
                    if (payload.containsKey("reference_trace_id")) {
                        if (source.get("trace_id").equals(payload.get("reference_trace_id"))) {
                            payload.put("restart_node_order", source.get("update_time"));
                        }
                    } else {
                        payload.put("restart_node_order", source.get("update_time"));
                        payload.put("reference_trace_id", source.get("trace_id"));
                        log.info(String.format("Found restart node {%s}, auto-set trace_id to {%s}", payload.get("restart_node_id"), source.get("trace_id")));
                    }
                }
            } else {
                log.warn(String.format("Restart node {%s} not found in ES", payload.get("restart_node_id")));
            }
        }

        // Set group_id: inherit if from_trace_id is provided, else new
        if (StringUtils.isNotBlank((String) payload.get("from_trace_id")) && StringUtils.isBlank((String) payload.get("group_id"))) {
            Map<String, Object> searchQuery = new HashMap<>();
            searchQuery.put("query", Map.of("term", Map.of("_id", payload.get("from_trace_id"))));
            searchQuery.put("size", 1);
            Map<String, Object> esResponse = esClient.search(Config.getAppName() + "_trace", searchQuery);

            if (esResponse.containsKey("hits") && ((Map<String, Object>) esResponse.get("hits")).containsKey("hits")) {
                List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");
                if (!hits.isEmpty()) {
                    Map<String, Object> source = (Map<String, Object>) hits.get(0).get("_source");
                    payload.put("group_id", source.get("group_id"));
                    payload.putIfAbsent("group_data", source.get("group_data"));
                }
            }
        }

        OxyResponse answer;
        try {
            if (payload.containsKey("current_trace_id")) {
                this.currentTraceId.set(payload.get("current_trace_id").toString());
            }

            OxyRequest oxyRequest = OxyRequest.builder().mas(this).build();

            Set<String> oxyRequestFields = new HashSet<>();
            for (Field field : OxyRequest.class.getDeclaredFields()) {
                oxyRequestFields.add(field.getName());
            }

            // Set payload values to oxyRequest fields
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String k = entry.getKey();
                String kCamel = JsonUtils.toCamelCase(k);
                Object v = entry.getValue();
                if (oxyRequestFields.contains(kCamel)) {
                    try {
                        Field field = oxyRequest.getClass().getDeclaredField(kCamel);
                        field.setAccessible(true);
                        field.set(oxyRequest, v);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    oxyRequest.getArguments().put(k, v);
                }
            }

            // Set default callee if not specified
            if (oxyRequest.getCallee() == null || oxyRequest.getCallee().isEmpty()) {
                oxyRequest.setCallee(masterAgentName);
            }
            answer = oxyRequest.start();

            // Send message to SSE client if sendMsgKey is provided
            if (sendMsgKey != null && !sendMsgKey.isEmpty()) {
                Map<String, Object> message = new HashMap<>();
                message.put("event", "close");
                message.put("data", "done");
                sendMessage(message, sendMsgKey);
            }
        } finally {
            currentTraceId.remove();
        }

        return answer;
    }

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 200;
    private static final int QUEUE_CAPACITY = 1024;
    private static final long KEEP_ALIVE_MS = 60_000;

    private static final ThreadFactory COMMON_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "commonutil-pool-" + index.getAndIncrement());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) ->
                    System.err.println("Uncaught exception in thread " + thread.getName() + ": " + ex)
            );
            return t;
        }
    };

    /**
     * Batch concurrent execution of queries
     *
     * <p>Use CompletableFuture to execute multiple queries concurrently, supporting concurrency control and performance monitoring.</p>
     *
     * @param queries       Query list
     * @param returnTraceId Whether to return trace_id information
     * @return Result list, format depends on returnTraceId parameter
     * @throws Exception Thrown when errors occur during batch processing execution
     */
    public List<Object> startBatchProcessing(List<String> queries, boolean returnTraceId) throws Exception {
        log.info("Starting batch processing of {} queries, returnTraceId={}", queries.size(), returnTraceId);

        // List to record execution times
        List<Long> costTimes = new ArrayList<>();

        // Create thread pool for concurrent execution
        ExecutorService executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_MS, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                COMMON_THREAD_FACTORY,
                new ThreadPoolExecutor.AbortPolicy()
        );

        try {
            // Create CompletableFuture list for concurrent execution
            List<CompletableFuture<Object>> futures = new ArrayList<>();

            for (String query : queries) {
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return handleSingleQuery(query, returnTraceId, costTimes);
                    } catch (Exception e) {
                        log.error("Query processing failed: {}", query, e);
                        // Return error information instead of throwing exception to avoid affecting other queries
                        if (returnTraceId) {
                            Map<String, Object> errorResult = new HashMap<>();
                            errorResult.put("output", "Error: " + e.getMessage());
                            errorResult.put("trace_id", "");
                            return errorResult;
                        } else {
                            return "Error: " + e.getMessage();
                        }
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all tasks to complete and collect results
            List<Object> results = new ArrayList<>();
            for (CompletableFuture<Object> future : futures) {
                results.add(future.get());
            }

            // Calculate average execution time
            if (!costTimes.isEmpty()) {
                double avgTime = costTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                log.info("Batch processing completed. Total queries: {}, Average execution time: {:.2f}ms",
                        queries.size(), avgTime);
            }

            log.info("Batch processing completed");
            return results;

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Internal method for handling single query
     *
     * @param query         Query content
     * @param returnTraceId Whether to return trace_id
     * @param costTimes     Time recording list (thread-safe)
     * @return Query result
     * @throws Exception Processing exception
     */
    private Object handleSingleQuery(String query, boolean returnTraceId, List<Long> costTimes) throws Exception {
        long startTime = System.currentTimeMillis();
        String fromTraceId = "";

        // Generate unique trace_id for each query, ensuring each request in batch processing has different trace_id
        String currentTraceId = CommonUtils.generateShortUUID();

        // Build request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("from_trace_id", fromTraceId);
        payload.put("current_trace_id", currentTraceId);
        payload.put("extra_arg", "value");

        OxyResponse oxyResponse = chatWithAgent(payload);
        fromTraceId = oxyResponse.getOxyRequest().getCurrentTraceId();

        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;

        // Thread-safely add execution time
        synchronized (costTimes) {
            costTimes.add(costTime);
        }

        // Return different format results based on returnTraceId parameter
        if (returnTraceId) {
            Map<String, Object> result = new HashMap<>();
            result.put("output", oxyResponse.getOutput());
            result.put("trace_id", oxyResponse.getOxyRequest().getCurrentTraceId());
            return result;
        } else {
            return oxyResponse.getOutput();
        }
    }

    /**
     * Handle restart node logic
     *
     * @param payload Request payload
     */
    private void handleRestartNode(Map<String, Object> payload) {
        try {
            String restartNodeId = payload.getOrDefault("restart_node_id", "").toString();
            String referenceTraceId = (String) payload.getOrDefault("reference_trace_id", "").toString();

            if (this.esClient instanceof BaseEs) {
                BaseEs es = (BaseEs) this.esClient;

                // Build query conditions
                Map<String, Object> query = new HashMap<>();
                Map<String, Object> termQuery = new HashMap<>();
                termQuery.put("_id", restartNodeId);
                query.put("term", termQuery);

                Map<String, Object> searchQuery = new HashMap<>();
                searchQuery.put("query", query);

                // Search nodes
                Map<String, Object> esResponse = es.search(Config.getAppName() + "_node", searchQuery);

                // Process search results
                if (esResponse != null && esResponse.containsKey("hits")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> hits = (Map<String, Object>) esResponse.get("hits");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");

                    if (!hitsList.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> firstHit = hitsList.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> source = (Map<String, Object>) firstHit.get("_source");

                        if (referenceTraceId.equals(source.get("trace_id"))) {
                            payload.put("restart_node_order", source.get("update_time"));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Failed to handle restart node: " + e.getMessage());
        }
    }

    public Object call(String callee, Map<String, Object> arguments) throws Exception {
        OxyRequest oxyRequest = OxyRequest.builder().mas(this).callee(callee).arguments(arguments).build();
        BaseOxy baseOxy = this.oxyNameToOxy.get(oxyRequest.getCallee());
        OxyResponse response = baseOxy.execute(oxyRequest);
        return response.getOutput();
    }

    /**
     * Check if it's an OxyRequest field
     *
     * @param fieldName Field name
     * @return Whether it's an OxyRequest field
     */
    private boolean isOxyRequestField(String fieldName) {
        // This needs to be implemented according to the actual OxyRequest class
        return Arrays.asList("query", "callee", "trace_id", "from_trace_id",
                "current_trace_id", "attachments").contains(fieldName);
    }

    /**
     * Determine if it's an agent
     *
     * @param oxyName Component name, cannot be null
     * @return Whether it's an agent
     * @throws IllegalArgumentException If oxyName is null
     */
    public boolean isAgent(String oxyName) {
        BaseOxy oxy = getOxyByName(oxyName);
        if (oxy == null) {
            return false;
        }

        // Here need to determine based on actual agent interface
        return (oxy instanceof BaseAgent || oxy instanceof BaseFlow);
    }

    /**
     * Start CLI mode
     *
     * @param firstQuery First query (optional)
     */
    public void startCliMode(String firstQuery) {
        Scanner scanner = new Scanner(System.in);
        String fromTraceId = "";

        try {
            if (firstQuery != null && !firstQuery.trim().isEmpty()) {
                System.out.println("You: " + firstQuery);
                Map<String, Object> payload = new HashMap<>();
                payload.put("query", firstQuery);
                payload.put("from_trace_id", fromTraceId);

                OxyResponse oxyResponse = chatWithAgent(payload);
                System.out.println("LLM: " + oxyResponse.getOutput());
            }

            while (true) {
                System.out.print("Enter your query: ");
                String query = scanner.nextLine().trim();

                if (Arrays.asList("exit", "quit", "bye").contains(query.toLowerCase())) {
                    break;
                }

                if (Arrays.asList("reset", "clear").contains(query.toLowerCase())) {
                    fromTraceId = "";
                    log.info("System: The session has been reset.");
                    continue;
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("query", query);
                payload.put("from_trace_id", fromTraceId);

                OxyResponse oxyResponse = chatWithAgent(payload);
                System.out.println("LLM: " + oxyResponse.getOutput());
            }

        } catch (Exception e) {
            log.error("CLI mode execution failed: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * Display MAS application startup information (refer to show_mas_info method in mas.py)
     */
    public void showMasInfo() {
        log.info("========================================");
        log.info("🚀 OxyGent MAS Application Startup Information");
        log.info("========================================");
        log.info("App Name     : " + Config.getAppName());
        log.info("Version      : " + Config.getApp().getVersion());
        log.info("Environment  : " + Config.getConfigFileEnv());
        if (serverProperties != null) {
            log.info("Port         : " + serverProperties.getPort());
        }
        log.info("Java Ver     : " + System.getProperty("java.version"));
        log.info("Cache Dir    : " + Config.getXfile().getSaveDir());
        log.info("Start Time   : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("========================================");
    }

    /**
     * Filter out some redundant fields before saving to ES
     *
     * @param _copy
     */
    public static void removeAbandonedFields(Map _copy) {
        Map _content = _copy;
        if (_copy.get("content") instanceof Map contentMap) {
            _content = contentMap;
        }
        if (_content.get("arguments") instanceof Map argumentsMap) {
            Map _arguments = argumentsMap;
            _arguments.remove("agent_config");
            _arguments.remove("messages");
        }
        if (_content.get("shared_data") instanceof Map shareddataMap) {
            Map _sharedData = shareddataMap;
            _sharedData.remove("_headers");
            Object requestId = JsonUtils.firstNotBlank(_content.get("request_id"), _content.get("req_id"), _content.get("requestId"));
            if (!"tool_call".equals(_copy.get("type")) || requestId == null || !Mas.firstQuerySet.contains(requestId)) {
                _sharedData.remove("files");
                _sharedData.remove("first_query_struct");
            }
        }
    }
}
