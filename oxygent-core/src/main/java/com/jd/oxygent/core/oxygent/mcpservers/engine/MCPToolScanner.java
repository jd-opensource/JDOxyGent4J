package com.jd.oxygent.core.oxygent.mcpservers.engine;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.engine.metadata.ToolMetadata;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class MCPToolScanner {

    /**
     * Scan all classes under the specified package to find methods with @MCPTool annotation
     */
    public static List<ToolMetadata> scanPackage(String packageName) throws Exception {
        List<Class<?>> classes = ClassScanner.findClasses(packageName);
        List<ToolMetadata> tools = new ArrayList<>();

        for (Class<?> clazz : classes) {
            // Check if the class has methods with @MCPTool annotation
            boolean hasMCPToolMethod = false;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(MCPTool.class)) {
                    hasMCPToolMethod = true;
                    break;
                }
            }

            if (hasMCPToolMethod) {
                tools.addAll(scanClass(clazz));
            }
        }

        return tools;
    }

    /**
     * Scan all methods in the specified class to find methods with @MCPTool annotation
     */
    public static List<ToolMetadata> scanClass(Class<?> clazz) throws Exception {
        List<ToolMetadata> tools = new ArrayList<>();

        // First check if the class has methods with @MCPTool annotation
        boolean hasMCPToolMethod = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MCPTool.class)) {
                hasMCPToolMethod = true;
                break;
            }
        }

        // If no methods with @MCPTool annotation are found, return empty list directly
        if (!hasMCPToolMethod) {
            return tools;
        }

        // Create class instance (only create when there are @MCPTool annotated methods)
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // Scan all public methods
        for (Method method : clazz.getDeclaredMethods()) {
            MCPTool annotation = method.getAnnotation(MCPTool.class);
            if (annotation != null) {
                ToolMetadata metadata = new ToolMetadata(annotation, method, instance);
                tools.add(metadata);
                log.info("[MCPToolScanner] Found tool: {}", annotation.name());
            }
        }

        return tools;
    }

    /**
     * Scan the specified tool class instance
     */
    public static List<ToolMetadata> scanInstance(Object instance) {
        List<ToolMetadata> tools = new ArrayList<>();
        Class<?> clazz = instance.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            MCPTool annotation = method.getAnnotation(MCPTool.class);
            if (annotation != null) {
                ToolMetadata metadata = new ToolMetadata(annotation, method, instance);
                tools.add(metadata);
                log.info("[MCPToolScanner] Found tool: {}", annotation.name());
            }
        }

        return tools;
    }
}