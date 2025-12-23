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
package com.jd.oxygent.core.oxygent.oxy.flows;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.oxy.BaseFlow;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.PydanticOutputParser;
import com.jd.oxygent.core.oxygent.utils.LogUtils;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Plan-and-Solve Workflow - Intelligent workflow implementing Plan-and-Solve prompting strategy
 *
 * <p>PlanAndSolve is an intelligent workflow implementation based on Plan-and-Solve prompting engineering ideas,
 * solving problems by decomposing complex problems into executable step sequences and executing these steps progressively.
 * This workflow is particularly suitable for complex task scenarios that require multi-step reasoning and dynamic adjustment.</p>
 *
 * <h3>Core Concept:</h3>
 * <p>The Plan-and-Solve method divides the problem-solving process into two core phases:</p>
 * <ul>
 *   <li><strong>Planning Phase</strong>: Analyze problems and formulate detailed execution plans</li>
 *   <li><strong>Solving Phase</strong>: Execute step by step according to plan and collect results</li>
 * </ul>
 *
 * <h3>Main Features:</h3>
 * <ul>
 *   <li>Intelligent planning: Automatically decompose complex problems into executable steps</li>
 *   <li>Step execution: Execute plan steps one by one and track progress</li>
 *   <li>Dynamic replanning: Adjust subsequent plans based on execution results (optional)</li>
 *   <li>Progress tracking: Real-time feedback on execution status and intermediate results</li>
 *   <li>Error recovery: Handle exceptions and errors during execution</li>
 * </ul>
 *
 * <h3>Workflow Process:</h3>
 * <ol>
 *   <li><strong>Problem Analysis</strong>: Understand complex problems input by users</li>
 *   <li><strong>Plan Formulation</strong>: Decompose problems into specific execution steps</li>
 *   <li><strong>Step Execution</strong>: Execute each step in the plan sequentially</li>
 *   <li><strong>Result Collection</strong>: Aggregate execution results of each step</li>
 *   <li><strong>Dynamic Adjustment</strong>: Adjust subsequent plans based on intermediate results (optional)</li>
 *   <li><strong>Answer Generation</strong>: Generate final answers based on all results</li>
 * </ol>
 *
 * <h3>Applicable Scenarios:</h3>
 * <ul>
 *   <li>Multi-step mathematical reasoning and calculation problems</li>
 *   <li>Complex data analysis and processing tasks</li>
 *   <li>Business processes that require step-by-step execution</li>
 *   <li>Knowledge reasoning and logical analysis</li>
 *   <li>Composite tasks requiring tool invocation</li>
 * </ul>
 *
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * var planAndSolve = PlanAndSolve.builder()
 *     .name("math_solver")
 *     .desc("Mathematical problem solver")
 *     .plannerAgentName("math_planner")
 *     .executorAgentName("math_executor")
 *     .enableReplanner(true)
 *     .maxReplanRounds(5)
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlanAndSolve extends BaseFlow {

    /**
     * Execution Plan Model - Represents decomposed execution step sequences
     *
     * <p>The Plan model is used to encapsulate the execution step list generated during the planning phase.
     * Each step is an executable atomic task that can complete the entire complex problem solving when executed in sequence.</p>
     *
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Plan {
        /**
         * Execution step list
         *
         * <p>A list of step descriptions arranged in execution order, where each step should be
         * a clear, specific, and executable task description.</p>
         */
        @JsonProperty("steps")
        private List<String> steps = new ArrayList<>();

    }

    /**
     * Response Model - Represents the final answer to the user
     *
     * <p>When the workflow determines that there is sufficient information to answer the user's question,
     * this model is used to encapsulate the final response content.</p>
     *
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        /**
         * Response content
         *
         * <p>Complete answer to the user's question, should be generated comprehensively
         * based on the results of previous execution steps.</p>
         */
        @JsonProperty("response")
        private String response;

    }

    /**
     * Action Model - Represents the decision result of the replanner
     *
     * <p>The Action model is used to encapsulate the decision results of the replanning phase,
     * which can be either continuing to execute a new plan or directly returning the final answer.
     * This design supports dynamic workflow control and intelligent decision-making.</p>
     *
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        /**
         * Action content
         *
         * <p>Can be a Response object (indicating direct answer) or Plan object (indicating continued plan execution).
         * Also supports Map format data for compatibility with different parsing results.</p>
         */
        @JsonProperty("action")
        private Object action;

        /**
         * Determine if it is a plan action
         *
         * @return Returns true if it is a new execution plan
         */
        public boolean isPlan() {
            return action instanceof Plan ||
                    (action instanceof Map && ((Map<?, ?>) action).containsKey("steps"));
        }

        /**
         * Get response content
         *
         * @return Optional wrapper of response object
         */
        public Optional<Response> getResponse() {
            if (action instanceof Response response) {
                return Optional.of(response);
            } else if (action instanceof Map<?, ?> map) {
                return Optional.ofNullable(map.get("response"))
                        .map(resp -> new Response((String) resp));
            }
            return Optional.empty();
        }

        /**
         * Get plan content
         *
         * @return Optional wrapper of plan object
         */
        @SuppressWarnings("unchecked")
        public Optional<Plan> getPlan() {
            if (action instanceof Plan plan) {
                return Optional.of(plan);
            } else if (action instanceof Map<?, ?> map) {
                return Optional.ofNullable(map.get("steps"))
                        .map(steps -> new Plan((List<String>) steps));
            }
            return Optional.empty();
        }
    }

    // ==================== Core Configuration Fields ====================

    /**
     * Maximum replanning rounds
     *
     * <p>Limits the maximum execution rounds of the workflow to prevent infinite loops.
     * Each round includes executing current steps and possible replanning processes.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("max_replan_rounds")
    @Builder.Default
    private int maxReplanRounds = 30;

    /**
     * Planner agent name
     *
     * <p>Identifier of the agent responsible for analyzing problems and generating execution plans.
     * This agent should have good problem decomposition and plan formulation capabilities.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("planner_agent_name")
    @Builder.Default
    private String plannerAgentName = "planner_agent";

    /**
     * Preset plan steps
     *
     * <p>Optional predefined execution step list. If this field is set,
     * the workflow will skip the planning phase and directly use the preset plan.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("pre_plan_steps")
    private List<String> prePlanSteps;

    /**
     * Whether to enable replanner
     *
     * <p>Controls whether to perform replanning after each step execution. When enabled,
     * the system will dynamically adjust subsequent plans based on execution results.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("enable_replanner")
    @Builder.Default
    private boolean enableReplanner = false;

    /**
     * Executor agent name
     *
     * <p>Identifier of the agent responsible for executing specific plan steps.
     * This agent should have strong task execution and tool usage capabilities.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("executor_agent_name")
    @Builder.Default
    private String executorAgentName = "executor_agent";

    /**
     * Replanner agent name
     *
     * <p>Identifier of the agent responsible for evaluating execution results and deciding whether plan adjustments are needed.
     * Only used when replanning functionality is enabled.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("replanner_agent_name")
    @Builder.Default
    private String replannerAgentName = "replanner_agent";

    /**
     * LLM model identifier
     *
     * <p>Language model identifier used for final answer generation. When the workflow
     * used when all steps are completed or maximum rounds are reached.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("llm_model")
    @Builder.Default
    private String llmModel = "default_llm";

    // ==================== Parsers and Processing Functions ====================

    /**
     * Planner response parser
     *
     * <p>Used to parse plan content returned by the planner, converting text to Plan objects.
     * Supports structured JSON parsing.</p>
     *
     * @since 1.0.0
     */
    private transient PydanticOutputParser<Plan> pydanticParserPlanner;

    /**
     * Replanner response parser
     *
     * <p>Used to parse action content returned by the replanner, converting text to Action objects.
     * Supports structured JSON parsing.</p>
     *
     * @since 1.0.0
     */
    private transient PydanticOutputParser<Action> pydanticParserReplanner;

    /**
     * Custom planner response parsing function
     *
     * <p>Optional custom parsing function for handling non-standard response formats from the planner.
     * If set, this function will be used with priority over the default parser.</p>
     *
     * @since 1.0.0
     */
    private transient Function<String, Plan> funcParsePlannerResponse;

    /**
     * Custom replanner response parsing function
     *
     * <p>Optional custom parsing function for handling non-standard response formats from the replanner.
     * If set, this function will be used with priority over the default parser.</p>
     *
     * @since 1.0.0
     */
    private transient Function<String, Action> funcParseReplannerResponse;

    /**
     * Initialize workflow components
     *
     * <p>Automatically called after object construction to initialize parsers and configure permitted tools.
     * Ensures all workflow components are properly configured and can be used normally.</p>
     *
     * @since 1.0.0
     */
    @PostConstruct
    public void init() {
        super.setCategory("agent");
        this.pydanticParserPlanner = new PydanticOutputParser<>(Plan.class, null, null);
        this.pydanticParserReplanner = new PydanticOutputParser<>(Action.class, null, null);
        this.addPermittedTools(Arrays.asList(
                plannerAgentName,
                executorAgentName,
                replannerAgentName
        ));
    }

    /**
     * Manual initialization method
     *
     * <p>Provides manual initialization capability for use in certain framework environments
     * where PostConstruct annotation may not take effect.</p>
     *
     * @since 1.0.0
     */
    public void initializeManually() {
        init();
    }


    /**
     * Execute Plan-and-Solve workflow
     *
     * <p>Core execution method that implements the complete plan-and-solve workflow process. This method
     * decomposes complex problems into executable steps, executes them progressively, and makes dynamic adjustments as needed.</p>
     *
     * <h4>Execution Process:</h4>
     * <ol>
     *   <li><strong>Initialization</strong>: Check configuration and initialize necessary components</li>
     *   <li><strong>Plan Formulation</strong>: Analyze problems and generate execution steps (if no preset plan)</li>
     *   <li><strong>Execution Loop</strong>: Execute plan step by step and collect results</li>
     *   <li><strong>Dynamic Adjustment</strong>: Replan based on execution results (if enabled)</li>
     *   <li><strong>Answer Generation</strong>: Generate final answers based on execution results</li>
     * </ol>
     *
     * @param oxyRequest Request object containing user questions and execution context
     * @return Response object containing final answer and execution status
     * @throws NullPointerException when oxyRequest is null
     * @throws RuntimeException     when workflow execution fails
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        var planStr = "";
        var pastSteps = new StringBuilder();
        var originalQuery = oxyRequest.getQuery();
        var planSteps = Optional.ofNullable(prePlanSteps)
                .map(ArrayList::new)
                .orElse(null);
        record TodoListData(@JsonProperty("list") List<String> list, @JsonProperty("currentStep") int currentStep) {
            private static final ObjectMapper M = new ObjectMapper();

            @Override
            public String toString() {
                try {
                    return M.writeValueAsString(this);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // Save original complete plan, fixed and unchanged after planning is completed
        List<String> originalPlanList = null;

        for (int currentRound = 0; currentRound <= maxReplanRounds; currentRound++) {
            // Perform planning in the first round when there is no preset plan
            if (currentRound == 0 && prePlanSteps == null) {
                planSteps = (ArrayList<String>) executePlanning(oxyRequest, originalQuery);
                originalPlanList = planSteps != null ? List.copyOf(planSteps) : List.of();
                planStr = Optional.ofNullable(planSteps).map(
                                list -> IntStream.rangeClosed(1, list.size())
                                        .mapToObj(i -> i + ". " + list.get(i - 1))
                                        .collect(Collectors.joining("\n")))
                        .orElse("");
                Map<String, Object> message = LogUtils.buildSseLog(oxyRequest, "todolist", planSteps);
                oxyRequest.sendMessage(message);
            } else if (currentRound == 0 && prePlanSteps != null) {
                // If there is a preset plan, also save the original copy
                originalPlanList = List.copyOf(prePlanSteps);
            }

            // Check if there are still steps to execute
            if (planSteps == null || planSteps.isEmpty()) {
                return new OxyResponse(OxyState.COMPLETED, "All plan steps have been completed");
            }

            // Execute current step
            var currentTask = planSteps.get(0);
            oxyRequest.sendMessage(LogUtils.buildSseLog(oxyRequest, "todolist", new TodoListData(originalPlanList, currentRound)));
            var taskFormatted = String.format("""
                    Current Step: %s
                    
                    Context: %s completed
                    
                    EXECUTION RULES: Complete this step efficiently and effectively.
                    - Prioritize using available tools when appropriate
                    - Leverage existing information first
                    - No Ask for clarification
                    - Deliver concrete results and outputs
                    - Mark step as DONE when finished
                    - **IMPORTANT: Respond in the same language as the user's original input**
                    
                    Execute now.
                    """, currentTask, pastSteps).strip();

            var executorResponse = oxyRequest.call(
                    new HashMap<String, Object>() {{
                        put("callee", executorAgentName);
                        put("arguments", new HashMap<String, Object>() {{
                            put("query", taskFormatted);
                        }});
                    }});

            // Update completed steps record
            pastSteps.append("\ntask: ").append(currentTask)
                    .append(", execute task result: ").append(executorResponse.getOutput());

            // Handle replanning or simple step removal
            if (enableReplanner) {
                var replanResult = executeReplanning(oxyRequest, originalQuery, planStr, pastSteps.toString());

                // Use Optional.ifPresentOrElse to simplify response checking
                var responseOpt = replanResult.getResponse();
                if (responseOpt.isPresent()) {
                    return new OxyResponse(OxyState.COMPLETED, responseOpt.get());
                }

                // If no response, there should be a new plan
                var stepsOpt = replanResult.getPlan();
                if (stepsOpt.isPresent()) {
                    planSteps = new ArrayList<>(stepsOpt.get().getSteps());
                }

                planStr = Optional.ofNullable(planSteps).map(
                                list -> IntStream.rangeClosed(1, list.size())
                                        .mapToObj(i -> i + ". " + list.get(i - 1))
                                        .collect(Collectors.joining("\n")))
                        .orElse("");
            } else {
                // Simple mode: remove the first completed step
                planSteps.remove(0);

                if (planSteps.isEmpty()) {
                    return new OxyResponse(OxyState.COMPLETED, executorResponse.getOutput().toString());
                }
            }
        }

        // If maximum replanning rounds are reached, use LLM to generate final response
        var userInputWithResults = String.format("Your objective was this：%s\n---\nFor the following plan：%s",
                originalQuery, planStr);

        var messageDicts = Arrays.asList(
                        Message.systemMessage("Please answer user questions based on the given plan."),
                        Message.userMessage(userInputWithResults)
                ).stream()
                .map(Message::toDict)
                .collect(Collectors.toList());

        var response = oxyRequest.call(
                new HashMap<String, Object>() {{
                    put("callee", llmModel);
                    put("arguments", new HashMap<String, Object>() {{
                        put("messages", messageDicts);
                    }});
                }}
        );

        return new OxyResponse(OxyState.COMPLETED, response.getOutput());
    }

    /**
     * Execute planning phase
     */
    @SneakyThrows
    private List<String> executePlanning(OxyRequest oxyRequest, String originalQuery) {
        var query = Optional.ofNullable(pydanticParserPlanner)
                .map(parser -> parser.format(originalQuery))
                .orElse(originalQuery);
        var response = oxyRequest.call(
                new HashMap<String, Object>() {{
                    put("callee", plannerAgentName);
                    put("arguments", new HashMap<String, Object>() {{
                        put("query", Objects.requireNonNull(query));
                    }});
                }});

        var rawOutput = response.getOutput().toString();

        // Parse planner response
        if (pydanticParserPlanner != null) {
            return pydanticParserPlanner.parse(rawOutput).getSteps();
        } else if (funcParsePlannerResponse != null) {
            return funcParsePlannerResponse.apply(rawOutput).getSteps();
        } else {
            throw new RuntimeException("No available planning response parser");
        }
    }


    /**
     * Execute replanning
     */
    @SneakyThrows
    private Action executeReplanning(OxyRequest oxyRequest, String originalQuery, String planStr, String pastSteps) {
        var replanQuery = String.format("""
                The target of user is:
                %s
                
                The origin plan is:
                %s
                
                We have finished the following steps:
                %s
                
                Please update the plan considering the mentioned information. If no more operation is supposed, Use **Response** to answer the user.
                Otherwise, please update the plan. The plan should only contain the steps to be executed, and do not
                include the past steps or any other information.
                """, originalQuery, planStr, pastSteps);

        var query = Optional.ofNullable(pydanticParserReplanner)
                .map(parser -> parser.format(replanQuery))
                .orElse(replanQuery);

        var replannerResponse =
                oxyRequest.call(
                        new java.util.HashMap<String, Object>() {{
                            put("callee", replannerAgentName);
                            put("arguments", new java.util.HashMap<String, Object>() {{
                                put("query", query);
                            }});
                        }}
                );

        // Parse replanner response
        if (pydanticParserReplanner != null) {
            return pydanticParserReplanner.parse(replannerResponse.getOutput().toString());
        } else if (funcParseReplannerResponse != null) {
            return funcParseReplannerResponse.apply(replannerResponse.getOutput().toString());
        } else {
            throw new RuntimeException("No available replanning response parser");
        }
    }


}
