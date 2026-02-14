package com.jd.oxygent.oxybank.core.interfaces;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Core components for knowledge base platform.
 *
 * 将 FastAPI 中的 QueryAPIInfo 与路由参数解析转为 Spring Web 的简单展示接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/core/endpoints")
public class EndpointShowController {

    @Data
    public static class QueryAPIInfo {

        /**
         * Interface name
         */
        private String name;

        /**
         * Interface path
         */
        private String path;

        /**
         * Interface body parameters
         */
        private Map<String, String> params;
    }

    /**
     * 在 Spring 中没有 FastAPI 的 APIRoute，这里仅示例返回空列表。
     * fixme: 如果需要真正扫描 Controller 方法参数，需要结合 RequestMappingHandlerMapping 反射实现。
     */
    @GetMapping("/list")
    public List<QueryAPIInfo> get_query_api_info() {
        // fixme: 使用 Spring 的 HandlerMethod/ParameterMetadata 解析真实参数类型
        return List.of();
    }

    /**
     * 获取字段类型，等价于 Python 的 get_field_type(FieldInfo)。
     *
     * 由于 Java 是静态类型，这里简化为直接返回类型 Class。
     */
    public Class<?> get_field_type(java.lang.reflect.Type fieldType) {
        if (fieldType == null) {
            return null;
        }
        if (fieldType instanceof Class<?>) {
            return (Class<?>) fieldType;
        }
        // fixme: 如需处理泛型、Optional 等更复杂类型，这里补充逻辑
        return Object.class;
    }

    /**
     * 示例：从一个 Bean 类型中解析字段名和类型字符串。
     */
    public Map<String, String> get_route_parameters(Class<?> requestType) {
        Map<String, String> field_info = new LinkedHashMap<>();
        if (requestType == null) {
            return field_info;
        }
        for (Field field : requestType.getDeclaredFields()) {
            Class<?> field_type = field.getType();
            String field_type_str = field_type == null ? "None" : field_type.getSimpleName();
            field_info.put(field.getName(), field_type_str);
        }
        return field_info;
    }
}