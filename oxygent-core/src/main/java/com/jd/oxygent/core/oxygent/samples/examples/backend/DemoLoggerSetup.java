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
import java.util.List;

/**
 * Demo class for logging and setup functionality
 *
 * <p>This class demonstrates how to implement logging and query processing functionality in Java:</p>
 * <ul>
 *   <li><strong>Logging</strong>: Use SLF4J and Lombok's @Slf4j annotation for logging</li>
 *   <li><strong>Query Logging</strong>: Record user query content in input processing functions</li>
 *   <li><strong>Input Processing</strong>: Demonstrate how to log and process input before agent processing</li>
 *   <li><strong>Log Formatting</strong>: Show structured log output format</li>
 * </ul>
 *
 * <h3>Logging Features</h3>
 * <ul>
 *   <li><strong>Structured Output</strong>: Use specific format to record query content</li>
 *   <li><strong>Request Tracking</strong>: Record detailed information for each user request</li>
 *   <li><strong>Debug Support</strong>: Facilitate viewing request flow during development and debugging</li>
 *   <li><strong>Audit Function</strong>: Can be used for system auditing and monitoring</li>
 * </ul>
 *
 * <h3>Application Scenarios</h3>
 * <ul>
 *   <li><strong>Development Debugging</strong>: Track request processing flow during development</li>
 *   <li><strong>System Monitoring</strong>: Monitor user query patterns and system usage</li>
 *   <li><strong>Issue Troubleshooting</strong>: Quickly locate user input when problems occur</li>
 *   <li><strong>Performance Analysis</strong>: Analyze processing of different query types</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoLoggerSetup {

    /**
     * Get default OxySpace configuration
     *
     * <p>Configuration includes:</p>
     * <ul>
     *   <li><strong>HttpLlm</strong>: HTTP LLM service configuration using standard connection parameters</li>
     *   <li><strong>ChatAgent</strong>: Chat agent configured with input processing function for logging</li>
     * </ul>
     *
     * <p>Note: ChatAgent uses funcProcessInput to process input,
     * allowing query content to be logged before agent processes the request.</p>
     *
     * <p>Uses JDK17's var keyword to simplify local variable declarations, improving code readability.</p>
     *
     * @return List of BaseOxy containing LLM and ChatAgent
     */
    @OxySpaceBean(value = "loggerSetupJavaOxySpace", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Apply JDK17's var keyword to simplify local variable declarations
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                // 1. HTTP LLM Configuration - standard large language model service configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(30)  // 30 second timeout setting
                        .build(),

                // 2. Chat agent configuration, including input processing function for logging
                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .funcProcessInput(DemoLoggerSetup::updateQuery)  // Set input processing function for logging
                        .build()
        );
    }

    /**
     * Input processing function - log user queries to log
     *
     * <p>This function is called before ChatAgent processes user requests, main functions:</p>
     * <ul>
     *   <li><strong>Query Extraction</strong>: Extract user query content from OxyRequest</li>
     *   <li><strong>Log Recording</strong>: Use INFO level to record query content</li>
     *   <li><strong>Formatted Output</strong>: Use specific format "{ query }" for output</li>
     *   <li><strong>Request Passing</strong>: Return OxyRequest as-is to continue processing flow</li>
     * </ul>
     *
     * <h4>Log Format Description:</h4>
     * <p>Log output format: <code>The current query is: { user query content }</code></p>
     * <p>This format facilitates:</p>
     * <ul>
     *   <li>Log parsing and filtering</li>
     *   <li>Automated monitoring and alerting</li>
     *   <li>Quick identification of query content</li>
     *   <li>Distinction from other system logs</li>
     * </ul>
     *
     * <h4>Usage Scenarios:</h4>
     * <ul>
     *   <li><strong>Debug Mode</strong>: View user input content during development</li>
     *   <li><strong>Monitor Mode</strong>: Monitor user query patterns in production environment</li>
     *   <li><strong>Audit Mode</strong>: Record all user interactions for compliance auditing</li>
     *   <li><strong>Analysis Mode</strong>: Collect data for user behavior analysis</li>
     * </ul>
     *
     * @param oxyRequest Request object containing user query
     * @return OxyRequest object returned as-is for subsequent processing flow to continue
     */
    public static OxyRequest updateQuery(OxyRequest oxyRequest) {
        // Extract user query content from request object
        String query = oxyRequest.getQuery();

        // Use INFO level to record query content, using structured format for subsequent processing
        log.info("The current query is: { " + query + " }");

        // Return request object as-is to ensure subsequent processing flow continues normally
        return oxyRequest;
    }

    /**
     * Application main entry point
     *
     * <p>Start Spring Boot application and register OxySpace configuration.</p>
     *
     * <p>Workflow after application startup:</p>
     * <ol>
     *   <li><strong>Receive Request</strong>: System receives user query requests</li>
     *   <li><strong>Log Recording</strong>: updateQuery function logs query content to log</li>
     *   <li><strong>Agent Processing</strong>: ChatAgent processes user query and generates response</li>
     *   <li><strong>Return Result</strong>: Return processing result to user</li>
     * </ol>
     *
     * <p><strong>Log Viewing Recommendations</strong>:</p>
     * <ul>
     *   <li>After starting the application, check console output for log information</li>
     *   <li>Send different query content and observe log recording format</li>
     *   <li>Can configure log levels and output targets (files, databases, etc.)</li>
     *   <li>Recommend configuring log rotation and archiving strategies in production environment</li>
     * </ul>
     *
     * <p><strong>Extension Suggestions</strong>:</p>
     * <ul>
     *   <li>Add request ID for tracking complete request lifecycle</li>
     *   <li>Record processing time for performance monitoring</li>
     *   <li>Add user information for personalized analysis</li>
     *   <li>Integrate external logging systems (like ELK Stack) for advanced analysis</li>
     * </ul>
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        // Get current class name and register OxySpace configuration
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
                Thread.currentThread().getStackTrace()[1].getClassName());

        log.info("DemoLoggerSetup starting...");
        log.info("Feature descriptions:");
        log.info("1. Demonstrate logging and query processing functionality");
        log.info("2. Show log recording in input processing functions");
        log.info("3. Provide structured query log format");
        log.info("4. Support system monitoring and debugging needs");

        // Start Spring Boot application
        ServerApp.main(args);

        log.info("Application startup completed, send queries to observe logging effects");
        log.info("Log format: The current query is: { Your query content }");
    }
}
