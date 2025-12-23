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

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <h3>OxyGent Agent Output Data Model</h3>
 *
 * <p>This class defines the standardized output format for OxyGent agents after task execution,
 * used to encapsulate Agent execution results and related attachments.
 * It provides a flexible data structure to support various types of task outputs, including text, structured data, files, etc.</p>
 *
 * <h3>Output Structure Description:</h3>
 * <ul>
 *   <li><strong>result</strong>: Main execution result, supports any type of data output</li>
 *   <li><strong>attachments</strong>: Attachment list, stores additional data generated during execution</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Agent task execution result encapsulation</li>
 *   <li>Tool call output standardization</li>
 *   <li>Complex task result aggregation</li>
 *   <li>Multimedia content output processing</li>
 * </ul>
 *
 * <h3>Result Type Examples:</h3>
 * <ul>
 *   <li>Text answers: String type direct replies</li>
 *   <li>Structured data: Map or custom objects</li>
 *   <li>List data: List type batch results</li>
 *   <li>Composite results: Composite objects containing multiple data types</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * OxyOutput output = OxyOutput.builder()
 *     .result("Task execution successful")
 *     .attachments(Arrays.asList(imageData, reportFile))
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class OxyOutput {

    /**
     * Main execution result
     * <p>Stores the core output content of Agent task execution, supports any type of data structure</p>
     *
     * <h4>Common Result Types:</h4>
     * <ul>
     *   <li><strong>String</strong>: Text answers, summaries, analysis results</li>
     *   <li><strong>Map</strong>: Structured data, configuration information, status reports</li>
     *   <li><strong>List</strong>: Batch processing results, search result lists</li>
     *   <li><strong>Object</strong>: Custom business objects, composite data structures</li>
     * </ul>
     *
     * <h4>Usage Notes:</h4>
     * <p>Callers need to parse the actual data type of result based on the specific task type</p>
     */
    private Object result;

    /**
     * Attachment list
     * <p>Stores additional data generated during execution, such as generated files, intermediate results, debug information, etc.</p>
     *
     * <h4>Attachment Type Examples:</h4>
     * <ul>
     *   <li><strong>File Data</strong>: Generated images, documents, report files</li>
     *   <li><strong>Intermediate Results</strong>: Reasoning processes, calculation steps, decision chains</li>
     *   <li><strong>Metadata</strong>: Execution time, performance metrics, error logs</li>
     *   <li><strong>Reference Materials</strong>: Related links, reference documents, data sources</li>
     * </ul>
     *
     * <h4>Data Organization:</h4>
     * <p>Each attachment typically contains type identification and actual data, facilitating categorized processing by callers</p>
     */
    private List<Object> attachments;
}
