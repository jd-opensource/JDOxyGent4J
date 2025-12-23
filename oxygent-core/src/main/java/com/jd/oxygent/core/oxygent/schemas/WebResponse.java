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
package com.jd.oxygent.core.oxygent.schemas;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

/**
 * <h3>Unified Web Response Format Model</h3>
 *
 * <p>This class defines the unified response format for all web interfaces in the OxyGent system,
 * standardizing API response structures. It uses the Builder pattern for flexible response data encapsulation.</p>
 *
 * <h3>Response Structure Description:</h3>
 * <ul>
 *   <li><strong>code</strong>: HTTP status code, indicates the result of request processing</li>
 *   <li><strong>message</strong>: Response message, provides human-readable status description</li>
 *   <li><strong>data</strong>: Response data payload, stores business data as key-value pairs</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>RESTful API response encapsulation</li>
 *   <li>Agent execution result return</li>
 *   <li>System status information transfer</li>
 *   <li>Standardized error information output</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * WebResponse response = WebResponse.builder()
 *     .code(200)
 *     .message("Operation successful")
 *     .data("result", executionResult)
 *     .data("timestamp", System.currentTimeMillis())
 *     .build();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class WebResponse {

    /**
     * HTTP status code
     * <p>Indicates the result state of request processing, default is 200 for success</p>
     * <ul>
     *   <li>200: Request successful</li>
     *   <li>400: Client request error</li>
     *   <li>500: Internal server error</li>
     * </ul>
     */
    @Builder.Default
    private int code = 200;

    /**
     * Response message
     * <p>Provides human-readable response status description, used for frontend display or log records</p>
     * <p>Default value is "SUCCESS", indicating operation successful</p>
     */
    @Builder.Default
    private String message = "SUCCESS";

    /**
     * Response data payload
     * <p>Stores actual business data, uses Map structure for flexible data organization</p>
     * <p>Supports single data item addition in Builder mode via @Singular annotation</p>
     *
     * <h4>Common Data Types:</h4>
     * <ul>
     *   <li>result: Main execution result</li>
     *   <li>list: List data</li>
     *   <li>total: Data total count</li>
     *   <li>timestamp: Timestamp</li>
     * </ul>
     */
    @Singular("data")
    private Map<String, Object> data;
}