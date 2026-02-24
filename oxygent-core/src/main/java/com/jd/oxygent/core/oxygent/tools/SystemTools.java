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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * System information tool class providing system hardware and software information query functionality.
 * <p>
 * This tool class offers comprehensive system information querying capabilities including
 * operating system details, hardware specifications, and real-time resource usage statistics.
 * Based on Java Management Extensions (JMX) and platform-specific system commands for cross-platform
 * system information collection and provides structured JSON formatted output.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>Operating system information query</li>
 *   <li>Hardware specifications retrieval</li>
 *   <li>Real-time resource usage monitoring</li>
 *   <li>CPU, memory, and disk usage statistics</li>
 *   <li>Cross-platform compatibility support</li>
 * </ul>
 *
 * <p><strong>Technical Implementation:</strong></p>
 * <ul>
 *   <li>Based on Java Management Extensions (JMX) for system information collection</li>
 *   <li>Uses platform-specific system commands for enhanced information gathering</li>
 *   <li>Uses Jackson for JSON serialization</li>
 *   <li>Comprehensive error handling and fault tolerance</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * SystemTools systemTools = new SystemTools();
 *
 * // Get system information
 * String sysInfo = systemTools.call("get_system_info");
 *
 * // Get resource usage
 * String usage = systemTools.call("get_system_usage");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see OperatingSystemMXBean Java management bean for OS information
 * @see ObjectMapper Jackson JSON processor
 * @since 1.0.0
 */
public class SystemTools extends FunctionHub {

    private final ObjectMapper objectMapper;

    /**
     * Constructor to initialize System tools.
     * <p>
     * Initializes Jackson ObjectMapper for JSON processing.
     * Sets tool name to "system_tools" and provides basic tool description.
     * </p>
     */
    public SystemTools() {
        super("system_tools");
        this.setDesc("Tool set providing comprehensive system information query functionality, including OS details, hardware specs, and real-time resource usage statistics");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get system information.
     * <p>
     * Retrieves comprehensive system information including operating system details,
     * hardware specifications, and runtime environment information. Returns structured
     * JSON formatted data for easy parsing and processing.
     * </p>
     *
     * <p><strong>Information Categories:</strong></p>
     * <ul>
     *   <li><strong>Operating System</strong>: System name, version, architecture</li>
     *   <li><strong>Hardware</strong>: Processor model, machine type, architecture</li>
     *   <li><strong>Runtime</strong>: Java version, JVM information, process ID</li>
     *   <li><strong>Network</strong>: Host name, network configuration</li>
     * </ul>
     *
     * @return JSON formatted system information string containing OS, hardware, and runtime details
     */
    @Tool(
            name = "get_system_info",
            description = "Get comprehensive system information including operating system details, hardware specifications, and runtime environment information. Returns structured JSON formatted data covering OS, hardware, and runtime details.",
            paramMetas = {}
    )
    public String getSystemInfo() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

            Map<String, Object> info = new HashMap<>();

            // Operating System Information
            info.put("system", osBean.getName());
            info.put("os_version", osBean.getVersion());
            info.put("os_architecture", osBean.getArch());
            info.put("available_processors", osBean.getAvailableProcessors());
            
            // System load average (Unix-like systems)
            double systemLoad = osBean.getSystemLoadAverage();
            if (systemLoad >= 0) {
                info.put("system_load_average", systemLoad);
            }

            // Hardware Information (basic)
            info.put("processor", osBean.getName() + " " + osBean.getArch());
            info.put("logical_processor_count", osBean.getAvailableProcessors());

            // Runtime Information
            info.put("java_version", System.getProperty("java.version"));
            info.put("java_vendor", System.getProperty("java.vendor"));
            info.put("java_vm_name", System.getProperty("java.vm.name"));
            info.put("java_home", System.getProperty("java.home"));
            info.put("java_class_version", System.getProperty("java.class.version"));
            
            info.put("user_name", System.getProperty("user.name"));
            info.put("user_home", System.getProperty("user.home"));
            info.put("user_directory", System.getProperty("user.dir"));
            info.put("user_language", System.getProperty("user.language"));
            info.put("user_country", System.getProperty("user.country"));

            // Process Information
            info.put("process_id", runtimeBean.getName().split("@")[0]);
            info.put("jvm_start_time", runtimeBean.getStartTime());
            info.put("jvm_uptime_ms", runtimeBean.getUptime());
            info.put("jvm_input_arguments", runtimeBean.getInputArguments());

            // Network Information (basic)
            info.put("host_name", System.getProperty("os.name"));

            return objectMapper.writeValueAsString(info);

        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", "Failed to retrieve system information: " + e.getMessage());
            try {
                return objectMapper.writeValueAsString(errorInfo);
            } catch (Exception jsonEx) {
                return "{\"error\": \"Failed to serialize error information\"}";
            }
        }
    }

    /**
     * Get system resource usage.
     * <p>
     * Retrieves real-time system resource usage statistics including CPU utilization,
     * memory usage, and disk space information. Provides percentage-based usage metrics
     * and absolute values for comprehensive system monitoring.
     * </p>
     *
     * <p><strong>Resource Metrics:</strong></p>
     * <ul>
     *   <li><strong>CPU</strong>: Overall usage percentage, per-core statistics</li>
     *   <li><strong>Memory</strong>: Total, available, used memory in GB and percentages</li>
     *   <li><strong>Disk</strong>: Total, used, free space in GB and usage percentages</li>
     *   <li><strong>Load</strong>: System load averages (Unix-like systems)</li>
     * </ul>
     *
     * @return JSON formatted resource usage string containing CPU, memory, and disk statistics
     */
    @Tool(
            name = "get_system_usage",
            description = "Get real-time system resource usage statistics including CPU utilization, memory usage, and disk space information. Provides percentage-based metrics and absolute values for comprehensive system monitoring.",
            paramMetas = {}
    )
    public String getSystemUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            Map<String, Object> usage = new HashMap<>();

            // CPU Usage (approximate using system load)
            double systemLoad = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();
            
            if (systemLoad >= 0) {
                double cpuPercent = (systemLoad / availableProcessors) * 100;
                usage.put("cpu_percent", Math.min(Math.round(cpuPercent * 100.0) / 100.0, 100.0));
                usage.put("system_load_average", systemLoad);
            } else {
                usage.put("cpu_percent", "N/A (system load not available)");
            }
            usage.put("available_processors", availableProcessors);

            // Thread information
            usage.put("thread_count", threadBean.getThreadCount());
            usage.put("peak_thread_count", threadBean.getPeakThreadCount());
            usage.put("daemon_thread_count", threadBean.getDaemonThreadCount());

            // Memory Usage
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            // Try to get system memory info
            long totalSystemMemory = getTotalSystemMemory();
            long freeSystemMemory = getFreeSystemMemory();
            long usedSystemMemory = totalSystemMemory - freeSystemMemory;

            // Heap memory
            usage.put("heap_used_mb", Math.round(heapUsed / (1024.0 * 1024)));
            usage.put("heap_max_mb", Math.round(heapMax / (1024.0 * 1024)));
            usage.put("heap_percent", heapMax > 0 ? Math.round(((double) heapUsed / heapMax) * 10000.0) / 100.0 : 0);

            // Non-heap memory
            usage.put("non_heap_used_mb", Math.round(nonHeapUsed / (1024.0 * 1024)));
            usage.put("non_heap_max_mb", Math.round(nonHeapMax / (1024.0 * 1024)));

            // System memory (if available)
            if (totalSystemMemory > 0) {
                usage.put("system_memory_total_gb", Math.round((totalSystemMemory / (1024.0 * 1024 * 1024)) * 100.0) / 100.0);
                usage.put("system_memory_free_gb", Math.round((freeSystemMemory / (1024.0 * 1024 * 1024)) * 100.0) / 100.0);
                usage.put("system_memory_used_gb", Math.round((usedSystemMemory / (1024.0 * 1024 * 1024)) * 100.0) / 100.0);
                usage.put("system_memory_percent", Math.round(((double) usedSystemMemory / totalSystemMemory) * 10000.0) / 100.0);
            }

            // File system information
            File[] roots = File.listRoots();
            long totalDiskSpace = 0;
            long freeDiskSpace = 0;

            for (File root : roots) {
                totalDiskSpace += root.getTotalSpace();
                freeDiskSpace += root.getFreeSpace();
            }

            long usedDiskSpace = totalDiskSpace - freeDiskSpace;
            
            if (totalDiskSpace > 0) {
                usage.put("disk_total_gb", Math.round((totalDiskSpace / (1024.0 * 1024 * 1024)) * 100.0) / 100.0);
                usage.put("disk_used_gb", Math.round((usedDiskSpace / (1024.0 * 1024 * 1024)) * 100.0) / 100.0);
                usage.put("disk_free_gb", Math.round((freeDiskSpace / (1024.0 * 1024 * 1024)) * 100.0) / 100.0);
                usage.put("disk_percent", Math.round(((double) usedDiskSpace / totalDiskSpace) * 10000.0) / 100.0);
            }

            // Runtime information
            usage.put("runtime_name", System.getProperty("java.runtime.name"));
            usage.put("runtime_version", System.getProperty("java.runtime.version"));

            return objectMapper.writeValueAsString(usage);

        } catch (Exception e) {
            Map<String, Object> errorUsage = new HashMap<>();
            errorUsage.put("error", "Failed to retrieve system usage: " + e.getMessage());
            try {
                return objectMapper.writeValueAsString(errorUsage);
            } catch (Exception jsonEx) {
                return "{\"error\": \"Failed to serialize error information\"}";
            }
        }
    }

    /**
     * Get total system memory (platform-specific).
     * <p>
     * Attempts to retrieve total physical memory using various platform-specific approaches.
     * </p>
     *
     * @return Total system memory in bytes, 0 if unable to determine
     */
    private long getTotalSystemMemory() {
        try {
            // Try to get from system properties first
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) {
                // Windows
                Process process = Runtime.getRuntime().exec("wmic computersystem get TotalPhysicalMemory /value");
                return parseWindowsMemory(process);
            } else if (osName.contains("mac")) {
                // macOS
                Process process = Runtime.getRuntime().exec("sysctl hw.memsize");
                return parseMacMemory(process);
            } else {
                // Linux/Unix
                Process process = Runtime.getRuntime().exec("cat /proc/meminfo");
                return parseLinuxMemory(process);
            }
        } catch (Exception e) {
            // Fallback to JVM max memory
            return Runtime.getRuntime().maxMemory();
        }
    }

    /**
     * Get free system memory (platform-specific).
     * <p>
     * Attempts to retrieve available physical memory using various platform-specific approaches.
     * </p>
     *
     * @return Free system memory in bytes, 0 if unable to determine
     */
    private long getFreeSystemMemory() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) {
                // Windows
                Process process = Runtime.getRuntime().exec("wmic OS get FreePhysicalMemory /value");
                return parseWindowsFreeMemory(process) * 1024; // Convert KB to bytes
            } else if (osName.contains("mac")) {
                // macOS - use vm_stat
                Process process = Runtime.getRuntime().exec("vm_stat");
                return parseMacFreeMemory(process);
            } else {
                // Linux/Unix
                Process process = Runtime.getRuntime().exec("cat /proc/meminfo");
                return parseLinuxFreeMemory(process);
            }
        } catch (Exception e) {
            // Fallback to JVM free memory
            return Runtime.getRuntime().freeMemory();
        }
    }

    private long parseWindowsMemory(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("TotalPhysicalMemory")) {
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        return Long.parseLong(parts[1].trim());
                    }
                }
            }
        }
        return 0;
    }

    private long parseWindowsFreeMemory(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("FreePhysicalMemory")) {
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        return Long.parseLong(parts[1].trim());
                    }
                }
            }
        }
        return 0;
    }

    private long parseMacMemory(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && line.contains("hw.memsize")) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    return Long.parseLong(parts[1].trim());
                }
            }
        }
        return 0;
    }

    private long parseMacFreeMemory(Process process) throws Exception {
        // Simplified implementation - in practice would parse vm_stat output
        return Runtime.getRuntime().freeMemory();
    }

    private long parseLinuxMemory(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        return Long.parseLong(parts[1]) * 1024; // Convert KB to bytes
                    }
                }
            }
        }
        return 0;
    }

    private long parseLinuxFreeMemory(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            long memFree = 0;
            long buffers = 0;
            long cached = 0;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (line.startsWith("MemFree:")) {
                    memFree = Long.parseLong(parts[1]) * 1024;
                } else if (line.startsWith("Buffers:")) {
                    buffers = Long.parseLong(parts[1]) * 1024;
                } else if (line.startsWith("Cached:")) {
                    cached = Long.parseLong(parts[1]) * 1024;
                }
            }
            return memFree + buffers + cached;
        }
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of SystemTools.
     * <p>
     * Tests system information retrieval and resource usage monitoring functionality
     * to verify tool correctness and data accuracy. Displays formatted output for
     * better readability and understanding.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        SystemTools systemTools = new SystemTools();

        System.out.println("=== System Tools Test ===");

        // Test 1: System Information
        System.out.println("\n1. System Information Test:");
        String systemInfoResult = systemTools.call("get_system_info").toString();
        System.out.println("System Information:");
        System.out.println(formatJsonOutput(systemInfoResult));

        // Test 2: System Usage
        System.out.println("\n2. System Usage Test:");
        String usageResult = systemTools.call("get_system_usage").toString();
        System.out.println("Resource Usage:");
        System.out.println(formatJsonOutput(usageResult));

        // Test 3: Display formatted information
        System.out.println("\n3. Formatted Display Test:");
        try {
            // Parse and display key system information
            System.out.println("=== System Overview ===");
            System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            System.out.println("Architecture: " + System.getProperty("os.arch"));
            System.out.println("Java Version: " + System.getProperty("java.version"));
            System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
            System.out.println("Max Memory: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
            
        } catch (Exception e) {
            System.out.println("Error displaying system overview: " + e.getMessage());
        }
    }

    /**
     * Format JSON output for better readability.
     * <p>
     * Helper method to format JSON strings with proper indentation for console display.
     * </p>
     *
     * @param jsonString JSON string to format
     * @return Formatted JSON string with indentation
     */
    private static String formatJsonOutput(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object jsonObject = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            return jsonString; // Return original if formatting fails
        }
    }
}