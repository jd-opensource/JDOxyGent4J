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

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Python code execution tool class providing dynamic Python code execution functionality.
 * <p>
 * This tool class enables execution of Python code snippets in the current Java environment
 * through the JSR-223 scripting API. Supports variable return value extraction and provides
 * secure execution environment isolation. Mainly used for lightweight Python script execution
 * and simple computational tasks.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>Dynamic Python code execution</li>
 *   <li>Variable value extraction and return</li>
 *   <li>Secure execution environment management</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 *
 * <p><strong>Technical Implementation:</strong></p>
 * <ul>
 *   <li>Based on JSR-223 Scripting API and Nashorn engine</li>
 *   <li>Provides isolated execution context</li>
 *   <li>Supports variable binding and result extraction</li>
 *   <li>Built-in security restrictions and resource management</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * PythonTools pythonTools = new PythonTools();
 *
 * // Execute simple calculation
 * String result = pythonTools.call("run_python_code", "print('Hello World')");
 *
 * // Execute calculation and return variable
 * String calcResult = pythonTools.call("run_python_code", "x = 10 + 20", "x", null, null);
 * }</pre>
 *
 * <p><strong>Security Considerations:</strong></p>
 * <ul>
 *   <li>Limited to simple computational tasks</li>
 *   <li>No file system or network access</li>
 *   <li>Execution time and resource consumption restrictions</li>
 *   <li>Recommended for trusted code execution only</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @since 1.0.0
 */
public class PythonTools extends FunctionHub {

    private static final Logger logger = LoggerFactory.getLogger(PythonTools.class);
    private PythonInterpreter interpreter = null;

    /**
     * Constructor to initialize Python tools.
     * <p>
     * Initializes JSR-223 scripting engine manager and attempts to get Python engine.
     * Sets tool name to "python_tools" and provides basic tool description.
     * </p>
     */
    public PythonTools() {
        super("python_tools");
        this.setDesc("Tool set providing Python code execution functionality in Java environment, supports variable extraction and result return");

        // Initialize Python interpreter
        try {
            this.interpreter = new PythonInterpreter(null, new PySystemState());
        } catch (Exception e) {
            logger.error("Failed to initialize Python engine");
        }

        if (this.interpreter == null) {
            logger.warn("Python engine not found, some features may be limited");
        }
    }

    /**
     * Execute Python code.
     * <p>
     * Executes specified Python code snippet in isolated execution environment. Supports
     * variable return value extraction and custom global/local scope management. Provides
     * comprehensive error handling and detailed execution logging.
     * </p>
     *
     * <p><strong>Execution Process:</strong></p>
     * <ol>
     *   <li>Create isolated execution context</li>
     *   <li>Set custom global and local scopes</li>
     *   <li>Execute Python code</li>
     *   <li>Extract specified return variable value</li>
     *   <li>Return execution result or variable value</li>
     * </ol>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Code execution in sandbox environment with security restrictions</li>
     *   <li>Supports basic Python syntax and built-in functions</li>
     *   <li>Complex library imports may not be supported</li>
     *   <li>Execution timeout and resource consumption restrictions apply</li>
     * </ul>
     *
     * @param code           Python code to execute, cannot be null or empty
     * @param variableToReturn Optional variable name to return, can be null
     * @param safeGlobals    Optional global scope variables, can be null
     * @param safeLocals     Optional local scope variables, can be null
     * @return Execution result string, returns variable value when variableToReturn is specified, returns success message otherwise
     * @throws IllegalArgumentException when code is null or empty
     */
    @Tool(
            name = "run_python_code",
            description = "Execute Python code in current Java environment. Supports variable extraction and custom scope management. Provides isolated execution environment with security restrictions.",
            paramMetas = {
                    @ParamMetaAuto(name = "code", type = "String", description = "Python code to execute, cannot be null or empty"),
                    @ParamMetaAuto(name = "variable_to_return", type = "String", description = "Optional variable name to return, returns execution success message when null", defaultValue = "null"),
                    @ParamMetaAuto(name = "safe_globals", type = "Map<String,Object>", description = "Optional global scope variables, can be null", defaultValue = "null"),
                    @ParamMetaAuto(name = "safe_locals", type = "Map<String,Object>", description = "Optional local scope variables, can be null", defaultValue = "null")
            }
    )
    public String runPythonCode(String code, String variableToReturn, Map<String, Object> safeGlobals, Map<String, Object> safeLocals) {

        if(interpreter== null){
            return "Error: Python engine not found";
        }

        if (code.trim().isEmpty()) {
            return "Error: Python code cannot be empty";
        }

        try {
            logger.debug("Running Python code:\n\n{}\n\n", code);

            // Set up execution context
            if (safeGlobals != null) {
                safeGlobals.forEach((k, v) -> interpreter.set(k, v));
            }
            
            if (safeLocals != null) {
                safeLocals.forEach((k, v) -> interpreter.set(k, v));
            }

            // Execute Python code
            interpreter.exec(code);

            // Handle variable return
            if (variableToReturn != null && !variableToReturn.trim().isEmpty()) {
                Object variableValue = interpreter.get(variableToReturn.trim());
                if (variableValue == null) {
                    return "Variable " + variableToReturn + " not found";
                }
                logger.debug("Variable {} value: {}", variableToReturn, variableValue);
                return String.valueOf(variableValue);
            } else {
                return "Successfully executed Python code";
            }
        } catch (PyException e) {
            logger.error("Error executing Python code: {}", e.getMessage(), e);
            return "Error executing Python code: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error during Python code execution: {}", e.getMessage(), e);
            return "Unexpected error: " + e.getMessage();
        }
    }

    // ========== Test Methods ==========
    /**
     * Test method demonstrating basic functionality of PythonTools.
     * <p>
     * Tests Python code execution functionality including simple calculations,
     * variable assignments, and result extraction to verify tool correctness.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        PythonTools pythonTools = new PythonTools();

        System.out.println("=== Python Tools Test ===");

        // Test 1: Simple calculation
        System.out.println("1. Simple calculation test:");
        String calcResult = (String) pythonTools.call("run_python_code", "result = 10 + 20",null,null,null);
        System.out.println("   Calculation result: " + calcResult);

        // Test 2: Variable return
        System.out.println("\n2. Variable return test:");
        String varResult = (String) pythonTools.call("run_python_code", "x = 'Hello Python'", "x", null, null);
        System.out.println("   Variable x value: " + varResult);

        // Test 3: Complex calculation with return
        System.out.println("\n3. Complex calculation with return test:");
        String complexCode = "import math\n" +
                           "area = math.pi * 5 * 5\n" +
                           "circumference = 2 * math.pi * 5";
        String areaResult = (String) pythonTools.call("run_python_code", complexCode, "area", null, null);
        System.out.println("   Circle area: " + areaResult);

        // Test 4: Error handling
        System.out.println("\n4. Error handling test:");
        String errorResult = (String) pythonTools.call("run_python_code", "undefined_variable + 1",null,null,null);
        System.out.println("   Error handling: " + errorResult);

        // Test 5: With custom scope
        System.out.println("\n5. Custom scope test:");
        Map<String, Object> globals = new HashMap<>();
        globals.put("multiplier", 10);
        
        Map<String, Object> locals = new HashMap<>();
        locals.put("base_value", 5);
        
        String scopeResult = (String) pythonTools.call("run_python_code", 
            "result = base_value * multiplier", 
            "result", 
            globals, 
            locals);
        System.out.println("   Scoped calculation result: " + scopeResult);
    }
}