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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Objects;

/**
 * Mathematical calculation tool class providing high-precision mathematical operations.
 * <p>
 * This tool class provides commonly used mathematical calculation functions, including
 * power operations and high-precision pi calculations. All calculations use BigDecimal
 * for high-precision arithmetic to ensure accuracy of calculation results. Particularly
 * suitable for scenarios requiring high-precision numerical calculations.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>Power Operations - Calculate specified powers of base numbers</li>
 *   <li>Pi Calculation - Calculate high-precision π values using Ramanujan series algorithm</li>
 *   <li>Factorial Calculation - Support large integer factorial operations</li>
 * </ul>
 *
 * <p><strong>Algorithm Features:</strong></p>
 * <ul>
 *   <li>Uses BigDecimal to ensure high-precision operations</li>
 *   <li>Adopts Ramanujan series fast convergence algorithm for π calculation</li>
 *   <li>Supports arbitrary precision numerical calculations</li>
 *   <li>Built-in parameter validation and error handling</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * MathTool mathTool = new MathTool();
 *
 * // Calculate 2 to the power of 10
 * Object result1 = mathTool.call("power", 2, 10);
 *
 * // Calculate pi with 20 decimal places precision
 * Object result2 = mathTool.call("calc_pi", 20);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see BigDecimal High-precision numerical calculation class
 * @since 1.0.0
 */
public class MathTool extends FunctionHub {

    /**
     * Constructor to initialize math tool and register related calculation functions.
     * <p>
     * Sets tool name to "math_tools" and registers power operations and pi calculation
     * as two core functions. All mathematical operations are based on high-precision
     * BigDecimal implementation.
     * </p>
     */
    public MathTool() {
        super("math_tools");
        this.setDesc("Tool set providing high-precision mathematical calculation functionality, including power operations and pi calculations");

        // Register power operation tool
        this.registerTool(
                "power",
                "Calculate specified powers of base numbers, supports integer and decimal power operations",
                this::calculatePower,
                Arrays.asList(
                        new ParamMeta("n", "Number", "Base number, supports integers and decimals", null),
                        new ParamMeta("m", "Number", "Exponent, supports integers and decimals", 2)
                )
        );

        // Register pi calculation tool
        this.registerTool(
                "calc_pi",
                "Calculate pi value with specified precision using Ramanujan series algorithm",
                this::calculatePi,
                Arrays.asList(
                        new ParamMeta("precision", "Number", "Number of decimal places, recommended not to exceed 100", null)
                )
        );
    }

    /**
     * Calculate power operations.
     * <p>
     * Uses Math.pow to calculate specified powers of base numbers. Supports integer
     * and decimal power operations. If exponent parameter is not provided, defaults
     * to calculating square.
     * </p>
     *
     * @param args Parameter array, args[0] is base, args[1] is exponent (optional, defaults to 2)
     * @return Power operation result
     */
    private Object calculatePower(Object... args) {
        try {
            Objects.requireNonNull(args, "Parameters cannot be null");
            if (args.length == 0) {
                return "Error: Missing base parameter";
            }

            var base = Double.parseDouble(args[0].toString());
            var exponent = args.length > 1 ? Double.parseDouble(args[1].toString()) : 2.0;

            var result = Math.pow(base, exponent);

            // Check if result is valid
            if (Double.isNaN(result)) {
                return "Error: Calculation result is NaN, please check input parameters";
            }
            if (Double.isInfinite(result)) {
                return "Error: Calculation result exceeds numerical range";
            }

            return result;
        } catch (ClassCastException e) {
            return "Error: Parameters must be numeric types";
        } catch (Exception e) {
            return "Power operation calculation failed: " + e.getMessage();
        }
    }

    /**
     * Calculate high-precision pi.
     * <p>
     * Uses Ramanujan series algorithm to calculate π value with specified precision.
     * This algorithm has fast convergence speed and is suitable for calculating
     * high-precision pi. Recommended precision should not exceed 100 digits to avoid
     * excessive calculation time.
     * </p>
     *
     * @param args Parameter array, args[0] is precision digits
     * @return Calculated high-precision pi
     */
    private Object calculatePi(Object... args) {
        try {
            Objects.requireNonNull(args, "Parameters cannot be null");
            if (args.length == 0) {
                return "Error: Missing precision parameter";
            }

            var precision = Integer.parseInt(args[0].toString());

            if (precision <= 0) {
                return "Error: Precision must be positive";
            }
            if (precision > 1000) {
                return "Error: Precision too high, recommended not to exceed 1000 digits";
            }

            var pi = calculatePiInternal(precision);
            return pi.toString();
        } catch (ClassCastException e) {
            return "Error: Precision parameter must be integer type";
        } catch (Exception e) {
            return "Pi calculation failed: " + e.getMessage();
        }
    }

    /**
     * Internal implementation of high-precision pi calculation.
     * <p>
     * Uses Ramanujan series formula to calculate π value:
     * 1/π = (2√2/9801) * Σ[k=0 to ∞] ((4k)! * (1103 + 26390k)) / ((k!)^4 * 396^(4k))
     * </p>
     *
     * <p><strong>Algorithm Features:</strong></p>
     * <ul>
     *   <li>Each term contributes approximately 8 significant digits</li>
     *   <li>Extremely fast convergence, suitable for high-precision calculations</li>
     *   <li>Uses BigDecimal to ensure calculation precision</li>
     * </ul>
     *
     * @param precision Required decimal places precision
     * @return High-precision pi value
     */
    private static BigDecimal calculatePiInternal(int precision) {
        var mathContext = new MathContext(precision + 10, RoundingMode.HALF_UP);
        var sum = BigDecimal.ZERO;
        var two = new BigDecimal("2");
        var sqrt2 = two.sqrt(mathContext);
        var coefficient = two.multiply(sqrt2).divide(new BigDecimal("9801"), mathContext);

        // Calculate series, each term contributes approximately 8 digits precision
        var maxTerms = Math.max(precision / 8 + 2, 5);
        for (int k = 0; k < maxTerms; k++) {
            var numerator = factorial(4 * k).multiply(new BigDecimal(1103 + 26390L * k));
            var denominator = factorial(k).pow(4, mathContext)
                    .multiply(new BigDecimal("396").pow(4 * k, mathContext));

            var term = numerator.divide(denominator, mathContext);
            sum = sum.add(term);

            // Early termination if current term is sufficiently small
            if (term.abs().compareTo(BigDecimal.ONE.divide(
                    BigDecimal.TEN.pow(precision + 5), mathContext)) < 0) {
                break;
            }
        }

        var piInverse = coefficient.multiply(sum);
        return BigDecimal.ONE.divide(piInverse, precision, RoundingMode.HALF_UP);
    }

    /**
     * Calculate factorial.
     * <p>
     * Uses BigDecimal to calculate factorial of large integers, supports arbitrarily
     * large positive integers. For 0 and 1, directly returns 1; for negative numbers,
     * throws exception.
     * </p>
     *
     * @param n Non-negative integer to calculate factorial
     * @return Factorial value of n
     * @throws IllegalArgumentException when n is negative
     */
    public static BigDecimal factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial calculation does not support negative numbers: " + n);
        }
        if (n <= 1) {
            return BigDecimal.ONE;
        }

        var result = BigDecimal.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigDecimal.valueOf(i));
        }
        return result;
    }

    /**
     * Static method for backward compatibility.
     * <p>
     * To maintain compatibility with existing code, the original static method interface
     * is retained. Internally calls the refactored calculatePiInternal method.
     * </p>
     *
     * @param precision Precision digits
     * @return Calculated pi
     * @deprecated Recommend using instance method call mathTool.call("calc_pi", precision)
     */
    @Deprecated
    public static BigDecimal calcPi(int precision) {
        return calculatePiInternal(precision);
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of MathTool.
     * <p>
     * Tests power operations and pi calculation functions to verify tool correctness
     * and performance.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        var mathTool = new MathTool();

        System.out.println("=== Math Tool Test ===");

        // Test power operations
        System.out.println("1. Power operation test:");
        var powerResult1 = mathTool.call("power", 2, 10);
        System.out.println("   2^10 = " + powerResult1);

        var powerResult2 = mathTool.call("power", 3.5, 2);
        System.out.println("   3.5^2 = " + powerResult2);

        // Test pi calculation
        System.out.println("\n2. Pi calculation test:");
        var piResult1 = mathTool.call("calc_pi", 10);
        System.out.println("   π(10 digits precision) = " + piResult1);

        var piResult2 = mathTool.call("calc_pi", 50);
        System.out.println("   π(50 digits precision) = " + piResult2);

        // Test error handling
        System.out.println("\n3. Error handling test:");
        var errorResult1 = mathTool.call("power");
        System.out.println("   No parameter power operation: " + errorResult1);

        var errorResult2 = mathTool.call("calc_pi", -5);
        System.out.println("   Negative precision pi: " + errorResult2);
    }
}
