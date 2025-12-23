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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

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
     * Deep copy of Map
     *
     * @param original
     * @return
     */
    public static <T> T deepCopy(T original) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(original);
            return objectMapper.readValue(json, (Class<T>) original.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
