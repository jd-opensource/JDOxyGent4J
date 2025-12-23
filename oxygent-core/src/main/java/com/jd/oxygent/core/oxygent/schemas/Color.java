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
package com.jd.oxygent.core.oxygent.schemas;

/**
 * <h3>Terminal Color Enum Class</h3>
 *
 * <p>The Color enum defines the standard color set for terminal display in the OxyGent framework.
 * This enum is mainly used for console text coloring, log level distinction, status indication, etc.
 * It provides a unified color standard and cross-platform terminal compatibility.</p>
 *
 * <h3>Design Goals</h3>
 * <ul>
 *   <li><strong>Standardization</strong>: Provides unified color definitions for UI consistency</li>
 *   <li><strong>Compatibility</strong>: Supports mainstream terminals and console color display</li>
 *   <li><strong>Extensibility</strong>: Reserved for future expansion, more colors and styles</li>
 *   <li><strong>Simplicity</strong>: Offers common basic colors, avoids unnecessary complexity</li>
 * </ul>
 *
 * <h3>Supported Colors</h3>
 * <ul>
 *   <li><strong>DEFAULT</strong>: Default color, usually the terminal's default text color</li>
 *   <li><strong>BLACK</strong>: Black, used for emphasis or background contrast</li>
 *   <li><strong>RED</strong>: Red, typically for error messages and warnings</li>
 *   <li><strong>GREEN</strong>: Green, usually for success messages and normal status</li>
 *   <li><strong>YELLOW</strong>: Yellow, often for warnings and notices</li>
 *   <li><strong>BLUE</strong>: Blue, commonly for information prompts and link text</li>
 *   <li><strong>MAGENTA</strong>: Magenta, for special markers and highlights</li>
 *   <li><strong>CYAN</strong>: Cyan, used for debug info and secondary text</li>
 *   <li><strong>WHITE</strong>: White, for highlighting and important text</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li><strong>Log Output</strong>: Use different colors based on log level (ERROR=Red, WARN=Yellow, INFO=Blue, etc.)</li>
 *   <li><strong>Status Display</strong>: Distinguish system states or operation results by color</li>
 *   <li><strong>Console UI</strong>: Build colorful command-line interfaces and prompts</li>
 *   <li><strong>Progress Indication</strong>: Use color to represent task progress and execution status</li>
 * </ul>
 *
 * <h3>Color Semantic Conventions</h3>
 * <p>For consistent user experience, it is recommended to follow these color semantic conventions:</p>
 * <ul>
 *   <li><strong>Success/Normal</strong>: GREEN</li>
 *   <li><strong>Error/Failure</strong>: RED</li>
 *   <li><strong>Warning/Attention</strong>: YELLOW</li>
 *   <li><strong>Info/Prompt</strong>: BLUE or CYAN</li>
 *   <li><strong>Debug/Detail</strong>: WHITE or DEFAULT</li>
 * </ul>
 *
 * <h3>Compatibility Notes</h3>
 * <ul>
 *   <li><strong>ANSI Support</strong>: Modern terminals generally support ANSI color codes</li>
 *   <li><strong>Windows Compatibility</strong>: PowerShell and CMD on Windows 10+ support color display</li>
 *   <li><strong>IDE Integration</strong>: Mainstream IDE consoles support color output</li>
 *   <li><strong>Degradation Handling</strong>: Environments that do not support color will fallback to plain text</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Use in log system
 * public void logMessage(String message, LogLevel level) {
 *     Color color = switch (level) {
 *         case ERROR -> Color.RED;
 *         case WARN -> Color.YELLOW;
 *         case INFO -> Color.BLUE;
 *         case DEBUG -> Color.CYAN;
 *         default -> Color.DEFAULT;
 *     };
 *     printWithColor(message, color);
 * }
 *
 * // Use in status display
 * public void showTaskStatus(TaskStatus status) {
 *     Color statusColor = switch (status) {
 *         case COMPLETED -> Color.GREEN;
 *         case FAILED -> Color.RED;
 *         case RUNNING -> Color.YELLOW;
 *         case PENDING -> Color.BLUE;
 *     };
 *     System.out.println(colorize("Task Status: " + status, statusColor));
 * }
 * }</pre>
 *
 * <h3>Extensibility</h3>
 * <p>This enum is designed for extensibility. Future versions may add:</p>
 * <ul>
 *   <li><strong>More Colors</strong>: Support for 256-color or true color mode</li>
 *   <li><strong>Style Attributes</strong>: Bold, italic, underline, etc.</li>
 *   <li><strong>Background Colors</strong>: Support for background and foreground combinations</li>
 *   <li><strong>Theme Support</strong>: Adaptation for light/dark theme colors</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum Color {
    /**
     * Default terminal color, indicating no specific color is applied and the terminal's default color scheme should be used.
     */
    DEFAULT,

    /**
     * Black foreground color (ANSI foreground code: 30).
     */
    BLACK,

    /**
     * Red foreground color (ANSI foreground code: 31).
     */
    RED,

    /**
     * Green foreground color (ANSI foreground code: 32).
     */
    GREEN,

    /**
     * Yellow foreground color (ANSI foreground code: 33).
     */
    YELLOW,

    /**
     * Blue foreground color (ANSI foreground code: 34).
     */
    BLUE,

    /**
     * Magenta (or fuchsia) foreground color (ANSI foreground code: 35).
     */
    MAGENTA,

    /**
     * Cyan (or aqua) foreground color (ANSI foreground code: 36).
     */
    CYAN,

    /**
     * White foreground color (ANSI foreground code: 37).
     */
    WHITE
}