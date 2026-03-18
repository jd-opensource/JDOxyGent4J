package com.jd.oxygent.core.oxygent.oxy.agents;

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
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plan and Solve Agent - Agent based on planning and execution paradigm
 *
 * <p>PlanAndSolveAgent implements the planning-execution paradigm, where complex tasks are first decomposed
 * into executable plans, then executed step by step, and finally summarized to provide complete answers.
 * This agent is particularly suitable for solving complex multi-step problems.</p>
 *
 * <p>Core Features:</p>
 * <ul>
 *     <li>Task Planning: Decompose complex tasks into executable step-by-step plans</li>
 *     <li>Sequential Execution: Execute planned steps in order and monitor execution results</li>
 *     <li>Dynamic Adjustment: Monitor execution process and replan when necessary</li>
 *     <li>Result Summary: Integrate execution results to provide comprehensive answers</li>
 *     <li>Memory Management: Maintain execution history and context information</li>
 * </ul>
 *
 * <p>Execution Process:</p>
 * <ol>
 *     <li>Planning Phase: Generate detailed execution plans based on user queries</li>
 *     <li>Execution Phase: Execute each step of the plan sequentially</li>
 *     <li>Monitoring Phase: Evaluate execution results and determine next actions</li>
 *     <li>Summary Phase: Integrate all execution results to generate final answers</li>
 * </ol>
 *
 * <p>Applicable Scenarios:</p>
 * <ul>
 *     <li>Complex Problem Solving: Multi-step reasoning and execution tasks</li>
 *     <li>Process Automation: Automated workflow execution</li>
 *     <li>Research Analysis: Multi-source information collection and integration</li>
 *     <li>Decision Support: Multi-factor analysis and judgment</li>
 * </ul>
 *
 * <p>Configuration Example:</p>
 * <pre>{@code
 * PlanAndSolveAgent planAndSolveAgent = PlanAndSolveAgent.builder()
 *     .name("Problem Solver")
 *     .maxReplanRounds(30)
 *     .plannerAgent("planner_agent")
 *     .executorAgent("executor_agent")
 *     .llmModel("gpt-4")
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString(callSuper = true)
@Slf4j
public class PlanAndSolveAgent extends LocalAgent {

    /**
     * Maximum replanning rounds
     * Limits the maximum number of replanning attempts to prevent infinite loops
     */
    @Builder.Default
    protected int maxReplanRounds = 30;

    /**
     * Planner agent name
     * Agent responsible for generating execution plans
     */
    @Builder.Default
    protected String plannerAgent = "planner_agent";

    /**
     * Executor agent name
     * Agent responsible for executing specific tasks
     */
    @Builder.Default
    protected String executorAgent = "executor_agent";

    /**
     * Initialize PlanAndSolveAgent
     *
     * <p>Perform agent initialization, including calling parent class initialization logic
     * and registering permitted sub-agents. This method ensures that the agent has complete
     * planning and execution capabilities.</p>
     *
     * @throws IllegalStateException if referenced agents do not exist
     */
    @Override
    public void init() {
        super.init();

        // Add permitted tools (sub-agents)
        this.addPermittedTool(this.plannerAgent);
        this.addPermittedTool(this.executorAgent);

        log.debug("PlanAndSolveAgent initialization completed: {}, planner: {}, executor: {}",
                this.getName(), this.plannerAgent, this.executorAgent);
    }

    /**
     * Execute planning and solving process
     *
     * <p>This is the core execution method of PlanAndSolveAgent, implementing the complete
     * planning-execution-summary workflow. The method will:</p>
     * <ul>
     *     <li>Generate execution plans through the planner agent</li>
     *     <li>Execute each step of the plan through the executor agent</li>
     *     <li>Monitor execution results and dynamically adjust plans</li>
     *     <li>Summarize all execution results to generate final answers</li>
     * </ul>
     *
     * @param oxyRequest Request object containing user queries and context information, cannot be null
     * @return Response object containing the final answer generated by the agent
     * @throws IllegalArgumentException if oxyRequest is null
     */
    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        // Get original query and short-term memory
        String originalQuery = oxyRequest.getQuery();
        List<Map<String, Object>> shortMemory = oxyRequest.getShortMemory(false);
        String pastPlan = "";
        List<String> pastSteps = new ArrayList<>();

        try {
            // Main replanning loop
            for (int currentRound = 0; currentRound <= maxReplanRounds; currentRound++) {
                // 1. Planning phase: Generate execution plan
                OxyResponse plannerResponse = callPlanner(oxyRequest, originalQuery, pastPlan, pastSteps);
                pastPlan = plannerResponse.getOutput().toString();

                // Parse plan (assuming JSON format)
                List<String> plans = parsePlans(pastPlan);

                // 2. Execution phase: Execute each step
                for (int currentStep = 0; currentStep < plans.size(); currentStep++) {
                    String currentTask = plans.get(currentStep);
                    log.debug("Executing step {}/{}: {}", currentStep + 1, plans.size(), currentTask);

                    // Execute current task
                    OxyResponse executorResponse = callExecutor(oxyRequest, currentTask);
                    String stepResult = executorResponse.getOutput().toString();

                    // Record execution results
                    pastSteps.add(String.format("task: %s, execute task result: %s", currentTask, stepResult));
                    String pastStepsStr = String.join("\n", pastSteps);

                    // If this is the last step, generate final answer
                    if (currentStep == plans.size() - 1) {
                        return generateFinalAnswer(oxyRequest, pastStepsStr, shortMemory, originalQuery);
                    } else {
                        // 3. Monitoring phase: Check if next step is reasonable
                        OxyResponse ctrlResponse = checkNextStep(oxyRequest, pastStepsStr, plans, currentStep, shortMemory, originalQuery);

                        String ctrlOutput = ctrlResponse.getOutput().toString().toLowerCase();
                        if (ctrlOutput.startsWith("complete")) {
                            return generateFinalAnswer(oxyRequest, pastStepsStr, shortMemory, originalQuery);
                        } else if (ctrlOutput.startsWith("replan")) {
                            // Need to replan, break current loop
                            break;
                        }
                        // If "continue", proceed to next step
                    }
                }
            }

            // Exceeded maximum rounds, generate final answer
            String pastStepsStr = String.join("\n", pastSteps);
            return generateFinalAnswer(oxyRequest, pastStepsStr, shortMemory, originalQuery);

        } catch (Exception e) {
            log.error("Error executing PlanAndSolveAgent: {}", e.getMessage(), e);
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Error executing planning and solving process: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Call planner agent to generate execution plan
     */
    private OxyResponse callPlanner(OxyRequest oxyRequest, String originalQuery, String pastPlan, List<String> pastSteps) {
        return oxyRequest.call(new HashMap(Map.of(
                "callee", this.plannerAgent,
                "arguments", new HashMap(Map.of(
                        "query", originalQuery,
                        "past_plan", pastPlan,
                        "past_steps", String.join("\n", pastSteps)
                ))
        )));
    }

    /**
     * Call executor agent to execute specific task
     */
    private OxyResponse callExecutor(OxyRequest oxyRequest, String task) {
        return oxyRequest.call(new HashMap(Map.of(
                "callee", this.executorAgent,
                "arguments", new HashMap(Map.of(
                        "query", "The current step to execute is: " + task
                )),
                "is_async_storage", false //Key point to track
        )));
    }

    /**
     * Check if next step is reasonable
     */
    private OxyResponse checkNextStep(OxyRequest oxyRequest, String pastStepsStr, List<String> plans,
                                      int currentStep, List<Map<String, Object>> shortMemory, String originalQuery) {
        Memory tempMemory = new Memory();
        tempMemory.addMessage(Message.systemMessage(
                String.format("You are an expert in supervising task execution. The historical execution steps are: \n%s.\nThe next step is: %s. \nIf the next step is reasonable, please reply \"continue\"; if it is unreasonable, please reply \"replan\"; if the task is already completed, please reply \"complete\". Do not provide any other content.",
                        pastStepsStr, plans.get(currentStep + 1))
        ));
        tempMemory.addMessages(Message.dictListToMessages(shortMemory));
        tempMemory.addMessage(Message.userMessage("The overall task is: " + originalQuery));

        return oxyRequest.call(new HashMap(Map.of(
                "callee", this.llmModel,
                "arguments", new HashMap(Map.of("messages", tempMemory))
        )));
    }

    /**
     * Generate final answer by summarizing execution results
     */
    private OxyResponse generateFinalAnswer(OxyRequest oxyRequest, String pastStepsStr,
                                            List<Map<String, Object>> shortMemory, String originalQuery) {
        Memory tempMemory = new Memory();
        tempMemory.addMessage(Message.systemMessage(
                String.format("You are an expert in task summarization. The historical execution steps are: \n%s.\nBased on the historical execution records and the user's overall task, please provide the user with a final response.",
                        pastStepsStr)
        ));
        tempMemory.addMessages(Message.dictListToMessages(shortMemory));
        tempMemory.addMessage(Message.userMessage("The current overall task is: " + originalQuery));

        OxyResponse finalResponse = oxyRequest.call(new HashMap(Map.of(
                "callee", this.llmModel,
                "arguments", new HashMap(Map.of("messages", tempMemory))
        )));

        return OxyResponse.builder()
                .state(OxyState.COMPLETED)
                .output(finalResponse.getOutput())
                .build();
    }

    /**
     * Parse execution plans from JSON format
     */
    @SuppressWarnings("unchecked")
    private List<String> parsePlans(String planJson) {
        try {
            return JsonUtils.readValue(planJson, List.class);
        } catch (Exception e) {
            log.warn("Failed to parse plans JSON, treating as single plan: {}", e.getMessage());
            List<String> singlePlan = new ArrayList<>();
            singlePlan.add(planJson);
            return singlePlan;
        }
    }
}
