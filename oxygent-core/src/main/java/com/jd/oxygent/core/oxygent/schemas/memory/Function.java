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
package com.jd.oxygent.core.oxygent.schemas.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Function Call Information Wrapper Class
 *
 * <p>This class is used to encapsulate function call information in the agent system, including function name and call arguments.
 * It is typically used to record and transfer detailed information of tool/function calls.</p>
 *
 * <p>Main Uses:</p>
 * <ul>
 *     <li>Store tool/function call information</li>
 *     <li>Support serialization and deserialization of tool calls</li>
 *     <li>Provide a unified function call data structure</li>
 *     <li>Facilitate call chain tracing and debugging</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>
 * // Create function call info
 * Function func = new Function("searchTool", "{\"query\":\"user query\"}");
 *
 * // Or use constructor
 * Function func = Function.of("calculatorTool", "{\"expression\":\"2+3\"}");
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Function {

    /**
     * Function name, identifies the specific function or tool to be called
     */
    private String name;

    /**
     * Function call arguments, usually a JSON-formatted string
     */
    private String arguments;

}
