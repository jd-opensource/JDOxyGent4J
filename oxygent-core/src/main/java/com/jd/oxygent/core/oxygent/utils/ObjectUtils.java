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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object operation utility class
 *
 * <p>Provides common object operation methods, including non-null checking, object comparison,
 * default value handling and other functions. This utility class is designed to simplify
 * object processing logic and improve code robustness and readability.</p>
 *
 * <p>Main Features:</p>
 * <ul>
 *     <li>Non-null value finding: Find the first non-null value from multiple values</li>
 *     <li>Safe object operations: Provide null-safe object handling methods</li>
 *     <li>Default value handling: Provide default values for null objects</li>
 *     <li>Object comparison: Safe object equality comparison</li>
 * </ul>
 *
 * <p>Usage Examples:</p>
 * <pre>{@code
 * // Get first non-null value
 * String result = ObjectUtils.firstNonNull(null, "", "default");  // Returns ""
 * String result2 = ObjectUtils.firstNonNull(null, null, "test");  // Returns "test"
 *
 * // Safe default value handling
 * String safe = ObjectUtils.defaultIfNull(value, "default");
 *
 * // Safe object comparison
 * boolean equal = ObjectUtils.equals(obj1, obj2);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class ObjectUtils {

    /**
     * Returns the first non-null value from the array
     *
     * <p>Finds and returns the first non-null value from the given value array.
     * If all values are null or the array is null or empty, returns null.</p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * ObjectUtils.firstNonNull(null, null)      = null
     * ObjectUtils.firstNonNull(null, "")        = ""
     * ObjectUtils.firstNonNull(null, null, "")  = ""
     * ObjectUtils.firstNonNull(null, "zz")      = "zz"
     * ObjectUtils.firstNonNull("abc", "def")    = "abc"
     * ObjectUtils.firstNonNull(null, "xyz", "*") = "xyz"
     * ObjectUtils.firstNonNull(Boolean.TRUE, Boolean.FALSE) = Boolean.TRUE
     * ObjectUtils.firstNonNull()                = null
     * }</pre>
     *
     * @param <T>    Array element type
     * @param values Array of values to check, can be null or empty
     * @return First non-null value, or null if no non-null value exists
     * @since 1.0.0
     */
    @SafeVarargs
    public static <T> T firstNonNull(final T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns default value if object is null, otherwise returns the object itself
     *
     * <p>Provides a safe way to handle potentially null objects, ensuring never returns null.</p>
     *
     * @param <T>          Object type
     * @param object       Object to check
     * @param defaultValue Default value when object is null, cannot be null
     * @return Returns object if object is not null, otherwise returns defaultValue
     * @throws IllegalArgumentException when defaultValue is null
     * @since 1.0.0
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        Objects.requireNonNull(defaultValue, "Default value cannot be null");
        return object != null ? object : defaultValue;
    }

    /**
     * Safely compares two objects for equality
     *
     * <p>Handles null-safe object comparison, two null objects are considered equal.</p>
     *
     * @param object1 First object
     * @param object2 Second object
     * @return Returns true if two objects are equal, otherwise false
     * @since 1.0.0
     */
    public static boolean equals(final Object object1, final Object object2) {
        return Objects.equals(object1, object2);
    }

    /**
     * Gets safe hash code of object
     *
     * <p>Returns the hash code of object, returns 0 if object is null.</p>
     *
     * @param object Object to get hash code for
     * @return Hash code of object, returns 0 if object is null
     * @since 1.0.0
     */
    public static int hashCode(final Object object) {
        return Objects.hashCode(object);
    }

    /**
     * Wraps object as Optional
     *
     * <p>Safely wraps a potentially null object as an Optional instance.</p>
     *
     * @param <T>    Object type
     * @param object Object to wrap
     * @return Optional containing the object, returns empty Optional if object is null
     * @since 1.0.0
     */
    public static <T> Optional<T> toOptional(final T object) {
        return Optional.ofNullable(object);
    }

    /**
     * Checks if object is null
     *
     * @param object Object to check
     * @return Returns true if object is null, otherwise false
     * @since 1.0.0
     */
    public static boolean isNull(final Object object) {
        return object == null;
    }

    /**
     * Checks if object is not null
     *
     * @param object Object to check
     * @return Returns true if object is not null, otherwise false
     * @since 1.0.0
     */
    public static boolean isNotNull(final Object object) {
        return object != null;
    }

    /**
     * Deep copy an object with high performance
     *
     * @param original The object to copy (can be null)
     * @return A deep copy of the object, or null if original is null
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T original) {
        if (original == null) {
            return null;
        }

        // Use IdentityHashMap for fast object identity tracking
        Map<Object, Object> copyMap = new IdentityHashMap<>();
        return (T) deepCopyInternal(original, copyMap);
    }

    /**
     * Internal recursive deep copy method with cycle detection
     */
    private static Object deepCopyInternal(Object original, Map<Object, Object> copyMap) {
        try {
            if (original == null) {
                return null;
            }

            // Return immutable objects directly
            if (isImmutable(original)) {
                return original;
            }

            // Check for cycles
            if (copyMap.containsKey(original)) {
                return copyMap.get(original);
            }

            Class<?> clazz = original.getClass();

            // Handle arrays
            if (clazz.isArray()) {
                return copyArray(original, copyMap);
            }

            // Handle collections
            if (original instanceof Collection) {
                return copyCollection((Collection<?>) original, copyMap);
            }

            // Handle maps
            if (original instanceof Map) {
                return copyMap((Map<?, ?>) original, copyMap);
            }

            // Handle custom objects
            return copyObject(original, clazz, copyMap);
        } catch (Exception e) {
            log.error("Error during deep copy", e);
            return null;
        }
    }

    /**
     * Check if an object is immutable
     */
    private static boolean isImmutable(Object obj) {
        Class<?> clazz = obj.getClass();

        // Primitive wrappers and String
        if (clazz.isPrimitive() || obj instanceof String) {
            return true;
        }

        // Number types
        if (obj instanceof Number) {
            return true;
        }

        // Boolean
        if (obj instanceof Boolean) {
            return true;
        }

        // Character
        if (obj instanceof Character) {
            return true;
        }

        // Enum types
        if (obj instanceof Enum) {
            return true;
        }

        // Java 8+ time types
        if (obj instanceof java.time.temporal.Temporal ||
                obj instanceof java.time.Duration ||
                obj instanceof java.time.Period) {
            return true;
        }

        // URI and URL
        if (obj instanceof java.net.URI || obj instanceof java.net.URL) {
            return true;
        }

        // UUID
        if (obj instanceof java.util.UUID) {
            return true;
        }

        return false;
    }

    /**
     * Copy an array
     */
    private static Object copyArray(Object original, Map<Object, Object> copyMap) {
        int length = Array.getLength(original);
        Class<?> componentType = original.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, length);

        // Register in copy map to handle self-references
        copyMap.put(original, newArray);

        if (componentType.isPrimitive()) {
            // Fast copy for primitive arrays
            System.arraycopy(original, 0, newArray, 0, length);
        } else {
            // Deep copy for object arrays
            for (int i = 0; i < length; i++) {
                Object element = Array.get(original, i);
                Object copiedElement = deepCopyInternal(element, copyMap);
                Array.set(newArray, i, copiedElement);
            }
        }

        return newArray;
    }

    /**
     * Copy a collection
     */
    private static Collection<?> copyCollection(Collection<?> original, Map<Object, Object> copyMap) {
        Collection<?> newCollection;

        try {
            // Try to create the same collection type
            newCollection = original.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Fallback to ArrayList for lists, HashSet for sets, LinkedList for queues
            if (original instanceof List) {
                newCollection = new ArrayList<>();
            } else if (original instanceof Set) {
                newCollection = new HashSet<>();
            } else if (original instanceof Queue) {
                newCollection = new LinkedList<>();
            } else {
                newCollection = new ArrayList<>();
            }
        }

        // Register in copy map
        copyMap.put(original, newCollection);

        // Deep copy elements
        for (Object element : original) {
            Object copiedElement = deepCopyInternal(element, copyMap);
            ((Collection<Object>) newCollection).add(copiedElement);
        }

        return newCollection;
    }

    /**
     * Copy a map
     */
    private static Map<?, ?> copyMap(Map<?, ?> original, Map<Object, Object> copyMap) {
        Map<?, ?> newMap;

        try {
            // Try to create the same map type
            newMap = original.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Fallback to HashMap or LinkedHashMap
            if (original instanceof LinkedHashMap) {
                newMap = new LinkedHashMap<>();
            } else {
                newMap = new HashMap<>();
            }
        }

        // Register in copy map
        copyMap.put(original, newMap);

        // Deep copy entries
        for (Map.Entry<?, ?> entry : original.entrySet()) {
            Object copiedKey = deepCopyInternal(entry.getKey(), copyMap);
            Object copiedValue = deepCopyInternal(entry.getValue(), copyMap);
            ((Map<Object, Object>) newMap).put(copiedKey, copiedValue);
        }

        return newMap;
    }

    /**
     * Cache for class fields to avoid repeated reflection
     */
    private static final ConcurrentHashMap<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Copy a custom object using reflection with caching
     */
    private static Object copyObject(Object original, Class<?> clazz, Map<Object, Object> copyMap) {
        try {
            // Create new instance
            Object newObj = clazz.getDeclaredConstructor().newInstance();
            // Register in copy map
            copyMap.put(original, newObj);

            // Get cached fields
            List<Field> fields = getFields(clazz);

            // Copy each field
            for (Field field : fields) {
                Object value = field.get(original);
                Object copiedValue = deepCopyInternal(value, copyMap);
                field.set(newObj, copiedValue);
            }

            return newObj;
        } catch (Exception e) {
            try {
                return clazz.getDeclaredMethod("deepCopy", (Class[]) null).invoke(original); // jdk14+ record class type do not have no-arg constructor, must manually write deepCopy method
            } catch (Exception ex) {
                throw new RuntimeException("Failed to copy object: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get all non-static fields for a class, including inherited ones, with caching
     */
    private static List<Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;

            // Traverse inheritance chain
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
                current = current.getSuperclass();
            }

            return fields;
        });
    }

}
