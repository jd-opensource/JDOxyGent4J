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
package com.jd.oxygent.core.oxygent.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tool method annotation for identifying tool methods that can be called by the OxyGent framework.
 * <p>
 * This annotation is used to mark specific tool methods that the framework will automatically
 * scan and register, making them callable by name. Annotated methods need to provide clear
 * functional descriptions and parameter metadata for the framework to perform correct parameter
 * matching and invocation.
 * </p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * @Tool(
 *     name = "calculate_sum",
 *     description = "Calculate the sum of two numbers",
 *     paramMetas = {
 *         @ParamMetaAuto(name = "a", type = "Number", description = "First number"),
 *         @ParamMetaAuto(name = "b", type = "Number", description = "Second number")
 *     }
 * )
 * public double calculateSum(double a, double b) {
 *     return a + b;
 * }
 * }</pre>
 *
 * <p><strong>Important Notes:</strong></p>
 * <ul>
 *   <li>Annotated methods must have public access level</li>
 *   <li>Method names should clearly express their functionality</li>
 *   <li>Parameter descriptions should accurately reflect parameter purpose and type</li>
 *   <li>Return value types should match actual business scenarios</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see ParamMetaAuto Parameter metadata annotation
 * @see com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub Tool execution framework
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {

    /**
     * Unique identifier name of the tool method.
     * <p>
     * This name is used to uniquely identify and call the tool method within the framework.
     * It is recommended to use underscore-separated lowercase naming convention, such as:
     * calculate_sum, read_file, etc.
     * </p>
     *
     * @return Tool method name, cannot be empty
     */
    String name();

    /**
     * Functional description of the tool method.
     * <p>
     * Clearly describes the functionality, purpose, and expected behavior of this method.
     * This description will be used for:
     * <ul>
     *   <li>Automatically generating tool documentation</li>
     *   <li>Helping AI understand tool functionality</li>
     *   <li>Developer reference and debugging</li>
     * </ul>
     * </p>
     *
     * @return Functional description, recommended to use mixed Chinese and English, cannot be empty
     */
    String description();

    /**
     * Parameter metadata array for describing method parameter information.
     * <p>
     * Each parameter should have corresponding metadata description, including parameter
     * name, type, description, and default value information. If the method has no parameters,
     * the default empty array can be used.
     * </p>
     *
     * @return Parameter metadata array, defaults to empty array
     * @see ParamMetaAuto Detailed description of parameter metadata annotation
     */
    ParamMetaAuto[] paramMetas() default {};
}