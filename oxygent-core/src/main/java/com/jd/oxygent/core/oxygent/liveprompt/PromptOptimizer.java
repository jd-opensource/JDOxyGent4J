package com.jd.oxygent.core.oxygent.liveprompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.llms.BaseLlM;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.ClassModelDumpUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Prompt optimization module for improving prompt quality.
 *
 * This module provides intelligent prompt optimization capabilities using LLM-based
 * analysis and improvement strategies. It supports framework-specific constraints
 * and custom optimization requirements.
 */
@Slf4j
public class PromptOptimizer {
    
    /**
     * Framework-specific constraint templates
     */
    private static final Map<String, Map<String, Object>> FRAMEWORK_CONSTRAINTS = Map.of(
        "react", Map.of(
            "required_format", """
The prompt MUST include these exact instructions for tool calling format:

3. When you need to use a tool, you must only respond with the exact JSON object format below, nothing else:
```json
{
    "think": "Your thinking (if analysis is needed)",
    "tool_name": "Tool name",
    "arguments": {
        "parameter_name": "parameter_value"
    }
}
```

And this instruction for sequential tool calling:
If solving the user's problem requires multiple tool calls, call only one tool at a time.
            """,
            "validation_rules", List.of(
                "Must contain the exact JSON format specification",
                "Must include 'call only one tool at a time' instruction",
                "Must preserve the ${tools_description} placeholder if present",
                "Must preserve the ${additional_prompt} placeholder if present"
            )
        ),
        "general", Map.of(
            "required_format", "",
            "validation_rules", List.of(
                "Clear and concise instructions",
                "Proper structure and formatting",
                "Appropriate for the use case"
            )
        )
    );
    
    /**
     * Base optimization prompt template
     */
    private static final String OPTIMIZATION_PROMPT_TEMPLATE = """
You are an expert prompt engineer specializing in improving prompt quality and effectiveness.

## Current Prompt to Optimize:
```
{current_prompt}
```

## Optimization Goals:
{optimization_goals}

## Framework-Specific Constraints:
{framework_constraints}

## User's Custom Requirements:
{custom_requirements}

## Task:
Analyze the current prompt and provide an optimized version that:

1. **Clarity**: Makes instructions more clear and unambiguous
2. **Structure**: Improves organization and flow
3. **Completeness**: Ensures all necessary components are present
4. **Effectiveness**: Enhances the prompt's ability to generate desired outputs
5. **Compliance**: Follows all framework-specific constraints

## Response Format:
Provide your response in the following JSON format:
```json
{
    "analysis": "Your analysis of the current prompt's strengths and weaknesses",
    "improvements": ["List of specific improvements made", "each as a separate item"],
    "optimized_prompt": "The complete optimized prompt text",
    "rationale": "Explanation of why these improvements will lead to better results",
    "validation": {
        "meets_constraints": true/false,
        "missing_elements": ["Any required elements that are still missing"],
        "warnings": ["Any potential issues or concerns"]
    }
}
```

Ensure the optimized prompt is production-ready and follows best practices in prompt engineering.
    """;
    
    /**
     * Predefined optimization strategies
     */
    private static final Map<String, String> OPTIMIZATION_STRATEGIES = Map.of(
        "clarity", """
- Improve clarity and specificity of instructions
- Remove ambiguous language
- Add concrete examples where helpful
- Simplify complex phrasing
        """,
        "structure", """
- Improve logical flow and organization
- Group related instructions together
- Use clear section headings
- Enhance readability with proper formatting
        """,
        "completeness", """
- Add missing essential components
- Ensure all necessary context is provided
- Include edge case handling
- Specify desired output format
        """,
        "effectiveness", """
- Strengthen task-oriented instructions
- Add role/context setting
- Improve constraint specification
- Enhance output quality expectations
        """,
        "comprehensive", """
Combine all optimization strategies: clarity, structure, completeness, and effectiveness.
This provides a full prompt engineering review and improvement.
        """
    );

    private Mas mas;
    private String llmModel;
    private static PromptOptimizer instance;
    
    // Static methods for global instance management
    public static PromptOptimizer getInstance(Mas mas, String llmModel) {
        synchronized (PromptOptimizer.class) {
            if (instance == null) {
                instance = new PromptOptimizer(mas, llmModel);
            }
            return instance;
        }
    }

    /**
     * Initialize the PromptOptimizer.
     *
     * @param llmModel Optional LLM model to use. If not provided, will auto-detect
     *                 from registered LLM classes.
     */
    public PromptOptimizer(Mas mas, String llmModel) {
        this.llmModel = autoDetectLlm(mas, llmModel);
        this.mas = mas;
    }
    
    /**
     * Auto-detect available LLM instance name.
     *
     * @param preferredLlm Preferred LLM instance name if provided
     * @return The LLM instance name to use
     * @throws IllegalArgumentException If no LLM instance is found
     */
    private String autoDetectLlm(Mas mas, String preferredLlm) {
        // If specific LLM is provided, validate it exists
        Map<String, BaseOxy> availableInstances = mas.getOxyNameToOxy();
        if (preferredLlm != null && !preferredLlm.isEmpty()) {
            if (availableInstances.keySet().contains(preferredLlm)) {
                log.info("Using specified LLM: {}", preferredLlm);
                return preferredLlm;
            } else {
                log.warn("Specified LLM '{}' not found in registered instances", preferredLlm);
                log.info("Attempting to auto-detect available LLM...");
            }
        }
        List<String> keys = availableInstances.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof BaseLlM)
                .map(Map.Entry::getKey)
                .toList();
        return keys.get(0);
    }

    /**
     * Optimize a prompt based on specified strategy and constraints.
     *
     * @param currentPrompt The original prompt to optimize
     * @param agentType Type of agent (e.g., "react", "general")
     * @param optimizationStrategy Strategy to use for optimization
     * @param customRequirements Additional user requirements
     * @param context Additional context information (e.g., use_case, target_audience, constraints)
     * @return Dictionary containing optimization results with keys:
     *         - analysis: Analysis of the original prompt
     *         - improvements: List of improvements made
     *         - optimizedPrompt: The optimized prompt text
     *         - rationale: Explanation of improvements
     *         - validation: Validation results
     */
    public Map<String, Object> optimize(
            String currentPrompt,
            String agentType,
            String optimizationStrategy,
            String customRequirements,
            Map<String, Object> context) {
        try {
            // Set default values if not provided
            if (agentType == null || agentType.isEmpty()) {
                agentType = "general";
            }
            if (optimizationStrategy == null || optimizationStrategy.isEmpty()) {
                optimizationStrategy = "comprehensive";
            }
            if (customRequirements == null) {
                customRequirements = "";
            }
            
            // Get optimization goals
            String optimizationGoals = getOptimizationGoals(optimizationStrategy);
            
            // Get framework constraints
            String frameworkConstraints = getFrameworkConstraints(agentType);
            
            // Enhance custom requirements with context if provided
            String enhancedRequirements = customRequirements;
            if (context != null && !context.isEmpty()) {
                List<String> contextInfo = new ArrayList<>();
                if (context.containsKey("use_case")) {
                    contextInfo.add("Use case: " + context.get("use_case"));
                }
                if (context.containsKey("target_audience")) {
                    contextInfo.add("Target audience: " + context.get("target_audience"));
                }
                if (context.containsKey("constraints")) {
                    contextInfo.add("Additional constraints: " + context.get("constraints"));
                }
                
                if (!contextInfo.isEmpty()) {
                    enhancedRequirements = customRequirements + "\n\nContext:\n" + String.join("\n", contextInfo);
                }
            }
            
            // Build optimization prompt
            String optimizerPrompt = buildOptimizationPrompt(
                    currentPrompt,
                    optimizationGoals,
                    frameworkConstraints,
                    enhancedRequirements
            );
            
            // Execute optimization via LLM
            Map<String, Object> result = executeOptimization(optimizerPrompt);
            
            // Validate result only if optimization succeeded
            if (result != null) {
                Object optimizedPrompt = result.get("optimized_prompt");
                if (optimizedPrompt != null && !optimizedPrompt.toString().isEmpty()) {
                    try {
                        result.put("validated", validateOptimizedPrompt(
                                optimizedPrompt.toString(),
                                agentType
                        ));
                    } catch (Exception e) {
                        log.warn("Validation failed: {}", e);
                        Map<String, Object> validation = new HashMap<>();
                        validation.put("meets_constraints", false);
                        validation.put("missing_elements", List.of("Validation error"));
                        validation.put("warnings", List.of("Validation failed: " + e));
                        result.put("validated", validation);
                    }
                } else {
                    // Optimized prompt is null or missing
                    Map<String, Object> validation = new HashMap<>();
                    validation.put("meets_constraints", false);
                    validation.put("missing_elements", List.of("No optimized prompt generated"));
                    validation.put("warnings", List.of("Optimization did not produce a result"));
                    result.put("validated", validation);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during prompt optimization", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e);
            errorResult.put("optimized_prompt", currentPrompt); // Fallback to original
            errorResult.put("analysis", "Optimization failed, returning original prompt");
            errorResult.put("improvements", new ArrayList<>());
            errorResult.put("rationale", "Optimization error: " + e);
            
            Map<String, Object> validation = new HashMap<>();
            validation.put("meets_constraints", false);
            validation.put("missing_elements", List.of("Optimization failed"));
            validation.put("warnings", List.of(e));
            errorResult.put("validation", validation);
            
            return errorResult;
        }
    }
    
    /**
     * Get optimization goals based on strategy.
     *
     * @param strategy The optimization strategy name
     * @return String describing optimization goals
     */
    private String getOptimizationGoals(String strategy) {
        return OPTIMIZATION_STRATEGIES.getOrDefault(
                strategy,
                OPTIMIZATION_STRATEGIES.get("comprehensive")
        );
    }
    
    /**
     * Get framework-specific constraints.
     *
     * @param agentType Type of agent
     * @return String describing required format and constraints
     */
    private String getFrameworkConstraints(String agentType) {
        @SuppressWarnings("unchecked")
        Map<String, Object> constraints = (Map<String, Object>) FRAMEWORK_CONSTRAINTS.getOrDefault(
                agentType, FRAMEWORK_CONSTRAINTS.get("general")
        );
        
        List<String> output = new ArrayList<>();
        
        String requiredFormat = (String) constraints.get("required_format");
        if (requiredFormat != null && !requiredFormat.isEmpty()) {
            output.add(requiredFormat);
        }
        
        @SuppressWarnings("unchecked")
        List<String> validationRules = (List<String>) constraints.get("validation_rules");
        if (validationRules != null && !validationRules.isEmpty()) {
            output.add("\nValidation Rules:");
            for (String rule : validationRules) {
                output.add("- " + rule);
            }
        }
        
        return output.isEmpty() ? "No specific constraints for this agent type." : String.join("\n", output);
    }
    
    /**
     * Build the optimization prompt for the LLM.
     *
     * @param currentPrompt Original prompt text
     * @param optimizationGoals Optimization goals description
     * @param frameworkConstraints Framework constraints
     * @param customRequirements User custom requirements
     * @return Complete optimization prompt
     */
    private String buildOptimizationPrompt(
            String currentPrompt,
            String optimizationGoals,
            String frameworkConstraints,
            String customRequirements) {
        return OPTIMIZATION_PROMPT_TEMPLATE
                .replace("{current_prompt}", currentPrompt)
                .replace("{optimization_goals}", optimizationGoals)
                .replace("{framework_constraints}", frameworkConstraints)
                .replace("{custom_requirements}", customRequirements.isEmpty() ? "None specified" : customRequirements);
    }
    
    /**
     * Execute the optimization using LLM.
     *
     * @param optimizerPrompt The optimization prompt to send to LLM
     * @return Parsed optimization result as dictionary
     */
    private Map<String, Object> executeOptimization(String optimizerPrompt) {
        try {
            log.info("Executing LLM optimization: {}", optimizerPrompt);

            BaseOxy baseOxy = mas.getOxyNameToOxy().get(llmModel);

            // Create request with messages format for OpenAILLM
            // Wrap the optimizer prompt as a system message
            List<Map<String, Object>> messages = new ArrayList(List.of(
            new HashMap(Map.of("role", "system",
                    "content", "You are an expert prompt engineering assistant.")),
                    new HashMap(Map.of("role", "user",
                    "content", optimizerPrompt))
            ));

            OxyRequest oxyRequest = new OxyRequest();
            for (Map<String, Object> msg : messages) {
                String roleValue = msg.get("role").toString();
                msg.put("role", roleValue.toLowerCase());
            }
            Memory memory = new Memory();
            memory.setMessages(Message.dictListToMessages(messages));

            oxyRequest.getArguments().put("messages", memory);

            OxyResponse oxyResponse = baseOxy.execute(oxyRequest);

            return parseOptimizationResult((String) oxyResponse.getOutput());
        } catch (Exception e) {
            log.error("Error executing LLM optimization", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Parse the LLM output into structured result.
     *
     * @param output Raw LLM output
     * @return Parsed dictionary with optimization results
     */
    private Map<String, Object> parseOptimizationResult(String output) {
        try {
            // Handle empty or null output
            if (output == null || output.isEmpty()) {
                return Map.of(
                    "analysis", "LLM returned empty or invalid output",
                    "improvements", List.of(),
                    "optimized_prompt", null,
                    "rationale", "Invalid output type: " + (output == null ? "null" : output.getClass().getName()),
                    "validation", Map.of(
                        "meets_constraints", false,
                        "missing_elements", List.of("Failed to generate optimized prompt"),
                        "warnings", List.of("LLM output was empty or None")
                    )
                );
            }
            
            // Try to extract JSON from the output
            Pattern jsonPattern = Pattern.compile("```json\s*(\\{.*?\\})\s*```", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(output);
            String jsonStr;
            
            if (matcher.find()) {
                jsonStr = matcher.group(1);
            } else {
                // Try to find JSON without code blocks
                Pattern simpleJsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
                Matcher simpleMatcher = simpleJsonPattern.matcher(output);
                if (simpleMatcher.find()) {
                    jsonStr = simpleMatcher.group(0);
                } else {
                    // No JSON found, return the raw output as the optimized prompt
                    return Map.of(
                        "analysis", "LLM did not return structured JSON, using raw output",
                        "improvements", List.of("Generated optimized prompt"),
                        "optimized_prompt", output,
                        "rationale", "The LLM returned a text response instead of JSON format. Using the raw output as the optimized prompt.",
                        "validation", Map.of(
                            "meets_constraints", false,
                            "missing_elements", List.of("JSON validation skipped"),
                            "warnings", List.of("Response was not in JSON format")
                        )
                    );
                }
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = JsonUtils.readValue(jsonStr, Map.class);
            
            // Ensure required fields are present
            List<String> requiredFields = List.of("analysis", "improvements", "optimized_prompt", "rationale");
            for (String field : requiredFields) {
                if (!result.containsKey(field)) {
                    result.put(field, "[Missing " + field + "]");
                }
            }
            
            if (!result.containsKey("validation")) {
                result.put("validation", Map.of(
                    "meets_constraints", true,
                    "missing_elements", List.of(),
                    "warnings", List.of()
                ));
            }
            
            return result;
            
        } catch (Exception e) {
            // JSON parsing failed, return raw output
            log.warn("Failed to parse JSON from LLM output: {}", e);
            return Map.of(
                "analysis", "Failed to parse JSON response",
                "improvements", List.of(),
                "optimized_prompt", null,
                "rationale", "JSON parsing error: " + e,
                "validation", Map.of(
                    "meets_constraints", false,
                    "missing_elements", List.of("Failed to parse JSON"),
                    "warnings", List.of("LLM output was not valid JSON")
                )
            );
        }
    }
    
    /**
     * Validate optimized prompt against framework constraints.
     *
     * @param optimizedPrompt The optimized prompt text
     * @param agentType Type of agent
     * @return Validation result as map
     */
    private Map<String, Object> validateOptimizedPrompt(String optimizedPrompt, String agentType) {
        if (StringUtils.isBlank(optimizedPrompt)) {
            return Map.of(
                    "meets_constraints", false,
                    "missing_elements", List.of("No prompt to validate"),
                    "warnings", List.of("Optimized prompt is empty or None")
            );
        }
        Map<String, Object> constraints = FRAMEWORK_CONSTRAINTS.getOrDefault(agentType, Map.of());
        List<String> validationRules = (List) constraints.getOrDefault("validation_rules", List.of());

        Map<String, Object> validationResult = new HashMap(Map.of(
            "meets_constraints", true,
            "missing_elements", new ArrayList<>(),
            "warnings", new ArrayList<>()
        ));
        for (String rule : validationRules) {
            // Check if the rule is satisfied
            if (rule.contains("Must contain") || rule.contains("Must include")) {
                // Extract what needs to be contained
                String keyword = rule.toLowerCase();
                if (keyword.contains("json format")) {
                    if (!optimizedPrompt.contains("```json") && optimizedPrompt.contains("{\"think\"")) {
                        ((List) validationResult.get("missing_elements")).add(rule);
                        validationResult.put("meets_constraints", false);
                    }
                } else if (keyword.contains("one tool at a time")) {
                    if (!optimizedPrompt.toLowerCase().contains("one tool at a time")) {
                        ((List) validationResult.get("missing_elements")).add(rule);
                        validationResult.put("meets_constraints", false);
                    }
                } else if (keyword.contains("${tools_description}")) {
                    if (!optimizedPrompt.toLowerCase().contains("${tools_description}")) {
                        ((List) validationResult.get("missing_elements")).add(rule);
                        validationResult.put("meets_constraints", false);
                    }
                } else if (keyword.contains("${additional_prompt}")) {
                    if (!optimizedPrompt.toLowerCase().contains("${additional_prompt}")) {
                        ((List) validationResult.get("warnings")).add(rule);
                    }
                }
            }
        }
        return validationResult;
    }
}