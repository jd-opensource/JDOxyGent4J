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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * Base Tool Class - Abstract base class for all tools in the OxyGent framework
 *
 * <p>BaseTool is the core base class of the tool system in the OxyGent intelligent agent framework,
 * providing unified interfaces and common configurations for various functional tools.
 * Tools are special Oxy instances that typically require permission validation and have shorter timeout periods
 * to ensure security and performance.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Mandatory Permission Validation: Tools require permission checks by default</li>
 *   <li>Timeout Control: Default 60-second execution timeout</li>
 *   <li>Category Management: Unified tool category identification</li>
 *   <li>Unified Interface: Standardized execution method signatures</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <p>Adopts the template method pattern, defining a standard framework for tool execution,
 * with specific functionality implementation completed by subclasses.
 * All tools inherit the basic capabilities of BaseOxy while adding tool-specific constraints and configurations.</p>
 *
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li>Permission validation enabled by default to prevent unauthorized access</li>
 *   <li>Execution timeout set to avoid long-term blocking</li>
 *   <li>Mandatory exception handling to ensure system stability</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class FileTool extends BaseTool {
 *     public FileTool() {
 *         super("file_tool", "File operation tool");
 *         setTimeout(120.0); // File operations may require longer time
 *     }
 *
 *     @Override
 *     protected OxyResponse _execute(OxyRequest request) {
 *         // Implement specific file operation logic
 *         return processFileOperation(request);
 *     }
 * }
 *
 * // Build tool instance
 * var tool = FileTool.builder()
 *     .timeout(90.0)
 *     .isPermissionRequired(true)
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
public abstract class BaseTool extends BaseOxy {

    /**
     * Whether permission validation is required
     *
     * <p>Controls whether this tool requires permission checks. For security reasons,
     * all tools require permission validation by default.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("is_permission_required")
    @Builder.Default
    private boolean isPermissionRequired = true;

    /**
     * Tool category identifier
     *
     * <p>Used to identify the tool's type classification for system management and routing.
     * All subclasses of BaseTool are categorized as "tool" by default.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("category")
    @Builder.Default
    private String category = "tool";

    /**
     * Execution timeout (seconds)
     *
     * <p>Maximum allowed time for tool execution. Execution will be interrupted if this time is exceeded,
     * preventing tools from blocking the entire system. Default is 60 seconds.</p>
     *
     * @since 1.0.0
     */
    @JsonProperty("timeout")
    @Builder.Default
    private double timeout = 60.0;

    /**
     * Constructor - Create tool with name and description
     *
     * @param name Tool name, cannot be null or empty string
     * @param desc Tool description, cannot be null or empty string
     * @throws NullPointerException     when name or desc is null
     * @throws IllegalArgumentException when name or desc is empty string
     * @since 1.0.0
     */
    public BaseTool(String name, String desc) {
        super(BaseOxy.builder()
                .name(Objects.requireNonNull(name, "Tool name cannot be null"))
                .desc(Objects.requireNonNull(desc, "Tool description cannot be null")));

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be empty string");
        }
        if (desc.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool description cannot be empty string");
        }
    }

    /**
     * Core method for executing tool logic
     *
     * <p>This is the core abstract method for tool execution. All concrete tool classes inheriting from BaseTool
     * must implement this method. This method defines the specific functionality implementation of the tool.</p>
     *
     * <h4>Implementation Requirements:</h4>
     * <ul>
     *   <li>Must perform null and validity validation on request parameters</li>
     *   <li>Requires comprehensive exception handling and error messages</li>
     *   <li>Should comply with the tool's timeout restrictions</li>
     *   <li>Return results should contain complete execution status and output</li>
     * </ul>
     *
     * <h4>Execution Flow:</h4>
     * <ol>
     *   <li>Validate the integrity and legality of request parameters</li>
     *   <li>Check permissions (if permission validation is enabled)</li>
     *   <li>Execute specific tool functionality logic</li>
     *   <li>Build and return standardized response results</li>
     * </ol>
     *
     * @param oxyRequest Tool execution request containing input parameters and execution context
     * @return Tool execution response containing processing results, status information, and metadata
     * @throws NullPointerException     when oxyRequest is null
     * @throws IllegalArgumentException when request parameters are invalid
     * @throws SecurityException        when permission validation fails
     * @throws RuntimeException         when tool execution times out or other runtime errors occur
     * @since 1.0.0
     */
    protected abstract OxyResponse _execute(OxyRequest oxyRequest);

}