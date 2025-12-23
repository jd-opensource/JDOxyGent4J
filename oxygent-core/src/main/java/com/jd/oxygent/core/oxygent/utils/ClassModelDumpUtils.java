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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class model dump utility class
 *
 * <h3>Functional Description</h3>
 * <ul>
 *   <li>Provides model_dump functionality similar to Python Pydantic</li>
 *   <li>Exports Java object fields to Map structure through reflection mechanism</li>
 *   <li>Supports flexible field filtering and naming conversion strategies</li>
 *   <li>Implements convenient tools for object serialization and data transmission</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li>Field include/exclude strategy: Support specifying inclusion or exclusion of specific fields</li>
 *   <li>Naming conversion: Automatically convert camelCase naming to snake_case naming</li>
 *   <li>Annotation support: Recognize @JsonIgnore and @ExcludeOption annotations</li>
 *   <li>Inheritance support: Traverse class inheritance hierarchy to get all fields</li>
 *   <li>Type safety: Provide comprehensive exception handling mechanism</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li>Data preprocessing before object serialization and deserialization</li>
 *   <li>Field filtering and formatting of API response data</li>
 *   <li>Dynamic construction of Data Transfer Objects (DTO)</li>
 *   <li>Object state export in debugging and logging</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ClassModelDumpUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Export object fields to Map (basic version)
     * <p>
     * Uses default strategy to export all non-static, non-transient, non-final, non-private fields of the object.
     * Automatically excludes null value fields and fields with @JsonIgnore, @ExcludeOption annotations.
     *
     * @param currentObject Object instance to be exported, cannot be null
     * @return Map&lt;String, Object&gt; Mapping table containing all qualified fields, key is snake_case field name, value is field value
     * @throws IllegalAccessException Thrown when field access permission is insufficient
     */
    public static Map<String, Object> modelDump(Object currentObject) throws IllegalAccessException {
        return modelDump(currentObject, new HashSet<>(), new HashSet<>(), true);
    }

    /**
     * Export object fields to Map, supports excluding specified fields
     * <p>
     * Based on basic export functionality, additionally excludes specified field name sets.
     * Suitable for scenarios that need to filter sensitive information or unnecessary fields.
     *
     * @param currentObject Object instance to be exported, cannot be null
     * @param excludeFields Set of field names to exclude, using original camelCase naming, cannot be null but can be empty
     * @return Map&lt;String, Object&gt; Mapping table containing fields, specified fields excluded
     * @throws IllegalAccessException Thrown when field access permission is insufficient
     */
    public static Map<String, Object> modelDump(Object currentObject, Set<String> excludeFields) throws IllegalAccessException {
        return modelDump(currentObject, new HashSet<>(), excludeFields, true);
    }

    /**
     * Export object fields to Map, supports complete filtering and configuration options
     * <p>
     * This is the core implementation method, providing the most complete field export control functionality.
     * Simulates Python Pydantic library's model_dump method, supports flexible field filtering strategies.
     *
     * <h4>Processing Logic</h4>
     * <ol>
     *   <li>Traverse object class and all its parent classes (up to Object class)</li>
     *   <li>Filter fields: Skip static, transient, final, private fields</li>
     *   <li>Check annotations: Skip fields marked with @JsonIgnore and @ExcludeOption</li>
     *   <li>Apply include/exclude strategy: Filter based on includeFields and excludeFields parameters</li>
     *   <li>Handle null values: Decide whether to include null fields based on excludeUnset parameter</li>
     *   <li>Naming conversion: Convert camelCase naming to snake_case naming</li>
     * </ol>
     *
     * <h4>Field Filtering Rules</h4>
     * <ul>
     *   <li>Static fields (static): Automatically skipped</li>
     *   <li>Transient fields (transient): Automatically skipped</li>
     *   <li>Final fields: Automatically skipped</li>
     *   <li>Private fields: Automatically skipped</li>
     *   <li>@JsonIgnore annotation: Automatically skipped</li>
     *   <li>@ExcludeOption annotation: Automatically skipped</li>
     * </ul>
     *
     * @param currentObject Object instance to be exported, cannot be null
     * @param includeFields Set of field names to include, includes all qualified fields when empty, cannot be null
     * @param excludeFields Set of field names to exclude, cannot be null but can be empty
     * @param excludeUnset  Whether to exclude unset (null) fields, true means skip null value fields
     * @return Map&lt;String, Object&gt; Field mapping table, key is snake_case field name, value is field value, sorted in dictionary order
     * @throws IllegalAccessException Thrown when reflection field access fails
     */
    public static Map<String, Object> modelDump(Object currentObject, Set<String> includeFields,
                                                Set<String> excludeFields,
                                                boolean excludeUnset) throws IllegalAccessException {
        Map<String, Object> result = new TreeMap<>();

        // Get fields of current class and all its parent classes
        Class<?> currentClass = currentObject.getClass();
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();

            for (Field field : fields) {
                // Skip static fields and transient fields, skip final fields, skip private fields

                if (Modifier.isStatic(field.getModifiers()) ||
                        Modifier.isTransient(field.getModifiers()) ||
                        Modifier.isFinal(field.getModifiers()) ||
                        Modifier.isPrivate(field.getModifiers())
                ) {
                    continue;
                }

                // Skip fields marked as JsonIgnore
                if (field.isAnnotationPresent(JsonIgnore.class)) {
                    continue;
                }
                // Skip fields marked as ExcludeOption
                if (field.isAnnotationPresent(ExcludeOption.class)) {
                    continue;
                }

                String fieldName = field.getName();

                // Skip excluded fields
                if (excludeFields.contains(fieldName)) {
                    continue;
                }

                // If include field list is specified, only process fields in the list
                if (!includeFields.isEmpty() && !includeFields.contains(fieldName)) {
                    continue;
                }

                // Set field accessible
                field.setAccessible(true);
                Object value = field.get(currentObject);

                // If excludeUnset is true, skip null values
                if (excludeUnset && value == null) {
                    continue;
                }

                // Convert field name to snake_case (Python style)
                String snakeCaseFieldName = toSnakeCase(fieldName);


                result.putIfAbsent(snakeCaseFieldName, value);
            }

            // Move to parent class
            currentClass = currentClass.getSuperclass();
        }
        return result;
    }

    /**
     * Convert camelCase naming to snake_case naming
     * <p>
     * Implements conversion from Java camelCase naming to Python style snake_case naming, following these rules:
     * <ul>
     *   <li>First letter lowercase remains unchanged</li>
     *   <li>When encountering uppercase letters, insert underscore before and convert uppercase to lowercase</li>
     *   <li>Consecutive uppercase letters are converted individually</li>
     * </ul>
     *
     * <h4>Conversion Examples</h4>
     * <ul>
     *   <li>className → class_name</li>
     *   <li>userId → user_id</li>
     *   <li>HTTPResponse → h_t_t_p_response</li>
     *   <li>simpleField → simple_field</li>
     * </ul>
     *
     * @param camelCase CamelCase naming string, can be null or empty string
     * @return String Snake_case naming string, returns original value when input is null or empty
     */
    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));

        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Get all field names of specified class for exclusion, but keep specified field
     * <p>
     * Gets all declared fields of specified class through reflection, builds an exclusion set
     * that contains all field names except keepField.
     * Commonly used for scenarios where only specific fields need to be kept from an object while excluding all others.
     *
     * <h4>Usage Scenarios</h4>
     * <ul>
     *   <li>Only need a specific field from parent class in inheritance class</li>
     *   <li>Keep only key fields during object serialization</li>
     *   <li>Filter sensitive or redundant information during data transmission</li>
     * </ul>
     *
     * @param baseClass Class object of base class, used to get field information, cannot be null
     * @param keepField Field name to keep, will not be added to exclusion set, cannot be null
     * @return Set<String> Set of all field names except keepField
     */
    public static Set<String> getFieldsExcept(Class<?> baseClass, String keepField) {
        Set<String> excludeFields = new HashSet<>();

        // Get all fields of Oxy base class
        Field[] fields = baseClass.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            // Exclude all fields except keepField
            if (!fieldName.equals(keepField)) {
                excludeFields.add(fieldName);
            }
        }

        return excludeFields;
    }

    /**
     * Get class attribute mapping table for saving
     * <p>
     * This is a convenience method that combines getFieldsExcept and modelDump methods
     * to quickly get serialized data of object while excluding most fields of specified base class.
     * Mainly used for object preprocessing in data persistence scenarios.
     *
     * <h4>Execution Flow</h4>
     * <ol>
     *   <li>Call getFieldsExcept to get field set to exclude</li>
     *   <li>Call modelDump to export object fields, excluding unnecessary fields</li>
     *   <li>Return field mapping table suitable for saving</li>
     * </ol>
     *
     * @param excludeAttrClass Base class whose fields to exclude, usually parent class or base class, cannot be null
     * @param keepField        Field name to keep from base class, cannot be null
     * @param currentObject    Object instance to process, cannot be null
     * @return Map&lt;String, Object&gt; Field mapping table suitable for saving, other fields of base class excluded
     * @throws IllegalAccessException Thrown when field access permission is insufficient
     */
    public static Map<String, Object> getClassAttrForSave(Class<?> excludeAttrClass, String keepField, Object currentObject) throws IllegalAccessException {
        // Exclude all fields of base class, only keep specified field
        Set<String> excludeFieldsSet = ClassModelDumpUtils.getFieldsExcept(excludeAttrClass, keepField);
        return ClassModelDumpUtils.modelDump(currentObject, excludeFieldsSet);
    }

}
