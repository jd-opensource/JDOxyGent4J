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
package com.jd.oxygent.core.oxygent.samples.examples.backend;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo class for message saving and sending functionality
 *
 * <p>This class demonstrates how to implement message saving and sending control functionality in Java:</p>
 * <ul>
 *   <li><strong>Message Configuration</strong>: Control message display and storage behavior in terminal</li>
 *   <li><strong>Message Send Control</strong>: Control message behavior through _is_stored and _is_send parameters</li>
 *   <li><strong>Input Processing Function</strong>: Send various types of test messages before agent processing</li>
 *   <li><strong>Message Type Testing</strong>: Demonstrate 4 different message storage and sending combinations</li>
 * </ul>
 *
 * <h3>Message Control Parameters</h3>
 * <ul>
 *   <li><strong>_is_stored</strong>: Controls whether messages are stored in persistent storage</li>
 *   <li><strong>_is_send</strong>: Controls whether messages are sent to clients or other components</li>
 * </ul>
 *
 * <h3>Test Scenarios</h3>
 * <table border="1">
 *   <tr><th>Type</th><th>_is_stored</th><th>_is_send</th><th>Behavior Description</th></tr>
 *   <tr><td>test1</td><td>false</td><td>false</td><td>Neither stored nor sent (temporary message)</td></tr>
 *   <tr><td>test2</td><td>false</td><td>true</td><td>Not stored but sent (real-time notification)</td></tr>
 *   <tr><td>test3</td><td>true</td><td>false</td><td>Stored but not sent (log recording)</td></tr>
 *   <tr><td>test4</td><td>true</td><td>true</td><td>Both stored and sent (complete message)</td></tr>
 * </table>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoSaveMessage {

    /**
     * Initialize message configuration
     *
     * <p>Configure message system behavior:</p>
     * <ul>
     *   <li><strong>setShowInTerminal(true)</strong>: Enable message display in terminal</li>
     *   <li><strong>setStored(true)</strong>: Enable message storage functionality</li>
     * </ul>
     *
     * <p>These configurations affect message processing throughout the MAS system.</p>
     */
    private static void initializeConfig() {
        try {
            log.info("Starting message configuration initialization");

            // Enable message display in terminal
            Config.getMessage().setShowInTerminal(true);

            // Enable message storage functionality
            Config.getMessage().setStored(true);

            log.info("Message configuration initialization completed - Terminal display: enabled, Message storage: enabled");

        } catch (Exception e) {
            log.error("Message configuration initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Message configuration initialization failed", e);
        }
    }

    /**
     * Get default OxySpace configuration
     *
     * <p>Configuration includes:</p>
     * <ul>
     *   <li><strong>HttpLlm</strong>: HTTP LLM service configuration</li>
     *   <li><strong>ChatAgent</strong>: Chat agent configured with input processing function</li>
     * </ul>
     *
     * <p>Note: ChatAgent uses funcProcessInput to process input,
     * allowing test messages to be sent before agent processes requests.</p>
     *
     * @return List of BaseOxy containing LLM and ChatAgent
     */
    @OxySpaceBean(value = "saveMessageJavaOxySpace", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Initialize message configuration
        initializeConfig();

        // Apply JDK17's var keyword to simplify local variable declarations
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                // 1. HTTP LLM Configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(30)
                        .build(),

                // 2. Chat agent configuration, including input processing function
                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .funcProcessInput(DemoSaveMessage::updateQuery)  // Set input processing function
                        .build()
        );
    }

    /**
     * Input processing function - demonstrate different types of message sending
     *
     * <p>This function is called before agent processes requests, used to send 4 different configured test messages:</p>
     *
     * <h4>Test Message Types:</h4>
     * <ol>
     *   <li><strong>test1</strong>: _is_stored=false, _is_send=false
     *       <br>Neither stored nor sent, used for temporary debug information</li>
     *   <li><strong>test2</strong>: _is_stored=false, _is_send=true
     *       <br>Not stored but sent, used for real-time notifications or temporary status updates</li>
     *   <li><strong>test3</strong>: _is_stored=true, _is_send=false
     *       <br>Stored but not sent, used for log recording or auditing</li>
     *   <li><strong>test4</strong>: _is_stored=true, _is_send=true
     *       <br>Both stored and sent, used for important business messages</li>
     * </ol>
     *
     * <p>Through these 4 combinations, you can test message system behavior in different scenarios.</p>
     *
     * @param oxyRequest Input OxyRequest object
     * @return Processed OxyRequest object (returned as-is)
     */
    public static OxyRequest updateQuery(OxyRequest oxyRequest) {
        log.info("=== Starting test message sending ===");

        // Create message template
        Map<String, Object> msg = new HashMap<>();

        // Test 1: Neither stored nor sent (temporary message)
        log.info("Sending test1 message - neither stored nor sent");
        msg.put("type", "test1");
        msg.put("content", "test1");
        msg.put("_is_stored", false);  // Not stored
        msg.put("_is_send", false);    // Not sent
        oxyRequest.sendMessage(msg);

        // Test 2: Not stored but sent (real-time notification)
        log.info("Sending test2 message - not stored but sent");
        msg.put("type", "test2");
        msg.put("content", "test2");
        msg.put("_is_stored", false);  // Not stored
        msg.put("_is_send", true);     // Sent
        oxyRequest.sendMessage(msg);

        // Test 3: Stored but not sent (log recording)
        log.info("Sending test3 message - stored but not sent");
        msg.put("type", "test3");
        msg.put("content", "test3");
        msg.put("_is_stored", true);   // Stored
        msg.put("_is_send", false);    // Not sent
        oxyRequest.sendMessage(msg);

        // Test 4: Both stored and sent (complete message)
        log.info("Sending test4 message - both stored and sent");
        msg.put("type", "test4");
        msg.put("content", "test4");
        msg.put("_is_stored", true);   // Stored
        msg.put("_is_send", true);     // Sent
        oxyRequest.sendMessage(msg);

        log.info("=== Test message sending completed ===");
        return oxyRequest;
    }

    /**
     * Application main entry point
     *
     * <p>Start Spring Boot application and register OxySpace configuration.</p>
     *
     * <p>After application startup, when receiving requests it will:</p>
     * <ol>
     *   <li>Call updateQuery function to send 4 types of test messages</li>
     *   <li>Then have ChatAgent process actual user queries</li>
     *   <li>Observe processing behavior of different message types</li>
     * </ol>
     *
     * <p><strong>Usage Recommendations</strong>:</p>
     * <ul>
     *   <li>After starting application, send query "hello" to trigger testing</li>
     *   <li>Observe log output to understand message processing flow</li>
     *   <li>Check actual effects of message storage and sending</li>
     * </ul>
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        // Get current class name and register OxySpace configuration
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
                Thread.currentThread().getStackTrace()[1].getClassName());

        log.info("DemoSaveMessage starting...");
        log.info("Feature descriptions:");
        log.info("1. Demonstrate message storage and sending control");
        log.info("2. Test 4 different message processing combinations");
        log.info("3. Show usage of input processing functions");
        log.info("4. Message configuration: Terminal display=enabled, Message storage=enabled");

        // Start Spring Boot application
        ServerApp.main(args);

        log.info("Application startup completed, send query 'hello' to test message functionality");
    }
}
