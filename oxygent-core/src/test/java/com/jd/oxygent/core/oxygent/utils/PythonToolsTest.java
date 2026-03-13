package com.jd.oxygent.core.oxygent.utils;

import com.jd.oxygent.core.oxygent.tools.PythonTools;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PythonToolsTest {

    PythonTools pythonTools = new PythonTools();

    @Test
    void simpleCodeExecution() {
        String code = "result = 2 + 3";
        String variableToReturn = "result";
        Map safeGlobals = null;
        Map safeLocals = null;
        String result = (String) pythonTools.call("run_python_code", code, variableToReturn, safeGlobals, safeLocals);
        Assert.assertEquals("5", result);
    }

    @Test
    void errorHandling() {
        String code = "raise ValueError('Test error')";
        String variableToReturn = null;
        Map safeGlobals = null;
        Map safeLocals = null;
        String result = (String) pythonTools.call("run_python_code", code, variableToReturn, safeGlobals, safeLocals);
        Assert.assertEquals("Error executing Python code: ValueError: Test error", result);
    }

    @Test
    void globalAndLocalVariables() {
        String code = """
                result = global_var * local_var
                """;
        String variableToReturn = "result";
        Map safeGlobals = Map.of("global_var", 11);
        Map safeLocals = Map.of("local_var", 13);
        String result = (String) pythonTools.call("run_python_code", code, variableToReturn, safeGlobals, safeLocals);
        Assert.assertEquals("143", result);
    }
}
