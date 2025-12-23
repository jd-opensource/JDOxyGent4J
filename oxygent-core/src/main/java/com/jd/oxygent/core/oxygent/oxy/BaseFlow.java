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
package com.jd.oxygent.core.oxygent.oxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * Base Flow Class - Abstract base class for all flow patterns in OxyGent framework
 *
 * <p>BaseFlow is the core base class for flow orchestration in OxyGent intelligent agent framework, 
 * providing unified interface and common property configuration for flow execution.
 * All concrete flow implementations (such as Workflow, PlanAndSolve, Reflexion, etc.) inherit from this class.</p>
 *
 * <h3>Main Functions:</h3>
 * <ul>
 *   <li>Define standard interface for flow execution</li>
 *   <li>Provide permission control and classification management</li>
 *   <li>Support master-slave flow patterns</li>
 *   <li>Inherit basic capabilities from BaseOxy</li>
 * </ul>
 *
 * <h3>Design Patterns:</h3>
 * <p>Adopts template method pattern, defining the framework structure for flow execution, 
 * with specific execution logic implemented by subclasses.
 * Combined with Builder pattern and Lombok annotations, providing flexible object construction.</p>
 *
 * <h3>Architecture Position:</h3>
 * <pre>
 * BaseOxy (Base abstract class)
 *    └── BaseFlow (Flow base class)
 *           ├── Workflow (Workflow)
 *           ├── PlanAndSolve (Plan and Solve)
 *           └── Reflexion (Reflection flow)
 * </pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class CustomFlow extends BaseFlow {
 *     @Override
 *     protected OxyResponse _execute(OxyRequest request) {
 *         // Implement specific flow logic
 *         return processFlow(request);
 *     }
 * }
 *
 * // Build flow instance
 * var flow = CustomFlow.builder()
 *     .isPermissionRequired(true)
 *     .category("workflow")
 *     .isMaster(false)
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseFlow extends BaseOxy {

    /**
     * Whether permission verification is required
     *
     * <p>Controls whether this flow needs permission checking. When set to true,
     * the system will verify user permissions before executing the flow.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("is_permission_required")
    private boolean isPermissionRequired = true;

    /**
     * Flow category identifier
     *
     * <p>Used to identify the type classification of the flow, facilitating system management and routing.
     * Common categories include: agent, workflow, tool, etc.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("category")
    private String category = "agent";

    /**
     * Whether it is a master flow
     *
     * <p>Identifies whether this flow is a master control flow. Master flows are usually responsible for 
     * overall coordination and control, while non-master flows serve as sub-flows being called.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("is_master")
    private boolean isMaster = false;

    /**
     * Core logic method for executing the flow
     *
     * <p>This is the core abstract method for flow execution. All concrete flow classes inheriting from BaseFlow 
     * must implement this method. This method adopts the template method pattern, where the parent class defines 
     * the execution framework and subclasses implement specific logic.</p>
     *
     * <h4>Implementation Requirements:</h4>
     * <ul>
     *   <li>Must perform non-null validation on request parameters</li>
     *   <li>Requires appropriate exception handling and error information</li>
     *   <li>Return result should include complete execution status</li>
     * </ul>
     *
     * <h4>Execution Process:</h4>
     * <ol>
     *   <li>Validate the validity of request parameters</li>
     *   <li>Execute specific flow logic</li>
     *   <li>Build and return response result</li>
     * </ol>
     *
     * @param oxyRequest Flow execution request containing input parameters and context information
     * @return Flow execution response containing processing results and status information
     * @throws NullPointerException When oxyRequest is null
     * @throws IllegalArgumentException When request parameters are invalid
     * @since 1.0.0
     */
    protected abstract OxyResponse _execute(OxyRequest oxyRequest);

}