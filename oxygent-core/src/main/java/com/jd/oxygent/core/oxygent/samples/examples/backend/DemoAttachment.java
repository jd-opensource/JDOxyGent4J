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
 * Demo class for attachment functionality
 *
 * <p>Demonstrates multimodal support and file attachment handling capabilities.
 * This class configures an OxySpace with multimodal LLM support and chat agents
 * that can process file attachments and multimedia content.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li><strong>Multimodal LLM</strong>: Supports processing of text, images, and other file types</li>
 *   <li><strong>Chat Agent</strong>: Interactive chat agent for Q&amp;A with attachment support</li>
 *   <li><strong>File Processing</strong>: Handles various file formats and attachments</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoAttachment {

    /**
     * Get default OxySpace configuration for attachment handling
     *
     * @return List of BaseOxy containing multimodal LLM and chat agent
     */
    @OxySpaceBean(value = "attachmentJavaOxySpace", defaultStart = true, query = "Introduce the content of the file")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. HTTP LLM Configuration - corresponds to Python version's oxy.HttpLLM, with concurrency limits
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .isMultimodalSupported(true)
                        .timeout(30)
                        .build(),

                // 2. Chat Agent - corresponds to Python version's oxy.ChatAgent, with concurrency limits
                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .build()
        );
    }

    /**
     * Application main entry point
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}