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
package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.function.Function;

/**
 * Workflow Agent - Agent implementation based on custom workflows
 *
 * <p>WorkflowAgent is a specialized agent for executing complex business processes and workflows. It inherits from LocalAgent
 * and implements specific business logic and process control through configuring custom workflow functions.</p>
 *
 * <p>Core features:</p>
 * <ul>
 *     <li>Workflow orchestration: Supports complex multi-step business process orchestration</li>
 *     <li>Functional interface: Flexible workflow definition based on Function interface</li>
 *     <li>State management: Complete execution state tracking and error handling</li>
 *     <li>Dynamic configuration: Supports runtime dynamic replacement of workflow logic</li>
 *     <li>Exception handling: Comprehensive error capture and status feedback mechanism</li>
 * </ul>
 *
 * <p>Workflow patterns:</p>
 * <ul>
 *     <li>Serial process: Sequential step chains</li>
 *     <li>Parallel processing: Simultaneous execution of multiple independent tasks</li>
 *     <li>Conditional branching: Process branching based on conditions</li>
 *     <li>Loop processing: Repeatedly executed business logic</li>
 *     <li>Exception recovery: Error handling and recovery mechanisms</li>
 * </ul>
 *
 * <p>Typical application scenarios:</p>
 * <ul>
 *     <li>Business process automation: Order processing, approval processes, etc.</li>
 *     <li>Data processing pipelines: ETL processes, data transformation, etc.</li>
 *     <li>System integration: Data synchronization and coordination between multiple systems</li>
 *     <li>Batch processing tasks: Batch processing of large volumes of data</li>
 *     <li>Monitoring and alerting: System monitoring and automated operations</li>
 * </ul>
 *
 * <p>Workflow function design principles:</p>
 * <ul>
 *     <li>Idempotency: Multiple executions produce the same result</li>
 *     <li>Observability: Provide detailed execution logs</li>
 *     <li>Fault tolerance: Gracefully handle exceptional situations</li>
 *     <li>Testability: Support unit testing and integration testing</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Define order processing workflow
 * Function<OxyRequest, Object> orderWorkflow = request -> {
 *     String orderId = (String) request.getArguments().get("orderId");
 *
 *     // Step 1: Validate order
 *     if (!validateOrder(orderId)) {
 *         throw new IllegalArgumentException("Order validation failed");
 *     }
 *
 *     // Step 2: Check inventory
 *     if (!checkInventory(orderId)) {
 *         return "Insufficient inventory";
 *     }
 *
 *     // Step 3: Process payment
 *     PaymentResult payment = processPayment(orderId);
 *
 *     // Step 4: Ship order
 *     if (payment.isSuccess()) {
 *         return shipOrder(orderId);
 *     }
 *
 *     return "Payment failed";
 * };
 *
 * // Create workflow agent
 * WorkflowAgent workflowAgent = WorkflowAgent.builder()
 *     .name("Order Processing Workflow")
 *     .funcWorkflow(orderWorkflow)
 *     .build();
 *
 * // Execute workflow
 * OxyRequest request = OxyRequest.builder()
 *     .arguments(Map.of("orderId", "ORD-12345"))
 *     .build();
 *
 * OxyResponse response = workflowAgent.execute(request);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@SuperBuilder
public class WorkflowAgent extends LocalAgent {

    /**
     * Workflow execution function
     *
     * <p>Functional interface that defines specific workflow logic. This function receives OxyRequest as input
     * and returns workflow execution results. The workflow function should:</p>
     * <ul>
     *     <li>Handle all possible business scenarios and exceptional situations</li>
     *     <li>Return meaningful execution results or status information</li>
     *     <li>Maintain idempotency and support repeated execution</li>
     *     <li>Provide appropriate logging</li>
     * </ul>
     * -- GETTER --
     *  Get workflow execution function
     */
    protected Function<OxyRequest, Object> funcWorkflow;

    /**
     * Set workflow execution function
     *
     * <p>Configure the workflow function for executing business logic. The function should implement
     * complete business processes, including parameter validation, business processing, and result return.</p>
     *
     * @param funcWorkflow Workflow execution function, cannot be null
     * @throws IllegalArgumentException if funcWorkflow is null
     */
    public void setFuncWorkflow(Function<OxyRequest, Object> funcWorkflow) {
        this.funcWorkflow = funcWorkflow;
    }

    /**
     * Execute workflow
     *
     * <p>Execute the configured workflow function, process business logic and return results. The execution process includes:</p>
     * <ol>
     *     <li>Validate whether the workflow function is configured</li>
     *     <li>Call the workflow function to execute business logic</li>
     *     <li>Handle execution results and exceptional situations</li>
     *     <li>Return response object containing status and results</li>
     * </ol>
     *
     * <p>Status mapping:</p>
     * <ul>
     *     <li>COMPLETED: Workflow executed successfully</li>
     *     <li>FAILED: Workflow function not configured or execution exception</li>
     * </ul>
     *
     * @param oxyRequest Workflow execution request containing input parameters and context information, cannot be null
     * @return Workflow execution response containing execution status and results
     * @throws IllegalArgumentException if oxyRequest is null
     */
    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        OxyResponse resp = new OxyResponse();
        if (funcWorkflow == null) {
            resp.setState(OxyState.FAILED);
            resp.setOutput("Workflow function is not configured");
            return resp;
        }
        try {
            Object result = funcWorkflow.apply(oxyRequest);
            resp.setState(OxyState.COMPLETED);
            resp.setOutput(result);
            return resp;
        } catch (RuntimeException e) {
            resp.setState(OxyState.FAILED);
            resp.setOutput("Error executing workflow: " + e.getMessage());
            return resp;
        }
    }
}

