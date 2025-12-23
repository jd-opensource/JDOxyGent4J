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

import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseFlow;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.logging.Logger;

/**
 * Base class for all agents in the OxyGent system
 *
 * <p>This class inherits from BaseFlow class and provides common functionality and infrastructure for agent implementation.
 * As the parent class of all agents, it uniformly manages the agent lifecycle, including request preprocessing,
 * data persistence, tracking management, and other core functions.</p>
 *
 * <p>Main features include:</p>
 * <ul>
 *     <li>Permission verification and access control</li>
 *     <li>Request trace chain management (trace management)</li>
 *     <li>Elasticsearch data persistence operations</li>
 *     <li>User session history management</li>
 *     <li>Root trace ID hierarchical structure management</li>
 *     <li>Agent category identification management</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * public class MyAgent extends BaseAgent {
 *     @Override
 *     public OxyResponse execute(OxyRequest request) {
 *         // Implement specific agent logic
 *         return super.execute(request);
 *     }
 * }
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseAgent extends BaseFlow {

    private static final Logger logger = Logger.getLogger(BaseAgent.class.getName());

    /**
     * Agent category identifier, used to distinguish different types of agents
     * Default value is "agent", subclasses can override this field as needed
     */
    @JsonProperty("category")
    @Builder.Default
    protected String category = "agent";

    /**
     * Input data structure schema definition
     * Used to validate and describe the expected input parameter format for the agent
     */
    @JsonProperty("input_schema")
    protected Map<String, Object> inputSchema;

    /**
     * Preprocess request
     *
     * <p>This method handles trace management and root trace ID setting for user requests. Main functions include:</p>
     * <ul>
     *     <li>Call parent class preprocessing logic</li>
     *     <li>Check if the request comes from a user and contains a source trace ID</li>
     *     <li>Retrieve parent trace information from Elasticsearch</li>
     *     <li>Build and maintain trace ID hierarchical structure</li>
     * </ul>
     *
     * @param oxyRequest Request object to be preprocessed, cannot be null
     * @return Preprocessed request object containing complete trace information
     * @throws IllegalArgumentException if oxyRequest is null
     */
    @Override
    protected OxyRequest preProcess(OxyRequest oxyRequest) {
        super.preProcess(oxyRequest);
        if ("user".equals(oxyRequest.getCallerCategory()) && CollectionUtils.isEmpty(oxyRequest.getRootTraceIds())) {
            if (oxyRequest.getFromTraceId() != null && !oxyRequest.getFromTraceId().isEmpty()) {
                Map<String, Object> response = queryParentTraceSync(oxyRequest);
                if (response != null && response.containsKey("hits")) {
                    Map<String, Object> hits = (Map<String, Object>) response.get("hits");
                    List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");
                    if (!hitsList.isEmpty()) {
                        Map<String, Object> source = (Map<String, Object>) hitsList.get(0).get("_source");
                        var rootTraceIds = source.get("root_trace_ids");
                        if (rootTraceIds instanceof String && StringUtils.isNotEmpty((String) rootTraceIds)) {
                            oxyRequest.setRootTraceIds(new ArrayList<>(Arrays.asList(((String) rootTraceIds).split("\\|"))));
                        } else {
                            oxyRequest.setRootTraceIds(new ArrayList<>());
                        }
                    } else {
                        oxyRequest.setRootTraceIds(new ArrayList<>());
                    }
                } else {
                    oxyRequest.setRootTraceIds(new ArrayList<>());
                }
                oxyRequest.getRootTraceIds().add(oxyRequest.getFromTraceId());
            }
        } else if ("user".equals(oxyRequest.getCallerCategory()) && (!CollectionUtils.isEmpty(oxyRequest.getRootTraceIds()))) {
            oxyRequest.getRootTraceIds().add(oxyRequest.getFromTraceId());
        }

        return oxyRequest;
    }

    /**
     * Save initial trace data before processing request
     *
     * <p>This method creates initial trace records for user requests and persists request information to Elasticsearch.
     * This provides basic data for subsequent request tracking and troubleshooting.</p>
     *
     * <p>Saved data includes:</p>
     * <ul>
     *     <li>Request ID and trace ID</li>
     *     <li>Shared data and group information</li>
     *     <li>Root trace ID hierarchical structure</li>
     *     <li>Input parameters (JSON format)</li>
     *     <li>Callee information and creation time</li>
     * </ul>
     *
     * @param oxyRequest Request object to save trace data for, cannot be null
     * @throws IllegalArgumentException if oxyRequest is null
     */
    protected void preSaveData(OxyRequest oxyRequest) {
        super.preSaveData(oxyRequest);
        if ("user".equals(oxyRequest.getCallerCategory())) {
            if (getMas() != null) {
                Map<String, Object> traceData = new HashMap<>();
                traceData.put("request_id", oxyRequest.getRequestId());
                traceData.put("trace_id", oxyRequest.getCurrentTraceId());
                traceData.put("shared_data", oxyRequest.getSharedData());
                traceData.put("group_id", oxyRequest.getGroupId());
                traceData.put("group_data", oxyRequest.getGroupData());
                traceData.put("from_trace_id", oxyRequest.getFromTraceId());
                traceData.put("root_trace_ids", String.join("|", oxyRequest.getRootTraceIds()));
                traceData.put("input", JsonUtils.toJSONString(oxyRequest.getArguments()));
                traceData.put("callee", oxyRequest.getCallee());
                traceData.put("output", "");
                traceData.put("create_time", CommonUtils.getFormatTime());
                this.getMas().getEsClient().index(Config.getAppName() + "_trace", oxyRequest.getCurrentTraceId(), traceData);
            } else {
                logger.warning("Save " + oxyRequest.getCallee() + " pre trace data error");
            }
        }
    }

    /**
     * Save complete trace and history data after request processing is complete
     *
     * <p>This method updates trace records, adds response output information, and saves session history records as needed.
     * This is the cleanup work after agent execution completion, ensuring all relevant data is correctly persisted.</p>
     *
     * <p>Main functions:</p>
     * <ul>
     *     <li>Update trace records and add output results</li>
     *     <li>Save conversation history of user sessions</li>
     *     <li>Record complete request-response pairs</li>
     *     <li>Maintain session context information</li>
     * </ul>
     *
     * @param oxyResponse Response object containing processing results, cannot be null
     * @throws IllegalArgumentException if oxyResponse is null or its contained request is null
     */
    public void postSaveData(OxyResponse oxyResponse) {
        super.postSaveData(oxyResponse);
        OxyRequest oxyRequest = oxyResponse.getOxyRequest();
        if ("user".equals(oxyRequest.getCallerCategory())) {
            if (getMas() != null) {
                Map<String, Object> traceData = new HashMap<>();
                traceData.put("request_id", oxyRequest.getRequestId());
                traceData.put("trace_id", oxyRequest.getCurrentTraceId());
                traceData.put("shared_data", oxyRequest.getSharedData());
                traceData.put("group_id", oxyRequest.getGroupId());
                traceData.put("group_data", oxyRequest.getGroupData());
                traceData.put("from_trace_id", oxyRequest.getFromTraceId());
                traceData.put("root_trace_ids", String.join("|", oxyRequest.getRootTraceIds()));
                traceData.put("input", JsonUtils.toJSONString(oxyRequest.getArguments()));
                traceData.put("callee", oxyRequest.getCallee());
                traceData.put("output", JsonUtils.toJSONString(oxyResponse.getOutput()));
                traceData.put("create_time", CommonUtils.getFormatTime());
                this.getMas().getEsClient().index(Config.getAppName() + "_trace", oxyRequest.getCurrentTraceId(), traceData);
            } else {
                logger.warning("Save " + oxyRequest.getCallee() + " post trace data error");
            }
        }
        if (oxyRequest.isSaveHistory()) {
            if (getMas() != null) {
                String currentSubSessionId = oxyRequest.getCurrentTraceId() + "__" + oxyRequest.getSessionName();
                Map<String, Object> history = new HashMap<>();
                history.put("query", oxyRequest.getQuery());
                history.put("answer", oxyResponse.getOutput());
                if (oxyResponse.getExtra() != null) {
                    history.putAll(oxyResponse.getExtra());
                }
                Map<String, Object> historyData = new HashMap<>();
                historyData.put("history_id", currentSubSessionId);
                historyData.put("session_name", oxyRequest.getSessionName());
                historyData.put("trace_id", oxyRequest.getCurrentTraceId());
                historyData.put("memory", JsonUtils.toJSONString(history));
                historyData.put("create_time", CommonUtils.getFormatTime());

                this.getMas().getEsClient().index(Config.getAppName() + "_history", currentSubSessionId, historyData);
            } else {
                logger.warning("Save " + oxyRequest.getCallee() + " history data error");
            }
        }
    }


    /**
     * Synchronously query parent trace information
     *
     * <p>Retrieve parent trace records for the specified trace ID from Elasticsearch, used to build trace chains.</p>
     *
     * @param oxyRequest Request object containing source trace ID
     * @return Elasticsearch query results containing parent trace information; returns null if query fails
     */
    private Map<String, Object> queryParentTraceSync(OxyRequest oxyRequest) {
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> term = new HashMap<>();
        term.put("_id", oxyRequest.getFromTraceId());
        query.put("term", term);
        Map<String, Object> searchQuery = new HashMap<>();
        searchQuery.put("query", query);
        return this.getMas().getEsClient().search(Config.getAppName() + "_trace", searchQuery);
    }

}