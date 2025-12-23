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
package com.jd.oxygent.core.oxygent.schemas.oxy;

/**
 * Oxy execution status enumeration
 *
 * <p>Defines various states of agent and tool execution in the OxyGent system.
 * These states are used to track the lifecycle of tasks from creation to final completion or failure.</p>
 *
 * <p>State transition relationships:</p>
 * <pre>
 * CREATED → RUNNING → {SUCCESS|COMPLETED|FAILED}
 *         ↘ PAUSED → RUNNING
 *         ↘ SKIPPED
 *         ↘ CANCELED
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum OxyState {

    /**
     * Created - Task has been created but not yet started execution
     */
    CREATED("Created", "Task has been created, waiting for execution"),

    /**
     * Running - Task is currently being executed
     */
    RUNNING("Running", "Task is executing"),

    /**
     * Completed - Task has successfully completed execution
     */
    COMPLETED("Completed", "Task execution completed"),

    /**
     * Success - Task executed successfully (similar semantics to COMPLETED, used to explicitly indicate success status)
     */
    SUCCESS("Success", "Task execution successful"),

    /**
     * Failed - Task execution failed
     */
    FAILED("Failed", "Task execution failed"),

    /**
     * Paused - Task execution has been paused and can be resumed
     */
    PAUSED("Paused", "Task execution has been paused"),

    /**
     * Skipped - Task has been skipped and will not execute
     */
    SKIPPED("Skipped", "Task has been skipped"),

    /**
     * Canceled - Task execution has been canceled
     */
    CANCELED("Canceled", "Task has been canceled");

    private final String displayName;
    private final String description;

    /**
     * Constructor
     *
     * @param displayName Display name
     * @param description Status description
     */
    OxyState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get display name
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get status description
     *
     * @return Detailed description of the status
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a final state (state that cannot be transitioned further)
     *
     * @return true if this is a final state
     */
    public boolean isFinalState() {
        return this == SUCCESS || this == COMPLETED || this == FAILED ||
                this == SKIPPED || this == CANCELED;
    }

    /**
     * Check if this is a successful state
     *
     * @return true if this is a successful state
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == COMPLETED;
    }

    /**
     * Check if this is an error state
     *
     * @return true if this is an error state
     */
    public boolean isError() {
        return this == FAILED;
    }

    /**
     * Check if execution can be recovered
     *
     * @return true if execution can be recovered
     */
    public boolean isRecoverable() {
        return this == PAUSED;
    }
}
