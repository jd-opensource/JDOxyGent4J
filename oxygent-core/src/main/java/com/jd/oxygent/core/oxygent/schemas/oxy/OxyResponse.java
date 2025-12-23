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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Task execution response wrapper class in OxyGent system
 *
 * <p>This class encapsulates the result information after oxy (agent/tool) execution,
 * including execution status, output content, additional information, and reference to the original request.</p>
 *
 * <p>Main purposes include:</p>
 * <ul>
 *     <li>Encapsulate execution results and status information</li>
 *     <li>Pass execution output and metadata</li>
 *     <li>Support chained calls and result passing</li>
 *     <li>Provide unified error handling interface</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * OxyResponse response = OxyResponse.builder()
 *     .state(OxyState.SUCCESS)
 *     .output("Execution successful")
 *     .extra(Map.of("duration", 1500))
 *     .build();
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OxyResponse {

    /**
     * Execution status, default is CREATED
     */
    @Builder.Default
    private OxyState state = OxyState.CREATED;

    /**
     * Execution output result, can be any type of object
     */
    private Object output;

    /**
     * Extra information mapping, used to pass metadata and extension information
     */
    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    /**
     * Associated original request object
     */
    private OxyRequest oxyRequest;

    /**
     * Construct response object with specified status and output
     *
     * @param state  Execution status, cannot be null
     * @param output Execution output, can be null
     * @throws NullPointerException if state is null
     */
    public OxyResponse(OxyState state, Object output) {
        this.state = Objects.requireNonNull(state, "Execution state cannot be null");
        this.output = output;
        this.extra = new HashMap<>();
        this.oxyRequest = null;
    }

    /**
     * Check if execution is successful
     *
     * @return true if status is SUCCESS or COMPLETED
     */
    public boolean isSuccess() {
        return state == OxyState.SUCCESS || state == OxyState.COMPLETED;
    }

    /**
     * Check if execution failed
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return state == OxyState.FAILED;
    }

    /**
     * Get string representation of output content
     *
     * @return String representation of output, empty string if output is null
     */
    public String getOutputAsString() {
        return output != null ? output.toString() : "";
    }

    /**
     * Safely get extra information
     *
     * @param key          Key name
     * @param defaultValue Default value
     * @return Corresponding value, returns default value if not exists
     */
    public Object getExtra(String key, Object defaultValue) {
        return Optional.ofNullable(extra)
                .map(map -> map.getOrDefault(key, defaultValue))
                .orElse(defaultValue);
    }

    /**
     * Add extra information
     *
     * @param key   Key name, cannot be null
     * @param value Value
     * @throws NullPointerException if key is null
     */
    public void putExtra(String key, Object value) {
        Objects.requireNonNull(key, "Extra information key cannot be null");
        if (this.extra == null) {
            this.extra = new HashMap<>();
        }
        this.extra.put(key, value);
    }

    /**
     * Shortcut method to create successful response
     *
     * @param output Output content
     * @return Response object with success status
     */
    public static OxyResponse success(Object output) {
        return OxyResponse.builder()
                .state(OxyState.SUCCESS)
                .output(output)
                .build();
    }

    /**
     * Shortcut method to create failure response
     *
     * @param errorMessage Error message
     * @return Response object with failure status
     */
    public static OxyResponse failure(String errorMessage) {
        return OxyResponse.builder()
                .state(OxyState.FAILED)
                .output(errorMessage)
                .build();
    }
}