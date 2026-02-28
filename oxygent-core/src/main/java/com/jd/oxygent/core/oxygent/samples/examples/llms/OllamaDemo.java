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
package com.jd.oxygent.core.oxygent.samples.examples.llms;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import java.util.Arrays;
import java.util.List;

/**
 * Ollama LLM Demo Class
 * Demonstrates how to configure and use Ollama LLM with HTTP API
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class OllamaDemo {

    /**
     * Get default OxySpace configuration for Ollama LLM
     *
     * @return BaseOxy list containing Ollama HTTP LLM configuration
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "ollamaDemoJavaOxySpace", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Parameter validation
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .baseUrl("http://localhost:11434/api/chat")
                        .modelName(EnvUtils.getEnv("DEFAULT_OLLAMA_MODEL"))
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize Ollama LLM configuration and start Spring Boot application
     * @param args command line arguments
     * @throws Exception when application startup fails
     */
    public static void main(String[] args) throws Exception {
        // Apply JDK17 var keyword
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}