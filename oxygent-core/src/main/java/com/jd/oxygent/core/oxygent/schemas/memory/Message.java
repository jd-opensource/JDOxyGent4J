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
package com.jd.oxygent.core.oxygent.schemas.memory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;
import java.util.function.Function;

/**
 * Dialogue Message Entity Class
 *
 * <p>This class represents a message in the agent system, supporting multiple role types and message content formats.
 * It is the fundamental data structure for building dialogue history and agent interactions.</p>
 *
 * <p>Supported Message Roles:</p>
 * <ul>
 *     <li>SYSTEM - System message, used to set agent behavior and context</li>
 *     <li>USER - User message, represents user input and requests</li>
 *     <li>ASSISTANT - Assistant message, represents agent replies</li>
 *     <li>TOOL - Tool message, represents tool invocation results</li>
 * </ul>
 *
 * <p>Supported Message Content Formats:</p>
 * <ul>
 *     <li>Plain text string</li>
 *     <li>Structured data list (supports multimodal content)</li>
 *     <li>Tool call information</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>
 * // Create system message
 * Message systemMsg = Message.systemMessage("You are a helpful assistant");
 *
 * // Create user message
 * Message userMsg = Message.userMessage("Please help me solve this problem");
 *
 * // Create tool message
 * Message toolMsg = Message.toolMessage("Execution result", "ToolName", "CallID");
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class Message {

    /**
     * Message Role Enum
     */
    public enum Role {
        /**
         * System role - for system instructions and configuration
         */
        @JsonProperty("system")
        SYSTEM("system", "System"),

        /**
         * User role - for user input and queries
         */
        @JsonProperty("user")
        USER("user", "User"),

        /**
         * Assistant role - for agent replies
         */
        @JsonProperty("assistant")
        ASSISTANT("assistant", "Assistant"),

        /**
         * Tool role - for tool execution results
         */
        @JsonProperty("tool")
        TOOL("tool", "Tool");

        private final String value;
        private final String displayName;

        Role(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Message role
     */
    private Role role;

    /**
     * Message content, supports string or list format
     */
    private Object content;

    /**
     * Tool call list, for tool calls of ASSISTANT role
     */
    private List<ToolCall> toolCalls;

    /**
     * Message name, usually for TOOL role indicating tool name
     */
    private String name;

    /**
     * Tool call ID, for TOOL role to associate with corresponding tool call
     */
    private String toolCallId;

    /**
     * Default constructor
     */
    public Message() {
    }

    /**
     * Constructor with specified role
     *
     * @param role Message role, cannot be null
     * @throws NullPointerException if role is null
     */
    public Message(Role role) {
        this.role = Objects.requireNonNull(role, "Message role cannot be null");
    }

    /**
     * Create user message
     *
     * @param content Message content, can be null
     * @return User message instance
     */
    public static Message userMessage(Object content) {
        var message = new Message(Role.USER);
        message.setContent(content);
        return message;
    }

    /**
     * Create system message
     *
     * @param content System message content, usually a string
     * @return System message instance
     */
    public static Message systemMessage(String content) {
        var message = new Message(Role.SYSTEM);
        message.setContent(content);
        return message;
    }

    /**
     * Create assistant message
     *
     * @param content Assistant reply content
     * @return Assistant message instance
     */
    public static Message assistantMessage(String content) {
        var message = new Message(Role.ASSISTANT);
        message.setContent(content);
        return message;
    }

    /**
     * Create tool message
     *
     * @param content    Tool execution result content
     * @param name       Tool name, cannot be null
     * @param toolCallId Tool call ID, cannot be null
     * @return Tool message instance
     * @throws NullPointerException if name or toolCallId is null
     */
    public static Message toolMessage(String content, String name, String toolCallId) {
        Objects.requireNonNull(name, "Tool name cannot be null");
        Objects.requireNonNull(toolCallId, "Tool call ID cannot be null");

        var message = new Message(Role.TOOL);
        message.setContent(content);
        message.setName(name);
        message.setToolCallId(toolCallId);
        return message;
    }

    /**
     * Convert from list of maps to list of messages
     *
     * <p>Supported map format:</p>
     * <ul>
     *     <li>role: role string (system/user/assistant)</li>
     *     <li>content: message content</li>
     * </ul>
     *
     * @param dictList List of maps, cannot be null
     * @return Converted list of messages
     * @throws NullPointerException if dictList is null
     */
    public static List<Message> dictListToMessages(List<Map<String, Object>> dictList) {
        Objects.requireNonNull(dictList, "Dict list can not be null");

        var lookup = Map.of(
                "system", (Function<Object, Message>) content -> Message.systemMessage((String) content),
                "user", (Function<Object, Message>) Message::userMessage,
                "assistant", (Function<Object, Message>) content -> Message.assistantMessage((String) content)
        );

        var result = new ArrayList<Message>();
        for (var msgDict : dictList) {
            if (msgDict == null) {
                continue;
            }

            var roleStr = msgDict.getOrDefault("role", "").toString();
            var content = msgDict.getOrDefault("content", "").toString();

            if (lookup.containsKey(roleStr) && content != null) {
                result.add(lookup.get(roleStr).apply(content));
            }
        }
        return result;
    }

    /**
     * Convert message to dictionary format
     *
     * @return Dictionary representation of the message
     */
    public Map<String, Object> toDict() {
        var message = new HashMap<String, Object>();

        message.put("role", role.name().toLowerCase());

        if (content != null) {
            message.put("content", content);
        }
        if (toolCalls != null && !toolCalls.isEmpty()) {
            message.put("tool_calls", toolCalls);
        }
        if (name != null && !name.trim().isEmpty()) {
            message.put("name", name);
        }
        if (toolCallId != null && !toolCallId.trim().isEmpty()) {
            message.put("tool_call_id", toolCallId);
        }

        return message;
    }

    /**
     * Check if it is a system message
     *
     * @return true if system message
     */
    public boolean isSystemMessage() {
        return role == Role.SYSTEM;
    }

    /**
     * Check if it is a user message
     *
     * @return true if user message
     */
    public boolean isUserMessage() {
        return role == Role.USER;
    }

    /**
     * Check if it is an assistant message
     *
     * @return Return true if it is an assistant message
     */
    public boolean isAssistantMessage() {
        return role == Role.ASSISTANT;
    }

    /**
     * Check if it is a tool message
     *
     * @return Return true if it is a tool message
     */
    public boolean isToolMessage() {
        return role == Role.TOOL;
    }

    /**
     * Get the string representation of the content
     *
     * @return String representation of the content, empty string if content is null
     */
    public String getContentAsString() {
        return content != null ? content.toString() : "";
    }

    /**
     * Check if the message contains tool calls
     *
     * @return Return true if it contains tool calls
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
