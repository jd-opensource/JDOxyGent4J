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
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Demo class for reading config.json properties file
 *
 * <p>This class ports functionality from the Python version, demonstrating how to implement in Java:</p>
 * <ul>
 *   <li><strong>Read user-specified config.json properties file and start service</strong></li>
 * </ul>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li><strong>Configuration Loading</strong>: Load configuration from external JSON files</li>
 *   <li><strong>Environment Support</strong>: Support different environments (dev, prod, etc.)</li>
 *   <li><strong>ReAct Agent</strong>: Demonstrates ReAct (Reasoning and Acting) agent functionality</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoConfig {

    /**
     * Get default OxySpace configuration
     *
     * @return List of BaseOxy containing HTTP LLM and ReAct Agent
     */
    @OxySpaceBean(value = "demoConfig", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                ReActAgent.builder()
                        .name("master_agent")
                        .build()
        );
    }

    /**
     * Application main entry point
     *
     * <p>Loads configuration from specified path and starts the server application.</p>
     *
     * @param args Command line arguments
     * @throws Exception When configuration loading or application startup fails
     */
    public static void main(String[] args) throws Exception {
        Config.loadConfigPath("C:/your/path/config.json", "dev");
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
