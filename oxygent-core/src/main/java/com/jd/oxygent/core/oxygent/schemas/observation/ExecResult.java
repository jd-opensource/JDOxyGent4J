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
package com.jd.oxygent.core.oxygent.schemas.observation;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>Execution Result Data Model</h3>
 *
 * <p>This class encapsulates the execution result of a single tool or component, used to standardize tool invocation return data in the OxyGent system.
 * Contains the executor identifier and response content, supporting unified result processing and analysis.</p>
 *
 * <h3>Data Structure:</h3>
 * <ul>
 *   <li><strong>executor</strong>: Executor name, identifies the specific tool or component</li>
 *   <li><strong>oxyResponse</strong>: OxyGent standard response, contains detailed execution result</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Tool invocation result encapsulation</li>
 *   <li>Execution chain trace recording</li>
 *   <li>Batch result aggregation processing</li>
 *   <li>Execution status monitoring and analysis</li>
 * </ul>
 *
 * <h3>Typical Executor Types:</h3>
 * <ul>
 *   <li>search: search tool</li>
 *   <li>calculator: calculation tool</li>
 *   <li>file_reader: file reading tool</li>
 *   <li>api_caller: API calling tool</li>
 *   <li>code_runner: code execution tool</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ExecResult result = new ExecResult(
 *     "search_engine",
 *     searchResponse
 * );
 * observationData.addExecResult(result);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecResult {

    /**
     * Executor name
     * <p>Identifies the tool or component that performed the specific task, used to distinguish different types of execution results</p>
     *
     * <h4>Naming Conventions:</h4>
     * <ul>
     *   <li>Use lowercase letters and underscores</li>
     *   <li>Name should be descriptive and clearly express tool function</li>
     *   <li>Avoid special characters and spaces</li>
     * </ul>
     *
     * <h4>Example Values:</h4>
     * <ul>
     *   <li>"web_search": Web search tool</li>
     *   <li>"file_upload": File upload tool</li>
     *   <li>"data_analyzer": Data analysis tool</li>
     * </ul>
     */
    String executor;

    /**
     * OxyGent response object
     * <p>Contains detailed result data from tool execution, following OxyGent system standard response format</p>
     *
     * <h4>Response content includes:</h4>
     * <ul>
     *   <li><strong>Status Info</strong>: Execution success/failure status</li>
     *   <li><strong>Output Data</strong>: Actual execution result content</li>
     *   <li><strong>Metadata</strong>: Execution time, performance metrics, etc.</li>
     *   <li><strong>Error Info</strong>: Detailed error description if failed</li>
     * </ul>
     *
     * <p>OxyResponse provides access to the complete execution context and result data</p>
     */
    OxyResponse oxyResponse;
}
