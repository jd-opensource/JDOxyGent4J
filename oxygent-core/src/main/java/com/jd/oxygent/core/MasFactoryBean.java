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
package com.jd.oxygent.core;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MAS Factory Bean - Spring factory class for creating and managing Multi-Agent System instances
 *
 * <p>MasFactoryBean is a Spring factory Bean in the OxyGent framework for creating MAS (Multi-Agent System) instances.
 * It is responsible for managing the creation, dependency injection, and initialization process of agent spaces, ensuring that MAS instances can be correctly
 * integrated into the Spring container and obtain the required dependencies.</p>
 *
 * <h3>Main Responsibilities:</h3>
 * <ul>
 *   <li><strong>Instance Creation</strong>: Create MAS instances based on configuration</li>
 *   <li><strong>Dependency Injection</strong>: Automatically inject Spring-managed dependencies</li>
 *   <li><strong>Lifecycle Management</strong>: Manage the initialization process of MAS instances</li>
 *   <li><strong>Agent Space Configuration</strong>: Set up and manage agents in OxySpace</li>
 * </ul>
 *
 * <h3>Factory Pattern Advantages:</h3>
 * <ul>
 *   <li>Support dynamic creation of MAS instances with different configurations</li>
 *   <li>Deep integration with Spring container, supporting dependency injection</li>
 *   <li>Provide flexible instance configuration and management mechanisms</li>
 *   <li>Support prototype pattern, creating new instances on each call</li>
 * </ul>
 *
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li>Need to dynamically create MAS systems with different agent combinations</li>
 *   <li>Manage complex multi-agent architectures in Spring environments</li>
 *   <li>Agent systems requiring dependency injection support</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3></search>
 * </search_and_replace>
 * <pre>{@code
 * // Use in Spring configuration
 * @Bean
 * public MasFactoryBean masFactory() {
 *     List<BaseOxy> agents = Arrays.asList(
 *         new ChatAgent("chat_agent"),
 *         new ReActAgent("react_agent")
 *     );
 *     return new MasFactoryBean("my_mas", agents);
 * }
 *
 * // Use in program
 * @Autowired
 * private MasFactoryBean masFactory;
 *
 * public void createMasSystem() {
 *     Mas mas = masFactory.getObject();
 *     // Use MAS system
 * }
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class MasFactoryBean implements FactoryBean<Mas> {

    /**
     * Agent space list
     *
     * <p>Contains all agent instances that compose the MAS system. These agents will be
     * registered in the created MAS instance to form a complete multi-agent system.</p>
     *
     * @since 1.0.0
     */
    private List<BaseOxy> oxySpace;
    private static final int MAX_CACHE_SIZE = 20;
    // FIFO cache: insertion-order LinkedHashMap, removes earliest inserted entries when size limit is exceeded
    private static final Map<String, CacheEntry> masCache = Collections.synchronizedMap(
            new LinkedHashMap<String, CacheEntry>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    // Cache entry: contains Mas instance and its corresponding oxySpace signature
    private static class CacheEntry {
        final Mas mas;
        final String signature;

        CacheEntry(Mas mas, String signature) {
            this.mas = mas;
            this.signature = signature;
        }
    }


    /**
     * MAS system name
     *
     * <p>Used to identify the name of the created MAS instance. This name will be used for
     * logging, monitoring, and system management scenarios.</p>
     *
     * @since 1.0.0
     */
    private String name;

    /**
     * Spring autowiring factory
     *
     * <p>Used to perform dependency injection on the created MAS instance, ensuring that MAS and its
     * internal components can obtain dependencies managed by the Spring container.</p>
     *
     * @since 1.0.0
     */
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    /**
     * Default constructor
     *
     * <p>Creates an uninitialized MAS factory instance. After using this constructor,
     * you need to set the agent space and name through the setOxySpace method.</p>
     *
     * @since 1.0.0
     */
    public MasFactoryBean() {
    }

    /**
     * Parameterized constructor
     *
     * <p>Creates a MAS factory instance with the specified name and agent space.
     * This is the recommended construction method to ensure the factory instance is fully initialized.</p>
     *
     * @param name     MAS system name, cannot be null or empty string
     * @param oxySpace Agent space list, cannot be null
     * @throws NullPointerException     Thrown when name or oxySpace is null
     * @throws IllegalArgumentException Thrown when name is empty string
     * @since 1.0.0
     */
    public MasFactoryBean(String name, List<BaseOxy> oxySpace) {
        this.name = Objects.requireNonNull(name, "MAS system name cannot be null");
        this.oxySpace = Objects.requireNonNull(oxySpace, "Agent space cannot be null");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("MAS system name cannot be empty string");
        }
    }

    /**
     * Set agent space configuration
     *
     * <p>Configure or update the agent space and system name of the MAS factory.
     * This method is typically called during Spring container initialization.</p>
     *
     * @param name     MAS system name, cannot be null or empty string
     * @param oxySpace Agent space list, cannot be null
     * @throws NullPointerException     Thrown when name or oxySpace is null
     * @throws IllegalArgumentException Thrown when name is empty string
     * @since 1.0.0
     */
    public void setOxySpace(String name, List<BaseOxy> oxySpace) {
        this.name = Objects.requireNonNull(name, "MAS system name cannot be null");
        this.oxySpace = Objects.requireNonNull(oxySpace, "Agent space cannot be null");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("MAS system name cannot be empty string");
        }

        // Use composite Key strategy (name+signature), so no need to delete old cache, allowing same-name different-space coexistence
    }

    /**
     * Create MAS instance - FactoryBean interface implementation
     *
     * <p>The Spring container calls this method to create MAS instances. This method will:</p>
     * <ol>
     *   <li>Create MAS instance using configured name and agent space</li>
     *   <li>Perform dependency injection through Spring container</li>
     *   <li>Set agent space</li>
     *   <li>Call initialization method</li>
     * </ol>
     *
     * @return Fully initialized MAS instance
     * @throws IllegalStateException Thrown when factory is not properly configured
     * @since 1.0.0
     */
    @Override
    public Mas getObject() {
        if (name == null || oxySpace == null) {
            throw new IllegalStateException("MAS factory not properly configured, missing name or agent space");
        }

        String signature = computeSignature(oxySpace);
        String cacheKey = name + "#" + signature;

        synchronized (masCache) {
            CacheEntry entry = masCache.get(cacheKey);
            if (entry != null) {
                return entry.mas;
            }

            // Generate new Mas instance and put into cache with FIFO strategy (LinkedHashMap handles overflow removal)
            Mas mas = new Mas(name, oxySpace);
            beanFactory.autowireBean(mas); // Inject Spring managed dependencies
            mas.setOxySpace(oxySpace);
            mas.init(); // Initialization logic

            masCache.put(cacheKey, new CacheEntry(mas, signature));
            return mas;
        }

    }

    /**
     * Create MAS instance with specified configuration
     *
     * <p>Create MAS instance using specified parameters, without relying on factory's pre-configuration.
     * This method provides greater flexibility, allowing dynamic creation of MAS systems with different configurations.</p>
     *
     * <h4>Creation Process:</h4>
     * <ol>
     *   <li>Create MAS instance</li>
     *   <li>Perform Spring dependency injection</li>
     *   <li>Set agent space</li>
     *   <li>Execute initialization</li>
     * </ol>
     *
     * @param name     MAS system name, cannot be null or empty string
     * @param oxySpace Agent space list, cannot be null
     * @return Fully initialized MAS instance
     * @throws NullPointerException     Thrown when name or oxySpace is null
     * @throws IllegalArgumentException Thrown when name is empty string
     * @since 1.0.0
     */
    public Mas createMas(String name, List<BaseOxy> oxySpace) {
        Objects.requireNonNull(name, "MAS system name cannot be null");
        Objects.requireNonNull(oxySpace, "Agent space cannot be null");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("MAS system name cannot be empty string");
        }

        Mas mas = new Mas(name, oxySpace);
        // Perform dependency injection through Spring container
        beanFactory.autowireBean(mas);
        mas.setOxySpace(oxySpace);
        mas.init();
        return mas;
    }

    /**
     * Get object type created by factory - FactoryBean interface implementation
     *
     * @return Class object of Mas class
     * @since 1.0.0
     */
    @Override
    public Class<?> getObjectType() {
        return Mas.class;
    }

    /**
     * Whether it's singleton mode - FactoryBean interface implementation
     *
     * <p>Returns false, indicating that each call to getObject() will create a new MAS instance.
     * This allows multiple independent multi-agent systems to run simultaneously.</p>
     *
     * @return false, indicating non-singleton mode
     * @since 1.0.0
     */
    @Override
    public boolean isSingleton() {
        return false;
    }

    /**
     * Calculate oxySpace signature for detecting configuration changes.
     * Signature is based on each Oxy's class name and logical name, hashed with order-stable string.
     */
    private String computeSignature(List<BaseOxy> oxySpace) {
        if (oxySpace == null || oxySpace.isEmpty()) {
            return "EMPTY";
        }
        String payload = oxySpace.stream()
                .map(o -> {
                    String cls = (o == null) ? "null" : o.getClass().getName();
                    String nm = (o == null || o.getName() == null) ? "null" : o.getName();
                    return cls + ":" + nm;
                })
                .collect(Collectors.joining("|"));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback: return original string directly (rarely occurs)
            return payload;
        }
    }
}