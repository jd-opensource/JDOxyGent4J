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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * JSON Processing Utility Class
 *
 * <p>Provides conversion between JSON strings and objects, object property copying, naming format conversion, and other functions.
 * This utility class encapsulates common JSON operations and simplifies the JSON data processing workflow.</p>
 *
 * <p>Main Functional Modules:</p>
 * <ul>
 *     <li>JSON Serialization and Deserialization: Mutual conversion between Map and JSON strings</li>
 *     <li>Naming Format Conversion: Mutual conversion between camelCase and snake_case naming</li>
 *     <li>Object Property Operations: Mutual conversion between JavaBean and Map, property copying</li>
 *     <li>JSON Format Validation: Check if string is in valid JSON format</li>
 *     <li>Utility Methods: Get first non-null value, etc.</li>
 * </ul>
 *
 * <p>Usage Examples:</p>
 * <pre>{@code
 * // JSON string conversion
 * Map<String, Object> map = JsonUtils.parseJsonString("{\"name\":\"test\"}");
 * String json = JsonUtils.mapToJsonString(map);
 *
 * // Naming format conversion
 * String camel = JsonUtils.toCamelCase("user_name");     // userName
 * String snake = JsonUtils.toSnakeCase("userName");      // user_name
 *
 * // Object to Map
 * Map<String, Object> result = JsonUtils.convertToMap(bean, "snake");
 *
 * // JSON format validation
 * boolean valid = JsonUtils.isJson("{\"test\":\"value\"}");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class JsonUtils {

    // Configured ObjectMapper instance, supports control characters, reusable
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);


    /**
     * Convert Map to JSON string
     *
     * <p>Converts a Map object to JSON format string. This method supports nested Map and List structures,
     * and can properly handle string escaping and various data types.</p>
     *
     * @param map Map object to convert, cannot be null
     * @return JSON format string
     * @throws IllegalArgumentException When map is null
     * @since 1.0.0
     */
    public static String mapToJsonString(Map<String, Object> map) {
        Objects.requireNonNull(map, "Map cannot be null");

        var sb = new StringBuilder();
        sb.append("{");
        var first = true;

        for (var entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"")
                    .append(escapeJsonString(entry.getKey()))
                    .append("\":");
            appendJsonValue(sb, entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parse JSON string to Map object
     *
     * <p>Parses JSON format string to Map object. This method supports basic JSON structure parsing,
     * including nested objects and arrays. The parsing result uses LinkedHashMap to maintain key insertion order.</p>
     *
     * @param json JSON format string, cannot be null or empty string
     * @return Parsed Map object, uses LinkedHashMap to maintain order
     * @throws IllegalArgumentException When json is null, empty string, or invalid format
     * @throws RuntimeException         When JSON parsing fails
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonString(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty string");
        }

        var trimmedJson = json.trim();
        if (!trimmedJson.startsWith("{") || !trimmedJson.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format: must start with { and end with }");
        }

        // Use LinkedHashMap to maintain insertion order
        var result = new LinkedHashMap<String, Object>();
        var content = trimmedJson.substring(1, trimmedJson.length() - 1).trim();

        if (content.isEmpty()) {
            return result;
        }

        try {
            // Simple key-value pair parsing (supports nested objects)
            var pairs = splitJsonPairs(content);
            for (var pair : pairs) {
                var keyValue = splitKeyValue(pair);
                if (keyValue.length == 2) {
                    var key = unescapeJsonString(keyValue[0].trim());
                    var value = parseJsonValue(keyValue[1].trim());
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON parsing failed: " + e.getMessage(), e);
        }

        return result;
    }

    public static void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"")
                    .append(escapeJsonString((String) value))
                    .append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof List) {
            sb.append("[");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                appendJsonValue(sb, list.get(i));
            }
            sb.append("]");
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            sb.append(mapToJsonString(map));
        } else {
            sb.append("\"")
                    .append(escapeJsonString(value.toString()))
                    .append("\"");
        }
    }

    public static String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String unescapeJsonString(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length() - 1);
        }
        return str.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    public static String[] splitJsonPairs(String content) {
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int braceLevel = 0;
        int bracketLevel = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }

            if (!inQuotes) {
                if (c == '{') {
                    braceLevel++;
                } else if (c == '}') {
                    braceLevel--;
                } else if (c == '[') {
                    bracketLevel++;
                } else if (c == ']') {
                    bracketLevel--;
                } else if (c == ',' && braceLevel == 0 && bracketLevel == 0) {
                    pairs.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }

        return pairs.toArray(new String[0]);
    }

    public static String[] splitKeyValue(String pair) {
        int colonIndex = -1;
        boolean inQuotes = false;

        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ':' && !inQuotes) {
                colonIndex = i;
                break;
            }
        }

        if (colonIndex == -1) {
            return new String[]{pair};
        }

        return new String[]{
                pair.substring(0, colonIndex),
                pair.substring(colonIndex + 1)
        };
    }

    public static Object parseJsonValue(String value) {
        value = value.trim();

        if ("null".equals(value)) {
            return null;
        } else if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescapeJsonString(value);
        } else if (value.startsWith("[") && value.endsWith("]")) {
            // Simple array parsing
            List<Object> list = new ArrayList<>();
            String arrayContent = value.substring(1, value.length() - 1).trim();
            if (!arrayContent.isEmpty()) {
                String[] elements = splitJsonPairs(arrayContent);
                for (String element : elements) {
                    list.add(parseJsonValue(element));
                }
            }
            return list;
        } else if (value.startsWith("{") && value.endsWith("}")) {
            return parseJsonString(value);
        } else {
            // Try to parse as number
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                return value; // Return as string
            }
        }
    }

    /**
     * Convert snake_case naming to camelCase naming
     *
     * <p>Converts snake_case format string to camelCase format.
     * Conversion rules: Characters after underscores are converted to uppercase, underscores are removed,
     * and the first letter is converted to lowercase.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * JsonUtils.toCamelCase("user_name")     = "userName"
     * JsonUtils.toCamelCase("first_name")    = "firstName"
     * JsonUtils.toCamelCase("USER_ID")       = "userId"
     * JsonUtils.toCamelCase("simple")        = "simple"
     * }</pre>
     *
     * @param snakeCase Snake_case format string, cannot be null or empty string
     * @return Converted camelCase format string
     * @throws IllegalArgumentException When snakeCase is null or empty string
     * @since 1.0.0
     */
    public static String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty string");
        }
        if (snakeCase.indexOf("_") < 0) {
            return snakeCase;
        }

        var camelCase = new StringBuilder();
        var capitalizeNext = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCase.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    camelCase.append(Character.toLowerCase(c));
                }
            }
        }

        // Ensure first letter is lowercase
        if (camelCase.length() > 0 && Character.isUpperCase(camelCase.charAt(0))) {
            camelCase.setCharAt(0, Character.toLowerCase(camelCase.charAt(0)));
        }

        return camelCase.toString();
    }

    /**
     * Convert camelCase naming to snake_case naming
     *
     * <p>Converts camelCase format string to snake_case format.
     * Conversion rules: Add underscore before uppercase letters (except first letter), convert all letters to lowercase.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * JsonUtils.toSnakeCase("userName")     = "user_name"
     * JsonUtils.toSnakeCase("firstName")    = "first_name"
     * JsonUtils.toSnakeCase("userId")       = "user_id"
     * JsonUtils.toSnakeCase("simple")       = "simple"
     * }</pre>
     *
     * @param camelCaseString CamelCase format string, cannot be null or empty string
     * @return Converted snake_case format string
     * @throws IllegalArgumentException When camelCaseString is null or empty string
     * @since 1.0.0
     */
    public static String toSnakeCase(String camelCaseString) {
        if (camelCaseString == null || camelCaseString.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty string");
        }

        var result = new StringBuilder();
        for (int i = 0; i < camelCaseString.length(); i++) {
            char c = camelCaseString.charAt(i);
            // If current character is uppercase and not the first character, add underscore before it
            if (Character.isUpperCase(c) && i > 0) {
                result.append("_");
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    /**
     * Convert Java object to Map
     *
     * @param bean
     * @return
     */
    public static Map<String, Object> convertToMap(Object bean, String snakecaseOrCamelCase) {
        Map<String, Object> map = new HashMap<>();
        if (bean == null) {
            return map;
        }
        try {
            for (PropertyDescriptor pd : java.beans.Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors()) {
                String propertyName = pd.getName();
                Method getter = pd.getReadMethod();
                if (getter != null) {
                    Object value = getter.invoke(bean);
                    if (snakecaseOrCamelCase == null) {
                        map.put(propertyName, value);
                    } else if ("snake".equals(snakecaseOrCamelCase)) {
                        map.put(JsonUtils.toSnakeCase(propertyName), value);
                    } else if ("camel".equals(snakecaseOrCamelCase)) {
                        map.put(JsonUtils.toCamelCase(propertyName), value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Check if string is in JSON format
     *
     * @param str String to check
     * @return true if valid JSON, false otherwise
     */
    public static boolean isJson(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        String pattern = "^(\\{.*\\}|\\[.*\\])$";
        return str.matches(pattern);
    }

    /**
     * Property copying between objects
     *
     * @param source
     * @param target
     */
    public static <T> T copyProperties(Object source, T target) {
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        for (Field field : sourceClass.getDeclaredFields()) {
            field.setAccessible(true); // Ensure access to private properties
            try {
                Object value = field.get(source); // Get property value
                if (value != null) { // Ensure value is not null
                    String fieldName = field.getName();
                    Field targetField = targetClass.getDeclaredField(fieldName);
                    targetField.setAccessible(true); // Ensure access to private properties
                    targetField.set(target, value); // Set value to target object's property with same name
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return target;
    }

    /**
     * Get first non-blank value
     *
     * @param values
     * @param <T>
     * @return
     */
    @SafeVarargs
    public static <T> T firstNotBlank(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T v : values) {
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v);
            if ("null".equals(s)) {
                continue;
            }
            if (s.trim().isEmpty()) {
                continue;
            }
            return v;
        }
        return null;
    }

    // ========== ObjectMapper Related Feature Extensions ==========

    /**
     * Get configured ObjectMapper instance
     * This instance is already configured to support control character parsing
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Serialize object to JSON string
     *
     * @param obj Object to serialize
     * @return JSON string
     * @throws RuntimeException When serialization fails
     */
    public static String writeValueAsString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Object serialization to JSON failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deserialize JSON string to object of specified type
     *
     * @param json      JSON string
     * @param valueType Target type
     * @param <T>       Return type
     * @return Deserialized object
     * @throws RuntimeException When deserialization fails
     */
    public static <T> T readValue(String json, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deserialize JSON string to object of specified type
     *
     * @param json      JSON string
     * @param valueType Target type
     * @param <T>       Return type
     * @return Deserialized object
     * @throws RuntimeException When deserialization fails
     */
    public static <T> T readValue(String json, Class<T> valueType, T defaultValue) {
        try {
            return OBJECT_MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization failed: {}", json, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Deserialize JSON string to object of specified type (supports generics)
     *
     * @param json    JSON string
     * @param typeRef Type reference
     * @param <T>     Return type
     * @return Deserialized object
     * @throws RuntimeException When deserialization fails
     */
    public static <T> T readValue(String json, TypeReference<T> typeRef) {
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JSON string to JsonNode
     *
     * @param json JSON string
     * @return JsonNode object
     * @throws RuntimeException When parsing fails
     */
    public static JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON parsing to tree structure failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deserialize byte array to object of specified type
     *
     * @param src       Byte array
     * @param valueType Target type
     * @param <T>       Return type
     * @return Deserialized object
     * @throws RuntimeException When deserialization fails
     */
    public static <T> T readValue(byte[] src, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(src, valueType);
        } catch (IOException e) {
            throw new RuntimeException("Byte array deserialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert object to byte array
     *
     * @param value Object to convert
     * @return Byte array
     * @throws RuntimeException When conversion fails
     */
    public static byte[] writeValueAsBytes(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Object serialization to byte array failed: " + e.getMessage(), e);
        }
    }

    /**
     * Object type conversion
     * Convert one object to another type of object (through JSON intermediary)
     *
     * @param fromValue   Source object
     * @param toValueType Target type
     * @param <T>         Return type
     * @return Converted object
     * @throws RuntimeException When conversion fails
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueType);
    }

    /**
     * Object type conversion (supports generics)
     *
     * @param fromValue      Source object
     * @param toValueTypeRef Target type reference
     * @param <T>            Return type
     * @return Converted object
     * @throws RuntimeException When conversion fails
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef);
    }

    // ========== Fastjson Compatible Methods (Implemented with Jackson) ==========

    /**
     * Convert object to JSON string
     * Compatible with fastjson's toJSONString method, implemented with Jackson
     *
     * @param object Object to convert
     * @return JSON string
     */
    public static String toJSONString(Object object) {
        return writeValueAsString(object);
    }

    /**
     * Convert object to formatted JSON string
     * Compatible with fastjson's toJSONString(object, SerializerFeature.PrettyFormat) method, implemented with Jackson
     *
     * <p>This method generates formatted JSON string with appropriate indentation and line breaks,
     * convenient for reading and debugging. Equivalent to using SerializerFeature.PrettyFormat in fastjson.</p>
     *
     * <p>Example output:</p>
     * <pre>{@code
     * {
     *   "name" : "test",
     *   "age" : 25,
     *   "address" : {
     *     "city" : "Beijing",
     *     "country" : "China"
     *   }
     * }
     * }</pre>
     *
     * @param object Object to convert
     * @return Formatted JSON string
     * @throws RuntimeException When serialization fails
     * @since 1.0.0
     */
    public static String toJSONStringPretty(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Object serialization to formatted JSON failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JSON string to object of specified type
     * Compatible with fastjson's parseObject method, implemented with Jackson
     *
     * @param text  JSON string
     * @param clazz Target type
     * @param <T>   Return type
     * @return Parsed object
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        return readValue(text, clazz);
    }

    /**
     * Perform generic JSON parsing
     * Compatible with fastjson's parse method, implemented with Jackson
     *
     * @param text JSON string
     * @return Parsed object (JsonNode)
     */
    public static Object parse(String text) {
        return readTree(text);
    }
}
