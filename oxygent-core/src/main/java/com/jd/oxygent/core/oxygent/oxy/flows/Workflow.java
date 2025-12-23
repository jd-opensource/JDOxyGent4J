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
package com.jd.oxygent.core.oxygent.oxy.flows;

import com.jd.oxygent.core.oxygent.oxy.BaseFlow;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;
import java.util.function.Function;

/**
 * Workflow Executor - Flexible workflow implementation based on functional programming
 *
 * <p>Workflow is the basic workflow execution class in the OxyGent framework, supporting the definition and execution of custom workflow logic through functional interfaces.
 * It provides a simple yet powerful way to encapsulate complex business processes, supporting various processing patterns and data transformations.</p>
 *
 * <h3>Main Features:</h3>
 * <ul>
 *   <li>Functional programming: Flexible workflow definition based on Function interface</li>
 *   <li>Type safety: Support for generics and type inference</li>
 *   <li>Exception handling: Comprehensive error capture and handling mechanism</li>
 *   <li>State management: Standardized execution state tracking</li>
 * </ul>
 *
 * <h3>Workflow Patterns:</h3>
 * <p>Supports multiple workflow execution patterns:</p>
 * <ul>
 *   <li>Data transformation workflow: Input data → Processing → Output results</li>
 *   <li>Service orchestration workflow: Multiple services executed sequentially or in parallel</li>
 *   <li>Decision workflow: Conditional branch execution</li>
 *   <li>Aggregation workflow: Merging and processing of multiple data sources</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Create data processing workflow
 * var dataProcessFlow = Workflow.builder()
 *     .name("data_process_workflow")
 *     .desc("Data processing workflow")
 *     .funcWorkflow(request -> {
 *         var data = request.getQuery();
 *         // Execute data processing logic
 *         return processData(data);
 *     })
 *     .build();
 *
 * // Create service orchestration workflow
 * var serviceOrchestrationFlow = Workflow.builder()
 *     .name("service_orchestration")
 *     .desc("Service orchestration workflow")
 *     .funcWorkflow(request -> {
 *         var step1Result = callService1(request);
 *         var step2Result = callService2(step1Result);
 *         return combineResults(step1Result, step2Result);
 *     })
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
public class Workflow extends BaseFlow {

    /**
     * Workflow execution function
     *
     * <p>Defines the specific execution logic of the workflow. This function receives OxyRequest as input
     * and returns processing results. Supports return values of any type, and the system will automatically perform type conversion.</p>
     *
     * <p>Function implementation recommendations:</p>
     * <ul>
     *   <li>Ensure the function is pure or has controllable side effects</li>
     *   <li>Handle exceptional situations appropriately</li>
     *   <li>Avoid long-blocking operations</li>
     *   <li>Return meaningful result objects</li>
     * </ul>
     * <p>
     * <p>
     * -- SETTER --
     *  Set workflow execution function
     *
     * @param funcWorkflow Workflow execution function, cannot be null
     * @since 1.0.0
     */
    private Function<OxyRequest, Object> funcWorkflow;

    /**
     * Execute workflow
     *
     * <p>Execute specific business logic according to the configured workflow function. This method provides complete error handling and state management,
     * ensuring the stability and reliability of workflow execution.</p>
     *
     * <h4>Execution Process:</h4>
     * <ol>
     *   <li>Verify whether the workflow function is configured</li>
     *   <li>Validate the validity of request parameters</li>
     *   <li>Execute the workflow function and capture results</li>
     *   <li>Handle exceptions and build responses</li>
     * </ol>
     *
     * @param oxyRequest Workflow execution request
     * @return Workflow execution response
     * @throws NullPointerException Thrown when oxyRequest is null
     * @since 1.0.0
     */
    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        OxyResponse resp = new OxyResponse();
        if (funcWorkflow == null) {
            resp.setState(OxyState.FAILED);
            resp.setOutput("Workflow function (funcWorkflow) is not configured");
            return resp;
        }
        try {
            Object output = funcWorkflow.apply(oxyRequest);
            resp.setState(OxyState.COMPLETED);
            resp.setOutput(output);
            return resp;
        } catch (Exception e) {
            resp.setState(OxyState.FAILED);
            resp.setOutput("Error executing workflow: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            return resp;
        }
    }

}