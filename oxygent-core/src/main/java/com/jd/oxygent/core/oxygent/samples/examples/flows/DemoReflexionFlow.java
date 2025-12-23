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
package com.jd.oxygent.core.oxygent.samples.examples.flows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.flows.Reflexion;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;

import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Reflexion Flow Demo Class
 * Demonstrates how to use the Java version of the OxyGent framework to build a multi-agent system based on the reflexion mechanism.
 * <p>
 * Main features include:
 * 1. General reflexion workflow - iteratively improves answer quality
 * 2. Math-specific reflexion workflow - specializes in verification and improvement of math problems
 * 3. Master agent - selects appropriate reflexion flow based on the question type
 * <p>
 * Reflexion flow principle:
 * 1. Worker agent generates an initial answer
 * 2. Reflexion agent evaluates answer quality
 * 3. If unsatisfactory, improve the answer based on feedback
 * 4. Repeat until satisfactory or max iterations reached
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoReflexionFlow {

    private static final int MAX_ITERATIONS = 3;
    private static final int DEMO_DELAY_MS = 2000;

    /**
     * General reflexion workflow function
     * Implements the external reflexion process and iteratively improves the answer quality.
     *
     * @param oxyRequest Request object
     * @return Response optimized by reflexion
     * @throws IllegalArgumentException if the request object is null
     */
    public static CompletableFuture<OxyResponse> reflexionWorkflow(OxyRequest oxyRequest) {
        Objects.requireNonNull(oxyRequest, "OxyRequest object cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Apply JDK17 var keyword to simplify local variable declarations
                var userQuery = Objects.requireNonNull(oxyRequest.getQuery(), "User query cannot be null");
                log.info("=== User Query ===\n{}\n", userQuery);

                var currentIteration = 0;
                var currentQuery = userQuery;

                while (currentIteration < MAX_ITERATIONS) {
                    currentIteration++;
                    log.info("=== Reflexion Round {} ===", currentIteration);

                    // Let worker_agent generate an answer
                    var workerResponse = callWorkerAgent(oxyRequest, currentQuery);
                    if (workerResponse.isEmpty()) {
                        break;
                    }

                    var workerAnswer = workerResponse.get();
                    log.info("Worker Answer:\n{}\n", workerAnswer);

                    // Let reflexion_agent evaluate answer quality
                    var evaluationResult = evaluateAnswer(oxyRequest, userQuery, workerAnswer);
                    if (evaluationResult.isEmpty()) {
                        break;
                    }

                    var reflexionResult = evaluationResult.get();
                    log.info("Reflexion Evaluation:\n{}\n", reflexionResult);

                    // Check evaluation result
                    if (isAnswerSatisfactory(reflexionResult)) {
                        log.info("=== Reflexion Complete, Answer Quality Satisfactory ===");
                        return buildSuccessResponse(currentIteration, workerAnswer);
                    }

                    // Extract improvement suggestions and update query
                    var improvementSuggestion = extractImprovementSuggestion(reflexionResult);
                    if (improvementSuggestion.isPresent()) {
                        currentQuery = String.format("%s\n\nEvaluation result is: %s. Please note the following improvement suggestions: %s",
                                userQuery, reflexionResult, improvementSuggestion.get());
                        log.info("Updated query based on improvement suggestions:\n{}\n", currentQuery);
                    }
                }

                // Reached maximum iterations, generate final answer
                return generateFinalAnswer(oxyRequest, userQuery);

            } catch (Exception e) {
                log.error("Reflexion workflow execution failed", e);
                return OxyResponse.builder()
                        .state(OxyState.FAILED)
                        .output("Reflexion workflow execution failed: " + e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Math-specific reflexion workflow
     * Specialized reflexion workflow for math problems, uses math expert and checker agents.
     *
     * @param oxyRequest Request object
     * @return Reflexion-optimized response for math problems
     * @throws IllegalArgumentException if the request object is null
     */
    public static CompletableFuture<OxyResponse> mathReflexionWorkflow(OxyRequest oxyRequest) {
        Objects.requireNonNull(oxyRequest, "OxyRequest object cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            try {
                var userQuery = Objects.requireNonNull(oxyRequest.getQuery(), "User query cannot be null");
                log.info("=== Math Problem Query ===\n{}\n", userQuery);

                var currentIteration = 0;
                var currentQuery = userQuery;

                while (currentIteration < MAX_ITERATIONS) {
                    currentIteration++;
                    log.info("=== Math Reflexion Round {} ===", currentIteration);

                    // Let math expert agent generate answer
                    var mathResponse = callMathExpert(oxyRequest, currentQuery);
                    if (mathResponse.isEmpty()) {
                        break;
                    }

                    var mathAnswer = mathResponse.get();
                    log.info("Math Expert Answer:\n{}\n", mathAnswer);

                    // Let math checker evaluate
                    var checkResult = checkMathAnswer(oxyRequest, userQuery, mathAnswer);
                    if (checkResult.isEmpty()) {
                        break;
                    }

                    var checkerResult = checkResult.get();
                    log.info("Math Check Result:\n{}\n", checkerResult);

                    // Check if passed
                    if (isMathAnswerCorrect(checkerResult)) {
                        log.info("=== Math Reflexion Complete, Solution Correct ===");
                        return buildMathSuccessResponse(currentIteration, mathAnswer);
                    }

                    // Extract correction suggestions
                    var correctionSuggestion = extractCorrectionSuggestion(checkerResult);
                    if (correctionSuggestion.isPresent()) {
                        currentQuery = String.format("%s\n\nCheck result is: %s. Please note the following correction suggestions: %s",
                                userQuery, checkerResult, correctionSuggestion.get());
                    }
                }

                // Generate final math answer
                return generateFinalMathAnswer(oxyRequest, userQuery);

            } catch (Exception e) {
                log.error("Math reflexion workflow execution failed", e);
                return OxyResponse.builder()
                        .state(OxyState.FAILED)
                        .output("Math reflexion workflow execution failed: " + e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Call worker agent to generate an answer
     *
     * @param oxyRequest Request object
     * @param query      Query content
     * @return Response from worker agent, or empty Optional if failed
     */
    private static Optional<String> callWorkerAgent(OxyRequest oxyRequest, String query) {
        try {
            var workerArgs = new HashMap<String, Object>();
            workerArgs.put("query", query);

            var workerResp = oxyRequest.call(Map.of(
                    "callee", "worker_agent",
                    "arguments", workerArgs
            ));
            return Optional.of(workerResp.getOutput().toString());
        } catch (Exception e) {
            log.error("Failed to call worker agent", e);
            return Optional.empty();
        }
    }

    /**
     * Evaluate answer quality
     *
     * @param oxyRequest Request object
     * @param userQuery  Original user query
     * @param answer     Answer to be evaluated
     * @return Evaluation result, or empty Optional if failed
     */
    private static Optional<String> evaluateAnswer(OxyRequest oxyRequest, String userQuery, String answer) {
        try {
            // Apply JDK17 Text Blocks to handle multi-line strings
            var evaluationQuery = String.format("""
                    Please evaluate the quality of the following answer:
                    
                    Original question: %s
                    
                    Answer: %s
                    
                    Please return the evaluation result in the following format:
                    Evaluation result: [Satisfactory/Unsatisfactory]
                    Evaluation reason: [Specific reason]
                    Improvement suggestions: [If unsatisfactory, please provide specific improvement suggestions]
                    """, userQuery, answer);

            var reflexionArgs = new HashMap<String, Object>();
            reflexionArgs.put("query", evaluationQuery);

            var reflexionResp = oxyRequest.call(Map.of(
                    "callee", "reflexion_agent",
                    "arguments", reflexionArgs
            ));
            return Optional.of(reflexionResp.getOutput().toString());
        } catch (Exception e) {
            log.error("Failed to evaluate answer", e);
            return Optional.empty();
        }
    }

    /**
     * Call math expert to generate answer
     *
     * @param oxyRequest Request object
     * @param query      Query content
     * @return Math expert response, or empty Optional if failed
     */
    private static Optional<String> callMathExpert(OxyRequest oxyRequest, String query) {
        try {
            var mathArgs = new HashMap<String, Object>();
            mathArgs.put("query", query);

            var mathResp = oxyRequest.call(Map.of(
                    "callee", "math_expert_agent",
                    "arguments", mathArgs
            ));
            return Optional.of(mathResp.getOutput().toString());
        } catch (Exception e) {
            log.error("Failed to call math expert", e);
            return Optional.empty();
        }
    }

    /**
     * Check math answer correctness
     *
     * @param oxyRequest Request object
     * @param userQuery  Original user query
     * @param mathAnswer Math answer
     * @return Check result, or empty Optional if failed
     */
    private static Optional<String> checkMathAnswer(OxyRequest oxyRequest, String userQuery, String mathAnswer) {
        try {
            // Apply JDK17 Text Blocks to handle multi-line strings
            var checkQuery = String.format("""
                    Please check the correctness and completeness of the following math solution:
                    
                    Question: %s
                    
                    Solution: %s
                    
                    Check points:
                    1. Are the calculation steps correct?
                    2. Are there any missing steps?
                    3. Is the final answer clear?
                    4. Is the problem-solving approach clear?
                    
                    Please return in the following format:
                    Check result: [Pass/Fail]
                    Problem description: [If there are problems, describe specifically]
                    Correction suggestions: [Specific correction suggestions]
                    """, userQuery, mathAnswer);

            var checkerArgs = new HashMap<String, Object>();
            checkerArgs.put("query", checkQuery);

            var checkerResp = oxyRequest.call(Map.of(
                    "callee", "math_checker_agent",
                    "arguments", checkerArgs
            ));
            return Optional.of(checkerResp.getOutput().toString());
        } catch (Exception e) {
            log.error("Failed to check math answer", e);
            return Optional.empty();
        }
    }

    /**
     * Check if answer is satisfactory
     *
     * @param evaluationResult Evaluation result
     * @return True if answer is satisfactory
     */
    private static boolean isAnswerSatisfactory(String evaluationResult) {
        Objects.requireNonNull(evaluationResult, "Evaluation result cannot be null");
        return evaluationResult.contains("Satisfactory") && !evaluationResult.contains("Unsatisfactory");
    }

    /**
     * Check if math answer is correct
     *
     * @param checkResult Check result
     * @return True if math answer is correct
     */
    private static boolean isMathAnswerCorrect(String checkResult) {
        Objects.requireNonNull(checkResult, "Check result cannot be null");
        return checkResult.contains("Pass") && !checkResult.contains("Fail");
    }

    /**
     * Extract improvement suggestions
     *
     * @param evaluationResult Evaluation result
     * @return Optional of improvement suggestions, or empty Optional if not found
     */
    private static Optional<String> extractImprovementSuggestion(String evaluationResult) {
        Objects.requireNonNull(evaluationResult, "Evaluation result cannot be null");

        // Apply JDK17 var keyword
        var lines = evaluationResult.split("\n");
        for (var line : lines) {
            if (line.contains("Improvement suggestions")) {
                var parts = line.split(":", 2);
                if (parts.length > 1) {
                    return Optional.of(parts[1].trim());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Extract correction suggestions
     *
     * @param checkResult Check result
     * @return Optional of correction suggestions, or empty Optional if not found
     */
    private static Optional<String> extractCorrectionSuggestion(String checkResult) {
        Objects.requireNonNull(checkResult, "Check result cannot be null");

        // Apply JDK17 var keyword
        var lines = checkResult.split("\n");
        for (var line : lines) {
            if (line.contains("Correction suggestions")) {
                var parts = line.split(":", 2);
                if (parts.length > 1) {
                    return Optional.of(parts[1].trim());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Build success response
     *
     * @param iterations Number of iterations
     * @param answer     Final answer
     * @return Successful OxyResponse
     */
    private static OxyResponse buildSuccessResponse(int iterations, String answer) {
        return OxyResponse.builder()
                .state(OxyState.COMPLETED)
                .output(String.format("Final answer optimized through %d rounds of reflexion:\n\n%s", iterations, answer))
                .build();
    }

    /**
     * Build math success response
     *
     * @param iterations Number of iterations
     * @param answer     Final answer
     * @return Successful OxyResponse
     */
    private static OxyResponse buildMathSuccessResponse(int iterations, String answer) {
        return OxyResponse.builder()
                .state(OxyState.COMPLETED)
                .output(String.format("Solution verified through %d rounds of math validation:\n\n%s", iterations, answer))
                .build();
    }

    /**
     * Generate final answer
     *
     * @param oxyRequest Request object
     * @param userQuery  User query
     * @return Final OxyResponse
     */
    private static OxyResponse generateFinalAnswer(OxyRequest oxyRequest, String userQuery) {
        log.info("=== Reached maximum iterations ({}), returning current best answer ===", MAX_ITERATIONS);

        try {
            var finalArgs = new HashMap<String, Object>();
            finalArgs.put("query", userQuery);

            var finalResp = oxyRequest.call(Map.of(
                    "callee", "worker_agent",
                    "arguments", finalArgs
            ));

            return OxyResponse.builder()
                    .state(OxyState.COMPLETED)
                    .output(String.format("Answer after %d reflexion attempts:\n\n%s",
                            MAX_ITERATIONS, finalResp.getOutput()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate final answer", e);
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Failed to generate final answer: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generate final math answer
     *
     * @param oxyRequest Request object
     * @param userQuery  User query
     * @return Final math OxyResponse
     */
    private static OxyResponse generateFinalMathAnswer(OxyRequest oxyRequest, String userQuery) {
        try {
            var finalArgs = new HashMap<String, Object>();
            finalArgs.put("query", userQuery);

            var finalResp = oxyRequest.call(Map.of(
                    "callee", "math_expert_agent",
                    "arguments", finalArgs
            ));

            return OxyResponse.builder()
                    .state(OxyState.COMPLETED)
                    .output(String.format("Answer verified through %d rounds of math validation:\n\n%s",
                            MAX_ITERATIONS, finalResp.getOutput()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate final math answer", e);
            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Failed to generate final math answer: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create OxySpace configuration
     * Define all agents, LLM models and workflows in the system
     *
     * @return List of BaseOxy objects
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    public static List<BaseOxy> createOxySpace() {
        var oxySpace = new ArrayList<BaseOxy>();

        try {
            // Apply JDK17 var keyword and parameter validation
            var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
            var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
            var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

            // LLM model configuration
            var defaultLlm = HttpLlm.builder()
                    .name("default_llm")
                    .apiKey(Objects.requireNonNull(apiKey, "API key cannot be null"))
                    .baseUrl(Objects.requireNonNull(baseUrl, "Base URL cannot be null"))
                    .modelName(Objects.requireNonNull(modelName, "Model name cannot be null"))
                    .timeout(240)
                    .llmParams(Map.of("temperature", 0.01))
                    .build();
            oxySpace.add(defaultLlm);

            // Worker Agent - responsible for generating initial answers
            var workerAgent = ReActAgent.builder()
                    .name("worker_agent")
                    .desc("Worker agent responsible for generating initial answers")
                    .llmModel("default_llm")
                    .build();
            oxySpace.add(workerAgent);

            // Reflexion Agent - responsible for evaluating answer quality
            var reflexionAgent = ChatAgent.builder()
                    .name("reflexion_agent")
                    .desc("Reflexion agent responsible for evaluating answer quality and providing improvement suggestions")
                    .llmModel("default_llm")
                    .build();
            oxySpace.add(reflexionAgent);

            // Math Expert Agent - specialized in handling math problems
            var mathExpertAgent = ChatAgent.builder()
                    .name("math_expert_agent")
                    .desc("Math expert providing detailed mathematical solutions")
                    .llmModel("default_llm")
                    .build();
            oxySpace.add(mathExpertAgent);

            // Math Checker Agent - checks math solutions
            var mathCheckerAgent = ChatAgent.builder()
                    .name("math_checker_agent")
                    .desc("Math checker verifying correctness of mathematical solutions")
                    .llmModel("default_llm")
                    .build();
            oxySpace.add(mathCheckerAgent);

            // General reflexion workflow agent
            var generalReflexion = Reflexion.builder()
                    .name("general_reflexion")
                    .workerAgent("worker_agent")
                    .reflexionAgent("reflexion_agent")
                    .maxReflexionRounds(3)
                    .build();
            oxySpace.add(generalReflexion);

            // Math reflexion workflow agent
            var mathReflexion = Reflexion.MathReflexion.builder()
                    .name("math_reflexion")
                    .workerAgent("math_expert_agent")
                    .reflexionAgent("math_checker_agent")
                    .maxReflexionRounds(3)
                    .build();
            oxySpace.add(mathReflexion);

            // Master agent - coordinates different reflexion agents based on question type
            var masterAgent = ReActAgent.builder()
                    .name("master_agent")
                    .isMaster(true)
                    .desc("Master agent that selects appropriate reflexion workflow based on question type")
                    .subAgents(Arrays.asList("general_reflexion", "math_reflexion"))
                    .llmModel("default_llm")
                    .build();
            oxySpace.add(masterAgent);

        } catch (Exception e) {
            log.error("Failed to create OxySpace configuration", e);
            throw new IllegalArgumentException("OxySpace configuration creation failed: " + e.getMessage(), e);
        }

        return oxySpace;
    }

    /**
     * Application main entry point
     * Starts the reflexion flow demo program
     *
     * @param args Command line arguments
     * @throws Exception when application startup fails
     */
    public static void main(String[] args) throws Exception {
        Objects.requireNonNull(args, "Command line arguments cannot be null");

        try {
            log.info("Starting reflexion flow demo program...");

            // Create OxySpace
            var oxySpace = createOxySpace();

            // Create and start MAS system
            var mas = new Mas("ReflexionDemo", oxySpace);
            mas.setOxySpace(oxySpace);
            mas.init();

            log.info("Reflexion flow MAS system initialization completed");

            // Demonstrate reflexion flow - using test queries
            var testQueries = List.of(
                    "Calculate the area of a circle with radius 5.",
                    "Solve equation 2x + 5 = 15",
                    "What is artificial intelligence? Please explain in detail.",
                    "Write a Java bubble sort algorithm"
            );

            for (var i = 0; i < testQueries.size(); i++) {
                var query = testQueries.get(i);
                log.info("Processing query {}: {}", i + 1, query);

                // Create demo interaction information
                var info = new HashMap<String, Object>();
                info.put("current_trace_id", "ReflexionDemo_" + System.currentTimeMillis() + "_" + i);
                info.put("from_trace_id", "");
                info.put("query", query);

                // Call multi-agent system to process query
                mas.chatWithAgent(info);

                log.info("Query {} completed", i + 1);

                // Add delay for better log observation
                Thread.sleep(DEMO_DELAY_MS);
            }

            log.info("Reflexion flow demo completed!");

        } catch (Exception e) {
            log.error("Reflexion flow demo program startup failed", e);
            throw new RuntimeException("Reflexion flow demo program startup failed: " + e.getMessage(), e);
        }
    }
}
