package com.jd.oxygent.test.tool;

import com.jd.oxygent.core.oxygent.tools.PythonTools;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPythonTools {

    PythonTools pythonTools = new PythonTools();

    @Test
    void simpleCodeExecution() {
        String code = "result = 2 + 3";
        String variableToReturn = "result";
        Map safeGlobals = null;
        Map safeLocals = null;
        String output = (String) pythonTools.call("run_python_code", code, variableToReturn, safeGlobals, safeLocals);
        Assert.assertEquals("5", output);

        code = "x = 10";
        output = (String) pythonTools.call("run_python_code", code, "x", safeGlobals, safeLocals);
        assertEquals("10", output);

        code = "x = 5";
        output = (String) pythonTools.call("run_python_code", code, "y", safeGlobals, safeLocals);
        assertEquals("Variable y not found", output);

        code = "result = test_var * 2";
        Map<String, Object> customGlobals = Map.of("test_var", 10);
        output = (String) pythonTools.call("run_python_code", code, "result", customGlobals, safeLocals);
        assertEquals("20", output);

        code = "message = 'Hello World'";
        output = (String) pythonTools.call("run_python_code", code, "message", safeGlobals, safeLocals);
        assertEquals("Hello World", output);

        code = "numbers = [1, 2, 3, 4, 5]";
        output = (String) pythonTools.call("run_python_code", code, "numbers", safeGlobals, safeLocals);
        assertEquals("[1, 2, 3, 4, 5]", output);

        code = "flag = True";
        output = (String) pythonTools.call("run_python_code", code, "flag", safeGlobals, safeLocals);
        assertEquals("True", output);
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

    @ParameterizedTest
    @ValueSource(strings = {"test_python_tools_1.py", "test_python_tools_2.py"})
    void runScript(String fileName) throws IOException, URISyntaxException {
        // 1. 通过 ClassLoader 获取资源的 URL
        URL resource = getClass().getClassLoader().getResource(fileName);

        if (resource == null) {
            throw new IllegalArgumentException("文件未找到: " + fileName);
        }

        // 2. 将 URL 转换为 Path 并读取
        // 注意：如果资源在 Jar 包内，此方法会抛出 FileSystemNotFoundException
        String code = Files.readString(Path.of(resource.toURI()));
        String variableToReturn = null;
        Map safeGlobals = null;
        Map safeLocals = null;
        String output = (String) pythonTools.call("run_python_code", code, variableToReturn, safeGlobals, safeLocals);
        Assert.assertEquals("5", output);
    }
}
