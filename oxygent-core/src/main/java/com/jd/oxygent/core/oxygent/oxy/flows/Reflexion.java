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
import com.jd.oxygent.core.oxygent.oxy.BaseFlow;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.PydanticOutputParser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

/**
 * Reflexion Workflow - Answer quality iterative improvement system based on reflection mechanism
 *
 * <p>Reflexion is an intelligent workflow based on reflection thinking, continuously improving answer quality
 * through multi-round iterative "generation-evaluation-improvement" cycles. This workflow simulates the
 * reflection process humans use when solving problems, capable of self-evaluation and continuous improvement of output results.</p>
 *
 * <h3>Core Philosophy:</h3>
 * <p>The Reflexion workflow is based on reflection theory in cognitive science, believing that high-quality problem solving requires:</p>
 * <ul>
 *   <li><strong>Generation</strong>: Produce initial solutions</li>
 *   <li><strong>Evaluation</strong>: Critically examine solutions</li>
 *   <li><strong>Reflection</strong>: Identify problems and improvement directions</li>
 *   <li><strong>Refinement</strong>: Optimize solutions based on reflection results</li>
 * </ul>
 *
 * <h3>Workflow Process:</h3>
 * <ol>
 *   <li><strong>Initial Generation</strong>: Worker Agent generates initial answers based on user questions</li>
 *   <li><strong>Quality Assessment</strong>: Reflexion Agent evaluates answer quality from multiple dimensions</li>
 *   <li><strong>Satisfaction Check</strong>: Determine whether answers meet quality standards</li>
 *   <li><strong>Reflective Improvement</strong>: If unsatisfactory, generate specific improvement suggestions</li>
 *   <li><strong>Iterative Optimization</strong>: Regenerate better answers based on suggestions</li>
 *   <li><strong>Convergent Output</strong>: Output final results after reaching satisfaction standards or maximum rounds</li>
 * </ol>
 *
 * <h3>Evaluation Dimensions:</h3>
 * <ul>
 *   <li><strong>Accuracy</strong>: Whether information is correct and factually reliable</li>
 *   <li><strong>Completeness</strong>: Whether user questions are comprehensively answered</li>
 *   <li><strong>Clarity</strong>: Whether expression is clear and easy to understand</li>
 *   <li><strong>Relevance</strong>: Whether content is on-topic and targeted</li>
 *   <li><strong>Practicality</strong>: Whether it provides actual help to users</li>
 * </ul>
 *
 * <h3>Applicable Scenarios:</h3>
 * <ul>
 *   <li>Professional consulting requiring high-quality answers</li>
 *   <li>In-depth analysis of complex problems</li>
 *   <li>Iterative optimization of creative content</li>
 *   <li>Quality improvement of technical documentation</li>
 *   <li>Accuracy verification of educational content</li>
 * </ul>
 *
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * var reflexionFlow = Reflexion.builder()
 *     .name("quality_improver")
 *     .desc("Answer quality improver based on reflection")
 *     .workerAgent("content_generator")
 *     .reflexionAgent("quality_evaluator")
 *     .maxReflexionRounds(3)
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
public class Reflexion extends BaseFlow {

    /**
     * Reflection Evaluation Result Model - Encapsulates complete information of answer quality assessment
     *
     * <p>This model is used to represent the evaluation results of the reflection agent on answer quality,
     * including satisfaction judgment, evaluation reasons, and specific improvement suggestions.</p>
     *
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReflectionEvaluation {
        /**
         * Whether satisfied with current answer
         *
         * <p>Overall judgment of the reflection agent on answer quality. True indicates that the answer
         * meets quality requirements and can be directly returned to the user; false indicates
         * further improvement is needed.</p>
         */
        @JsonProperty("is_satisfactory")
        private boolean isSatisfactory;

        /**
         * Evaluation reason
         *
         * <p>Detailed explanation of specific reasons why the answer is considered satisfactory or unsatisfactory.
         * Should be analyzed from multiple dimensions such as accuracy, completeness, clarity, etc.</p>
         */
        @JsonProperty("evaluation_reason")
        private String evaluationReason;

        /**
         * Improvement suggestions
         *
         * <p>When the answer is unsatisfactory, provide specific improvement directions and suggestions.
         * These suggestions will be used to guide the next round of answer generation.</p>
         */
        @JsonProperty("improvement_suggestions")
        private String improvementSuggestions = "";

        /**
         * Convert to dictionary format
         *
         * @return Map object containing all fields
         */
        public Map<String, Object> toDict() {
            Map<String, Object> map = new HashMap<>();
            map.put("is_satisfactory", isSatisfactory);
            map.put("evaluation_reason", evaluationReason);
            map.put("improvement_suggestions", improvementSuggestions);
            return map;
        }

        /**
         * Check if evaluation result is valid
         *
         * @return true if contains valid evaluation reason
         */
        public boolean isValid() {
            return evaluationReason != null && !evaluationReason.trim().isEmpty();
        }
    }

    // ==================== Configuration Parameters ====================

    /**
     * Maximum reflection rounds
     *
     * <p>Limits the maximum number of iterations for the reflection workflow to prevent infinite loops.
     * Each round includes answer generation, quality assessment, and possible improvement processes.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("max_reflexion_rounds")
    @Builder.Default
    private int maxReflexionRounds = 3;

    /**
     * Worker agent name
     *
     * <p>The identifier of the agent responsible for generating and improving answers. This agent should
     * have strong content generation and comprehension capabilities.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("worker_agent")
    @Builder.Default
    private String workerAgent = "worker_agent";

    /**
     * Reflection agent name
     *
     * <p>The identifier of the agent responsible for evaluating answer quality and providing improvement suggestions.
     * This agent should have critical thinking and quality assessment capabilities.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("reflexion_agent")
    @Builder.Default
    private String reflexionAgent = "reflexion_agent";

    // ==================== Parsers and Processing Functions ====================

    /**
     * Custom worker agent response parsing function
     *
     * <p>Optional custom parsing function for handling worker agent responses.
     * If not set, default string parsing will be used.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    private Function<String, String> funcParseWorkerResponse = null;

    /**
     * Custom reflection agent response parsing function
     *
     * <p>Optional custom parsing function for handling reflection agent responses.
     * If not set, default parsing will be used.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    private Function<String, ReflectionEvaluation> funcParseReflexionResponse = null;

    /**
     * Pydantic output parser
     *
     * <p>Structured parser for parsing reflection agent output.
     * Supports converting text responses to ReflectionEvaluation objects.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    private PydanticOutputParser<ReflectionEvaluation> pydanticParserReflexion = null;

    /**
     * LLM model identifier
     *
     * <p>Language model identifier for final answer generation. When maximum rounds
     * are reached without obtaining a satisfactory answer, this model is used to generate the final response.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("llm_model")
    @Builder.Default
    private String llmModel = "default_llm";

    // Evaluation template
    @JsonProperty("evaluation_template")
    @Builder.Default
    private String evaluationTemplate = """
            Please evaluate the quality of the following answer:
            
            Original Question: {query}
            
            Answer: {answer}
            
            Please evaluate based on these criteria:
            1. Accuracy: Is the information correct and factual?
            2. Completeness: Does it fully address the user's question?
            3. Clarity: Is it well-structured and easy to understand?
            4. Relevance: Does it stay focused on the user's needs?
            5. Helpfulness: Does it provide practical value to the user?
            
            Return your evaluation in the following format:
            - is_satisfactory: true/false
            - evaluation_reason: [Detailed explanation]
            - improvement_suggestions: [Specific recommendations if unsatisfactory]
            """;

    @JsonProperty("improvement_template")
    @Builder.Default
    private String improvementTemplate = """
            {original_query}
            
            Please improve your previous answer based on the following feedback:
            {improvement_suggestions}
            
            Previous answer: {previous_answer}
            """;

    /**
     * Initialization method
     */
    public void initializeReflexion() {
        // Add permitted tools
        List<String> permittedTools = new ArrayList<>();
        permittedTools.add(workerAgent);
        permittedTools.add(reflexionAgent);
        this.addPermittedTools(permittedTools);

        // Set default parsing functions
        if (funcParseWorkerResponse == null) {
            funcParseWorkerResponse = this::defaultParseWorkerResponse;
        }

        if (funcParseReflexionResponse == null) {
            funcParseReflexionResponse = this::defaultParseReflexionResponse;
        }

        // Initialize Pydantic parser
        if (pydanticParserReflexion == null) {
            pydanticParserReflexion = new PydanticOutputParser<>(
                    ReflectionEvaluation.class, null, null);
        }
    }

    /**
     * Default Worker response parser
     */
    private String defaultParseWorkerResponse(String response) {
        return response != null ? response.trim() : "";
    }

    /**
     * Default Reflexion response parser
     */
    private ReflectionEvaluation defaultParseReflexionResponse(String response) {
        if (pydanticParserReflexion != null) {
            try {
                return pydanticParserReflexion.parse(response);
            } catch (Exception e) {
                log.warn("Pydantic parsing failed, using text parsing: {}", e.getMessage());
            }
        }
        return parseReflexionText(response);
    }

    /**
     * Parse reflection response in text format
     */
    private ReflectionEvaluation parseReflexionText(String response) {
        if (response == null || response.trim().isEmpty()) {
            return new ReflectionEvaluation(false, "No response provided", "");
        }

        String[] lines = response.split("\n");
        boolean isSatisfactory = false;
        String evaluationReason = "";
        String improvementSuggestions = "";

        for (String line : lines) {
            String trimmedLine = line.trim().toLowerCase();

            if (trimmedLine.contains("satisfactory")) {
                isSatisfactory = !trimmedLine.contains("unsatisfactory");
            } else if (trimmedLine.contains("evaluation result:")) {
                isSatisfactory = trimmedLine.contains("satisfactory") &&
                        !trimmedLine.contains("unsatisfactory");
            } else if (trimmedLine.startsWith("evaluation reason:")) {
                evaluationReason = line.substring(line.indexOf(":") + 1).trim();
            } else if (trimmedLine.startsWith("improvement suggestions:")) {
                improvementSuggestions = line.substring(line.indexOf(":") + 1).trim();
            }
        }

        if (evaluationReason.isEmpty()) {
            evaluationReason = "No specific reason provided";
        }

        return new ReflectionEvaluation(isSatisfactory, evaluationReason, improvementSuggestions);
    }

    /**
     * Execute reflection workflow
     *
     * <p>Implements complete reflection mechanism workflow, continuously improving answer quality
     * through multi-round iterative "generation-evaluation-improvement" cycles until quality
     * requirements are met or maximum rounds are reached.</p>
     *
     * <h4>Execution Process:</h4>
     * <ol>
     *   <li><strong>Initialization</strong>: Configure parsers and permitted tools</li>
     *   <li><strong>Answer Generation</strong>: Worker Agent generates initial answer</li>
     *   <li><strong>Quality Assessment</strong>: Reflexion Agent evaluates answer quality</li>
     *   <li><strong>Satisfaction Check</strong>: Determine if quality standards are met</li>
     *   <li><strong>Iterative Improvement</strong>: Generate improved version based on evaluation feedback</li>
     *   <li><strong>Convergent Output</strong>: Return satisfactory answer or final attempt result</li>
     * </ol>
     *
     * @param oxyRequest Request object containing user questions and execution context
     * @return Response object containing optimized answer and reflection process information
     * @throws NullPointerException when oxyRequest is null
     * @since 1.0.0
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        // Initialize
        initializeReflexion();

        String originalQuery = oxyRequest.getQuery();
        String currentQuery = originalQuery;

        log.info("Starting reflection process, query: {}", originalQuery);

        for (int currentRound = 0; currentRound <= maxReflexionRounds; currentRound++) {
            log.info("Reflection round {}", currentRound + 1);

            try {
                // Step 1: Get answer from Worker Agent
                Map<String, Object> workerArgs = new HashMap<>();
                workerArgs.put("query", currentQuery);

                OxyResponse workerResponse = oxyRequest.call(Map.of(
                        "callee", workerAgent,
                        "arguments", workerArgs
                ));

                String currentAnswer = funcParseWorkerResponse.apply(
                        workerResponse.getOutput().toString());
                log.info("Worker answer: {}...",
                        currentAnswer.length() > 200 ? currentAnswer.substring(0, 200) : currentAnswer);

                // Step 2: Evaluate using Reflexion Agent
                String evaluationQuery = evaluationTemplate
                        .replace("{query}", originalQuery)
                        .replace("{answer}", currentAnswer);

                if (pydanticParserReflexion != null) {
                    evaluationQuery = pydanticParserReflexion.format(evaluationQuery);
                }

                Map<String, Object> reflexionArgs = new HashMap<>();
                reflexionArgs.put("query", evaluationQuery);

                OxyResponse reflexionResponse = oxyRequest.call(Map.of(
                        "callee", reflexionAgent,
                        "arguments", reflexionArgs
                ));

                ReflectionEvaluation evaluation = funcParseReflexionResponse.apply(
                        reflexionResponse.getOutput().toString());
                log.info("Evaluation result: {}", evaluation.isSatisfactory());

                // Step 3: Check if satisfactory
                if (evaluation.isSatisfactory()) {
                    log.info("Answer is satisfactory after {} rounds", currentRound + 1);

                    Map<String, Object> extra = new HashMap<>();
                    extra.put("reflexion_rounds", currentRound + 1);
                    extra.put("final_evaluation", evaluation.toDict());

                    return OxyResponse.builder()
                            .state(OxyState.COMPLETED)
                            .output(String.format("Final answer after %d rounds of reflection optimization:\n\n%s",
                                    currentRound + 1, currentAnswer))
                            .extra(extra)
                            .build();
                }

                // Step 4: If unsatisfactory and max rounds not reached, prepare improvement query
                if (currentRound < maxReflexionRounds) {
                    if (evaluation.getImprovementSuggestions() != null &&
                            !evaluation.getImprovementSuggestions().trim().isEmpty()) {

                        currentQuery = improvementTemplate
                                .replace("{original_query}", originalQuery)
                                .replace("{improvement_suggestions}", evaluation.getImprovementSuggestions())
                                .replace("{previous_answer}", currentAnswer);

                        log.info("Updated query based on improvement suggestions: {}", evaluation.getImprovementSuggestions());
                    } else {
                        // If no specific suggestions, retry with original query
                        currentQuery = String.format("%s\n\nPlease provide a better answer. Previous attempt had issues: %s",
                                originalQuery, evaluation.getEvaluationReason());
                    }
                }

            } catch (Exception e) {
                log.error("Reflection round {} execution failed", currentRound + 1, e);

                return OxyResponse.builder()
                        .state(OxyState.FAILED)
                        .output("Reflection process execution failed: " + e.getMessage())
                        .build();
            }
        }

        // Reached maximum rounds but still unsatisfactory
        log.warn("Reached maximum reflection rounds ({})", maxReflexionRounds + 1);

        try {
            // Generate final answer
            String finalQuery = String.format("""
                    Original user question: %s
                    
                    Please provide the best possible answer based on the above question, considering previous feedback and attempts.
                    """, originalQuery);

            List<Message> finalMessages = Arrays.asList(
                    Message.systemMessage("You need to provide the best possible answer based on previous attempts and feedback."),
                    Message.userMessage(finalQuery)
            );

            Map<String, Object> finalArgs = new HashMap<>();
            finalArgs.put("messages", finalMessages.stream()
                    .map(Message::toDict)
                    .toArray());

            OxyResponse finalResponse = oxyRequest.call(Map.of(
                    "callee", this.llmModel,
                    "arguments", finalArgs
            ));

            Map<String, Object> extra = new HashMap<>();
            extra.put("reflexion_rounds", maxReflexionRounds + 1);
            extra.put("reached_max_rounds", true);

            return OxyResponse.builder()
                    .state(OxyState.COMPLETED)
                    .output(String.format("Answer after %d rounds of reflection attempts:\n\n%s",
                            maxReflexionRounds + 1, finalResponse.getOutput()))
                    .extra(extra)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate final answer", e);

            return OxyResponse.builder()
                    .state(OxyState.FAILED)
                    .output("Reflection process completed but failed to generate final answer: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Math-specific reflection workflow
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class MathReflexion extends Reflexion {

        public MathReflexion(Map<String, Object> kwargs) {
            super();

            // Set default agents for math problems
            if (!kwargs.containsKey("workerAgent")) {
                this.setWorkerAgent("math_expert_agent");
            }
            if (!kwargs.containsKey("reflexionAgent")) {
                this.setReflexionAgent("math_checker_agent");
            }

            // Set math-specific evaluation template
            if (!kwargs.containsKey("evaluationTemplate")) {
                this.setEvaluationTemplate("""
                        Please check the correctness and completeness of the following mathematical solution:
                        
                        Problem: {query}
                        
                        Solution: {answer}
                        
                        Check points:
                        1. Are the calculation steps correct?
                        2. Are there any missing steps?
                        3. Is the final answer clear?
                        4. Is the problem-solving approach clear?
                        5. Are mathematical formulas and theorems applied correctly?
                        
                        Please return evaluation results in the following format:
                        - is_satisfactory: true/false (use true for pass, false for fail)
                        - evaluation_reason: [Detailed check explanation]
                        - improvement_suggestions: [If failed, provide specific correction suggestions]
                        """);
            }

            // Apply other parameters
            kwargs.forEach((key, value) -> {
                try {
                    switch (key) {
                        case "maxReflexionRounds": {
                            if (!(value instanceof Number)) {
                                throw new IllegalArgumentException("Key 'maxReflexionRounds' requires a number, got: " + value);
                            }
                            this.setMaxReflexionRounds(((Number) value).intValue());
                            break;
                        }
                        case "workerAgent": {
                            if (!(value instanceof String)) {
                                throw new IllegalArgumentException("Key 'workerAgent' requires a string, got: " + value);
                            }
                            this.setWorkerAgent((String) value);
                            break;
                        }
                        case "reflexionAgent": {
                            if (!(value instanceof String)) {
                                throw new IllegalArgumentException("Key 'reflexionAgent' requires a string, got: " + value);
                            }
                            this.setReflexionAgent((String) value);
                            break;
                        }
                        case "evaluationTemplate": {
                            if (!(value instanceof String)) {
                                throw new IllegalArgumentException("Key 'evaluationTemplate' requires a string, got: " + value);
                            }
                            this.setEvaluationTemplate((String) value);
                            break;
                        }
                        case "improvementTemplate": {
                            if (!(value instanceof String)) {
                                throw new IllegalArgumentException("Key 'improvementTemplate' requires a string, got: " + value);
                            }
                            this.setImprovementTemplate((String) value);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unsupported configuration key: " + key);
                    }
                } catch (Exception e) {
                    log.warn("Failed to set parameter {}: {}", key, e.getMessage());
                }
            });
        }
    }

    /**
     * Factory method for creating reflection workflow agents
     */
    public static List<Reflexion> createReflexionFlowAgents() {
        List<Reflexion> agents = new ArrayList<>();

        // General reflection workflow
        agents.add(Reflexion.builder()
                .name("general_reflexion_flow")
                .desc("General reflection workflow for answer quality improvement")
                .workerAgent("worker_agent")
                .reflexionAgent("reflexion_agent")
                .maxReflexionRounds(3)
                .build());

        // Math reflection workflow
        agents.add(new MathReflexion(Map.of(
                "name", "math_reflexion_flow",
                "desc", "Specialized reflection workflow for math problems",
                "maxReflexionRounds", 3
        )));

        // Detailed reflection workflow
        agents.add(Reflexion.builder()
                .name("detailed_reflexion_flow")
                .desc("Detailed reflection workflow with custom evaluation criteria")
                .workerAgent("detailed_worker_agent")
                .reflexionAgent("detailed_reflexion_agent")
                .maxReflexionRounds(5)
                .evaluationTemplate("""
                        Comprehensively evaluate this answer:
                        
                        Question: {query}
                        Answer: {answer}
                        
                        Please score on a 1-10 scale:
                        - Accuracy and factual correctness
                        - Information completeness
                        - Clarity and readability
                        - Practicality
                        - Professionalism
                        
                        Provide detailed feedback and specific improvement suggestions.
                        
                        Format:
                        - is_satisfactory: true/false (true only if all aspects score 8+ points)
                        - evaluation_reason: [Detailed scoring and analysis]
                        - improvement_suggestions: [Specific actionable improvement suggestions]
                        """)
                .build());

        return agents;
    }
}
