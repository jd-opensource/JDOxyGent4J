package com.jd.oxygent.core.oxygent.function_hubs.chart;

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
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.samples.server.utils.BrowserOpener;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flowchart generation tools class
 * Generates Mermaid flowcharts from text descriptions and returns HTML file paths
 */
public class FlowImageGenTools extends FunctionHub {

    private static final Logger logger = Logger.getLogger(FlowImageGenTools.class.getName());

    // API configuration
    private final String apiBaseUrl = System.getenv("OPENAI_BASE_URL");
    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private final String modelName = System.getenv("OPENAI_MODEL_NAME");

    // Default prompt template
    private static final String DEFAULT_PROMPT_TEMPLATE = """
        你是一个专业的流程图设计师，请根据以下解析出的步骤结构生成一个简洁清晰的 Mermaid 流程图代码。
        
        步骤结构：%s
        
        ## 流程图设计规则：
        1. **节点类型选择**：
           - 普通步骤使用方框：`A[步骤名称]`
           - 判断条件使用菱形：`B{{是否通过？}}`
           - 开始/结束使用圆角：`Start([开始])` 或 `End([结束])`
        
        2. **连接线标注**：
           - 判断分支要标注条件：`-->|是 | 或 -->|否 |`
           - 重要流向可以添加说明：`-->|提交审核 |`
        
        3. **样式美化**：
           - 开始节点：`style Start fill:#c8e6c9,stroke:#4caf50,stroke-width:2px`
           - 结束节点：`style End fill:#e3f2fd,stroke:#2196f3,stroke-width:2px`
           - 决策节点：`style Decision fill:#fff3e0,stroke:#ff9800,stroke-width:2px`
        
        ## 示例参考：
        ```mermaid
        flowchart TD
            Start([开始]) --> A[需求分析]
            A --> B[系统设计]
            B --> C[编码实现]
            C --> D[系统测试]
            D --> E{{测试通过？}}
            E -->|是 | F[部署上线]
            E -->|否 | C
            F --> End([结束])
            
            style Start fill:#c8e6c9,stroke:#4caf50,stroke-width:2px
            style End fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
            style E fill:#fff3e0,stroke:#ff9800,stroke-width:2px
        ```
        
        要求：
        - 只返回 Mermaid 代码，以 ```mermaid 开头，以 ``` 结尾
        - 严格按照提供的步骤顺序生成流程
        - 保持流程图简洁清晰，不添加额外的详细描述框
        - 确保代码语法正确且逻辑清晰
        - 不要包含任何解释或说明文字
        """;

    public FlowImageGenTools() {
        super("flow_image_gen_tools");
        this.setDesc("Generate Mermaid flowcharts from text descriptions and return HTML file paths");
    }

    /**
     *     Generate Mermaid flowchart from text description and open in browser
     * @param description Text description of the flowchart
     * @param outputPath Output HTML file path, defaults to "flowchart.html"
     * @return Path to the generated HTML file
     */
    @Tool(
            name = "generateFlowChart",
            description = "根据文本描述生成 Mermaid 流程图并返回 HTML 文件路径。此工具使用 OpenAI API 将文本描述转换为 Mermaid 流程图代码，然后生成可视化的 HTML 文件并在浏览器中打开。",
            paramMetas = {
                    @ParamMetaAuto(name = "description", type = "String", description = "流程图的文本描述"),
                    @ParamMetaAuto(name = "outputPath", type = "String", description = "输出的 HTML 文件路径")
            }
    )
    public String generateFlowChart(String description, String outputPath) {
        try {
            // Process output path
            String finalOutputPath = processOutputPath(outputPath);

            // Ensure output directory exists
            Path outputDir = Paths.get(finalOutputPath).getParent();
            if (outputDir != null && !Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Call API to generate Mermaid code
            String mermaidCode = callOpenAIApi(description);

            // Create HTML file and render flowchart
            boolean success = createHtmlWithMermaid(mermaidCode, finalOutputPath);

            if (success) {
                // Automatically open generated file in browser
                openInBrowser(finalOutputPath);
                return "✅ Flowchart generated and opened in browser: " + finalOutputPath;
            } else {
                return "❌ Error generating flowchart";
            }

        } catch (Exception e) {
            logger.severe("Error executing generateFlowChart function: " + e.getMessage());
            return "❌ Error generating flowchart: " + e.getMessage();
        }
    }

    /**
     * Process output path
     */
    private String processOutputPath(String outputPath) throws IOException {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            // Generate default filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "flowchart_" + timestamp + ".html";

            // Use output folder under project root directory
            String currentDir = System.getProperty("user.dir");
            String projectRoot = currentDir;

            // If currently in examples/other directory, need to go back to project root
            if (currentDir.endsWith("examples/other") || currentDir.endsWith("examples\\other")) {
                projectRoot = Paths.get(currentDir).getParent().getParent().toString();
            }

            Path outputDir = Paths.get(projectRoot, "output");
            Files.createDirectories(outputDir);

            return outputDir.resolve(filename).toString();
        } else {
            Path path = Paths.get(outputPath);
            if (!path.isAbsolute()) {
                // Handle relative path
                String currentDir = System.getProperty("user.dir");

                if (currentDir.endsWith("examples/other") || currentDir.endsWith("examples\\other")) {
                    Path projectRoot = Paths.get(currentDir).getParent().getParent();
                    Path outputDir = projectRoot.resolve("output");
                    Files.createDirectories(outputDir);
                    return outputDir.resolve(path.getFileName()).toString();
                } else {
                    return path.toAbsolutePath().toString();
                }
            }
            return outputPath;
        }
    }

    /**
     * Call OpenAI API to generate Mermaid code
     */
    private String callOpenAIApi(String description) {
        // Check API configuration
        if (apiKey == null || apiKey.isEmpty() || apiBaseUrl == null || apiBaseUrl.isEmpty() || modelName == null || modelName.isEmpty()) {
            logger.warning("API configuration incomplete, using sample flowchart");
            return generateSampleMermaid(description);
        }

        try {
            String prompt = String.format(DEFAULT_PROMPT_TEMPLATE, description);

            // Build request body
            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> message = new LinkedHashMap<>();
                                message.put("role", "user");
                                message.put("content", prompt);
                                messages.add(message);

            Map<String, Object> requestData = new LinkedHashMap<>();
                                requestData.put("model", modelName);
                                requestData.put("messages", messages);
                                requestData.put("temperature", 0.1);
                                requestData.put("max_tokens", 2000);
                                requestData.put("stream", false);

            // Send HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJSONString(requestData)))
                    .build();

            logger.info("Calling OpenAI compatible API to generate flowchart code...");
            logger.info("Request URL: " + apiBaseUrl + "/chat/completions");
            logger.info("Request model: " + modelName);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("API response status: " + response.statusCode());

            if (response.statusCode() != 200) {
                logger.warning("API request failed, status code: " + response.statusCode());
                logger.warning("Response content: " + response.body());
                return generateSampleMermaid(description);
            }

            // Parse response
            Map<String, Object> result = JsonUtils.readValue(response.body(), Map.class);

            if (result.containsKey("choices") && ((List<?>) result.get("choices")).size() > 0) {
                Map<String, Object> choice = (Map<String, Object>) ((List<?>) result.get("choices")).get(0);
                Map<String, Object> messageData = (Map<String, Object>) choice.get("message");
                String content = (String) messageData.get("content");

                logger.info("API call successful, content length: " + content.length());

                // Extract Mermaid code
                String mermaidCode = extractMermaidCode(content);
                if (mermaidCode == null || mermaidCode.isEmpty()) {
                    logger.warning("Failed to extract valid Mermaid code from API response, will use sample flowchart");
                    return generateSampleMermaid(description);
                }

                logger.info("Successfully extracted Mermaid code");
                return mermaidCode;
            } else {
                logger.warning("Unrecognized API response format: " + result);
                return generateSampleMermaid(description);
            }

        } catch (Exception e) {
            logger.severe("Error calling API: " + e.getMessage());
            return generateSampleMermaid(description);
        }
    }

    /**
     * Extract Mermaid code from API response
     */
    private String extractMermaidCode(String content) {
        // Try to extract ```mermaid ... ``` format code block
        if (content.contains("```mermaid") && content.contains("```")) {
            int startIndex = content.indexOf("```mermaid") + 11;
            int endIndex = content.indexOf("```", startIndex);
            if (endIndex > startIndex) {
                return content.substring(startIndex, endIndex).trim();
            }
        }

        // If no explicit markers, try to extract parts that look like Mermaid code
        Pattern pattern = Pattern.compile("(?i)(graph\\s+|flowchart\\s+|sequencediagram|classDiagram).*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String[] lines = content.split("\n");
            int startIdx = -1;
            int endIdx = -1;

            for (int i = 0; i < lines.length; i++) {
                if (startIdx == -1 && lines[i].toLowerCase().matches(".*(graph\\s+|flowchart\\s+|sequencediagram|classDiagram).*")) {
                    startIdx = i;
                } else if (startIdx != -1 && lines[i].trim().isEmpty() && i > startIdx + 3) {
                    endIdx = i;
                    break;
                }
            }

            if (startIdx != -1) {
                if (endIdx == -1) endIdx = lines.length;
                return String.join("\n", Arrays.copyOfRange(lines, startIdx, endIdx)).trim();
            }
        }

        return null;
    }

    /**
     * Generate sample Mermaid flowchart
     */
    private String generateSampleMermaid(String description) {
        List<String> steps = parseDescriptionToSteps(description);

        if (steps.isEmpty()) {
            // Return default software development flowchart
            return """
                flowchart TD
                    A[需求分析] --> B[系统设计]
                    B --> C[技术选型]
                    C --> D[架构设计]
                    D --> E[编码实现]
                    E --> F[单元测试]
                    F --> G[集成测试]
                    G --> H{测试通过？}
                    H -->|是 | I[代码审查]
                    H -->|否 | E
                    I --> J[部署准备]
                    J --> K[生产部署]
                    K --> L[监控运维]
                    L --> M[用户反馈]
                    M --> N{需要优化？}
                    N -->|是 | A
                    N -->|否 | O[项目完成]
                    
                    style A fill:#e1f5fe
                    style O fill:#c8e6c9
                    style H fill:#fff3e0
                    style N fill:#fff3e0""";
        }

        return generateFlowchartFromSteps(steps);
    }

    /**
     * Parse description text and extract process steps
     */
    private List<String> parseDescriptionToSteps(String description) {
        if (description == null || description.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> steps = new ArrayList<>();

        // Priority method: Identify numbered format step descriptions
        String[] numberedPatterns = {
                "(?:^|\\n)\\s*(\\d+)[.、]\\s*([^\\n\\d]+?)(?=\\s*\\d+[.、]|\\s*$)",
                "(?:^|\\n)\\s*第 ([一二三四五六七八九十]+) 步 [：:]?\\s*([^\\n]+?)(?=\\s*第 [一二三四五六七八九十]+ 步 |\\s*$)",
                "(?:^|\\n)\\s*步骤 (\\d+)[：:]?\\s*([^\\n]+?)(?=\\s*步骤\\d+|\\s*$)"
        };

        for (String patternStr : numberedPatterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(description);

            if (matcher.find()) {
                matcher.reset();
                while (matcher.find()) {
                    if (matcher.groupCount() >= 2) {
                        String stepDesc = matcher.group(2).trim();
                        stepDesc = stepDesc.replaceAll("[，。；：]$", "");
                        stepDesc = stepDesc.replaceAll("^[：:]", "");
                        if (!stepDesc.isEmpty()) {
                            steps.add(stepDesc);
                        }
                    }
                }

                if (!steps.isEmpty()) {
                    return cleanAndStandardizeSteps(steps);
                }
            }
        }

        // Method 1: Split by arrow
        if (description.contains("→") || description.contains("->")) {
            String text = description.replace("→", "->").replace(" ", "");
            String[] parts = text.split("->");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    steps.add(part.trim());
                }
            }
        }
        // Method 2: Split by Chinese punctuation
        else if (description.matches(".*[，、；和].*")) {
            String[] parts = description.split("[，、；和]");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    steps.add(part.trim());
                }
            }
        }
        // 方法 3: 智能识别关键词
        else {
            Map<String, String> commonKeywords = new LinkedHashMap<>();
            commonKeywords.put("开始", "开始");
            commonKeywords.put("启动", "启动");
            commonKeywords.put("初始化", "初始化");
            commonKeywords.put("需求", "需求分析");
            commonKeywords.put("分析", "需求分析");
            commonKeywords.put("设计", "系统设计");
            commonKeywords.put("架构", "架构设计");
            commonKeywords.put("编码", "编码实现");
            commonKeywords.put("开发", "开发实现");
            commonKeywords.put("实现", "功能实现");
            commonKeywords.put("测试", "测试验证");
            commonKeywords.put("验证", "验证确认");
            commonKeywords.put("部署", "系统部署");
            commonKeywords.put("发布", "系统发布");
            commonKeywords.put("上线", "系统上线");
            commonKeywords.put("监控", "监控运维");
            commonKeywords.put("维护", "系统维护");
            commonKeywords.put("审核", "审核确认");
            commonKeywords.put("审批", "审批处理");
            commonKeywords.put("检查", "检查验证");
            commonKeywords.put("确认", "确认处理");
            commonKeywords.put("完成", "流程完成");
            commonKeywords.put("结束", "结束");

            for (Map.Entry<String, String> entry : commonKeywords.entrySet()) {
                if (description.contains(entry.getKey())) {
                    steps.add(entry.getValue());
                }
            }

            if (steps.isEmpty()) {
                steps.add("开始");
                steps.add("处理");
                steps.add("结束");
            }
        }

        return cleanAndStandardizeSteps(steps);
    }

    /**
     * 清理和标准化步骤名称
     */
    private List<String> cleanAndStandardizeSteps(List<String> steps) {
        List<String> cleanedSteps = new ArrayList<>();

        for (String step : steps) {
            // 移除多余的词汇和标点
            step = step.replaceAll("^(第\\d+ 步 | 步骤\\d+|阶段\\d+)[：:]?", "");
            step = step.replaceAll("[：:，。；]$", "");
            step = step.replaceAll("^[：:]", "");
            step = step.trim();

            // 标准化常见步骤名称
            Map<String, String> standardizations = new HashMap<>();
            standardizations.put("需求", "需求分析");
            standardizations.put("分析", "需求分析");
            standardizations.put("设计", "系统设计");
            standardizations.put("编码", "编码实现");
            standardizations.put("开发", "开发实现");
            standardizations.put("测试", "测试验证");
            standardizations.put("部署", "系统部署");
            standardizations.put("发布", "系统发布");

            if (standardizations.containsKey(step)) {
                step = standardizations.get(step);
            }

            if (!step.isEmpty()) {
                cleanedSteps.add(step);
            }
        }

        return cleanedSteps.isEmpty() ? Arrays.asList("开始", "处理", "结束") : cleanedSteps;
    }

    /**
     * 判断步骤是否为决策节点
     */
    private boolean isDecisionNode(String step) {
        return step.contains("?") ||
                step.contains("是否") ||
                step.contains("检查") ||
                step.contains("判断") ||
                step.contains("确认") ||
                step.contains("审核") ||
                step.contains("验证") ||
                step.contains("测试");
    }

    /**
     * 根据步骤列表生成 Mermaid 流程图代码
     */
    private String generateFlowchartFromSteps(List<String> steps) {
        if (steps.isEmpty()) {
            return "";
        }

        StringBuilder flowchart = new StringBuilder("flowchart TD\n");

        // 处理单步骤情况
        if (steps.size() == 1) {
            String step = steps.get(0);
            if (isDecisionNode(step)) {
                flowchart.append("    A{").append(step).append("}\n");
            } else {
                flowchart.append("    A[").append(step).append("]\n");
            }
            flowchart.append("    style A fill:#e1f5fe\n");
            return flowchart.toString();
        }

        // 多步骤处理
        String firstStep = steps.get(0);
        String lastStep = steps.get(steps.size() - 1);

        boolean hasStart = firstStep.toLowerCase().contains("开始") ||
                firstStep.toLowerCase().contains("启动") ||
                firstStep.toLowerCase().contains("start") ||
                firstStep.toLowerCase().contains("初始");

        boolean hasEnd = lastStep.toLowerCase().contains("结束") ||
                lastStep.toLowerCase().contains("完成") ||
                lastStep.toLowerCase().contains("结果") ||
                lastStep.toLowerCase().contains("end") ||
                lastStep.toLowerCase().contains("完毕");

        // 生成节点连接
        for (int i = 0; i < steps.size(); i++) {
            char currentId = (char) ('A' + i);
            String step = steps.get(i);

            // 第一个步骤：从开始节点连接
            if (i == 0 && !hasStart) {
                if (isDecisionNode(step)) {
                    flowchart.append("    Start([开始]) --> ").append(currentId).append("{").append(step).append("}\n");
                } else {
                    flowchart.append("    Start([开始]) --> ").append(currentId).append("[").append(step).append("]\n");
                }
            }

            // 中间步骤连接
            if (i < steps.size() - 1) {
                char nextId = (char) ('A' + i + 1);
                String nextStep = steps.get(i + 1);

                String currentNode = isDecisionNode(step) ?
                        currentId + "{" + step + "}" :
                        currentId + "[" + step + "]";

                String nextNode = isDecisionNode(nextStep) ?
                        nextId + "{" + nextStep + "}" :
                        nextId + "[" + nextStep + "]";

                // 如果当前是决策节点，可能需要添加条件标签
                if (isDecisionNode(step)) {
                    if (nextStep.contains("通过") || nextStep.contains("成功") ||
                            nextStep.contains("是") || nextStep.contains("正确")) {
                        flowchart.append("    ").append(currentNode).append(" -->|是 | ").append(nextNode).append("\n");
                    } else if (nextStep.contains("失败") || nextStep.contains("否") ||
                            nextStep.contains("错误") || nextStep.contains("拒绝")) {
                        flowchart.append("    ").append(currentNode).append(" -->|否 | ").append(nextNode).append("\n");
                    } else {
                        flowchart.append("    ").append(currentNode).append(" --> ").append(nextNode).append("\n");
                    }
                } else {
                    flowchart.append("    ").append(currentNode).append(" --> ").append(nextNode).append("\n");
                }
            }

            // 最后一个步骤：连接到结束节点
            if (i == steps.size() - 1 && !hasEnd) {
                String currentNode = isDecisionNode(step) ?
                        currentId + "{" + step + "}" :
                        currentId + "[" + step + "]";
                flowchart.append("    ").append(currentNode).append(" --> End([完成])\n");
            }
        }

        // 添加样式
        if (!hasStart) {
            flowchart.append("    style Start fill:#c8e6c9\n");
        } else {
            flowchart.append("    style A fill:#c8e6c9\n");
        }

        if (!hasEnd) {
            flowchart.append("    style End fill:#e3f2fd\n");
        } else {
            char lastId = (char) ('A' + steps.size() - 1);
            flowchart.append("    style ").append(lastId).append(" fill:#e3f2fd\n");
        }

        // 决策节点样式
        for (int i = 0; i < steps.size(); i++) {
            if (isDecisionNode(steps.get(i))) {
                char nodeId = (char) ('A' + i);
                flowchart.append("    style ").append(nodeId).append(" fill:#fff3e0\n");
            }
        }

        return flowchart.toString();
    }

    /**
     * 创建包含 Mermaid 流程图的 HTML 文件
     */
    private boolean createHtmlWithMermaid(String mermaidCode, String outputPath) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>交互式 Mermaid 流程图编辑器</title>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js\"></script>\n" +
                "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.js\"></script>\n" +
                "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/javascript/javascript.min.js\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.css\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/theme/dracula.min.css\">\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n" +
                "        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
                "        h1, h2 { color: #333; text-align: center; }\n" +
                "        .editor-container { display: flex; flex-wrap: wrap; gap: 20px; margin: 20px 0; }\n" +
                "        .editor-panel { flex: 1; min-width: 300px; }\n" +
                "        .preview-panel { flex: 1; min-width: 300px; border: 1px solid #ddd; border-radius: 4px; padding: 10px; background-color: white; }\n" +
                "        .CodeMirror { height: 400px; border: 1px solid #ddd; border-radius: 4px; }\n" +
                "        .button-group { margin: 20px 0; text-align: center; }\n" +
                "        button { background-color: #4CAF50; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; margin: 0 5px; font-size: 14px; }\n" +
                "        button:hover { background-color: #45a049; }\n" +
                "        .mermaid { display: flex; justify-content: center; margin: 20px 0; min-height: 200px; border: 1px solid #eee; padding: 10px; border-radius: 4px; background-color: #fafafa; }\n" +
                "        .footer { margin-top: 30px; text-align: center; color: #666; font-size: 0.9em; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>交互式 Mermaid 流程图编辑器</h1>\n" +
                "        <div class=\"editor-container\">\n" +
                "            <div class=\"editor-panel\">\n" +
                "                <h2>Mermaid 代码编辑器</h2>\n" +
                "                <textarea id=\"code-editor\">" + escapeHtml(mermaidCode) + "</textarea>\n" +
                "            </div>\n" +
                "            <div class=\"preview-panel\">\n" +
                "                <h2>实时预览</h2>\n" +
                "                <div id=\"preview\" class=\"mermaid\">\n" + mermaidCode + "\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"button-group\">\n" +
                "            <button onclick=\"updatePreview()\">更新预览</button>\n" +
                "            <button onclick=\"exportSVG()\">导出 SVG</button>\n" +
                "            <button onclick=\"exportPNG()\">导出 PNG</button>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>由 OxyGent 和 Mermaid.js 提供支持 | " + timestamp + "</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        mermaid.initialize({ startOnLoad: true, theme: 'default', securityLevel: 'loose', flowchart: { htmlLabels: true, curve: 'basis' }});\n" +
                "        var editor = CodeMirror.fromTextArea(document.getElementById('code-editor'), { mode: 'javascript', theme: 'dracula', lineNumbers: true });\n" +
                "        function updatePreview() {\n" +
                "            const code = editor.getValue();\n" +
                "            const previewDiv = document.getElementById('preview');\n" +
                "            previewDiv.textContent = code;\n" +
                "            setTimeout(() => { mermaid.init(undefined, '#preview'); }, 300);\n" +
                "        }\n" +
                "        function exportSVG() {\n" +
                "            const svgElement = document.querySelector('.mermaid svg');\n" +
                "            if (svgElement) {\n" +
                "                const svgData = new XMLSerializer().serializeToString(svgElement);\n" +
                "                const blob = new Blob([svgData], {type: 'image/svg+xml;charset=utf-8'});\n" +
                "                const url = URL.createObjectURL(blob);\n" +
                "                const link = document.createElement('a');\n" +
                "                link.href = url;\n" +
                "                link.download = 'flowchart.svg';\n" +
                "                link.click();\n" +
                "            }\n" +
                "        }\n" +
                "        function exportPNG() {\n" +
                "            const svgElement = document.querySelector('.mermaid svg');\n" +
                "            if (svgElement) {\n" +
                "                const canvas = document.createElement('canvas');\n" +
                "                const ctx = canvas.getContext('2d');\n" +
                "                const svgData = new XMLSerializer().serializeToString(svgElement);\n" +
                "                const img = new Image();\n" +
                "                img.onload = function() {\n" +
                "                    canvas.width = img.width;\n" +
                "                    canvas.height = img.height;\n" +
                "                    ctx.drawImage(img, 0, 0);\n" +
                "                    const pngUrl = canvas.toDataURL('image/png');\n" +
                "                    const link = document.createElement('a');\n" +
                "                    link.href = pngUrl;\n" +
                "                    link.download = 'flowchart.png';\n" +
                "                    link.click();\n" +
                "                };\n" +
                "                img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        try {
            Files.writeString(Paths.get(outputPath), htmlContent);
            logger.info("已保存流程图到：" + outputPath);
            return true;
        } catch (IOException e) {
            logger.severe("保存 HTML 文件时出错：" + e.getMessage());
            return false;
        }
    }

    /**
     * 在浏览器中打开文件
     */
    private void openInBrowser(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                BrowserOpener.open(file.toURI().toString());
                logger.info("已在浏览器中打开：" + filePath);
            }
        } catch (Exception e) {
            logger.warning("打开浏览器时出错：" + e.getMessage());
        }
    }

    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
