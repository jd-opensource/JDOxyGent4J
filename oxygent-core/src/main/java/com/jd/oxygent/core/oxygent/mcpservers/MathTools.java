package com.jd.oxygent.core.oxygent.mcpservers;


import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mathematical tools providing power calculation and pi calculation.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class MathTools {

    /**
     * Power calculator tool
     *
     * @param base Base number
     * @param exponent Exponent (default is 2)
     * @return The result of base raised to the power of exponent
     */
    @MCPTool(name = "power",
            description = "Power calculator tool")
    public Map<String, Object> power(
            @ToolParam(description = "base")
            int base,
            @ToolParam(description = "index", defaultValue = "2")
            int exponent) {

        double result = Math.pow(base, exponent);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("base", base);
        response.put("exponent", exponent);
        response.put("result", result);
        response.put("type", "integer");

        return response;
    }

    /**
     * Calculate pi to specified precision
     * Uses the Ramanujan formula for pi calculation
     *
     * @param precision How many digits after the decimal point
     * @return Pi value with specified precision
     */
    @MCPTool(name = "calc_pi",
            description = "Pi calculation tool")
    public Map<String, Object> calcPi(
            @ToolParam(description = "How many digits after the dot")
            int precision) {

        // Set the precision for BigDecimal calculations
        MathContext mc = new MathContext(precision + 10, RoundingMode.HALF_UP);

        // Calculate pi using Ramanujan formula
        BigDecimal sum = BigDecimal.ZERO;
        int terms = precision / 8 + 1;

        for (int k = 0; k <= terms; k++) {
            // Calculate term using Ramanujan formula
            BigDecimal term = calculateRamanujanTerm(k, mc);
            sum = sum.add(term);
        }

        // Pi = 1 / sum
        BigDecimal pi = BigDecimal.ONE.divide(sum, mc);

        // Round to requested precision
        pi = pi.setScale(precision, RoundingMode.HALF_UP);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("precision", precision);
        response.put("pi", pi.toString());
        response.put("terms_calculated", terms + 1);
        response.put("formula", "Ramanujan");

        return response;
    }

    /**
     * Calculate a single term of the Ramanujan series for pi
     */
    private BigDecimal calculateRamanujanTerm(int k, MathContext mc) {
        // Constants in the formula
        BigDecimal sqrt2 = sqrt(new BigDecimal(2), mc);
        BigDecimal numeratorConst = new BigDecimal(2).multiply(sqrt2);
        BigDecimal denominatorConst = new BigDecimal(9801);

        // (1103 + 26390k)
        BigDecimal a = new BigDecimal(1103)
                .add(new BigDecimal(26390).multiply(new BigDecimal(k)));

        // (4k)! / (k!)^4
        BigDecimal factorial4k = factorial(4 * k);
        BigDecimal factorialK = factorial(k);
        BigDecimal factorialK4 = factorialK.pow(4);
        BigDecimal b = factorial4k.divide(factorialK4, mc);

        // 396^(4k)
        BigDecimal c = new BigDecimal(396).pow(4 * k);

        // Calculate the term: (2√2/9801) * ((4k)!/(k!)^4) * (1103+26390k) / 396^(4k)
        BigDecimal term = numeratorConst.divide(denominatorConst, mc)
                .multiply(b)
                .multiply(a)
                .divide(c, mc);

        return term;
    }

    /**
     * Calculate factorial of n using BigDecimal for precision
     */
    private BigDecimal factorial(int n) {
        if (n == 0 || n == 1) {
            return BigDecimal.ONE;
        }

        BigDecimal result = BigDecimal.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(new BigDecimal(i));
        }
        return result;
    }

    /**
     * Calculate square root using BigDecimal
     */
    private BigDecimal sqrt(BigDecimal value, MathContext mc) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal x = new BigDecimal(Math.sqrt(value.doubleValue()));
        if (value.subtract(x.multiply(x)).abs().compareTo(new BigDecimal("1E-10")) < 0) {
            return x;
        }

        // Use Newton's method for better precision
        for (int i = 0; i < 10; i++) {
            x = x.add(value.divide(x, mc)).divide(new BigDecimal(2), mc);
        }
        return x;
    }

    /**
     * Combined mathematical operations tool
     *
     * @param operation Operation to perform: "power", "pi", or "both"
     * @param base Base for power calculation (required for power operations)
     * @param exponent Exponent for power calculation (default 2)
     * @param precision Precision for pi calculation (default 10)
     * @return Combined results based on requested operations
     */
    @MCPTool(name = "calculate",
            description = "Perform mathematical calculations")
    public Map<String, Object> calculate(
            @ToolParam(description = "Operation: 'power', 'pi', or 'both'")
            String operation,
            @ToolParam(description = "Base number for power calculation", required = false)
            Integer base,
            @ToolParam(description = "Exponent for power calculation", defaultValue = "2", required = false)
            Integer exponent,
            @ToolParam(description = "Precision for pi calculation", defaultValue = "10", required = false)
            Integer precision) {

        Map<String, Object> response = new LinkedHashMap<>();

        switch (operation.toLowerCase()) {
            case "power":
                if (base == null) {
                    throw new IllegalArgumentException("Base is required for power calculation");
                }
                Map<String, Object> powerResult = power(base, exponent != null ? exponent : 2);
                response.put("operation", "power");
                response.putAll(powerResult);
                break;

            case "pi":
                Map<String, Object> piResult = calcPi(precision != null ? precision : 10);
                response.put("operation", "pi");
                response.putAll(piResult);
                break;

            case "both":
                if (base == null) {
                    throw new IllegalArgumentException("Base is required for combined calculation");
                }
                Map<String, Object> powerBoth = power(base, exponent != null ? exponent : 2);
                Map<String, Object> piBoth = calcPi(precision != null ? precision : 10);

                response.put("operation", "both");
                response.put("power_result", powerBoth.get("result"));
                response.put("power_details", powerBoth);
                response.put("pi_result", piBoth.get("pi"));
                response.put("pi_details", piBoth);
                break;

            default:
                throw new IllegalArgumentException("Invalid operation. Use 'power', 'pi', or 'both'");
        }

        return response;
    }

    /**
     * Main method to start the MCP server
     *
     * @param args Command line arguments
     */
    @EnableMcpServer(mode = "stdio")
    public static void main(String[] args) {
        McpServer.start();
    }
}