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
package com.jd.oxygent.core.oxygent.utils;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Log processing utility class
 *
 * <h3>Functional Description</h3>
 * <ul>
 *   <li>Provides SSE (Server-Sent Events) log data construction functionality</li>
 *   <li>Supports log formatting for OxyRequest objects</li>
 *   <li>Implements path extraction for nested Map data</li>
 *   <li>Unified log timestamp format processing</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li>SSE log construction: Convert OxyRequest to standardized log format</li>
 *   <li>Message type mapping: Automatic recognition and conversion of message types</li>
 *   <li>Path extraction: Support dot-separated nested Map data access</li>
 *   <li>Time formatting: Use nanosecond precision timestamps</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li>Real-time log streaming: Build SSE format log data</li>
 *   <li>Request tracking: Record OxyRequest processing flow</li>
 *   <li>System monitoring: Generate structured monitoring logs</li>
 *   <li>Debug analysis: Provide detailed execution trace information</li>
 * </ul>
 *
 * <h3>Log Data Structure</h3>
 * <ul>
 *   <li>Contains identification information such as request ID, trace ID</li>
 *   <li>Records caller and callee information</li>
 *   <li>Supports message type and subtype classification</li>
 *   <li>Contains shared data and group data</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class LogUtils {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    /**
     * Build SSE (Server-Sent Events) log data mapping table
     * <p>
     * Converts OxyRequest objects and message information into standardized SSE log format.
     * Generated log data contains complete request context and message content, suitable for real-time log streaming.
     *
     * <h4>Message Type Mapping</h4>
     * <ul>
     *   <li>"think" → record_type: "think", message_sub_type: "reasoning"</li>
     *   <li>"input"/"query" → record_type: "input", message_sub_type: null</li>
     *   <li>"todolist" → record_type: "output", message_sub_type: "task_planning"</li>
     *   <li>"answer" → record_type: "output", message_sub_type: "final_response"</li>
     *   <li>Others → record_type: "output", message_sub_type: null</li>
     * </ul>
     *
     * <h4>Included Log Fields</h4>
     * <ul>
     *   <li>Identification info: case_id, request_id, message_id, etc.</li>
     *   <li>Tracking info: current_trace_id, from_trace_id, call_stack, etc.</li>
     *   <li>Call info: caller, callee, caller_category, callee_category</li>
     *   <li>Message info: message, content, message_type, message_sub_type</li>
     *   <li>Context info: shared_data, group_data, arguments</li>
     *   <li>Time info: create_time (nanosecond precision)</li>
     * </ul>
     *
     * @param oxyRequest  OxyRequest object containing request context information, cannot be null
     * @param messageType Message type identifier used to determine record_type and message_sub_type
     * @param message     Message content object, can be any type of data
     * @return Map&lt;String, Object&gt; Standardized SSE log data mapping table
     */
    public static Map<String, Object> buildSseLog(OxyRequest oxyRequest, String messageType, Object message) {
        var arguments = oxyRequest.getArguments();

        var logData = new HashMap<String, Object>();
        var content = new HashMap<String, Object>();

        logData.put("type", messageType);
        logData.put("content", content);

        content.put("case_id", oxyRequest.getRequestId());
        content.put("event", "message");
        content.put("group_id", oxyRequest.getGroupId());
        content.put("current_trace_id", oxyRequest.getCurrentTraceId());
        content.put("from_trace_id", oxyRequest.getFromTraceId());
        content.put("request_id", oxyRequest.getRequestId());
        content.put("caller", oxyRequest.getCaller());
        content.put("callee", oxyRequest.getCallee());
        content.put("caller_category", oxyRequest.getCallerCategory());
        content.put("callee_category", oxyRequest.getCalleeCategory());
        content.put("call_stack", oxyRequest.getCallStack());
        content.put("seq", arguments.getOrDefault("seq", 1));
        content.put("record_type", switch (messageType) {
            case "think" -> "think";
            case "input", "query" -> "input";
            default -> "output";
        });
        content.put("message_id", UUID.randomUUID().toString());
        content.put("message", message);
        content.put("query", oxyRequest.getQuery());
        content.put("content", message);
        content.put("message_type", messageType);
        content.put("message_sub_type", switch (messageType) {
            case "think" -> "reasoning";
            case "todolist" -> "task_planning";
            case "answer" -> "final_response";
            default -> null;
        });
        content.put("regenerate_flag", 0);
        content.put("shared_data", oxyRequest.getSharedData());
        content.put("group_data", oxyRequest.getGroupData());
        content.put("arguments", oxyRequest.getArguments());
        content.put("create_time", LocalDateTime.now().format(formatter));

        return logData;
    }

    /**
     * Extract value from nested Map by path
     * <p>
     * Supports extracting values from multi-level nested Map structures using dot-separated paths.
     * Similar to JSON path access, provides safe deep data extraction functionality.
     *
     * <h4>Path Format</h4>
     * <ul>
     *   <li>Use dot (.) to separate nested levels</li>
     *   <li>For example: "user.profile.name" represents map.get("user").get("profile").get("name")</li>
     *   <li>Supports nested access of arbitrary depth</li>
     * </ul>
     *
     * <h4>Safety</h4>
     * <ul>
     *   <li>Returns default value when path does not exist, no exception thrown</li>
     *   <li>Returns default value when intermediate node is null</li>
     *   <li>Returns default value when intermediate node is not Map type</li>
     * </ul>
     *
     * @param map          Source data mapping table, may contain nested structure
     * @param path         Dot-separated access path, such as "level1.level2.field"
     * @param defaultValue Default return value when path does not exist or access fails
     * @return String String representation of extracted value, returns default value on failure
     */
    private static String extractFromNested(Map<String, Object> map, String path, String defaultValue) {
        var parts = path.split("\\.");
        Object current = map;

        for (var part : parts) {
            if (current instanceof Map<?, ?> currentMap) {
                current = currentMap.get(part);
            } else {
                return defaultValue;
            }
        }

        return current != null ? current.toString() : defaultValue;
    }

    /**
     * Control log colors
     */
    public static final String ANSI_RESET_ALL = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_ORANGE = "\u001B[33m";
    public static final String ANSI_GRAY = "\u001B[90m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_ITALIC = "\u001B[3m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";
    public static final String ANSI_INVERSE = "\u001B[7m";
    public static final String ANSI_STRIKETHROUGH = "\u001B[9m";
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_MAGENTA_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    public static final String ANSI_RESET_BACKGROUND = "\u001B[49m";
    public static final String ANSI_BOLD_ON = "\u001B[1m";
    public static final String ANSI_BOLD_OFF = "\u001B[22m";

    public static void main(String[] args) {
        System.out.println("Here's some text");
        System.out.println(ANSI_ORANGE + "and now the text is yellow" + ANSI_RESET_ALL);
        System.out.println(ANSI_CYAN + "and now the text is cyan" + ANSI_RESET_ALL);
        System.out.println("and now back to the default");
    }
}
