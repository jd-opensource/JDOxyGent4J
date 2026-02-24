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

import java.util.Arrays;
import java.util.List;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.PlanAndSolveAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * Plan and Solve Agent Demo Class
 * Demonstrates how to configure PlanAndSolveAgent with planner and executor agents
 * for complex multi-step problem solving tasks
 *
 * <p>This demo showcases the planning-execution paradigm where:</p>
 * <ul>
 *     <li>Planner agent generates step-by-step execution plans</li>
 *     <li>Executor agent executes individual tasks</li>
 *     <li>Master agent coordinates the entire process</li>
 * </ul>
 *
 * <p>Example usage scenario: "What time is it now? Please save it into time.txt."</p>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoPlanAndSolveAgent {
    /**
     * Get default OxySpace configuration containing PlanAndSolve agent setup
     *
     * @return BaseOxy list containing all agents and tools for the demo
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "planAndSolveAgentJavaOxySpace", defaultStart = true, 
                  query = "What time is it now? Please save it into time.txt.")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. LLM Configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),

                // 2. Planner Agent - Responsible for generating execution plans
                ChatAgent.builder()
                        .name("planner_agent")
                        .llmModel("default_llm")
                        .prompt("""
                                The origin plan is:
                                {past_plan}

                                We have finished the following steps:
                                {past_steps}

                                Please update the plan considering the mentioned information.
                                Otherwise, please update the plan. The plan should only contain the steps to be executed, and do not 
                                include the past steps or any other information.
                                Please reply in JSON list format only, nothing else
                                """)
                        .build(),

                // 3. Time Tools - Utility functions for time operations
                PresetTools.TIME_TOOLS,
                // 4. File Tools - Utility functions for file operations
                PresetTools.FILE_TOOLS,
                // 5. Executor Agent - Responsible for executing individual tasks
                ReActAgent.builder()
                        .name("executor_agent")
                        .llmModel("default_llm")
                        .additionalPrompt("You should only execute the current step, and do not execute other steps in our plan. Do not execute more than one step continuously or skip any step.")
                        .tools(Arrays.asList("get_current_time", "get_current_date", "write_file", "read_file"))
                        .build(),

                // 6. Master Agent - Coordinates the entire planning and solving process
                PlanAndSolveAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .llmModel("default_llm")
                        .plannerAgent("planner_agent")
                        .executorAgent("executor_agent")
                        .maxReplanRounds(30)
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize PlanAndSolve agent and start Spring Boot application
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