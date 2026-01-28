package com.jd.oxygent.core.oxygent.mcpservers.engine;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.engine.metadata.ToolMetadata;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
public class McpServer {

    // Main startup class, used to find @EnableMcpServer annotation
    private static Class<?> mainClass;
    
    // Startup mode, default is stdio
    private static String mode = "stdio";
    
    // Localhost address, default is 127.0.0.1
    private static String localhost="127.0.0.1";
    
    // Port number, default is 8080
    private static String port= "8080";
    
    // Transport protocol, default is sse
    private static String transport="sse";
    
    // Whether to automatically scan tools, enabled by default
    private static boolean autoScan = true;
    
    // Array of base package paths for scanning, default is empty
    private static String[] scanBasePackages = {};

    /**
     * Start MCP server
     */
    public static void start() {
        // Get call stack, find main class with @EnableMcpServer annotation
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        findMainClass(stackTrace);

        if (mainClass == null) {
            throw new IllegalStateException("No startup class with @EnableMcpServer annotation found");
        }

        // Parse annotation information
        parseEnableMcpServerAnnotation();

        // Get package name and class name
        String className = mainClass.getName();
        String packageName = mainClass.getPackage().getName();

        log.info("[McpServer] Startup mode: {}", mode);
        log.info("[McpServer] Startup class: {}", className);
        log.info("[McpServer] Auto scan: {}", autoScan);

        // Scan tools
        List<ToolMetadata> tools = new ArrayList<>();
        if (autoScan) {
            try {
                if (scanBasePackages.length > 0) {
                    // Case 1: Scan package paths specified -> scan specified packages
                    for (String basePackage : scanBasePackages) {
                        log.info("[McpServer] Scanning package: {}", basePackage);
                        tools.addAll(MCPToolScanner.scanPackage(basePackage));
                    }
                } else {
                    // Case 2: No scan package paths specified -> only scan the startup class itself
                    log.info("[McpServer] Only scanning startup class: {}", className);
                    tools.addAll(MCPToolScanner.scanClass(mainClass));
                }
            } catch (Exception e) {
                log.error("[McpServer] Failed to scan tools: {}", e.getMessage());
                log.debug("Error details:", e);
            }
        }

        log.info("[McpServer] Found {} tools", tools.size());

        // Start MCPServerLauncher
        MCPServerLauncher launcher = new MCPServerLauncher();
        launcher.start(mode, className, packageName, tools,localhost,port,transport);
    }

    private static void findMainClass(StackTraceElement[] stackTrace) {
        log.info("[McpServer] Finding startup class...");

        for (int i = stackTrace.length - 1; i >= 0; i--) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            String methodName = element.getMethodName();

            // Skip unnecessary classes
            if (shouldSkipClass(className)) {
                continue;
            }

            try {
                Class<?> clazz = Class.forName(className);

                // 1. First check if the class has @EnableMcpServer annotation
                EnableMcpServer classAnnotation = clazz.getAnnotation(EnableMcpServer.class);
                if (classAnnotation != null) {
                    mainClass = clazz;
                    mode = classAnnotation.mode();
                    localhost = classAnnotation.localhost();
                    port = classAnnotation.port();
                    transport = classAnnotation.transport();
                    log.info("[McpServer] ✓ Found @EnableMcpServer annotation on class: {}", className);
                    return;
                }

                // 2. If not, check if the specified method has it (e.g., main method)
                // Try to get annotations on the method
                Map<String, Method> methodMap = Arrays.stream(clazz.getDeclaredMethods())
                        .collect(Collectors.toMap(
                                Method::getName,  // key: method name
                                method -> method, // value: method object
                                (existing, replacement) -> existing // if there are duplicate methods, keep the first one
                        ));
                Method method = methodMap.get(methodName);
                EnableMcpServer methodAnnotation = method.getAnnotation(EnableMcpServer.class);
                if (methodAnnotation != null) {
                    mainClass = clazz;
                    mode = methodAnnotation.mode();
                    localhost = methodAnnotation.localhost();
                    port = methodAnnotation.port();
                    transport = methodAnnotation.transport();
                    log.info("[McpServer] ✓ Found @EnableMcpServer annotation on method: {}.{}", className, methodName + "()");
                    return;
                }
            } catch (ClassNotFoundException e) {
                log.warn("[McpServer] ! Failed to load class: {}", className);
            } catch (Exception e) {
                log.warn("[McpServer] ! Error checking class {}: {}", className, e.getMessage());
            }
        }

        // If not found, provide detailed error information
        throwStartupException(stackTrace);
    }

    private static boolean shouldSkipClass(String className) {
        // Skip inner classes, anonymous classes, and McpServer itself
        return className.contains("$") ||
                className.equals(McpServer.class.getName()) ||
                className.startsWith("java.") ||
                className.startsWith("sun.") ||
                className.startsWith("com.sun.");
    }

    private static void throwStartupException(StackTraceElement[] stackTrace) {
        log.error("[McpServer] ✗ No @EnableMcpServer annotation found");
        log.info("[McpServer] Call stack analysis:");

        // Find possible startup class candidates
        List<String> candidateClasses = new ArrayList<>();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (!shouldSkipClass(className)) {
                candidateClasses.add(className + "." + methodName + "()");
            }
        }

        if (candidateClasses.isEmpty()) {
            throw new IllegalStateException(
                    "No startup class found. Please ensure:\n" +
                            "1. Add @EnableMcpServer annotation to the startup class\n" +
                            "2. Or add @EnableMcpServer annotation to the startup method\n" +
                            "Examples:\n" +
                            "Class annotation:\n" +
                            "@EnableMcpServer(mode=\"stdio\")\n" +
                            "public class YourApplication { ... }\n\n" +
                            "Method annotation:\n" +
                            "public class YourApplication {\n" +
                            "    @EnableMcpServer(mode=\"stdio\")\n" +
                            "    public static void main(String[] args) { ... }\n" +
                            "}"
            );
        }

        // Generate useful error information
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("No @EnableMcpServer annotation found.\n\n");
        errorMsg.append("Possible startup locations (please add @EnableMcpServer annotation at the following locations):\n");

        for (String candidate : candidateClasses) {
            errorMsg.append("  - " + candidate + "\n");
        }

        errorMsg.append("\nSolutions：\n");
        errorMsg.append("1. Add @EnableMcpServer annotation to class or method\n");
        errorMsg.append("2. Or use McpServer.start(YourClass.class) to explicitly specify the startup class\n");

        throw new IllegalStateException(errorMsg.toString());
    }

    /**
     * Parse @EnableMcpServer annotation configuration
     */
    private static void parseEnableMcpServerAnnotation() {
        EnableMcpServer annotation = mainClass.getAnnotation(EnableMcpServer.class);
        if (annotation != null) {
            mode = annotation.mode();
            autoScan = annotation.autoScan();
            scanBasePackages = annotation.scanBasePackages();
        }
    }

    /**
     * Manually set startup class (for testing)
     */
    public static void setMainClass(Class<?> clazz) {
        mainClass = clazz;
    }

    /**
     * Get current mode
     */
    public static String getMode() {
        return mode;
    }
}