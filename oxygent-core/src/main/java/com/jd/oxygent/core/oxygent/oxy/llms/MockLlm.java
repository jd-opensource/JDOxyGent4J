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
package com.jd.oxygent.core.oxygent.oxy.llms;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;


import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Mock LLM implementation for testing purposes.
 *
 * <p>This class provides a mock large language model implementation that
 * simulates LLM responses without actual model inference. It is primarily
 * used for testing and development purposes.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Deterministic Responses</strong>: Returns predefined mock responses</li>
 *   <li><strong>Configurable Mock Function</strong>: Supports custom mock processing logic</li>
 *   <li><strong>Async Simulation</strong>: Simulates async behavior of real LLM</li>
 *   <li><strong>Error Handling</strong>: Proper error state management</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Unit testing of agent workflows</li>
 *   <li>Integration testing without external LLM dependencies</li>
 *   <li>Development and debugging of LLM-dependent features</li>
 *   <li>Performance testing of agent logic</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MockLlm mockLlm = new MockLlm();
 * mockLlm.setFuncMockProcess((request) -> {
 *     return "Mock response for: " + request.getQuery();
 * });
 *
 * OxyResponse response = mockLlm._execute(oxyRequest);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class MockLlm extends BaseLlM {

    /**
     * Mock processing function - corresponds to Python's func_mock_process
     *
     * <p>Function interface for custom mock processing logic</p>
     */
    private Function<OxyRequest, String> funcMockProcess;

    /**
     * Initialize Mock LLM
     * <p>Sets up the mock processing function</p>
     */
    @Override
    public void init() {
        super.init();
        if (this.funcMockProcess == null) {
            this.funcMockProcess = this::_mockProcess;
            log.debug("Using default mock process function");
        } else {
            log.debug("Using custom mock process function");
        }
    }

    /**
     * Default mock processing function - corresponds to Python version's _mock_process.
     *
     * <p>Simulates async processing with 1-second delay.</p>
     *
     * @param oxyRequest The request to process
     * @return Mock output string
     */
    private String _mockProcess(OxyRequest oxyRequest) {
        try {
            TimeUnit.SECONDS.sleep(1);
            return "output";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Mock processing interrupted");
            return "Error: Mock processing interrupted";
        }
    }

    /**
     * Execute mock LLM inference
     *
     * <p>This method simulates LLM behavior by calling the mock processing function.
     * Matches Python version's async _execute signature and behavior.</p>
     *
     * @param oxyRequest The request containing messages and parameters
     * @return OxyResponse with mock output
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        try {
            log.info("Executing mock LLM request");

            // Call the mock processing function directly (sync version)
            String output = funcMockProcess.apply(oxyRequest);

            return OxyResponse.builder()
                    .state(OxyState.COMPLETED)
                    .output(output)
                    .oxyRequest(oxyRequest)
                    .build();

        } catch (Exception e) {
            log.error("Mock LLM execution failed: {}", e.getMessage(), e);
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Error: " + e.getMessage())
                    .oxyRequest(oxyRequest)
                    .build();
        }
    }
}
