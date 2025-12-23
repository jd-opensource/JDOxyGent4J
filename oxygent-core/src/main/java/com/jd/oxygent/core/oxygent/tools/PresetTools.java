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

/**
 * Preset tool collection management class providing unified access interface for OxyGent framework built-in tools.
 * <p>
 * This class serves as a tool registry, managing and providing various functional tool instances
 * built into the framework. All tools are pre-configured and initialized for direct use.
 * Supports tool lookup by name and provides tool metadata query functionality.
 * </p>
 *
 * <p><strong>Built-in Tool List:</strong></p>
 * <ul>
 *   <li>{@link TimeTool} - Time processing tool, supports timezone query and conversion</li>
 *   <li>{@link FileTool} - File operation tool, supports file read/write/delete</li>
 *   <li>{@link MathTool} - Mathematical calculation tool, supports high-precision operations</li>
 *   <li>{@link JokeTools} - Joke generation tool, provides entertainment functionality</li>
 * </ul>
 *
 * <p><strong>Design Features:</strong></p>
 * <ul>
 *   <li>Thread-safe singleton pattern implementation</li>
 *   <li>Lazy initialization to improve startup performance</li>
 *   <li>Unified tool access interface</li>
 *   <li>Comprehensive error handling and null value checking</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Get time tool
 * FunctionHub timeTool = PresetTools.getTimeTool();
 * String currentTime = (String) timeTool.call("get_current_time", "Asia/Shanghai");
 *
 * // Get tool by name
 * Optional<FunctionHub> tool = PresetTools.getToolByName("time_tools");
 *
 * // Get all tool names
 * Set<String> toolNames = PresetTools.getAllToolNames();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see TimeTool Time processing tool
 * @see FileTool File operation tool
 * @see MathTool Mathematical calculation tool
 * @see JokeTools Joke generation tool
 * @since 1.0.0
 */
public final class PresetTools {

    // ========== Preset Tool Instances (Lazy Initialization) ==========

    /**
     * Time processing tool instance.
     * <p>
     * Provides cross-timezone time query and conversion functionality, supports IANA
     * standard timezone formats. Specially optimized for Chinese users, defaults to
     * Asia/Shanghai timezone.
     * </p>
     */
    public static final FunctionHub TIME_TOOLS = new TimeTool();

    /**
     * File operation tool instance.
     * <p>
     * Provides basic file system operation functionality, including file read/write
     * and delete. All operations use UTF-8 encoding to ensure correct handling of
     * Chinese characters.
     * </p>
     */
    public static final FunctionHub FILE_TOOLS = new FileTool();

    /**
     * Mathematical calculation tool instance.
     * <p>
     * Provides high-precision mathematical operation functionality, including power
     * operations and pi calculations. Uses BigDecimal to ensure calculation precision,
     * suitable for scientific computing scenarios.
     * </p>
     */
    public static final FunctionHub MATH_TOOLS = new MathTool();

    public static final FunctionHub IMAGE_GEN_TOOLS = new ImageGenTools();
    public static final FhMathTools FH_MATH_TOOLS = new FhMathTools();

}