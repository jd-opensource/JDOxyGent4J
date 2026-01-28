package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Simplified mathematical tools service with only the two Python functions.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class MathToolsStreamable {

    /**
     * Power calculator tool - exact Python equivalent
     *
     * @param n Base number
     * @param m Exponent (default: 2)
     * @return n raised to the power of m
     */
    @MCPTool(name = "power",
            description = "Power calculator tool")
    public double power(
            @ToolParam(description = "base")
            int n,
            @ToolParam(description = "index", defaultValue = "2")
            int m) {

        // Use Math.pow for consistency with Python
        return Math.pow(n, m);
    }

    /**
     * Pi calculation tool using Ramanujan's formula - exact Python equivalent
     *
     * @param prec Number of digits after the decimal point
     * @return Pi calculated to the specified precision
     */
    @MCPTool(name = "calc_pi",
            description = "Pi calculation tool")
    public String calcPi(
            @ToolParam(description = "How many digits after the dot")
            int prec) {

        try {
            // Set precision similar to Python's Decimal.getcontext().prec
            MathContext mc = new MathContext(prec + 10);
            BigDecimal sum = BigDecimal.ZERO;

            // Python: for k in range(int(prec / 8) + 1):
            int maxK = prec / 8 + 1;

            for (int k = 0; k <= maxK; k++) {
                // Python: a = 2 * Decimal.sqrt(Decimal(2)) / 9801
                BigDecimal a = BigDecimal.valueOf(2)
                        .multiply(sqrt(BigDecimal.valueOf(2), mc))
                        .divide(BigDecimal.valueOf(9801), mc);

                // Python: b = math.factorial(4 * k) * (1103 + 26390 * k)
                BigInteger factorial4k = factorial(4 * k);
                BigDecimal b = new BigDecimal(factorial4k)
                        .multiply(BigDecimal.valueOf(1103 + 26390L * k));

                // Python: c = pow(math.factorial(k), 4) * pow(396, 4 * k)
                BigInteger factorialK4 = factorial(k).pow(4);
                BigDecimal c = new BigDecimal(factorialK4)
                        .multiply(BigDecimal.valueOf(396).pow(4 * k, mc));

                // Python: x = x + a * b / c
                BigDecimal term = a.multiply(b, mc)
                        .divide(c, mc);

                sum = sum.add(term, mc);
            }

            // Python: return 1 / x
            BigDecimal pi = BigDecimal.ONE.divide(sum, mc);

            // Format with specified precision
            return pi.setScale(prec, RoundingMode.HALF_UP).toString();

        } catch (Exception e) {
            return "Error calculating pi: " + e.getMessage();
        }
    }

    /**
     * Helper method: Calculate square root with high precision
     */
    private BigDecimal sqrt(BigDecimal value, MathContext mc) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ArithmeticException("Cannot compute square root of negative number");
        }

        // Use Babylonian method (Heron's method)
        BigDecimal x0 = BigDecimal.ZERO;
        BigDecimal x1 = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));

        while (!x0.equals(x1)) {
            x0 = x1;
            x1 = value.divide(x0, mc);
            x1 = x1.add(x0, mc);
            x1 = x1.divide(BigDecimal.valueOf(2), mc);
        }

        return x1;
    }

    /**
     * Helper method: Calculate factorial
     */
    private BigInteger factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        }

        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    /**
     * Additional useful math tool: Calculate area of a circle
     *
     * @param radius Radius of the circle
     * @param precision Number of decimal places
     * @return Area of the circle
     */
    @MCPTool(name = "circle_area",
            description = "Calculate area of a circle")
    public String circleArea(
            @ToolParam(description = "Radius of the circle")
            double radius,
            @ToolParam(description = "Number of decimal places", defaultValue = "10")
            int precision) {

        if (radius < 0) {
            return "Error: Radius cannot be negative";
        }

        MathContext mc = new MathContext(precision + 10);
        BigDecimal r = BigDecimal.valueOf(radius);

        // Calculate pi with same precision
        BigDecimal pi = new BigDecimal(calcPi(precision));

        // Area = π * r^2
        BigDecimal area = pi.multiply(r.pow(2, mc), mc);

        return area.setScale(precision, RoundingMode.HALF_UP).toString();
    }

    /**
     * Additional useful math tool: Convert between degrees and radians
     *
     * @param degrees Angle in degrees
     * @return Angle in radians
     */
    @MCPTool(name = "degrees_to_radians",
            description = "Convert degrees to radians")
    public String degreesToRadians(
            @ToolParam(description = "Angle in degrees")
            double degrees,
            @ToolParam(description = "Number of decimal places", defaultValue = "10")
            int precision) {

        MathContext mc = new MathContext(precision + 10);
        BigDecimal deg = BigDecimal.valueOf(degrees);
        BigDecimal pi = new BigDecimal(calcPi(precision));

        // radians = degrees * π / 180
        BigDecimal radians = deg.multiply(pi, mc)
                .divide(BigDecimal.valueOf(180), mc);

        return radians.setScale(precision, RoundingMode.HALF_UP).toString();
    }

    /**
     * Main method to start the MCP server
     *
     * @param args Command line arguments
     */
    @EnableMcpServer(mode = "web",transport = "streamable")
    public static void main(String[] args) {
        McpServer.start();
    }
}