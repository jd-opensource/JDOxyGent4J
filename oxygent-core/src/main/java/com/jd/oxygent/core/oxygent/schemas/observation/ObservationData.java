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

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyOutput;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h3>Observation Data Collector</h3>
 *
 * <p>This class is used to collect and manage observation data during the execution of OxyGent agents, including tool invocation results, execution status, etc.
 * Provides flexible data aggregation and formatting functions, supporting unified processing and output of multimodal content.</p>
 *
 * <h3>Core Functions:</h3>
 * <ul>
 *   <li><strong>Execution Result Collection</strong>: Aggregates execution results from multiple tools</li>
 *   <li><strong>Formatted Output</strong>: Supports text and multimodal content formats</li>
 *   <li><strong>Data Conversion</strong>: Converts complex results into readable formats</li>
 *   <li><strong>Attachment Handling</strong>: Unified handling of attachments during execution</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Agent execution process monitoring</li>
 *   <li>Tool invocation chain tracing</li>
 *   <li>Aggregated display of execution results</li>
 *   <li>Multimodal content integration</li>
 *   <li>Debug information collection</li>
 * </ul>
 *
 * <h3>Data Flow:</h3>
 * <ol>
 *   <li>Add individual execution results via addExecResult()</li>
 *   <li>Use toStr() to get observation data in plain text format</li>
 *   <li>Use toContent() to get content format supporting multimodal</li>
 * </ol>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ObservationData {

    /**
     * Execution result list
     * <p>Stores all tool execution results, in execution order</p>
     * <p>Each result contains executor name and corresponding response data</p>
     */
    private List<ExecResult> execResults = new ArrayList<>();

    /**
     * Default constructor
     * <p>Initializes an empty observation data collector</p>
     */
    public ObservationData() {
    }

    /**
     * Add execution result
     * <p>Adds a new execution result to the observation data, used to track the complete process of tool invocation</p>
     *
     * @param execResult Tool execution result, contains executor info and response data
     */
    public void addExecResult(ExecResult execResult) {
        execResults.add(execResult);
    }

    /**
     * Convert to string format
     * <p>Converts all execution results to human-readable text format, each result as a separate paragraph</p>
     *
     * <h4>Output Format:</h4>
     * <pre>
     * Tool [tool name] execution result: result content
     *
     * Tool [tool name2] execution result: result content 2
     * </pre>
     *
     * @return Formatted text string containing all execution results
     */
    public String toStr() {
        List<String> outs = new ArrayList<>();
        for (ExecResult execResult : execResults) {
            String prefix = "Tool [" + execResult.getExecutor() + "] execution result: ";
            Object output = execResult.getOxyResponse().getOutput();
            if (output instanceof OxyOutput) {
                outs.add(prefix + ((OxyOutput) output).getResult().toString());
            } else {
                outs.add(prefix + output.toString());
            }
        }
        return String.join("\n\n", outs);
    }

    /**
     * Convert to content format
     * <p>Converts execution results to content format supporting multimodal; return structure depends on multimodal support</p>
     *
     * <h4>Processing Logic:</h4>
     * <ul>
     *   <li>Extract text content from all execution results</li>
     *   <li>Collect all attachment data (images, files, etc.)</li>
     *   <li>Organize return data based on multimodal support</li>
     * </ul>
     *
     * <h4>Return Format:</h4>
     * <ul>
     *   <li><strong>Multimodal Mode</strong>: Returns a List structure with attachments and text</li>
     *   <li><strong>Plain Text Mode</strong>: Returns concatenated string</li>
     * </ul>
     *
     * @param isMultimodalSupported Whether multimodal content is supported
     * @return Formatted content object, type depends on multimodal support
     */
    public Object toContent(boolean isMultimodalSupported) {
        List<Object> queryAttachments = new ArrayList<>();
        List<String> outs = new ArrayList<>();

        for (ExecResult execResult : execResults) {
            String prefix = "Tool [" + execResult.getExecutor() + "] execution result: ";
            Object output = execResult.getOxyResponse().getOutput();
            if (output instanceof OxyOutput oxyOutput) {
                // Process attachments (simplified)
                queryAttachments.addAll(oxyOutput.getAttachments());
                outs.add(prefix + toJson(oxyOutput.getResult()));
            } else {
                outs.add(prefix + toJson(output));
            }
        }

        if (isMultimodalSupported && !queryAttachments.isEmpty()) {
            List<Object> result = new ArrayList<>(queryAttachments);
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", String.join("\n\n", outs));
            result.add(textContent);
            return result;
        } else {
            return String.join("\n\n", outs);
        }
    }

    /**
     * Object to JSON string
     * <p>Converts object to JSON-formatted string for unified data serialization</p>
     * <p>Note: Simplified implementation; for production use Jackson or Gson</p>
     *
     * @param obj Object to convert
     * @return JSON-formatted string; returns "null" for null objects
     */
    private String toJson(Object obj) {
        // Simplified JSON conversion - in real implementation, use Jackson or Gson
        if (obj == null) {
            return "null";
        }
        return obj.toString();
    }

}
