package com.jd.oxygent.core.oxygent.samples.examples.agent;

/**
 * Evaluate and Evolve Demo Class
 * Demonstrates how to evaluate and evolve agent performance through data processing and analysis
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EvaluateAndEvolveDemo {

    private static final Pattern JSON_PATTERN = Pattern.compile("```json\\s*(.*?)\\s*```", Pattern.DOTALL);

    public static List<BaseOxy> getDefaultOxySpace() {
        String sftPrompt = """
                **Your Task**
                    Act as a strict **SFT data reviewer**. Each time, you will evaluate **a single sample**, which includes:
                
                    ```json
                    {
                    "node_id": "9rZhhWFhiZkrnUMf",
                    "input": "<A JSON string containing a messages array: each item has a role and content>",
                    "output": "<Candidate assistant reply>"
                    }
                    ```
                
                    You need to parse the `messages` inside `input`, and based on the *system instructions* and *user queries*, determine whether the `output` qualifies as a high-quality SFT positive sample.
                
                    **Evaluation Criteria** (All must be satisfied to mark as "keep")
                
                    1. **Follows system instructions / tool invocation rules**
                    - If the system requires calling a specific agent or outputting JSON, the `output` must follow.
                    - If the system explicitly prohibits directly answering professional questions but the `output` does so → discard.
                    2. **Fulfills user needs and is factually correct**
                    - The response must be logically sound, factually accurate, properly formatted, and polite.
                    3. **No violations / low-quality content**
                    - No privacy breaches, offensive language, or meaningless filler.
                    4. **Clear and fluent language**
                    - The language should be smooth and clear (in a single language or with reasonable multilingual use).
                
                    **Output Format**
                    Output a single JSON object **only** (no extra text):
                
                    ```json
                    {
                    "node_id": "9rZhhWFhiZkrnUMf",
                    "keep": true | false,          // true = suitable for SFT; false = discard
                    "reason": "<within 20 characters>"
                    }
                    ```
                
                    Example `reason`s: `"Follows flow"`, `"Missing agent call"`, `"Irrelevant answer"`, `"Format error"`.
                
                    **Additional Notes**
                
                    - Only evaluate the current sample; do not consider cross-sample context.
                    - If the `input` cannot be parsed, return `"keep": false`, `"reason": "Invalid input"`.
                    - Your output **must strictly follow the JSON format** above, or it will be treated as invalid.
                """;

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .isSaveData(false)
                        .build(),
                ChatAgent.builder()
                        .isMaster(true)
                        .name("sft_agent")
                        .prompt(sftPrompt)
                        .llmModel("default_llm")
                        .isSaveData(false)
                        .build()
        );
    }

    public static class Result {
        public final List<String> appNodeData;
        public final List<String> datas;

        public Result(List<String> appNodeData, List<String> datas) {
            this.appNodeData = appNodeData;
            this.datas = datas;
        }
    }

    public static Result processEsResponse(ObjectMapper objectMapper, Map<String, Object> esResponse) {
        List<String> appNodeData = new ArrayList<>();
        List<String> datas = new ArrayList<>();

        // Check if hits exists and is not empty
        @SuppressWarnings("unchecked")
        Map<String, Object> hitsWrapper = (Map<String, Object>) esResponse.get("hits");
        if (hitsWrapper == null) {
            return new Result(appNodeData, datas);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsWrapper.get("hits");
        if (hits == null || hits.isEmpty()) {
            return new Result(appNodeData, datas);
        }

        for (Map<String, Object> hit : hits) {
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) hit.get("_source");
            if (source == null) {
                continue;
            }

            String nodeId = source.getOrDefault("node_id", "").toString();
            String inputJsonStr = source.getOrDefault("input", "").toString();
            String output = source.getOrDefault("output", "").toString();

            if (inputJsonStr == null) {
                continue;
            }

            try {
                // Parse input field as JSON object
                JsonNode inputNode = objectMapper.readTree(inputJsonStr);
                JsonNode argumentsNode = inputNode.get("arguments");
                if (argumentsNode == null) {
                    continue;
                }

                // Build JSON string in app_node_data (note: output needs to be escaped)
                String escapedOutput = output == null ? "" : output.replace("\"", "\\\"");

                String itemJson = String.format(
                        "{\n" +
                                "    \"node_id\": \"%s\",\n" +
                                "    \"input\": %s,\n" +
                                "    \"output\": \"%s\"\n" +
                                "}",
                        nodeId == null ? "" : nodeId,
                        argumentsNode.toString(),
                        escapedOutput
                );
                appNodeData.add(itemJson);

                // Extract arguments.messages and serialize as JSON string
                JsonNode messagesNode = argumentsNode.get("messages");
                if (messagesNode != null) {
                    String messagesJson = objectMapper.writeValueAsString(messagesNode);
                    datas.add(messagesJson);
                } else {
                    datas.add("null"); // Or handle according to requirements
                }

            } catch (Exception e) {
                // Optional: log error, skip exception items
                continue;
            }
        }

        return new Result(appNodeData, datas);
    }

    public static Result getLlmNodeEsData(Mas mas) {
        Map<String, Object> esResponse = mas.getEsClient().search(
                mas.getName() + "_node",
                new HashMap<String, Object>() {{
                    put("query", new HashMap<String, Object>() {{
                        put("term", new HashMap<String, Object>() {{
                            put("node_type", "llm");
                        }});
                    }});
                    put("size", 32);
                    put("sort", Arrays.asList(
                            new HashMap<String, Object>() {{
                                put("create_time", new HashMap<String, Object>() {{
                                    put("order", "desc");
                                }});
                            }}
                    ));
                }}
        );

        return EvaluateAndEvolveDemo.processEsResponse(new ObjectMapper(), esResponse);

    }

    public static void parseResults(String toJsonlPath, List<String> datas, List<Object> results) throws IOException {
        List<String> datasets = new ArrayList<>();

        for (int i = 0; i < datas.size(); i++) {
            String data = datas.get(i);
            String result = results.get(i).toString();

            Matcher match = JSON_PATTERN.matcher(result);
            if (match.find()) {
                String jsonStr = match.group(1);
                // Manual parsing of keep field in JSON (without using external libraries)
                if (jsonStr.contains("\"keep\"") && jsonStr.contains("true")) {
                    // Simple JSON parsing: find "keep": true
                    int keepIndex = jsonStr.indexOf("\"keep\"");
                    if (keepIndex != -1) {
                        int valueStart = jsonStr.indexOf(":", keepIndex);
                        if (valueStart != -1) {
                            String valueStr = jsonStr.substring(valueStart + 1).trim();
                            if (valueStr.startsWith("true")) {
                                datasets.add(data);
                            }
                        }
                    }
                }
            }
        }

        log.info(
                "Filter out " + (datas.size() - datasets.size()) +
                        " samples and keep " + datasets.size() + " samples."
        );

        try (FileWriter writer = new FileWriter(toJsonlPath)) {
            for (int i = 0; i < datasets.size(); i++) {
                writer.write(datasets.get(i));
                if (i < datasets.size() - 1) {
                    writer.write("\n");
                }
            }
        }

        log.info(
                "The SFT training data has been generated to the directory " + toJsonlPath + "."
        );
    }

    public static void main(String[] args) throws Exception {
        String toJsonlPath = "sft_dataset.jsonl";
        Mas mas = new Mas("app", getDefaultOxySpace());
        mas.init();
        Result llmNodeEsData = getLlmNodeEsData(mas);
        List<Object> results = mas.startBatchProcessing(llmNodeEsData.appNodeData, false);
        parseResults(toJsonlPath, llmNodeEsData.datas, results);


    }
}
