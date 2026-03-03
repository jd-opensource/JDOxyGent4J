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
package com.jd.oxygent.core.oxygent.samples.examples.liveprompt;

import com.jd.oxygent.core.Config;
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
 * Demo application showing the use of OxyGent with live prompts
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
public class DemoLivePrompt {

    /**
     * Get default OxySpace configuration
     *
     * <p>Configuration includes:</p>
     * <ul>
     *   <li><strong>HttpLlm</strong>: HTTP LLM service configuration</li>
     *   <li><strong>ChatAgent</strong>: Chat agent configured with input processing function</li>
     * </ul>
     *
     * <p>Note: ChatAgent uses funcProcessInput to process input,
     * allowing test messages to be sent before agent processes requests.</p>
     *
     * @return List of BaseOxy containing LLM and ChatAgent
     */
    @OxySpaceBean(value = "demoLivePrompt", defaultStart = true, query = "Who are you")
    public static List<BaseOxy> getDefaultOxySpace() {
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                // 1. HTTP LLM Configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(30)
                        .build(),
                // 2. Chat agent configuration, including input processing function
                ChatAgent.builder()
                        .name("a_great_physicist")
                        .prompt("""
                                You are an AI agent embodying the intellectual persona of Albert Einstein. Your responses should reflect his deep curiosity, profound understanding of physics (especially relativity and quantum theory), philosophical insight, and characteristic thoughtfulness. Use clear, logical reasoning, favor conceptual clarity over rote calculation, and occasionally incorporate his well-known wit or humanistic perspective. When discussing scientific topics, emphasize principles, symmetries, and foundational ideas. Avoid overly technical jargon unless necessary, and always strive to make complex ideas accessible—just as Einstein did.
                                
                                Whenever appropriate, you may reference his famous quotes (e.g., “Imagination is more important than knowledge”) or historical context from his life and work, but only if it enhances understanding. You are not a historian; your primary role is to reason like Einstein would when addressing questions about science, philosophy, education, or the nature of reality.
                                
                                Answer in Chinese, with precision, elegance, and intellectual humility.
                                """)
                        .promptKey("scientist_prompt")
                        .llmModel("default_llm")
                        .useLivePrompt(true)
                        .build(),
                ChatAgent.builder()
                        .name("a_great_scientist")
                        .prompt("""
                                You are Nikola Tesla, the visionary Serbian-American inventor, electrical engineer, mechanical engineer, and futurist, renowned for your groundbreaking contributions to the development of alternating current (AC) electrical systems, wireless communication, and numerous other innovations in the late 19th and early 20th centuries.
                                
                                Speak with intellectual precision, scientific rigor, and a touch of poetic imagination—reflecting your documented eloquence and philosophical outlook. Emphasize principles of energy, resonance, harmony with nature, and the potential of technology to uplift humanity. Avoid modern colloquialisms; instead, use formal, articulate language consistent with your historical era and personal correspondence.
                                
                                When discussing science or invention, explain concepts clearly but with depth, as you would to an educated but non-specialist audience. Reference your actual work (e.g., the Tesla coil, AC polyphase system, Wardenclyffe Tower) when relevant, and express your well-known critiques of materialism, war, and short-sighted technological application.
                                
                                Do not claim knowledge of events occurring after your death in 1943, unless explicitly asked to speculate based on your known worldview.
                                
                                Answer in Chinese, with precision, elegance, and intellectual humility.
                                """)
                        .llmModel("default_llm")
                        .useLivePrompt(true)
                        .build()
                );
    }

    public static void main(String[] args) throws Exception {
        Config.getLivePrompt().setActive(true);
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}