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
package com.jd.oxygent.core.oxygent.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Environment Variable Management Utility Class
 *
 * <h3>Functional Description</h3>
 * <ul>
 *   <li>Provides unified interface for environment variable retrieval and parsing</li>
 *   <li>Supports automatic conversion and validation for multiple data types</li>
 *   <li>Provides convenient methods for common system configuration retrieval</li>
 *   <li>Supports default value mechanism to improve system robustness</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li>Type Safety: Supports automatic conversion for String and List<String> types</li>
 *   <li>Default Value Support: Uses default values when environment variables are not set</li>
 *   <li>Exception Handling: Provides comprehensive error handling and logging</li>
 *   <li>System Integration: Provides predefined methods for common system parameters</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li>Application Configuration Management: Retrieve database connections, service ports, etc.</li>
 *   <li>Environment Adaptation: Adjust behavior based on different environments (development/testing/production)</li>
 *   <li>System Monitoring: Retrieve CPU core count, memory configuration, and other system information</li>
 *   <li>Service Discovery: Retrieve server lists, cluster configurations, and other information</li>
 * </ul>
 *
 * <h3>Supported Environment Variable Types</h3>
 * <ul>
 *   <li>String: Single string value</li>
 *   <li>List<String>: Comma-separated string list</li>
 *   <li>Integer: Integer value (converted from String)</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class EnvUtils {

    private static final Logger logger = LoggerFactory.getLogger(EnvUtils.class);

    /**
     * Get string value of environment variable
     * <p>
     * This is the most basic environment variable retrieval method that directly reads
     * the value of the specified key from system environment variables.
     * If the environment variable does not exist, returns the provided default value.
     *
     * <h4>Usage Examples</h4>
     * <pre>{@code
     * String dbUrl = EnvUtils.getEnv("DATABASE_URL", "jdbc:mysql://localhost:3306/test");
     * String logLevel = EnvUtils.getEnv("LOG_LEVEL", "INFO");
     * }</pre>
     *
     * @param key        Environment variable name, cannot be null or empty string
     * @param defaultVal Default value, returned when environment variable does not exist, can be null
     * @return String Environment variable value or default value
     */
    public static String getEnv(String key, String defaultVal) {
        String value = getEnv(key);
        return value != null ? value : defaultVal;
    }

    public static String getEnv(String key) {
        String value = System.getenv(key);
        log.info("system get env key:{}, env value:{}", key, value);
        return value;
    }

    /**
     * Get environment variable value with type checking
     * <p>
     * Provides type-safe environment variable retrieval mechanism with automatic type
     * conversion and validation. Currently supports automatic conversion for String
     * and List<String> types.
     *
     * <h4>Supported Type Conversions</h4>
     * <ul>
     *   <li>String.class: Directly returns the string value of environment variable</li>
     *   <li>List.class: Converts comma-separated string to string list</li>
     * </ul>
     *
     * <h4>List Type Conversion Rules</h4>
     * <ul>
     *   <li>Split string by comma</li>
     *   <li>Automatically trim whitespace from each element</li>
     *   <li>Filter out empty string elements</li>
     * </ul>
     *
     * <h4>Usage Examples</h4>
     * <pre>{@code
     * // Get string type
     * String dbHost = EnvUtils.getEnvVar("DB_HOST", String.class, "localhost");
     *
     * // Get list type
     * List<String> servers = EnvUtils.getEnvVar("SERVER_LIST", List.class,
     *     Arrays.asList("server1", "server2"));
     *
     * // Environment variable SERVER_LIST=web1,web2,web3 will be converted to ["web1", "web2", "web3"]
     * }</pre>
     *
     * @param <T>          Generic type of return value
     * @param key          Environment variable name, cannot be null or empty string
     * @param expectedType Expected type, currently supports String.class or List.class
     * @param defaultVal   Default value used when environment variable does not exist, type must match expectedType
     * @return T Converted environment variable value or default value
     * @throws IllegalArgumentException When environment variable is not set and no default value provided, or type not supported, or default value type mismatch
     */
    @SuppressWarnings("unchecked")
    public static <T> T getEnvVar(String key, Class<T> expectedType, T defaultVal) {
        String value = System.getenv(key);

        if (value == null && defaultVal == null) {
            throw new IllegalArgumentException(
                    "Environment variable '" + key + "' is not set and no default value provided. " +
                            "Please check your .env or system env."
            );
        }

        // If environment variable is not set, return default value (and check default value type)
        if (value == null) {
            if (defaultVal != null && !expectedType.isInstance(defaultVal)) {
                throw new IllegalArgumentException(
                        "Default value for '" + key + "' is not of expected type: " + expectedType.getSimpleName()
                );
            }
            return defaultVal;
        }

        // Handle String type
        if (expectedType == String.class) {
            return (T) value;
        }

        // Handle List<String> type
        if (expectedType == List.class || expectedType == (Class<?>) List.class) {
            try {
                List<String> valueList = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty()) // Filter out empty strings
                        .collect(Collectors.toList());
                return (T) valueList;
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Environment variable '" + key + "' type error: cannot parse '" + value + "' as List<String>.", e
                );
            }
        }

        throw new IllegalArgumentException(
                "Unsupported expectedType '" + expectedType.getSimpleName() + "' for environment variable '" + key + "'."
        );
    }

    /**
     * Get application log storage path
     * <p>
     * Retrieves the log file storage path from the LOG_PATH environment variable.
     * Mainly used for configuring the output directory of logging frameworks.
     *
     * @return String Log path, defaults to "/export/Logs"
     */
    public static String getEnvForLogPath() {
        return getEnv("LOG_PATH", "/export/Logs");
    }

    /**
     * Get available CPU core count configuration
     * <p>
     * Retrieves the number of available CPU cores from the AVAILABLE_CORES environment variable.
     * Mainly used for configuring thread pool size, parallel processing task count, and other performance-related parameters.
     * Provides error tolerance for numeric parsing failures.
     *
     * @return int CPU core count, defaults to 2 if parsing fails
     */
    public static int getEnvForCpuCount() {
        String val = getEnv("AVAILABLE_CORES", "2");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.warn("Invalid CPU count '{}', using default 2", val);
            return 2;
        }
    }

    /**
     * Get HTTP service run attribute.
     * Used for backup only, typically used in bin/start.sh.
     *
     * @return Run attribute, defaults to -1
     */
    public static int getEnvForRunAttr() {
        String val = getEnv("RUN_ATTR", "-1");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Get runtime environment (such as yachain).
     *
     * @return Runtime environment, defaults to "local"
     */
    public static String getEnvForRunProfile() {
        return getEnv("YACHAIN_RUN_PROFILE", "local");
    }

    /**
     * Get schedule configuration file for task scheduling.
     *
     * @return Schedule configuration, defaults to "false"
     */
    public static String getScheduleProfile() {
        return getEnv("SCHEDULE_JOB", "false");
    }

    /**
     * Get engine intelligent configuration file for task scheduling.
     *
     * @return Engine configuration, defaults to "yachain_group"
     */
    public static String getEngineIntelligentProfile() {
        return getEnv("ENGINE", "yachain_group");
    }

    /**
     * Get application deployment stage identifier
     * <p>
     * Retrieves the current application's deployment environment identifier from
     * the DEPLOYMENT_STAGE environment variable. Used to distinguish different
     * runtime environments for implementing environment-specific logic branches.
     *
     * <h4>Environment Mapping</h4>
     * <ul>
     *   <li>"prod" → 1 (Production environment)</li>
     *   <li>"dev" → 2 (Development environment)</li>
     *   <li>Other values → 3 (Local debug environment)</li>
     * </ul>
     *
     * @return int Deployment stage code: 1-Production environment, 2-Development environment, 3-Local debug environment
     */
    public static int getEnvForDeploymentStage() {
        String deploymentStage = getEnv("DEPLOYMENT_STAGE", "local");
        switch (deploymentStage) {
            case "prod":
                return 1;
            case "dev":
                return 2;
            default:
                return 3;
        }
    }

    /**
     * Determine if current environment is production
     * <p>
     * Determines whether the current runtime environment is production by checking
     * the DEPLOYMENT_STAGE environment variable. Commonly used for implementing
     * environment-specific feature switches and configuration switching.
     *
     * @return boolean true indicates production environment, false indicates non-production environment
     */
    public static boolean isProdEnv() {
        return "prod".equals(getEnv("DEPLOYMENT_STAGE", "local"));
    }

    /**
     * Get local machine IP address
     * <p>
     * Retrieves the local machine's IP address through system API, mainly used for
     * service registration, logging, and other scenarios. Provides exception handling
     * mechanism to ensure a usable IP address is returned even when network configuration is abnormal.
     *
     * @return String Local machine IP address, returns "127.0.0.1" if retrieval fails
     */
    public static String getLocalIp() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Failed to get local IP, using 127.0.0.1", e);
            return "127.0.0.1";
        }
    }

    /**
     * Get machine group identifier
     * <p>
     * Retrieves the group identifier of the current machine from the GROUP_ID environment variable.
     * Mainly used for cluster management, load balancing, and service grouping scenarios.
     *
     * @return int Group identifier, defaults to 0 if parsing fails
     */
    public static int getEnvForGroupId() {
        String val = getEnv("GROUP_ID", "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.warn("Invalid GROUP_ID '{}', using default 0", val);
            return 0;
        }
    }

    public static void main(String[] args) {
        // Get string
        String logPath = EnvUtils.getEnvForLogPath();
        System.out.println("logPath = " + logPath);

        // Get integer
        int cpuCount = EnvUtils.getEnvForCpuCount();
        System.out.println("cpuCount = " + cpuCount);

        // Use getEnvVar to get type-checked values
        String profile = EnvUtils.getEnvVar("YACHAIN_RUN_PROFILE", String.class, "local");
        List<String> servers = EnvUtils.getEnvVar("SERVER_LIST", List.class, List.of("localhost"));
        System.out.println("profile = " + profile);
        System.out.println("servers = " + servers);

        // Check environment
        if (EnvUtils.isProdEnv()) {
            // Production environment logic
        }
    }
}