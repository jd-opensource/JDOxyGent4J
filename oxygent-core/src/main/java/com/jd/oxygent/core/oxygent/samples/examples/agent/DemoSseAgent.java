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
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.SSEAgent;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * SSE (Server-Sent Events) Agent Demo Class
 * Demonstrates how to configure and use SSE agents for real-time data stream processing
 * <p>
 * The AppMathAgent class needs to be started.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoSseAgent {

    /**
     * Create OxySpace configuration containing SSE agent
     *
     * @return BaseOxy list containing SSE agent
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    public static List<BaseOxy> createOxySpace() {

        var headers = new HashMap<String, String>();

        var serverUrl = "http://127.0.0.1:8092";

        var sseAgent = SSEAgent.builder()
                .isMaster(true)
                .name("time_agent")
                .desc("An tool for time query")
                .customHeaders(headers)
                .serverUrl(serverUrl)
                .isOxyAgent(true)
                .build();

        return Arrays.asList(sseAgent);
    }

    /**
     * Application main entry point
     * Initialize SSE agent and execute chat functionality
     *
     * @param args command line arguments
     * @throws JsonProcessingException when JSON processing fails
     */
    public static void main(String[] args) throws Exception {

        var oxySpace = createOxySpace();
        var mas = new Mas("SSEAgentDemo", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        var payload = new HashMap<String, Object>();
        payload.put("query", "query current time");

        try {
            mas.chatWithAgent(payload);
        } catch (Exception e) {
            log.error("Chat execution failed", e);
        }
    }


}
