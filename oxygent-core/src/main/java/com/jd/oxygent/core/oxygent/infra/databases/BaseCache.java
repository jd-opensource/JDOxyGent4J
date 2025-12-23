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
package com.jd.oxygent.core.oxygent.infra.databases;

/**
 * Redis Database Base Interface
 * <p>
 * Provides unified Redis data access interface for OxyGent system, supporting key-value storage,
 * list operations, hash table management, distributed locks and other core functions.
 * This interface adopts contract-oriented design to ensure consistency across different Redis
 * implementations (local/remote/cluster).
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *     <li>Data Structure Support: String, List, Hash, Set, ZSet and other Redis core data types</li>
 *     <li>Expiration Strategy: Support TTL settings, automatic cleanup of expired data</li>
 *     <li>Distributed Features: Provide enterprise-level functions like distributed locks, message queues</li>
 *     <li>Performance Optimization: Connection pool management, pipeline operations, batch processing</li>
 * </ul>
 *
 * <h3>Technical Features</h3>
 * <ul>
 *     <li>High Performance: In-memory storage, microsecond-level response time</li>
 *     <li>High Availability: Support master-slave replication, sentinel mode, cluster mode</li>
 *     <li>Persistence: Dual protection with RDB snapshots and AOF logs</li>
 *     <li>Atomic Operations: All single commands are atomic</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *     <li>Cache System: Hot data caching, reducing database pressure</li>
 *     <li>Session Storage: User sessions, shopping carts and other temporary data</li>
 *     <li>Counters: Access statistics, rate limiting control</li>
 *     <li>Message Queue: Task distribution, event notification</li>
 *     <li>Distributed Lock: Ensure consistency of concurrent operations</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BaseCache {

    /**
     * Pop element from the right side of list
     * <p>
     * Remove and return the last element of the list (right side). If the list is empty, return null.
     * Commonly used for consumer pattern in queues.
     *
     * @param key List key name, cannot be null or empty string
     * @return The popped element, null if the list is empty
     * @throws IllegalArgumentException When key is null or empty string
     */
    String rpop(String key);

    /**
     * Blocking pop element from the right side of list
     * <p>
     * Remove and return an element from the right side of the list. If the list is empty,
     * block and wait until there is an element or timeout occurs.
     * Suitable for implementing consumer pattern in message queues.
     *
     * <h4>Blocking Features</h4>
     * <ul>
     *     <li>Empty List Blocking: When the list is empty, the command will block and wait</li>
     *     <li>Timeout Control: Can set maximum wait time to avoid infinite waiting</li>
     *     <li>Multiple Clients: Multiple clients can simultaneously block and wait for the same list</li>
     *     <li>Fair Distribution: Distribute elements in the order of waiting</li>
     * </ul>
     *
     * @param key     List key name, cannot be null or empty string
     * @param timeout Maximum wait time (seconds), 0 means infinite wait
     * @return Popped element, null if timeout occurs
     * @throws IllegalArgumentException When key is null or empty string
     */
    Object brpop(String key, int timeout);

    /**
     * Blocking pop element from the right side of list (default timeout)
     * <p>
     * Blocking pop operation with default timeout.
     *
     * @param key List key name, cannot be null or empty string
     * @return Popped element, null if timeout occurs
     * @throws IllegalArgumentException When key is null or empty string
     */
    Object brpop(String key);

    /**
     * Push multiple elements to the left side of list
     * <p>
     * Insert one or more values to the head of the list. Supports various data types.
     * This is an atomic operation, all values are either inserted successfully or all fail.
     *
     * @param key    List key name, cannot be null or empty string
     * @param values Values to insert, cannot be null or empty array
     * @return Length of the list after operation
     * @throws IllegalArgumentException When parameters are invalid
     */
    int lpush(String key, String... values);

    /**
     * Close Redis connection and clean up resources
     * <p>
     * Safely close Redis client connections and release related resources.
     * This method should be called before application shutdown to avoid resource leaks.
     *
     * <h4>Cleanup Content</h4>
     * <ul>
     *     <li>Close all connections in the connection pool</li>
     *     <li>Stop background monitoring threads</li>
     *     <li>Clear local cache data</li>
     *     <li>Release occupied memory resources</li>
     * </ul>
     */
    void close();

}