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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter metadata auto-generation annotation for describing parameter information of tool methods.
 * <p>
 * This annotation is specifically used to describe detailed information about method parameters
 * in the paramMetas property of {@link Tool} annotation, including parameter name, type,
 * description, and default value. The framework performs parameter matching, type conversion,
 * and validation based on this metadata information.
 * </p>
 *
 * <p><strong>Supported Parameter Types:</strong></p>
 * <ul>
 *   <li>String - String type (default)</li>
 *   <li>Number - Numeric type (including int, double, long, etc.)</li>
 *   <li>Boolean - Boolean type</li>
 *   <li>Object - Complex object type</li>
 *   <li>Array - Array type</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * @Tool(
 *     name = "process_data",
 *     description = "Process data and return result",
 *     paramMetas = {
 *         @ParamMetaAuto(
 *             name = "input",
 *             type = "String",
 *             description = "Input data content"
 *         ),
 *         @ParamMetaAuto(
 *             name = "count",
 *             type = "Number",
 *             description = "Processing count",
 *             defaultValue = "1"
 *         ),
 *         @ParamMetaAuto(
 *             name = "enabled",
 *             type = "Boolean",
 *             description = "Whether to enable processing",
 *             defaultValue = "true"
 *         )
 *     }
 * )
 * public String processData(String input, int count, boolean enabled) {
 *     // Method implementation
 * }
 * }</pre>
 *
 * <p><strong>Important Notes:</strong></p>
 * <ul>
 *   <li>Parameter names must match actual method parameter names</li>
 *   <li>Type descriptions should accurately reflect actual parameter types</li>
 *   <li>Default values must be convertible to corresponding parameter types</li>
 *   <li>Description information should clearly explain parameter purpose and constraints</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see Tool Tool method annotation
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // Only for internal annotation use, not directly annotating code
public @interface ParamMetaAuto {

    /**
     * Parameter name, must match the actual parameter name of the method.
     * <p>
     * This name is used for parameter matching and binding. The framework maps
     * input parameter values to corresponding method parameters based on this name.
     * Therefore, accuracy of the name must be ensured.
     * </p>
     *
     * @return Parameter name, cannot be empty
     */
    String name();

    /**
     * Parameter type description.
     * <p>
     * Used to describe the data type of the parameter, supports description of
     * basic types and complex types. Common types include: String, Number, Boolean,
     * Object, Array, etc. The framework performs parameter validation and type
     * conversion based on this type information.
     * </p>
     *
     * @return Parameter type, defaults to "String"
     */
    String type() default "String";

    /**
     * Parameter functionality description.
     * <p>
     * Detailed explanation of the parameter's purpose, role, and possible constraint
     * conditions. This description will be used for generating documentation and
     * helping developers understand the parameter's meaning. Clear functional
     * descriptions are recommended.
     * </p>
     *
     * @return Parameter description, can be empty but recommended to provide
     */
    String description() default "";

    /**
     * Parameter default value.
     * <p>
     * When the caller does not provide this parameter value, the framework will use
     * this default value. The default value must be a string representation that can
     * be correctly converted to the corresponding parameter type. For example: use
     * "0", "1.5" for numeric types, use "true", "false" for boolean types.
     * </p>
     *
     * @return String representation of default value, empty string indicates no default value
     */
    String defaultValue() default "";
}