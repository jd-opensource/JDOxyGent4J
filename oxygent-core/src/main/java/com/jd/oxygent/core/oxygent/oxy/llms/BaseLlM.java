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
package com.jd.oxygent.core.oxygent.oxy.llms;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.infra.multimodal.MultimodalResourceType;
import com.jd.oxygent.core.oxygent.utils.*;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static com.jd.oxygent.core.oxygent.utils.CommonUtils.getFormatTime;
import static com.jd.oxygent.core.oxygent.utils.ResourceUrlDetectorUtil.extractPathsFromStructuredString;

/**
 * Large Language Model Base Class - Abstract base class for all LLM implementations in OxyGent framework
 *
 * <p>BaseLlM is the core base class for large language model integration in the OxyGent agent framework, providing
 * unified interfaces and common functionality for various LLM services. This class encapsulates common logic for LLM
 * interaction, including multimodal processing, message management, and chain of thought extraction.</p>
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Multimodal Support</strong>: Handle various input types including text, images, videos</li>
 *   <li><strong>Message Management</strong>: Standardized message format conversion and processing</li>
 *   <li><strong>Chain of Thought Extraction</strong>: Automatically identify and forward LLM's thinking process</li>
 *   <li><strong>Media Conversion</strong>: Support automatic URL to Base64 conversion</li>
 *   <li><strong>Error Handling</strong>: User-friendly error messages and exception handling</li>
 *   <li><strong>Performance Control</strong>: Timeout management and resource limitations</li>
 * </ul>
 *
 * <h3>Multimodal Processing:</h3>
 * <p>Supports intelligent processing of various input modes:</p>
 * <ul>
 *   <li>Text Input: Standard text message processing</li>
 *   <li>Image Input: Support for URL and Base64 format images</li>
 *   <li>Video Input: Video file processing and conversion</li>
 *   <li>Mixed Input: Combined processing of text and media</li>
 * </ul>
 *
 * <h3>Chain of Thought Mechanism:</h3>
 * <p>Automatically identify and process LLM's thinking process:</p>
 * <ul>
 *   <li>XML Format: Thinking content enclosed in &lt;think&gt;...&lt;/think&gt; tags</li>
 *   <li>JSON Format: Structured thinking containing "think" field</li>
 *   <li>Real-time Forwarding: Push thinking process to frontend display in real-time</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class CustomLlM extends BaseLlM {
 *     public CustomLlM() {
 *         super();
 *         setIsMultimodalSupported(true);
 *         setTimeout(120.0);
 *     }
 *
 *     @Override
 *     protected OxyResponse _execute(OxyRequest request) {
 *         // Implement specific LLM calling logic
 *         return callLlmService(request);
 *     }
 * }
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseLlM extends BaseOxy {
    private static final Logger logger = Logger.getLogger(BaseLlM.class.getName());

    /**
     * LLM category identifier
     *
     * <p>Used to identify this component as a large language model type, facilitating system recognition and routing.
     * The default category for all LLM implementations is "llm".</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected String category = "llm";

    /**
     * Request timeout (seconds)
     *
     * <p>Maximum waiting time for LLM requests. Since large language model inference
     * may require a long time, the default is set to 300 seconds (5 minutes).</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected double timeout = 300.0;

    /**
     * LLM parameter configuration
     *
     * <p>Parameter mapping passed to LLM service, such as temperature,
     * max_tokens, top_p and other model parameters.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected Map<String, Object> llmParams = new HashMap<>();

    /**
     * Whether to send chain of thought messages
     *
     * <p>Controls whether to send LLM's thinking process to frontend display.
     * When enabled, users can see the AI's reasoning process.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected boolean isSendThink = Config.getMessage().isSendThink();

    /**
     * Friendly error message text
     *
     * <p>User-friendly error message displayed when LLM call fails.
     * Avoids exposing technical details and sensitive information to users.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    @ExcludeOption
    protected String friendlyErrorText = "Sorry, I encountered some issues. Please try again later.";

    /**
     * Whether multimodal input is supported
     *
     * <p>Identifies whether this LLM supports processing input types other than text,
     * such as images, videos, audio and other multimedia content.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected boolean isMultimodalSupported = false;

    @Builder.Default
    protected boolean isDisableSystemPrompt = false;

    /**
     * Whether to convert URLs to Base64
     *
     * <p>Controls whether to automatically convert image or video URLs to Base64 format.
     * Some LLM services require media content to be provided in Base64 format.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected boolean isConvertUrlToBase64 = false;

    /**
     * Maximum image pixels
     *
     * <p>Maximum pixel count limit allowed for a single image, used to control
     * image size and processing cost. Default is 10 million pixels.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected int maxImagePixels = 10_000_000;

    /**
     * Maximum video file size (bytes)
     *
     * <p>Maximum allowed size for video files, used to prevent oversized files
     * from causing processing timeouts or resource exhaustion. Default is 12MB.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected int maxVideoSize = 12 * 1024 * 1024;

    /**
     * Maximum non-media file size (bytes)
     *
     * <p>Maximum size limit for non-media files used for Base64 embedding.
     * Mainly used for size control of documents, text files, etc. Default is 2MB.</p>
     *
     * @since 1.0.0
     */
    @Builder.Default
    protected long maxFileSizeBytes = 2 * 1024 * 1024;

    /**
     * Preprocess multimodal input messages
     *
     * <p>Converts messages in OxyRequest to the standard format required by LLM services. This method handles various types of input,
     * including plain text, multimodal content (images, videos) and structured data.</p>
     *
     * <h4>Processing Logic:</h4>
     * <ul>
     *   <li>Extract message list from request</li>
     *   <li>Classify processing based on roles (user, assistant, system)</li>
     *   <li>Maintain original structure of multimodal content</li>
     *   <li>Convert plain text content to string format</li>
     * </ul>
     *
     * <h4>Multimodal Support:</h4>
     * <p>When multimodal support is enabled, this method will:</p>
     * <ul>
     *   <li>Identify image and video URLs</li>
     *   <li>Convert URLs to Base64 format based on configuration</li>
     *   <li>Validate media file size and format limitations</li>
     *   <li>Handle composite content mixing text and media</li>
     * </ul>
     *
     * @param oxyRequest Request object containing message data
     * @return Converted message list, each message contains role and content fields
     * @throws NullPointerException     Thrown when oxyRequest is null
     * @throws IllegalArgumentException Thrown when message format is incorrect
     * @since 1.0.0
     */
    protected List<Map<String, Object>> _getMessages(OxyRequest oxyRequest) {
        Memory memory = (Memory) oxyRequest.getArguments().get("messages");
        List<Message> messages = memory.getMessages();

        try {
            // merge system prompt
            if (isDisableSystemPrompt &&
                    !messages.isEmpty() && "system".equalsIgnoreCase(messages.get(0).getRole().name())) {

                // 拼接 system prompt 和 user message
                String systemContent = messages.get(0).getContent().toString();
                String userContent = messages.get(1).getContent().toString();
                String combinedContent = systemContent + "\nUser Input: " + userContent;

                // 更新第二条消息（用户消息）的内容
                messages.get(1).setContent(combinedContent);
                // 移除第一条 system 消息
                messages = messages.subList(1, messages.size());
                // 更新 arguments 中的 messages（注意：subList 是视图，若需要独立副本可 new ArrayList<>(...)）
                oxyRequest.getArguments().put("messages", new ArrayList<>(messages));
            }

            // Preprocess messages for multimodal input
            if (!isMultimodalSupported) {
                List<Map<String, Object>> messagesProcessed = new ArrayList<>();
                for (Message message : messages) {
                    Map<String, Object> processedMessage = new HashMap<>();
                    processedMessage.put("role", message.getRole().name().toLowerCase());
                    processedMessage.put("content", message.getContent());
                    messagesProcessed.add(processedMessage);
                }
                return messagesProcessed;
            }

            List<Map<String, Object>> messagesTemp = new ArrayList<>();

            for (Message message : messages) {
                String role = message.getRole().name().toLowerCase();
                Object content = message.getContent();

                List<Map<String, Object>> contentItems = new ArrayList<>();
                if ("user".equals(role)) {
                    if (content instanceof List<?>) {
                        for (Map<String, Object> part : (List<Map<String, Object>>) content) {

                            Map<String, Object> tmp = new HashMap<>(part);
                            if (tmp.get("part") != null) {
                                Map<String, String> temp = (Map<String, String>) tmp.get("part");
                                String contentType = temp.get("content_type");
                                String data = temp.get("data");
                                if (contentType.equals("path") || contentType.equals("url")) {
                                    tmp.put("file_path", data);
                                    tmp.put("type", "file");
                                } else if (contentType.equals("text/plain")) {
                                    tmp.put("text", data);
                                    tmp.put("type", "text");
                                }
                            }
                            if ("text".equals(tmp.get("type"))) {
                                Object result = processMultipartTextContent(tmp.getOrDefault("text", "").toString(), messagesTemp);
                                if (result instanceof String) {
                                    contentItems.add(new HashMap<>(Map.of("type", "text", "text", result)));
                                } else if (result instanceof List) {
                                    logger.info(JsonUtils.toJSONString(result));
                                    contentItems.addAll((List<Map<String, Object>>) result);
                                }
                            } else if ("file".equals(tmp.get("type")) && tmp.containsKey("file_path")) {
                                // Apply a patch, waiting for local_agent's multimodal capability to be migrated
                                Map<String, List<?>> multimodalPart = new HashMap<>();
                                multimodalPart.put("files", Arrays.asList(tmp.get("file_path")));
                                List<Map<String, Object>> processedResources = processMultimodalResources(multimodalPart, isConvertUrlToBase64);
                                contentItems.addAll(processedResources);
//                                contentItems.add(new HashMap<>(Map.of("type", "image_url", "image_url", new HashMap<>(Map.of("url", tmp.get("file_path"))))));
                            } else {
                                contentItems.add(tmp);
                            }
                        }
                    } else if (content instanceof String && ResourceUrlDetectorUtil.containsResourceUrl((String) content)) {
                        // When user passes one or more path/url through text, first parse out path/url, then convert to base64 based on flag isConvertUrlToBase64
                        Map<String, List<?>> multimodalPart = new HashMap<>();
                        multimodalPart.put("files", ResourceUrlDetectorUtil.extractPathsFromStructuredString((String) content));
                        List<Map<String, Object>> processedResources = processMultimodalResources(multimodalPart, isConvertUrlToBase64);
                        if (!processedResources.isEmpty()) {
                            contentItems.addAll(processedResources);
                        }

                        // Also store query in history
                        contentItems.add(new HashMap<>(Map.of("type", "text", "text", (String) content)));
                    }
                }

                Map<String, Object> processedMessage = new HashMap<>();
                processedMessage.put("role", role);
                processedMessage.put("content", contentItems.isEmpty() ? content : contentItems);
                messagesTemp.add(processedMessage);
            }

            // Hold URL if conversion is disabled
            if (!isConvertUrlToBase64) {
                return messagesTemp;
            }

            return messagesTemp;


        } catch (Exception e) {
            logger.warning("Error processing messages: " + e.getMessage());
            throw new CompletionException(e);
        }
    }

    /**
     * Processing scenarios:
     * Case 1: Simple image link
     * String content = "Please view this image ![Product Image](https://jd.com/product.png)";
     * // Output: [{type: "text", content: "Please view this image "},
     * //        {type: "image_url", content: "![Product Image](https://jd.com/product.png)", desc: "Product Image", link: "https://jd.com/product.png"}]
     * Case 2: Multiple mixed media content
     * String content = "Analyze documents and charts: [Technical Document](./docs/spec.pdf) and ![Architecture Diagram](https://jd.com/diagram.png)";
     * // Output contains 4 elements: text, doc_url, text, image_url
     * Case 3: Video file
     * String content = "Watch demo video ![Demo Video](demo.mp4)";
     * // Output: [{type: "video_url", content: "![Demo Video](demo.mp4)", desc: "Demo Video", link: "demo.mp4"}]
     * Case 4: Pure text content
     * String content = "This is a plain text message";
     * // Output: [{type: "text", content: "This is a plain text message"}]
     * Case 5: Complex mixed content
     * String content = "Please analyze the following materials:\n\nDocument: [API Documentation](./api-docs.pdf)\nImage: ![Flowchart](workflow.png)\nVideo: ![Demo](tutorial.mp4)\n\nThank you!";
     * // Output contains: text, doc_url, text, image_url, text, video_url, text
     */
    private Object processMultipartTextContent(String content, List<Map<String, Object>> messagesTemp) {
        // Parse text
        List<Map<String, Object>> items = CommonUtils.parseMixedString(content);

        // Replace file URLs with text content
        List<Integer> docUrlIndices = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            if ("doc_url".equals(item.get("type"))) {
                docUrlIndices.add(i);
            }
        }

        for (int i : docUrlIndices) {
            Map<String, Object> item = items.get(i);
            String itemLink = item.getOrDefault("link", "").toString();
            Path filePath = Paths.get(itemLink);

            if (Files.exists(filePath)) {
                try {
                    String docContent = Files.readString(filePath);
                    items.set(i, Map.of(
                            "type", "text",
                            "content", "The content of the `" + item.get("desc") + "` is: " + docContent + " --- "
                    ));
                } catch (IOException e) {
                    logger.warning("Failed to read file: " + itemLink);
                    items.set(i, Map.of(
                            "type", "text",
                            "content", item.get("content")
                    ));
                }
            } else {
                items.set(i, Map.of(
                        "type", "text",
                        "content", item.get("content")
                ));
            }
        }

        // Check if it's pure text
        boolean isPureText = items.stream().allMatch(item -> "text".equals(item.get("type")));

        // Concatenate or build structured content
        if (isPureText) {
            return items.stream()
                    .map(item -> item.getOrDefault("content", "").toString())
                    .reduce("", String::concat);
        } else {
            List<Map<String, Object>> structuredContent = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String itemType = item.getOrDefault("type", "").toString();
                if ("text".equals(itemType)) {
                    structuredContent.add(Map.of(
                            "type", itemType,
                            itemType, item.get("content")
                    ));
                } else if ("image_url".equals(itemType) || "video_url".equals(itemType)) {
                    structuredContent.add(Map.of(
                            "type", "text",
                            "text", "The content of the `" + item.get("desc") + "` is: "
                    ));
                    Map<String, String> urlMap = new HashMap<>();
                    urlMap.put("url", item.getOrDefault("link", "").toString());
                    Map<String, Object> mediaItem = new HashMap<>();
                    mediaItem.put("type", itemType);
                    mediaItem.put(itemType, urlMap);
                    structuredContent.add(mediaItem);
                }
            }
            return structuredContent;
        }
    }

    /**
     * Core method for executing LLM requests
     *
     * <p>This is the core method that all LLM implementations must provide, used to handle specific
     * language model invocation logic. Each LLM subclass needs to implement this method according to its specific API
     * and protocol.</p>
     *
     * <h4>Implementation Requirements:</h4>
     * <ul>
     *   <li>Handle preprocessed message formats</li>
     *   <li>Call corresponding LLM service API</li>
     *   <li>Handle API responses and error conditions</li>
     *   <li>Return standardized OxyResponse objects</li>
     * </ul>
     *
     * @param oxyRequest LLM request object containing messages and parameters
     * @return LLM execution response containing generated content and status information
     * @since 1.0.0
     */
    protected abstract OxyResponse _execute(OxyRequest oxyRequest);

    /**
     * Send chain of thought messages after response generation
     *
     * <p>Automatically executed after LLM generates response, used to extract and forward LLM's thinking process
     * to frontend. This method supports recognition and processing of multiple chain of thought formats.</p>
     *
     * <h4>Supported Chain of Thought Formats:</h4>
     * <ul>
     *   <li><strong>XML Format</strong>：&lt;think&gt;thinking content&lt;/think&gt;</li>
     *   <li><strong>JSON Format</strong>：JSON objects containing "think" field</li>
     *   <li><strong>Plain Text</strong>：Fallback processing when format cannot be recognized</li>
     * </ul>
     *
     * <h4>Processing Flow:</h4>
     * <ol>
     *   <li>Check if chain of thought sending functionality is enabled</li>
     *   <li>Extract thinking content from LLM response</li>
     *   <li>Build standardized chain of thought messages</li>
     *   <li>Send to frontend through message channel</li>
     * </ol>
     *
     * @param oxyResponse LLM response object containing generated content
     * @since 1.0.0
     */
    @Override
    protected void postSendMessage(OxyResponse oxyResponse) {
        super.postSendMessage(oxyResponse);

        OxyRequest oxyRequest = oxyResponse.getOxyRequest();
        if (isSendThink) {
            try {
                String msg = "";
                String output = (String) oxyResponse.getOutput();

                if (output != null && output.contains("</think>")) { // Parse
                    msg = output.split("</think>")[0]
                            .replace("<think>", "")
                            .trim();
                } else {
                    Map<String, Object> toolCallDict = extractFirstJson(output);
                    if (toolCallDict != null && toolCallDict.containsKey("think")) {
                        msg = String.valueOf(toolCallDict.get("think")).trim();
                    } else { // Unrecognized format
                        msg = output;
                    }
                }

                if (StringUtils.isNotBlank(msg)) {
                    Map<String, Object> message = new HashMap<>();
                    message.put("type", "think");
                    message.put("current_trace_id", oxyRequest.getCurrentTraceId());
                    message.put("from_trace_id", oxyRequest.getFromTraceId());
                    message.put("group_id", oxyRequest.getGroupId());
                    message.put("request_id", oxyRequest.getRequestId());
                    message.put("caller", oxyRequest.getCaller());
                    message.put("caller_category", oxyRequest.getCallerCategory());
                    message.put("callee", oxyRequest.getCallee());
                    message.put("callee_category", oxyRequest.getCalleeCategory());
                    message.put("content", msg);
                    oxyRequest.sendMessage(message);
                }
            } catch (Exception e) {
                logger.info("Error in postSendMessage: " + e.getMessage() +
                        " trace_id=" + oxyRequest.getCurrentTraceId() +
                        " node_id=" + oxyRequest.getNodeId());
            }
        }
    }

    /**
     * Extract JSON object from text
     *
     * <p>Attempts to parse the first valid JSON object from the given text.
     * This method is primarily used to extract chain of thought or other
     * structured information from LLM's structured responses.</p>
     *
     * <h4>Processing Features:</h4>
     * <ul>
     *   <li>Automatically clean markdown code block markers</li>
     *   <li>Fault-tolerant handling of invalid JSON formats</li>
     *   <li>Return empty Map instead of null to avoid NPE</li>
     * </ul>
     *
     * @param text Text string containing JSON
     * @return Parsed JSON object, returns empty Map if parsing fails
     * @since 1.0.0
     */
    protected Map<String, Object> extractFirstJson(String text) {
        if (StringUtils.isNotBlank(text)) {
            text = CommonUtils.extractFirstJson(text);
            return JsonUtils.readValue(text, Map.class, new HashMap<>());
        }
        return new HashMap<>();
    }

    /**
     * Process multimodal resources
     * Structure of multimodalPart is like: {"urls": [...], "files": [...], ...}
     */
    private List<Map<String, Object>> processMultimodalResources(Map<String, List<?>> multimodalPart, boolean isConvertUrlToBase64) {
        List<Map<String, Object>> processedResources = new ArrayList<>();
        multimodalPart.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .forEach(entry -> {
                    String resourceType = entry.getKey();
                    List<?> resourceList = entry.getValue();

                    MultimodalResourceType processor = MultimodalResourceType.fromResourceType(resourceType);
                    List<Map<String, Object>> processed = processor.processResources(resourceList, isConvertUrlToBase64);
                    processedResources.addAll(processed);
                });

        return processedResources;
    }
}