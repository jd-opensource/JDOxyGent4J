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

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Demo class for batch processing and semaphore limiting functionality
 *
 * <p>This class ports functionality from the Python version, demonstrating how to implement in Java:</p>
 * <ul>
 *   <li><strong>Semaphore Control</strong>: Limit the concurrency count of LLM and Agent</li>
 *   <li><strong>Batch Processing</strong>: Process multiple requests in batches</li>
 *   <li><strong>Concurrency Control</strong>: Control concurrent execution through semaphore parameters</li>
 *   <li><strong>Spring Boot Integration</strong>: Seamless integration with Spring Boot framework</li>
 * </ul>
 *
 * <h3>Python Version Correspondence</h3>
 * <pre>{@code
 * # Python version
 * oxy_space = [
 *     oxy.HttpLLM(
 *         name="default_llm",
 *         api_key=os.getenv("OXY_LLM_API_KEY"),
 *         base_url=os.getenv("OXY_LLM_BASE_URL"),
 *         model_name=os.getenv("OXY_LLM_MODEL_NAME"),
 *         semaphore=4,  # limit concurrency to 4
 *     ),
 *     oxy.ChatAgent(
 *         name="chat_agent",
 *         llm_model="default_llm",
 *         semaphore=6,  # limit concurrency to 6
 *     ),
 * ]
 *
 * outs = await mas.start_batch_processing(["hello"] * 10, return_trace_id=True)
 * }</pre>
 *
 * <h3>Java Version Implementation</h3>
 * <ul>
 *   <li><strong>semaphoreCount()</strong>: Use semaphore method to set concurrency limits</li>
 *   <li><strong>startBatchProcessing()</strong>: Batch processing method corresponding to Python version</li>
 *   <li><strong>Collections.nCopies()</strong>: Generate repeated element lists</li>
 *   <li><strong>Stream API</strong>: Process batch processing results</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoBatchAndSemaphore {

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
     *         semaphore=4,  # limit concurrency to 4
     *     ),
     *     oxy.ChatAgent(
     *         name="chat_agent",
     *         llm_model="default_llm",
     *         semaphore=6,  # limit concurrency to 6
     *     ),
     * ]
     * }</pre>
     *
     * @return List of BaseOxy containing LLM and ChatAgent with semaphore limits configured
     */
    @OxySpaceBean(value = "batchAndSemaphoreJavaOxySpace", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. HTTP LLM Configuration - corresponds to Python version's oxy.HttpLLM, with concurrency limits
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .semaphoreCount(2)
                        .timeout(30)
                        .build(),

                // 2. Chat Agent - corresponds to Python version's oxy.ChatAgent, with concurrency limits
                ChatAgent.builder()
                        .name("chat_agent")
                        .llmModel("default_llm")
                        .semaphoreCount(2)
                        .build()
        );
    }

    /**
     * Demonstrate batch processing functionality - using Mas.startBatchProcessing() method
     *
     * <p>This method demonstrates how to use the newly implemented batch processing functionality,
     * corresponding to Python version's batch processing call:</p>
     * <pre>{@code
     * async def main():
     *     async with MAS(oxy_space=oxy_space) as mas:
     *         outs = await mas.start_batch_processing(["hello"] * 10, return_trace_id=True)
     *         [print(out) for out in outs]
     * }</pre>
     *
     * <h3>Advantages of New Implementation</h3>
     * <ul>
     *   <li><strong>True Concurrency</strong>: Uses CompletableFuture for genuine concurrent processing</li>
     *   <li><strong>Performance Improvement</strong>: Concurrent processing significantly improves performance compared to sequential processing</li>
     *   <li><strong>Resource Optimization</strong>: Automatically manages thread pools, avoiding resource waste</li>
     *   <li><strong>Error Isolation</strong>: Single query failure does not affect other queries</li>
     * </ul>
     *
     * @throws Exception When MAS operations fail
     */

    public static void demonstrateBatchProcessing() throws Exception {
        log.info("Starting demonstration of batch processing and semaphore limiting functionality");

        try {
            // Create MAS instance
            Mas mas = MasFactoryRegistry.getFactory().createMas();

            // Create 10 "hello" requests, corresponding to Python version's ["hello"] * 10
            List<String> queries = Collections.nCopies(10, "hello");
            log.info("Preparing batch processing for {} requests: {}", queries.size(), queries);

            boolean returnTraceId = true;

            // 🎯 Use newly implemented startBatchProcessing method
            // Corresponds to Python version: outs = await mas.start_batch_processing(["hello"] * 10, return_trace_id=True)
            log.info("Calling mas.startBatchProcessing() for concurrent batch processing...");
            List<Object> results = mas.startBatchProcessing(queries, returnTraceId);

            // Print results, corresponding to Python version's [print(out) for out in outs]
            log.info("Batch processing completed, received {} responses", results.size());

            results.forEach(result -> {
                log.info("Batch processing result: {}", result);
            });

            // Statistics
            long successCount = results.stream()
                    .filter(result -> result != null)
                    .count();

            log.info("Batch processing statistics - Total requests: {}, Successful responses: {}", queries.size(), successCount);
            log.info("✅ Batch processing demonstration completed! Used true concurrent processing for better performance");

        } catch (Exception e) {
            log.error("Batch processing demonstration failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Application main entry point
     *
     * <p>Corresponds to Python version's main function:</p>
     * <pre>{@code
     * async def main():
     *     async with MAS(oxy_space=oxy_space) as mas:
     *         outs = await mas.start_batch_processing(["hello"] * 10, return_trace_id=True)
     *         [print(out) for out in outs]
     *
     * if __name__ == "__main__":
     *     asyncio.run(main())
     * }</pre>
     *
     * <p>Java version provides two running modes:</p>
     * <ol>
     *   <li><strong>Spring Boot Web Service</strong>: Start through OpenOxySpringBootApplication</li>
     *   <li><strong>Batch Processing Demo</strong>: Demonstrate through demonstrateBatchProcessing() method</li>
     * </ol>
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {

        // Get current class name and register OxySpace
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);

        log.info("DemoBatchAndSemaphore starting...");

        // Choose running mode:
        // Mode 1: Start Spring Boot Web Service (default)
//        OpenOxySpringBootApplication.main(args);

        // Mode 2: Demonstrate batch processing functionality (uncomment the code below)
        demonstrateBatchProcessing();

        log.info("Application startup completed");
        log.info("Semaphore configuration - LLM concurrency limit: 2, ChatAgent concurrency limit: 2");
        log.info("You can test batch processing and concurrency control functionality through the web interface");
    }
}