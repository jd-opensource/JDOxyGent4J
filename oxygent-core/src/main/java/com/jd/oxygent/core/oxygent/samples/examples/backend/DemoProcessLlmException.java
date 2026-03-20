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
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo class for demonstrating handle exception in OxyGent HttpLlm
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
@Slf4j
public class DemoProcessLlmException {

    @OxySpaceBean(value = "demoProcessLlmException", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey("BAD API KEY")
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .funcProcessLlmException(e -> {
                            log.error("LLM request exception", e);
                            OxyResponse oxyResponse = new OxyResponse();
                            oxyResponse.setOutput("We have encountered a minor issue. Please try again later.");
                            return oxyResponse;
                        })
                        .build(),
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .maxReactRounds(1)
                        .build()
        );
    }

    /**
     * Application main entry point - demonstrates manual MAS creation and various interaction modes
     *
     * @param args Command line arguments
     * @throws Exception When MAS operations fail
     */
    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
