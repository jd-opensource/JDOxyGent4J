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

import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-Memory Queue Manager
 *
 * <p>Provides memory-based queue management functionality, simulating Redis queue operations. Supports concurrent access
 * to multiple named queues, with each queue identified by a unique key. This utility class is suitable for local
 * development, testing environments, or scenarios that do not depend on external Redis.</p>
 *
 * <p>Main Features:</p>
 * <ul>
 *     <li>Queue Management: Supports creation and management of multiple named queues</li>
 *     <li>Thread Safety: Uses ConcurrentHashMap and ConcurrentLinkedDeque to ensure concurrent safety</li>
 *     <li>Redis Compatibility: Provides API interfaces compatible with Redis queue operations</li>
 *     <li>Double-ended Operations: Supports enqueue and dequeue operations at both head and tail of queues</li>
 * </ul>
 *
 * <p>Supported Operations:</p>
 * <ul>
 *     <li>rpush: Tail enqueue (right push)</li>
 *     <li>lpush: Head enqueue (left push)</li>
 *     <li>rpop: Tail dequeue (right pop)</li>
 *     <li>lpop: Head dequeue (left pop)</li>
 * </ul>
 *
 * <p>Usage Examples:</p>
 * <pre>{@code
 * // Enqueue operations
 * InMemoryQueueManager.rpush("task_queue", "task1");
 * InMemoryQueueManager.rpush("task_queue", "task2");
 *
 * // Dequeue operations
 * String task = InMemoryQueueManager.lpop("task_queue");  // Returns "task1"
 *
 * // Check queue size
 * int size = InMemoryQueueManager.size("task_queue");
 *
 * // Clear queue
 * InMemoryQueueManager.clear("task_queue");
 * }</pre>
 *
 * <p>Important Notes:</p>
 * <ul>
 *     <li>All data is stored in memory and will be lost after application restart</li>
 *     <li>Suitable for lightweight queue operations, not recommended for storing large amounts of data</li>
 *     <li>Queues are automatically created on first use</li>
 *     <li>Operations on empty queues return null</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class InMemoryQueueManager {

    /**
     * Queue storage mapping, key is queue name, value is corresponding deque
     */
    private static final Map<String, Deque<String>> queueMap = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation
     */
    private InMemoryQueueManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Tail enqueue (right push)
     *
     * <p>Adds a value to the tail of the specified queue. Creates the queue automatically if it doesn't exist.
     * This operation is equivalent to Redis RPUSH command.</p>
     *
     * @param key   Queue name, cannot be null or empty string
     * @param value Value to add, cannot be null
     * @throws IllegalArgumentException When key is null or empty string, or value is null
     * @since 1.0.0
     */
    public static void rpush(String key, String value) {
        validateKey(key);
        Objects.requireNonNull(value, "Value cannot be null");

        queueMap.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(value);
    }

    /**
     * Head enqueue (left push)
     *
     * <p>Adds a value to the head of the specified queue. Creates the queue automatically if it doesn't exist.
     * This operation is equivalent to Redis LPUSH command.</p>
     *
     * @param key   Queue name, cannot be null or empty string
     * @param value Value to add, cannot be null
     * @throws IllegalArgumentException When key is null or empty string, or value is null
     * @since 1.0.0
     */
    public static void lpush(String key, String value) {
        validateKey(key);
        Objects.requireNonNull(value, "Value cannot be null");

        queueMap.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addFirst(value);
    }

    /**
     * Tail dequeue (right pop)
     *
     * <p>Retrieves and removes a value from the tail of the specified queue.
     * This operation is equivalent to Redis RPOP command.</p>
     *
     * @param key Queue name, cannot be null or empty string
     * @return Value from the tail of the queue, returns null if queue doesn't exist or is empty
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static String rpop(String key) {
        validateKey(key);

        var queue = queueMap.get(key);
        return queue != null ? queue.pollLast() : null;
    }

    /**
     * Head dequeue (left pop)
     *
     * <p>Retrieves and removes a value from the head of the specified queue.
     * This operation is equivalent to Redis LPOP command.</p>
     *
     * @param key Queue name, cannot be null or empty string
     * @return Value from the head of the queue, returns null if queue doesn't exist or is empty
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static String lpop(String key) {
        validateKey(key);

        var queue = queueMap.get(key);
        return queue != null ? queue.pollFirst() : null;
    }

    /**
     * Get queue size
     *
     * <p>Returns the number of elements in the specified queue.</p>
     *
     * @param key Queue name, cannot be null or empty string
     * @return Number of elements in the queue, returns 0 if queue doesn't exist
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static int size(String key) {
        validateKey(key);

        var queue = queueMap.get(key);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Check if queue is empty
     *
     * @param key Queue name, cannot be null or empty string
     * @return true if queue is empty or doesn't exist, false otherwise
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static boolean isEmpty(String key) {
        return size(key) == 0;
    }

    /**
     * Clear specified queue
     *
     * <p>Removes all elements from the queue but keeps the queue itself.</p>
     *
     * @param key Queue name, cannot be null or empty string
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static void clear(String key) {
        validateKey(key);

        var queue = queueMap.get(key);
        if (queue != null) {
            queue.clear();
        }
    }

    /**
     * Remove queue
     *
     * <p>Completely removes the specified queue and all its elements.</p>
     *
     * @param key Queue name, cannot be null or empty string
     * @return true if queue existed and was removed, false otherwise
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static boolean remove(String key) {
        validateKey(key);

        return queueMap.remove(key) != null;
    }

    /**
     * Check if queue exists
     *
     * @param key Queue name, cannot be null or empty string
     * @return true if queue exists, false otherwise
     * @throws IllegalArgumentException When key is null or empty string
     * @since 1.0.0
     */
    public static boolean exists(String key) {
        validateKey(key);

        return queueMap.containsKey(key) && !queueMap.get(key).isEmpty();
    }

    /**
     * Get all queue names
     *
     * @return Set containing all queue names
     * @since 1.0.0
     */
    public static java.util.Set<String> getAllQueueNames() {
        return new java.util.HashSet<>(queueMap.keySet());
    }

    /**
     * Validate queue key validity
     *
     * @param key Queue name
     * @throws IllegalArgumentException When key is null or empty string
     */
    private static void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty string");
        }
    }
}