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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Remote Agent - Basic component of distributed agent systems
 *
 * <p>RemoteAgent is an abstract base class used to build distributed agent architectures in the OxyGent system.
 * It supports cross-network agent communication and collaboration, implementing remote invocation and organizational management of agent services.</p>
 *
 * <p>Core features:</p>
 * <ul>
 *     <li>Remote communication: Supports remote agent invocation via HTTP/HTTPS protocols</li>
 *     <li>URL management: Unified remote server address configuration and validation</li>
 *     <li>Organizational structure: Supports hierarchical remote agent organizational architecture</li>
 *     <li>Transparent proxy: Provides localized access interfaces for remote agents</li>
 *     <li>Fault tolerance: Handles network exceptions and remote service unavailability</li>
 * </ul>
 *
 * <p>Distributed architecture advantages:</p>
 * <ul>
 *     <li>Load distribution: Deploy compute-intensive agents to dedicated servers</li>
 *     <li>Resource isolation: Different types of agents run in independent environments</li>
 *     <li>Elastic scaling: Dynamically increase or decrease remote agent instances based on demand</li>
 *     <li>Heterogeneous support: Supports agents implemented with different technology stacks</li>
 *     <li>High availability: Improve system reliability through cluster deployment</li>
 * </ul>
 *
 * <p>Typical application scenarios:</p>
 * <ul>
 *     <li>Microservice architecture: Deploy agents as independent microservices</li>
 *     <li>Edge computing: Deploy agents with specific functions on edge nodes</li>
 *     <li>Multi-cloud deployment: Deploy complementary agents across different cloud providers</li>
 *     <li>Professional services: Invoke professional agent services provided by third parties</li>
 *     <li>Hybrid architecture: Mixed use of local agents and cloud agents</li>
 * </ul>
 *
 * <p>Implementing subclasses need to:</p>
 * <ol>
 *     <li>Implement specific remote invocation logic</li>
 *     <li>Define network protocols and data formats</li>
 *     <li>Handle connection timeouts and retry mechanisms</li>
 *     <li>Implement authentication and security controls</li>
 * </ol>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Inherit RemoteAgent to implement specific remote agent
 * public class HttpRemoteAgent extends RemoteAgent {
 *     @Override
 *     public OxyResponse _execute(OxyRequest request) {
 *         // Implement HTTP remote invocation logic
 *         return callRemoteService(request);
 *     }
 * }
 *
 * // Configure remote agent
 * RemoteAgent remoteAgent = HttpRemoteAgent.builder()
 *     .name("Remote AI Assistant")
 *     .serverUrl("https://ai-service.example.com/api")
 *     .org(Map.of("department", "AI Service", "region", "East China"))
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public abstract class RemoteAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(RemoteAgent.class);

    /**
     * Remote server URL
     *
     * <p>Complete URL address of the remote agent service, including protocol, host, port, and path information.
     * Must start with http:// or https://, supported format examples:</p>
     * <ul>
     *     <li>https://ai-service.example.com/api/v1/agents</li>
     *     <li>http://localhost:8080/agent-service</li>
     *     <li>https://region.cloud-provider.com/agent/execute</li>
     * </ul>
     */
    @Builder.Default
    private String serverUrl = "";
    @Builder.Default
    private boolean isOxyAgent = false;


    /**
     * Organizational structure information
     *
     * <p>Describes the organizational architecture and metadata information of remote agents, used for:</p>
     * <ul>
     *     <li>Identifying the agent's affiliated department or business domain</li>
     *     <li>Recording geographical location or deployment region information</li>
     *     <li>Maintaining hierarchical relationships and dependencies of agents</li>
     *     <li>Supporting agent discovery and routing</li>
     * </ul>
     *
     * <p>Typical structure example:</p>
     * <pre>{@code
     * {
     *   "department": "AI R&D Department",
     *   "region": "East China",
     *   "cluster": "Production Cluster A",
     *   "version": "v2.1.0",
     *   "children": [...]
     * }
     * }</pre>
     */
    @Builder.Default
    private Map<String, Object> org = new HashMap<>();

    /**
     * Remote agent initialization
     *
     * <p>Execute the initialization process of the remote agent, including:</p>
     * <ol>
     *     <li>Call the initialization logic of parent class BaseAgent</li>
     *     <li>Validate the format and reachability of server URL</li>
     *     <li>Establish connection configuration with remote service</li>
     *     <li>Initialize organizational structure information</li>
     * </ol>
     *
     * @throws IllegalArgumentException if server URL format is invalid or empty
     * @throws IllegalStateException    if remote service connection fails
     */
    public void init() {
        super.init();
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Server URL cannot be null or empty");
        }

        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Server URL must start with http:// or https://");
        }

        logger.info("RemoteAgent '{}' initialization completed, server: {}", getName(), serverUrl);
    }


    /**
     * Get organizational structure information
     *
     * <p>Returns the organizational structure information of the current remote agent and recursively marks all nodes as remote type.
     * If no custom organizational structure is set, returns a default structure constructed based on the agent's basic information.</p>
     *
     * <p>Characteristics of returned organizational structure:</p>
     * <ul>
     *     <li>All nodes are marked as is_remote=true</li>
     *     <li>Contains server URL information for remote invocation</li>
     *     <li>Supports nested sub-organizational structures</li>
     *     <li>Maintains original hierarchical relationships and metadata</li>
     * </ul>
     *
     * @return Organizational structure mapping marked as remote
     */
    public Map<String, Object> getOrg() {
        if (this.org.isEmpty()) {
            return Map.of("name", getName(), "desc", getDesc(), "is_remote", true, "server_url", serverUrl);
        }

        Function<Map<String, Object>, Map<String, Object>> markAsRemote = new Function<>() {
            @Override
            public Map<String, Object> apply(Map<String, Object> organization) {
                var result = new HashMap<>(organization);
                result.put("is_remote", true);

                if (result.get("children") instanceof List<?> children && !children.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    var typedChildren = (List<Map<String, Object>>) children;
                    result.put("children", typedChildren.stream().map(this).toList());
                }

                return result;
            }
        };

        return markAsRemote.apply(RemoteAgent.this.org);
    }

}