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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chained event processor
 *
 * <h3>Functional Description</h3>
 * <ul>
 *   <li>Provides business ID-based chained asynchronous event processing mechanism</li>
 *   <li>Supports sequential execution of CreateEvent and UpdateEvent</li>
 *   <li>Implements asynchronous processing and task chain concatenation through CompletableFuture</li>
 *   <li>Ensures events under the same business ID are executed sequentially in chronological order</li>
 * </ul>
 *
 * <h3>Design Features</h3>
 * <ul>
 *   <li>Adopts chain of responsibility pattern for ordered event processing execution</li>
 *   <li>Uses ConcurrentHashMap to ensure thread-safe task management</li>
 *   <li>Provides exception handling mechanism to prevent single task failure from affecting the entire chain</li>
 *   <li>Supports automatic event cleanup and memory management</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li>Need to ensure operations on the same business entity are executed in order</li>
 *   <li>Asynchronously handle state changes in complex business processes</li>
 *   <li>Avoid data inconsistency issues caused by concurrent modifications</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ChainedEventProcessor {
    private final static Map<String, CompletableFuture<Void>> tastkFutures = new ConcurrentHashMap<>();

    /**
     * Process chained events
     * <p>
     * Different processing strategies based on event type (CreateEvent or UpdateEvent):
     * <ul>
     *   <li>CreateEvent: Create new task chain and store it in task mapping table</li>
     *   <li>UpdateEvent: Remove corresponding task from task mapping table and concatenate to end of existing task chain</li>
     * </ul>
     *
     * @param event Chained event to be processed, cannot be null
     * @return CompletableFuture<Void> Asynchronous task execution result
     * <ul>
     *   <li>For CreateEvent: Returns newly created task Future</li>
     *   <li>For UpdateEvent: Returns concatenated task Future</li>
     *   <li>For invalid events: Returns null</li>
     * </ul>
     */
    public static CompletableFuture<Void> processEvent(ChainedEvent event) {

        String bizId = event.getBizId();
        if (event instanceof CreateEvent) {
            CompletableFuture<Void> headFuture = CompletableFuture
                    .runAsync(event.getRunnable())
                    .exceptionally(throwable -> {
                        log.error("createEvent execute fail:", throwable);
                        return null;
                    });

            tastkFutures.put(bizId, headFuture);
            return headFuture;
        } else if (event instanceof UpdateEvent) {
            CompletableFuture<Void> endFuture = tastkFutures.remove(bizId);

            if (endFuture != null) {
                return endFuture.thenRunAsync(event.getRunnable());
            } else {
                log.warn("updateEvent haven't pre createEvent, this bizId:{}", bizId);
                return CompletableFuture.runAsync(event.getRunnable());
            }
        }
        log.error("processEvent handler must haven createEvent or updateEvent, current bizId:{}", bizId);
        return null;
    }

    /**
     * Create a create event
     * <p>
     * Constructs a CreateEvent instance to start a new business processing chain.
     * CreateEvent typically serves as the starting event for a business process.
     *
     * @param bizId    Business identifier, used to associate subsequent update events, cannot be null or empty string
     * @param runnable Business logic to be executed, cannot be null
     * @return CreateEvent Create event instance
     */
    public static ChainedEvent createEvent(String bizId, Runnable runnable) {
        return new CreateEvent(bizId, runnable);
    }

    /**
     * Create an update event
     * <p>
     * Constructs an UpdateEvent instance to concatenate to existing business processing chain.
     * UpdateEvent will wait for the corresponding bizId's CreateEvent to complete before executing.
     *
     * @param bizId    Business identifier, must match the bizId of previous CreateEvent, cannot be null or empty string
     * @param runnable Business logic to be executed, cannot be null
     * @return UpdateEvent Update event instance
     */
    public static ChainedEvent updateEvent(String bizId, Runnable runnable) {
        return new UpdateEvent(bizId, runnable);
    }

    /**
     * Chained event abstract base class
     * <p>
     * Defines the basic structure and properties of chained events, all concrete event types inherit from this class.
     * Each event contains unique identifier, business identifier, timestamp, and execution logic.
     *
     * @author OxyGent Team
     * @version 1.0.0
     * @since 1.0.0
     */
    @Getter
    static abstract class ChainedEvent {
        /**
         * Event unique identifier
         */
        protected final String eventId;
        /**
         * Business identifier, used to associate multiple events of the same business
         */
        protected final String bizId;
        /**
         * Event creation timestamp
         */
        protected final Long timestamp;
        /**
         * Event execution logic
         */
        protected final Runnable runnable;

        /**
         * Construct chained event
         *
         * @param bizId    Business identifier, cannot be null
         * @param runnable Event execution logic, cannot be null
         */
        public ChainedEvent(String bizId, Runnable runnable) {
            this.eventId = UUID.randomUUID().toString();
            this.bizId = bizId;
            this.timestamp = System.currentTimeMillis();
            this.runnable = runnable;
        }
    }

    /**
     * Create event
     * <p>
     * Represents the starting event of a business process, creates new task chain and stores it in task mapping table.
     * Create events are typically used to initialize the processing flow for a business entity.
     *
     * @author OxyGent Team
     * @version 1.0.0
     * @since 1.0.0
     */
    static final class CreateEvent extends ChainedEvent {
        /**
         * Construct create event
         *
         * @param bizId    Business identifier, cannot be null
         * @param runnable Event execution logic, cannot be null
         */
        public CreateEvent(String bizId, Runnable runnable) {
            super(bizId, runnable);
        }
    }

    /**
     * Update event
     * <p>
     * Represents subsequent events in business process, concatenates to the end of existing task chain for execution.
     * Update events ensure execution only starts after the corresponding create event completes.
     *
     * @author OxyGent Team
     * @version 1.0.0
     * @since 1.0.0
     */
    static final class UpdateEvent extends ChainedEvent {
        /**
         * Construct update event
         *
         * @param bizId    Business identifier, must match the corresponding create event, cannot be null
         * @param runnable Event execution logic, cannot be null
         */
        public UpdateEvent(String bizId, Runnable runnable) {
            super(bizId, runnable);
        }
    }
}
