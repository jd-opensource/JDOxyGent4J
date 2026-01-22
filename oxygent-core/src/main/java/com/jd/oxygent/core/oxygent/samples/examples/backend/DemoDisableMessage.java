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
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Demo Class for disable think, tool_call, observation to speed up response time
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
public class DemoDisableMessage {

    /**
     * Get default OxySpace configuration containing ReAct agent
     *
     * @return BaseOxy list containing ReAct agent
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "demoDisableMessage", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Apply JDK17 var keyword and parameter validation
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .isSendThink(false)
                        .isSendObservation(false)
                        .isSendToolCall(false)
                        .isDetailedToolCall(false)
                        .isDetailedObservation(false)
                        .build(),
                ChatAgent.builder()
                        .name("master_agent")
                        .llmModel("default_llm")
                        .isMaster(true)
                        .isSendObservation(false)
                        .isSendToolCall(false)
                        .isSendAnswer(false)
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize ReAct agent and start Spring Boot application
     *
     * @param args command line arguments
     * @throws Exception when application startup fails
     */
    public static void main(String[] args) throws Exception {
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
