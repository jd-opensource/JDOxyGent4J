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
import com.jd.oxygent.core.oxygent.oxy.agents.BaseAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Demo class for global data storage functionality
 *
 * <p>This class ports functionality from the Python version, demonstrating how to implement in Java:</p>
 * <ul>
 *   <li><strong>Global Data Storage</strong>: Use MAS.global_data to store persistent data across calls</li>
 *   <li><strong>Custom Agent</strong>: Extend BaseAgent to implement custom business logic</li>
 *   <li><strong>Counter Functionality</strong>: Maintain call count counter to demonstrate data persistence</li>
 *   <li><strong>State Management</strong>: Use OxyState to manage agent execution state</li>
 * </ul>
 *
 * <h3>Python Version Correspondence</h3>
 * <pre>{@code
 * # Python version
 * class CounterAgent(BaseAgent):
 *     async def execute(self, oxy_request: OxyRequest):
 *         cnt = oxy_request.get_global_data("counter", 0) + 1
 *         oxy_request.set_global_data("counter", cnt)
 *
 *         return OxyResponse(
 *             state=OxyState.COMPLETED,
 *             output=f"This MAS has been called {cnt} time(s).",
 *             oxy_request=oxy_request,
 *         )
 *
 * oxy_space = [
 *     oxy.HttpLLM(...),
 *     CounterAgent(
 *         name="master_agent",
 *         is_master=True,
 *     ),
 * ]
 * }</pre>
 *
 * <h3>Java Version Implementation</h3>
 * <ul>
 *   <li><strong>extends BaseAgent</strong>: Extend BaseAgent to implement custom agent</li>
 *   <li><strong>getGlobalData()/setGlobalData()</strong>: Read/write operations for global data</li>
 *   <li><strong>OxyResponse.builder()</strong>: Use builder pattern to construct response</li>
 *   <li><strong>OxyState.COMPLETED</strong>: Set execution completed state</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoGlobalData {

    /**
     * Counter Agent class
     *
     * <p>Corresponds to Python version's CounterAgent class:</p>
     * <pre>{@code
     * class CounterAgent(BaseAgent):
     *     async def execute(self, oxy_request: OxyRequest):
     *         cnt = oxy_request.get_global_data("counter", 0) + 1
     *         oxy_request.set_global_data("counter", cnt)
     *
     *         return OxyResponse(
     *             state=OxyState.COMPLETED,
     *             output=f"This MAS has been called {cnt} time(s).",
     *             oxy_request=oxy_request,
     *         )
     * }</pre>
     *
     * <p>This agent maintains a global counter that increments on each call and returns the current count.</p>
     */
    public static class CounterAgent extends BaseAgent {

        public CounterAgent() {
            super();
            this.setName("master_agent");
            this.setMaster(true);
        }

        @Override
        public OxyResponse execute(OxyRequest oxyRequest) {
            try {
                log.info("CounterAgent starting execution, current request: {}", oxyRequest.getQuery());

                // Get current counter value, default to 0, corresponds to Python version's get_global_data("counter", 0)
                Object counterObj = oxyRequest.getGlobalData("counter");
                int currentCount = counterObj != null ? (Integer) counterObj : 0;

                // Increment counter
                int newCount = currentCount + 1;

                // Set new counter value, corresponds to Python version's set_global_data("counter", cnt)
                oxyRequest.setGlobalData("counter", newCount);

                // Build response message
                String output = String.format("This MAS has been called %d time(s).", newCount);

                log.info("Counter updated: {} -> {}", currentCount, newCount);
                log.info("Response message: {}", output);

                // Return response, corresponds to Python version's OxyResponse construction
                return OxyResponse.builder()
                        .state(OxyState.COMPLETED)  // corresponds to Python version's state=OxyState.COMPLETED
                        .output(output)  // corresponds to Python version's output
                        .oxyRequest(oxyRequest)  // corresponds to Python version's oxy_request=oxy_request
                        .build();

            } catch (Exception e) {
                log.error("CounterAgent execution failed: {}", e.getMessage(), e);

                return OxyResponse.builder()
                        .state(OxyState.FAILED)
                        .output("Counter agent execution failed: " + e.getMessage())
                        .oxyRequest(oxyRequest)
                        .build();
            }
        }

        @Override
        protected OxyResponse _execute(OxyRequest oxyRequest) {
            return null;
        }
    }

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
     *     CounterAgent(
     *         name="master_agent",
     *         is_master=True,
     *     ),
     * ]
     * }</pre>
     *
     * @return List of BaseOxy containing LLM and CounterAgent
     */
    @OxySpaceBean(value = "globalDataJavaOxySpace", defaultStart = true, query = "first")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. HTTP LLM Configuration - corresponds to Python version's oxy.HttpLLM
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .timeout(30)
                        .build(),

                // 2. Counter Agent - corresponds to Python version's CounterAgent
                new CounterAgent()
        );
    }

    /**
     * Demonstrate global data functionality
     *
     * <p>Corresponds to test logic in Python version's main function:</p>
     * <pre>{@code
     * async def main():
     *     async with MAS(oxy_space=oxy_space) as mas:
     *         # first call → counter = 1
     *         r1 = await mas.chat_with_agent({"query": "first"})
     *         print(r1.output)
     *
     *         # second call → counter = 2
     *         r2 = await mas.chat_with_agent({"query": "second"})
     *         print(r2.output)
     *
     *         # third call → counter = 3
     *         r3 = await mas.chat_with_agent({"query": "third"})
     *         print(r3.output)
     * }</pre>
     *
     * @throws Exception When MAS operations fail
     */
    public static void demonstrateGlobalData() throws Exception {
        log.info("Starting demonstration of global data storage functionality");

        try {
            // Create MAS instance
            Mas mas = MasFactoryRegistry.getFactory().createMas();

            // first call → counter = 1
            log.info("=== First call ===");
            Map<String, Object> payload1 = Map.of("query", "first");
            OxyResponse response1 = mas.chatWithAgent(payload1);
            log.info("First call response: {}", response1.getOutput());

            // second call → counter = 2
            log.info("=== Second call ===");
            Map<String, Object> payload2 = Map.of("query", "second");
            OxyResponse response2 = mas.chatWithAgent(payload2);
            log.info("Second call response: {}", response2.getOutput());

            // third call → counter = 3
            log.info("=== Third call ===");
            Map<String, Object> payload3 = Map.of("query", "third");
            OxyResponse response3 = mas.chatWithAgent(payload3);
            log.info("Third call response: {}", response3.getOutput());

            log.info("Global data demonstration completed - counter should increment from 1 to 3");

        } catch (Exception e) {
            log.error("Global data demonstration failed: {}", e.getMessage(), e);
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
     *         # Execute multiple calls to test global data persistence...
     *
     * if __name__ == "__main__":
     *     asyncio.run(main())
     * }</pre>
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        // Get current class name and register OxySpace
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);

        log.info("DemoGlobalData starting...");
        log.info("Feature descriptions:");
        log.info("1. Demonstrate global data storage functionality");
        log.info("2. Show implementation of custom agents");
        log.info("3. Demonstrate data persistence across calls");
        log.info("4. Counter will increment on each call");

        // Choose running mode:
        // Mode 1: Start Spring Boot Web Service (default)
        ServerApp.main(args);

        // Mode 2: Demonstrate global data functionality (uncomment the code below)
        // demonstrateGlobalData();

        log.info("Application startup completed, you can test global data functionality through the web interface");
        log.info("You will see incremented counter values on each call");
    }
}