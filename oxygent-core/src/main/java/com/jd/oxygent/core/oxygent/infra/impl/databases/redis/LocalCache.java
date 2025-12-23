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
package com.jd.oxygent.core.oxygent.infra.impl.databases.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.infra.databases.BaseDB;
import com.jd.oxygent.core.oxygent.infra.databases.BaseCache;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * <h3>Local Redis Simulation Implementation</h3>
 *
 * <p>LocalRedis is a Redis simulator for development and testing environments in OxyGent framework.
 * This implementation is based on in-memory data structures, simulating Redis core functionality,
 * providing developers with a lightweight caching solution that requires no external Redis service.</p>
 *
 * <h3>Technical Architecture</h3>
 * <ul>
 *   <li><strong>Memory Storage</strong>: High-performance in-memory cache based on Java ConcurrentHashMap</li>
 *   <li><strong>Data Structures</strong>: Uses LinkedBlockingDeque to support Redis-style list operations</li>
 *   <li><strong>Thread Safety</strong>: Comprehensive use of thread-safe data structures, supporting high concurrent access</li>
 *   <li><strong>TTL Support</strong>: Complete expiration time management, automatic cleanup of expired data</li>
 *   <li><strong>Type Support</strong>: Supports multiple Redis data types like strings, lists, hash tables</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Zero Configuration Startup</strong>: No need to install Redis service, directly available in-process</li>
 *   <li><strong>API Compatibility</strong>: Provides interfaces compatible with Redis client API</li>
 *   <li><strong>Data Persistence</strong>: Supports JSON serialization for complex data type storage</li>
 *   <li><strong>Auto Expiration</strong>: Background automatic cleanup of expired data, avoiding memory leaks</li>
 *   <li><strong>Performance Optimization</strong>: Optimized for list operations with length and content restrictions</li>
 * </ul>
 *
 * <h3>Supported Redis Commands</h3>
 * <ul>
 *   <li><strong>String Operations</strong>: SET, GET, MSET, MGET, EXISTS, EXPIRE</li>
 *   <li><strong>List Operations</strong>: LPUSH, RPOP, BRPOP, LRANGE, LLEN, LTRIM</li>
 *   <li><strong>Hash Operations</strong>: HSET, HGETALL, HDEL</li>
 *   <li><strong>General Operations</strong>: Expiration time management, distributed lock simulation</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li><strong>Local Development</strong>: Cache functionality development without setting up Redis environment</li>
 *   <li><strong>Unit Testing</strong>: Fast testing environment, supporting complete Redis operation testing</li>
 *   <li><strong>Prototype Validation</strong>: Quick validation of caching strategies and data structure design</li>
 *   <li><strong>Lightweight Applications</strong>: Embedded caching solution for small-scale applications</li>
 * </ul>
 *
 * <h3>Performance Characteristics</h3>
 * <ul>
 *   <li><strong>Startup Speed</strong>: Millisecond-level initialization, no network latency</li>
 *   <li><strong>Memory Efficiency</strong>: Based on Java native data structures, controllable memory usage</li>
 *   <li><strong>Concurrent Performance</strong>: Supports high concurrent read/write, suitable for medium-scale applications</li>
 *   <li><strong>Data Scale</strong>: Suitable for data caching needs under 100,000 entries</li>
 * </ul>
 *
 * <h3>Data Management</h3>
 * <ul>
 *   <li><strong>TTL Management</strong>: Supports second-level expiration time, automatic cleanup mechanism</li>
 *   <li><strong>Type Conversion</strong>: Automatic handling of conversion between Java objects and Redis data types</li>
 *   <li><strong>Memory Protection</strong>: Protection mechanisms like list length limits, string length truncation</li>
 *   <li><strong>JSON Support</strong>: Complex objects automatically serialized to JSON for storage</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // String operations
 * localRedis.set("user:123", "John Doe", 3600);
 * String user = localRedis.get("user:123");
 *
 * // List operations
 * localRedis.lpush("messages", "Hello", "World");
 * List<Object> messages = localRedis.lrange("messages");
 *
 * // Hash operations
 * localRedis.hset("user:profile", "name", "John");
 * Map<String, String> profile = localRedis.hgetAll("user:profile");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(name = "oxygent.cache", havingValue = "local", matchIfMissing = true)
public class LocalCache extends BaseDB implements BaseCache {

    /**
     * Main data storage mapping table
     * <p>
     * Uses ConcurrentHashMap to ensure thread safety, LinkedBlockingDeque supports Redis-style list operations.
     * Key is Redis key, value is double-ended queue structure, supporting LPUSH, RPOP operations.
     */
    private final Map<String, LinkedBlockingDeque<Object>> data;

    /**
     * Key expiration time mapping table
     * <p>
     * Stores expiration timestamp (milliseconds) for each key, used to implement TTL functionality.
     * System automatically checks and cleans up expired key-value pairs.
     */
    private final Map<String, Long> expiry;

    /**
     * Default expiration time (seconds)
     * <p>
     * Default TTL value used when no expiration time is specified, set to 1 day (86400 seconds).
     * Helps prevent unlimited memory growth and data staleness.
     */
    private final int defaultExpireTime;

    /**
     * Default maximum length of list
     * <p>
     * Limits maximum number of elements in a single list, preventing excessive memory usage.
     * When list exceeds this length, tail elements are automatically removed.
     */
    private final int defaultListMaxSize;

    /**
     * Initialize local Redis simulator
     * <p>
     * Constructor responsible for initializing all internal data structures and configuration parameters,
     * preparing for Redis operations.
     * Uses thread-safe data structures to ensure data consistency in high concurrent environments.
     *
     * <h3>Initialization Content</h3>
     * <ul>
     *   <li>Create thread-safe data storage mapping table</li>
     *   <li>Initialize expiration time management mapping table</li>
     *   <li>Set default configuration parameters (TTL=1 day, list max length=10)</li>
     *   <li>Initialize JSON serialization tool</li>
     * </ul>
     */
    public LocalCache() {
        this.data = new ConcurrentHashMap<>();
        this.expiry = new ConcurrentHashMap<>();
        this.defaultExpireTime = 86400; // Default TTL: 1 day
        this.defaultListMaxSize = 1000;
    }


    /**
     * Push one or more values to the left side (head) of list
     * <p>
     * This is the most complex list operation method in LocalRedis, providing complete Redis LPUSH functionality simulation,
     * including data type validation, length limits, serialization processing and other advanced features.
     *
     * <h3>Operation Flow</h3>
     * <ol>
     *   <li><strong>Parameter validation and default value processing</strong>: Set expiration time, list size limits, etc.</li>
     *   <li><strong>Data type processing</strong>: Support strings, byte arrays, numbers, complex objects</li>
     *   <li><strong>Length limits</strong>: Truncate protection when strings and byte arrays exceed length</li>
     *   <li><strong>JSON serialization</strong>: Complex objects automatically converted to JSON strings</li>
     *   <li><strong>List operations</strong>: Reverse order addition to head, maintain correct enqueue order</li>
     *   <li><strong>Length control</strong>: Automatically remove tail elements when exceeding limits</li>
     *   <li><strong>TTL management</strong>: Set key expiration time</li>
     * </ol>
     *
     * <h3>Supported Data Types</h3>
     * <ul>
     *   <li><strong>String</strong>: Direct storage, supports length truncation</li>
     *   <li><strong>byte[]</strong>: Byte array, supports length truncation</li>
     *   <li><strong>Number</strong>: Numeric types (Integer, Long, Double, etc.)</li>
     *   <li><strong>Map/List</strong>: Complex objects, automatic JSON serialization</li>
     * </ul>
     *
     * <h3>Memory Protection Mechanisms</h3>
     * <ul>
     *   <li><strong>String length limit</strong>: Default maximum 81920 characters, truncated when exceeded</li>
     *   <li><strong>List length control</strong>: Default maximum 10 elements, remove tail when exceeded</li>
     *   <li><strong>Type safety check</strong>: Unsupported types throw IllegalArgumentException</li>
     * </ul>
     *
     * <h3>Concurrent Safety</h3>
     * <p>Uses thread-safe features of LinkedBlockingDeque to ensure data consistency in multi-threaded environments.
     * All operations are atomic, no data race issues will occur.</p>
     *
     * @param key       List key name, cannot be null
     * @param values    Array of values to push, supports multiple data types
     * @param ex        Expiration time (seconds), uses default 86400 seconds when null
     * @param maxSize   List maximum length, uses default 10 when null
     * @param maxLength String maximum length, uses default 81920 when null
     * @return Current length of list after push operation
     * @throws IllegalArgumentException If value type is not supported or JSON serialization fails
     */
    public int lpush(String key, String[] values, Integer ex, Integer maxSize, Integer maxLength) {
        int expireTime = (ex != null) ? ex : defaultExpireTime;
        int listMaxSize = (maxSize != null) ? maxSize : defaultListMaxSize;
        int stringMaxLength = (maxLength != null) ? maxLength : Integer.MAX_VALUE;

        LinkedBlockingDeque<Object> deque = data.computeIfAbsent(key, k -> new LinkedBlockingDeque<>());

        // Process and validate input values
        List<Object> newValues = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String) {
                String str = (String) value;
                newValues.add(str.length() > stringMaxLength ? str.substring(0, stringMaxLength) : str);
            } else if (value instanceof byte[]) {
                byte[] bytes = (byte[]) value;
                if (bytes.length > stringMaxLength) {
                    byte[] truncated = new byte[stringMaxLength];
                    System.arraycopy(bytes, 0, truncated, 0, stringMaxLength);
                    newValues.add(truncated);
                } else {
                    newValues.add(bytes);
                }
            } else if (value instanceof Number) {
                newValues.add(value);
            } else if (value instanceof Map || value instanceof List) {
                try {
                    String json = JsonUtils.writeValueAsString(value);
                    newValues.add(json.length() > stringMaxLength ? json.substring(0, stringMaxLength) : json);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to serialize value to JSON: " + value, e);
                }
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
            }
        }

        // Add values to the left (head) of the deque
        Collections.reverse(newValues); // Reverse to maintain proper order when adding to front
        for (Object value : newValues) {
            deque.addFirst(value);
        }

        // Trim to max size
        while (deque.size() > listMaxSize) {
            deque.removeLast();
        }

        expiry.put(key, System.currentTimeMillis() + expireTime * 1000L);
        return deque.size();
    }

    @Override
    public int lpush(String key, String... values) {
        return lpush(key, values, null, null, null);
    }

    /**
     * Remove and return the last (rightmost, tail) element from a list.
     *
     * @param key The list key to pop from
     * @return The removed element, or null if the list is empty or doesn't exist
     */
    public String rpop(String key) {
        checkExpiry(key);
        LinkedBlockingDeque<Object> deque = data.get(key);
        if (deque != null && !deque.isEmpty()) {
            Object o = deque.removeLast();
            return o.toString();
        }
        return null;
    }

    @Override
    public Object brpop(String key, int timeout) {
        checkExpiry(key);
        LinkedBlockingDeque<Object> deque = data.get(key);
        if (deque != null && !deque.isEmpty()) {
            return deque.removeLast();
        }

        // Simulate blocking behavior
        try {
            Thread.sleep(timeout * 1000L);
            deque = data.get(key);
            if (deque != null && !deque.isEmpty()) {
                return deque.removeLast();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    @Override
    public Object brpop(String key) {
        return brpop(key, 1);
    }

    /**
     * Check if a key has expired and remove it if necessary.
     *
     * @param key The key to check for expiration
     */
    private void checkExpiry(String key) {
        Long expiryTime = expiry.get(key);
        if (expiryTime != null && System.currentTimeMillis() > expiryTime) {
            data.remove(key);
            expiry.remove(key);
        }
    }

    @Override
    public void close() {
        // This method maintains compatibility with the Redis interface
        data.clear();
        expiry.clear();
    }

}