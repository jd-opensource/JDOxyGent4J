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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo class for adding router functionality
 *
 * <p>This class ports functionality from the Python version, demonstrating how to implement in Java:</p>
 * <ul>
 *   <li><strong>HTTP LLM Configuration</strong>: Configure large language model services</li>
 *   <li><strong>Workflow Agent</strong>: Implement custom business processes</li>
 *   <li><strong>HTTP Client Calls</strong>: Call REST APIs to retrieve data</li>
 *   <li><strong>Spring Boot Integration</strong>: Seamless integration with Spring Boot framework</li>
 * </ul>
 *
 * <h3>Python Version Correspondence</h3>
 * <pre>{@code
 * # Python version
 * async def workflow(oxy_request: OxyRequest):
 *     async with httpx.AsyncClient() as client:
 *         response = await client.get(url="http://127.0.0.1:8080/api_name")
 *         return response.json()
 *
 * oxy_space = [
 *     oxy.HttpLLM(...),
 *     oxy.WorkflowAgent(name="master_agent", llm_model="default_llm", func_workflow=workflow)
 * ]
 * }</pre>
 *
 * <h3>Java Version Implementation</h3>
 * <ul>
 *   <li><strong>HttpClient</strong>: Uses Java 11's HttpClient to replace Python's httpx</li>
 *   <li><strong>Function Interface</strong>: Uses Function&lt;OxyRequest, Object&gt; to replace Python's async function</li>
 *   <li><strong>WorkflowAgent</strong>: Directly uses Java version of WorkflowAgent</li>
 *   <li><strong>Environment Variables</strong>: Supports configuring LLM parameters through environment variables</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoAddRouter {

    /**
     * HTTP client instance
     * Uses Java 11's HttpClient, corresponding to Python version's httpx.AsyncClient
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Workflow function implementation
     *
     * <p>Corresponds to Python version's workflow function:</p>
     * <pre>{@code
     * async def workflow(oxy_request: OxyRequest):
     *     async with httpx.AsyncClient() as client:
     *         response = await client.get(url="http://127.0.0.1:8080/api_name")
     *         return response.json()
     * }</pre>
     *
     * <p>This function performs the following steps:</p>
     * <ol>
     *   <li>Create HTTP request to local API endpoint</li>
     *   <li>Send GET request to retrieve data</li>
     *   <li>Parse JSON response and return result</li>
     * </ol>
     *
     * @param oxyRequest Workflow request object containing input parameters and context information
     * @return Result data from API call
     */
    private static final Function<OxyRequest, Object> WORKFLOW_FUNCTION = oxyRequest -> {
        try {
            log.info("Starting workflow execution, calling API endpoint: /api_name");

            // Build HTTP request, corresponding to Python version's client.get(url="http://127.0.0.1:8080/api_name")
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8888/get_organization"))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // Send request and get response
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("API call successful, status code: {}", response.statusCode());

            if (response.statusCode() == 200) {
                // Parse JSON response, corresponding to Python version's response.json()
                var responseBody = response.body();
                log.info("API response content: {}", responseBody);

                // Use FastJSON to parse response
                var jsonResult = JsonUtils.toJSONString(responseBody);
                log.info("Workflow execution successful, returning result: {}", jsonResult);

                return jsonResult;
            } else {
                var errorMessage = "API call failed, status code: " + response.statusCode();
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }

        } catch (Exception e) {
            log.error("Workflow execution exception: {}", e.getMessage(), e);
            throw new RuntimeException("Workflow execution failed: " + e.getMessage(), e);
        }
    };

    /**
     * Get default OxySpace configuration
     *
     * <p>Corresponds to Python version's oxy_space configuration:</p>
     * <pre>{@code
     * oxy_space = [
     *     oxy.HttpLLM(
     *         name="default_llm",
     *         api_key=os.getenv("OXY_LLM_API_KEY"),
     *         base_url=os.getenv("OXY_LLM_BASE_URL"),
     *         model_name=os.getenv("OXY_LLM_MODEL_NAME"),
     *     ),
     *     oxy.WorkflowAgent(
     *         name="master_agent",
     *         llm_model="default_llm",
     *         func_workflow=workflow,
     *     ),
     * ]
     * }</pre>
     *
     * @return List of BaseOxy containing LLM and WorkflowAgent
     * @throws IllegalArgumentException When configuration parameters are invalid
     */
    @OxySpaceBean(value = "addRouterJavaOxySpace", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                // 1. HTTP LLM Configuration - corresponds to Python version's oxy.HttpLLM
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .llmParams(Map.of("temperature", 0.01))
                        .timeout(30)
                        .build(),

                // 2. Workflow Agent - corresponds to Python version's oxy.WorkflowAgent
                WorkflowAgent.builder()
                        .isMaster(true)
                        .name("master_agent")
                        .llmModel("default_llm")
                        .funcWorkflow(WORKFLOW_FUNCTION)
                        .build()
        );
    }

    /**
     * Application main entry point
     *
     * <p>Corresponds to Python version's main function:</p>
     * <pre>{@code
     * async def main():
     *     async with MAS(oxy_space=oxy_space, routers=[router]) as mas:
     *         await mas.start_web_service(first_query="hello")
     *
     * if __name__ == "__main__":
     *     asyncio.run(main())
     * }</pre>
     *
     * <p>Java version starts web service through Spring Boot framework, automatically registering API routes.</p>
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        // Get current class name and register OxySpace
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);

        // Start Spring Boot application, this will automatically register routes in DemoApiController
        ServerApp.main(args);

        log.info("Application startup completed, you can test using the following methods:");
        log.info("1. Visit http://localhost:8080/api_name to test API");
        log.info("2. Chat with master_agent, it will call the above API");
    }
}
