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
        // 配置 ObjectMapper 忽略未知属性
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
            // 解析请求参数
            Map<String, Object> params = parseParameters(req, endpoint);

            // 调用目标方法
            Object result = invokeEndpoint(endpoint, params);

            // 返回结果
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
        // TODO: 这里可以根据需要实现路径参数解析

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

        // 检查是否只有一个参数且是复杂对象
        if (paramTypes.length == 1 && !isSimpleType(paramTypes[0])) {
            // 在调用前预处理 params
            Map<String, Object> processedParams = preprocessParamsForType(params, paramTypes[0]);
            // 单个复杂对象参数：直接将整个params映射到对象
            args[0] = objectMapper.convertValue(processedParams, paramTypes[0]);
        } else {
            // 多个参数或简单类型参数：使用原有的逻辑
            String[] paramNames = Arrays.stream(method.getParameters())
                    .map(p -> p.getName())
                    .toArray(String[]::new);

            for (int i = 0; i < paramTypes.length; i++) {
                String paramName = paramNames[i];
                Class<?> paramType = paramTypes[i];

                if (isSimpleType(paramType)) {
                    // 简单类型：使用原有的逻辑
                    Object paramValue = params.get(paramName);
                    args[i] = (paramValue != null) ? convertType(paramValue, paramType) : getDefaultValue(paramType);
                } else {
                    // 复杂对象类型：尝试从params中提取对应字段
                    Object paramValue = params.get(paramName);
                    if (paramValue != null) {
                        if (paramValue instanceof Map) {
                            // 如果是Map，转换为目标类型
                            args[i] = objectMapper.convertValue(paramValue, paramType);
                        } else {
                            // 其他类型，尝试直接转换
                            args[i] = objectMapper.convertValue(paramValue, paramType);
                        }
                    } else {
                        // 如果参数名不在params中，尝试检查是否有单独的字段
                        args[i] = tryCreateFromFields(params, paramType);
                    }
                }
            }
        }

        // 调用方法
        return method.invoke(serviceInstance, args);
    }

    /**
     * 检查是否是简单类型
     */
    private boolean isSimpleType(Class<?> type) {
        return type == String.class ||
                type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class ||
                type == Boolean.class || type == boolean.class ||
                type == Byte.class || type == byte.class ||
                type == Short.class || type == short.class ||
                type == Character.class || type == char.class ||
                type.isPrimitive();
    }

    /**
     * 获取类型的默认值
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
        } else if (type == Float.class || type == float.class) {
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
     * 尝试从params的字段创建对象
     */
    private Object tryCreateFromFields(Map<String, Object> params, Class<?> targetType) {
        try {
            // 使用反射创建对象实例
            Object instance = targetType.getDeclaredConstructor().newInstance();

            // 遍历所有字段，从params中设置值
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
     * 根据目标类型预处理 params，处理类型不匹配
     */
    private Map<String, Object> preprocessParamsForType(Map<String, Object> params, Class<?> targetType) {
        // 创建 params 的副本，避免修改原始数据
        Map<String, Object> processed = new HashMap<>(params);

        // 获取目标类的字段信息
        Map<String, Class<?>> fieldTypes = new HashMap<>();
        for (java.lang.reflect.Field field : targetType.getDeclaredFields()) {
            field.setAccessible(true);
            fieldTypes.put(field.getName(), field.getType());
        }

        // 预处理每个字段
        for (Map.Entry<String, Class<?>> entry : fieldTypes.entrySet()) {
            String fieldName = entry.getKey();
            Class<?> fieldType = entry.getValue();

            if (processed.containsKey(fieldName)) {
                Object value = processed.get(fieldName);

                // 特殊处理：String字段但收到对象或数组
                if (fieldType == String.class && value != null) {
                    if (value instanceof Map || value instanceof Collection ||
                            (value.getClass().isArray() && !(value instanceof byte[]))) {
                        // 将对象/数组转换为JSON字符串
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