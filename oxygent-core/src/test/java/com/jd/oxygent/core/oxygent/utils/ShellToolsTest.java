package com.jd.oxygent.core.oxygent.utils;

import com.jd.oxygent.core.oxygent.tools.ShellTools;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ShellToolsTest {

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of ShellTools.
     * <p>
     * Tests shell command execution functionality including directory listing,
     * system information queries, and error handling to verify tool correctness
     * and cross-platform compatibility.
     * </p>
     *
     */
    @Test
    void test() {
        ShellTools shellTools = new ShellTools();

        System.out.println("=== Shell Tools Test ===");

        // Test 1: Directory listing using args list (cross-platform)
        System.out.println("1. Directory listing test (using args list):");
        List<String> dirArgs = System.getProperty("os.name").toLowerCase().contains("windows")
                ? List.of("dir")
                : List.of("ls", "-la");
        String dirResult = (String) shellTools.call("run_shell_command", dirArgs, 5, null);
        System.out.println("   Directory listing result:");
        System.out.println(dirResult);

        // Test 2: Directory listing using command string (cross-platform)
        System.out.println("\n2. Directory listing test (using command string):");
        List<String> dirCommand = System.getProperty("os.name").toLowerCase().contains("windows")
                ? List.of("dir")
                : List.of("ls -la");
        String dirResult2 = (String) shellTools.call("run_shell_command", dirCommand, 5, null);
        System.out.println("   Directory listing result:");
        System.out.println(dirResult2);

//         Test 3: Current directory
        System.out.println("\n3. Current directory test:");
        String pwdCommand = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "cd"
                : "pwd";
        String pwdResult = (String) shellTools.call("execute_shell_command", pwdCommand, null);
        System.out.println("   Current directory: " + pwdResult);

        // Test 4: curl
        System.out.println("\n3. curl test:");
        String curlCommand = "curl -s \"wttr.in/Beijing?format=3\"";
        String sysResult = (String) shellTools.call("execute_shell_command", curlCommand, 3000);
        System.out.println("   curl (first 3 lines):");
        System.out.println(sysResult);

        // Test 5: Error handling
        System.out.println("\n5. Error handling test:");
        String errorResult = (String) shellTools.call("execute_shell_command", "nonexistent-command", null);
        System.out.println("   Error handling result: " + errorResult);

        // Test 6: Working directory test
        System.out.println("\n6. Working directory test:");
        String tempDir = System.getProperty("java.io.tmpdir");
        String dirTestResult = (String) shellTools.call("execute_shell_command",
                System.getProperty("os.name").toLowerCase().contains("windows")
                        ? "dir"
                        : "ls -la",
                null);
        System.out.println("   Temp directory listing:");
        System.out.println(dirTestResult);

        // Test 7: command pipline
        System.out.println("\n3. System information test:");
        String pipline = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "mkdir deleteme | echo Hello World > deleteme\\deleteme.txt"
                : "mkdir deleteme | echo Hello World > deleteme/deleteme.txt";
        String piplineResult = (String) shellTools.call("execute_shell_command", pipline, 1000);
        System.out.println("   goto to " + System.getProperty("user.dir") + " to check generated file");
        System.out.println(piplineResult);
    }
}
