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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Field exclusion marker annotation
 *
 * <h3>Functional Description</h3>
 * <ul>
 *   <li>Used to mark fields that need to be excluded during object export process</li>
 *   <li>Works in conjunction with ClassModelDumpUtils utility class</li>
 *   <li>Provides fine-grained field export control capability</li>
 *   <li>Supports runtime reflection checking and processing</li>
 * </ul>
 *
 * <h3>Design Purpose</h3>
 * <ul>
 *   <li>Data Security: Exclude sensitive fields to avoid accidental leakage</li>
 *   <li>Performance Optimization: Reduce unnecessary field serialization overhead</li>
 *   <li>Interface Specification: Control the field scope of API response data</li>
 *   <li>Version Compatibility: Flexibly adjust export fields without affecting class structure</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li>Exclude sensitive information fields like passwords, tokens</li>
 *   <li>Exclude internal state, cache and other non-business fields</li>
 *   <li>Exclude temporary calculation results or intermediate variables</li>
 *   <li>Exclude deprecated fields related to version compatibility</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * public class User {
 *     private String username;
 *     private String email;
 *
 *     @ExcludeOption
 *     private String password;        // Exclude sensitive field
 *
 *     @ExcludeOption
 *     private transient String token; // Exclude temporary field
 *
 *     // getter/setter methods...
 * }
 *
 * // When using ClassModelDumpUtils for export, fields marked with @ExcludeOption will be automatically skipped
 * Map<String, Object> userMap = ClassModelDumpUtils.modelDump(user);
 * // userMap only contains username and email, excludes password and token
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see ClassModelDumpUtils#modelDump(Object)
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeOption {
}
