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

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Dialogue Memory Management Class
 *
 * <p>This class is responsible for managing conversation history between agents and users or other agents.
 * It provides functions for message storage, retrieval, and trimming to ensure reasonable memory usage.</p>
 *
 * <p>Main functions include:</p>
 * <ul>
 *     <li>Message history storage and management</li>
 *     <li>Automatic memory trimming based on maximum message count</li>
 *     <li>Support for batch message operations</li>
 *     <li>Provide various data format conversions</li>
 * </ul>
 *
 * <p>Memory trimming strategy:</p>
 * <ul>
 *     <li>When message count exceeds maximum limit, prioritize deleting middle messages</li>
 *     <li>Preserve system messages and latest conversation messages</li>
 *     <li>Ensure conversation coherence and context integrity</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * Memory memory = new Memory(20);
 * memory.addMessage(Message.systemMessage("You are a helpful assistant"));
 * memory.addMessage(Message.userMessage("Hello"));
 * List&lt;Message&gt; recent = memory.getRecentMessages(5);
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class Memory {

    /**
     * Message list, stored in chronological order
     */
    private List<Message> messages = new ArrayList<>();

    /**
     * Maximum message count limit, default is 10
     */
    private int maxMessages = 10;

    /**
     * Default constructor
     *
     * <p>Uses the default maximum message count limit (10 messages)</p>
     */
    public Memory() {
        // Use default value
    }

    /**
     * Constructor with specified maximum message count
     *
     * @param maxMessages Maximum message count, must be greater than 0
     * @throws IllegalArgumentException if maxMessages is less than or equal to 0
     */
    public Memory(int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("Maximum message count must be greater than 0");
        }
        this.maxMessages = maxMessages;
    }

    /**
     * Constructor with initial message list
     *
     * @param messages Initial message list, cannot be null
     * @throws NullPointerException if messages is null
     */
    public Memory(List<Message> messages) {
        this.messages = new ArrayList<>(Objects.requireNonNull(messages, "Message list cannot be null"));
    }

    /**
     * Add a single message
     *
     * @param message Message to add, cannot be null
     * @throws NullPointerException if message is null
     */
    public void addMessage(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        messages.add(message);
    }

    /**
     * Add multiple messages
     *
     * @param messages List of messages to add, cannot be null
     * @throws NullPointerException if messages is null
     */
    public void addMessages(List<Message> messages) {
        Objects.requireNonNull(messages, "Message list cannot be null");
        this.messages.addAll(messages);
    }

    /**
     * Clear all messages
     */
    public void clear() {
        messages.clear();
    }

    /**
     * Get the most recent n messages
     *
     * @param n Number of messages to get, must be >= 0
     * @return List of recent messages; if n is 0 or no messages, returns empty list
     * @throws IllegalArgumentException if n &lt; 0
     */
    public List<Message> getRecentMessages(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Message count cannot be negative");
        }
        if (n == 0 || messages.isEmpty()) {
            return Collections.emptyList();
        }

        var size = messages.size();
        var start = Math.max(0, size - n);
        return new ArrayList<>(messages.subList(start, size));
    }

    /**
     * Get all messages
     *
     * @return Copy of all messages
     */
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Get message count
     *
     * @return Current number of stored messages
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Check if memory is empty
     *
     * @return true if no messages
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * Convert memory messages to a list of maps (dictionary format)
     *
     * <p>Performs memory trimming before conversion to ensure message count is within the limit</p>
     *
     * @return List of message dictionaries
     */
    public List<Map<String, Object>> toDictList() {
        trimMemory();
        return messages.stream()
                .map(Message::toDict)
                .collect(Collectors.toList());
    }

    /**
     * Perform memory trimming operation
     *
     * <p>Trimming strategy:</p>
     * <ul>
     *     <li>Triggered when message count exceeds maximum limit</li>
     *     <li>Deletes middle messages (index 1 and 2) first</li>
     *     <li>Keeps system message (usually at index 0) and latest messages</li>
     *     <li>If only system and one user message remain but still over limit, deletes the last one</li>
     * </ul>
     */
    private void trimMemory() {
        while (messages.size() > maxMessages && messages.size() > 1) {
            if (messages.size() >= 3) {
                // Delete middle messages (index 1 and 2), keep system and latest messages
                messages.remove(1);
                if (messages.size() > maxMessages) {
                    messages.remove(1); // Index shifts after first removal
                }
            } else {
                // If only system and one message remain but still over limit, delete last
                messages.remove(messages.size() - 1);
            }
        }
    }

    /**
     * Set maximum message count and trigger trimming
     *
     * @param maxMessages New maximum message count, must be > 0
     * @throws IllegalArgumentException if maxMessages &lt;= 0
     */
    public void setMaxMessages(int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("Maximum message count must be greater than 0");
        }
        this.maxMessages = maxMessages;
        trimMemory();
    }
}
