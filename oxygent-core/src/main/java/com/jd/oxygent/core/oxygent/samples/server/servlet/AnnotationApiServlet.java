package com.jd.oxygent.core.oxygent.samples.server.servlet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.samples.server.scanner.ApiEndpointScanner;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Annotation-driven API Servlet
 * Handles all interfaces registered via @ApiEndpoint annotation
 */
@Slf4j
public class AnnotationApiServlet extends HttpServlet {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Configure ObjectMapper to ignore unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        log.info("AnnotationApiServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRequest(req, resp, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRequest(req, resp, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRequest(req, resp, "PUT");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRequest(req, resp, "DELETE");
    }

    /**
     * Unified request handling
     */
    private void handleRequest(HttpServletRequest req, HttpServletResponse resp, String httpMethod) throws IOException {
        String path = req.getPathInfo();

        // Get endpoint information
        ApiEndpointScanner.EndpointInfo endpoint = ApiEndpointScanner.getEndpoint(path, httpMethod);

        if (endpoint == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            sendJsonResponse(resp, Map.of(
                    "error", "Endpoint not found",
                    "path", path,
                    "method", httpMethod
            ));
            return;
        }

        try {
            // Parse request parameters
            Map<String, Object> params = parseParameters(req, endpoint);

            // Invoke target method
            Object result = invokeEndpoint(endpoint, params);

            // Return result
            sendJsonResponse(resp, result);

        } catch (Exception e) {
            log.error("Failed to process API request: {} {}", httpMethod, path, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJsonResponse(resp, Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Parse request parameters
     */
    private Map<String, Object> parseParameters(HttpServletRequest req,
                                                ApiEndpointScanner.EndpointInfo endpoint) throws IOException {
        Map<String, Object> params = new HashMap<>();

        // Parse path parameters
        // TODO: Path parameter parsing can be implemented here if needed

        // Parse query parameters
        req.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });

        // Parse JSON request body (for POST/PUT, etc.)
        if ("POST".equals(req.getMethod()) || "PUT".equals(req.getMethod()) || "PATCH".equals(req.getMethod())) {
            String contentType = req.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                BufferedReader reader = req.getReader();
                String body = reader.lines().collect(Collectors.joining());
                if (!body.isEmpty()) {
                    try {
                        Map<String, Object> bodyParams = objectMapper.readValue(body, Map.class);
                        params.putAll(bodyParams);
                    } catch (Exception e) {
                        log.warn("Failed to parse JSON body", e);
                    }
                }
            }
        }

        return params;
    }

    /**
     * Invoke endpoint method
     */
    private Object invokeEndpoint(ApiEndpointScanner.EndpointInfo endpoint,
                                  Map<String, Object> params) throws Exception {
        Method method = endpoint.getMethod();
        Object serviceInstance = endpoint.getServiceInstance();

        // Prepare method parameters
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        // Check if there's only one parameter and it's a complex object
        if (paramTypes.length == 1 && !isSimpleType(paramTypes[0])) {
            // Preprocess params before invocation
            Map<String, Object> processedParams = preprocessParamsForType(params, paramTypes[0]);
            // Single complex object parameter: directly map entire params to object
            args[0] = objectMapper.convertValue(processedParams, paramTypes[0]);
        } else {
            // Multiple parameters or simple type parameters: use existing logic
            String[] paramNames = Arrays.stream(method.getParameters())
                    .map(p -> p.getName())
                    .toArray(String[]::new);

            for (int i = 0; i < paramTypes.length; i++) {
                String paramName = paramNames[i];
                Class<?> paramType = paramTypes[i];

                if (isSimpleType(paramType)) {
                    // Simple type: use existing logic
                    Object paramValue = params.get(paramName);
                    args[i] = (paramValue != null) ? convertType(paramValue, paramType) : getDefaultValue(paramType);
                } else {
                    // Complex object type: try to extract corresponding fields from params
                    Object paramValue = params.get(paramName);
                    if (paramValue != null) {
                        if (paramValue instanceof Map) {
                            // If it's a Map, convert to target type
                            args[i] = objectMapper.convertValue(paramValue, paramType);
                        } else {
                            // Other types, try direct conversion
                            args[i] = objectMapper.convertValue(paramValue, paramType);
                        }
                    } else {
                        // If parameter name is not in params, try to check if there are separate fields
                        args[i] = tryCreateFromFields(params, paramType);
                    }
                }
            }
        }

        // Invoke method
        return method.invoke(serviceInstance, args);
    }

    /**
     * Check if it's a simple type
     */
    private boolean isSimpleType(Class<?> type) {
        return type == String.class ||
                type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Double.class || type == float.class ||
                type == Boolean.class || type == boolean.class ||
                type == Byte.class || type == byte.class ||
                type == Short.class || type == short.class ||
                type == Character.class || type == char.class ||
                type.isPrimitive();
    }

    /**
     * Get default value for type
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == String.class) {
            return "";
        } else if (type == Integer.class || type == int.class) {
            return 0;
        } else if (type == Long.class || type == long.class) {
            return 0L;
        } else if (type == Double.class || type == double.class) {
            return 0.0;
        } else if (type == Double.class || type == float.class) {
            return 0.0f;
        } else if (type == Boolean.class || type == boolean.class) {
            return false;
        } else if (type == Byte.class || type == byte.class) {
            return (byte) 0;
        } else if (type == Short.class || type == short.class) {
            return (short) 0;
        } else if (type == Character.class || type == char.class) {
            return '\u0000';
        }
        return null;
    }

    /**
     * Try to create object from params fields
     */
    private Object tryCreateFromFields(Map<String, Object> params, Class<?> targetType) {
        try {
            // Use reflection to create object instance
            Object instance = targetType.getDeclaredConstructor().newInstance();

            // Iterate through all fields, set values from params
            for (java.lang.reflect.Field field : targetType.getDeclaredFields()) {
                if (params.containsKey(field.getName())) {
                    field.setAccessible(true);
                    Object value = params.get(field.getName());
                    if (value != null) {
                        field.set(instance, convertType(value, field.getType()));
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            log.warn("Failed to create instance of {} from params", targetType.getName(), e);
            return null;
        }
    }

    /**
     * Preprocess params according to target type, handle type mismatches
     */
    private Map<String, Object> preprocessParamsForType(Map<String, Object> params, Class<?> targetType) {
        // Create a copy of params to avoid modifying original data
        Map<String, Object> processed = new HashMap<>(params);

        // Get field information of the target class
        Map<String, Class<?>> fieldTypes = new HashMap<>();
        for (java.lang.reflect.Field field : targetType.getDeclaredFields()) {
            field.setAccessible(true);
            fieldTypes.put(field.getName(), field.getType());
        }

        // Preprocess each field
        for (Map.Entry<String, Class<?>> entry : fieldTypes.entrySet()) {
            String fieldName = entry.getKey();
            Class<?> fieldType = entry.getValue();

            if (processed.containsKey(fieldName)) {
                Object value = processed.get(fieldName);

                // Special handling: String field but received object or array
                if (fieldType == String.class && value != null) {
                    if (value instanceof Map || value instanceof Collection ||
                            (value.getClass().isArray() && !(value instanceof byte[]))) {
                        // Convert object/array to JSON string
                        try {
                            processed.put(fieldName, objectMapper.writeValueAsString(value));
                        } catch (Exception e) {
                            log.debug("Failed to convert {} to JSON string: {}", fieldName, e.getMessage());
                        }
                    }
                }
            }
        }

        return processed;
    }

    /**
     * Type conversion
     */
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;

        try {
            if (targetType == String.class) {
                return value.toString();
            } else if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return Integer.parseInt(value.toString());
            } else if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return Long.parseLong(value.toString());
            } else if (targetType == Double.class || targetType == double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.parseDouble(value.toString());
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
                return Boolean.parseBoolean(value.toString());
            }
        } catch (Exception e) {
            log.warn("Type conversion failed: {} to {}", value, targetType, e);
        }

        return value;
    }

    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter writer = resp.getWriter();
        String json = objectMapper.writeValueAsString(data);
        writer.write(json);
        writer.flush();
    }
}