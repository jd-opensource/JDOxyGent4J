package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mathematical tools service with support for multiple transport modes.
 * Supports stdio, web, and SSE (Server-Sent Events) transport.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class MathToolSSE {

    /**
     * Power calculator tool
     *
     * @param n Base number
     * @param m Exponent (default: 2)
     * @return n raised to the power of m
     */
    @MCPTool(name = "power",
            description = "Power calculator tool")
    public Map<String, Object> power(
            @ToolParam(description = "base")
            int n,
            @ToolParam(description = "index", defaultValue = "2")
            int m) {

        try {
            double result = Math.pow(n, m);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("base", n);
            response.put("exponent", m);
            response.put("result", result);
            response.put("formula", String.format("%d^%d = %.6f", n, m, result));

            return response;

        } catch (Exception e) {
            return Map.of(
                    "error", "Failed to calculate power",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * Pi calculation tool using Ramanujan's formula
     *
     * @param prec Number of digits after the decimal point
     * @return Pi calculated to the specified precision
     */
    @MCPTool(name = "calc_pi",
            description = "Pi calculation tool")
    public Map<String, Object> calcPi(
            @ToolParam(description = "How many digits after the dot")
            int prec) {

        try {
            if (prec < 0) {
                return Map.of(
                        "error", "Invalid precision",
                        "message", "Precision must be non-negative"
                );
            }

            if (prec > 1000) {
                return Map.of(
                        "error", "Precision too high",
                        "message", "Precision must be <= 1000 for performance reasons"
                );
            }

            // Set precision for calculations
            MathContext mc = new MathContext(prec + 20); // Extra precision for intermediate calculations
            BigDecimal sum = BigDecimal.ZERO;

            int maxK = prec / 8 + 1;
            BigDecimal progressStep = BigDecimal.valueOf(100).divide(BigDecimal.valueOf(maxK + 1), mc);
            BigDecimal progress = BigDecimal.ZERO;

            // 如果是SSE模式，可以在这里发送进度更新
            System.err.println("[MathTool] Calculating pi with precision: " + prec + " digits");

            for (int k = 0; k <= maxK; k++) {
                // Calculate term using Ramanujan's formula
                BigDecimal a = BigDecimal.valueOf(2)
                        .multiply(sqrt(BigDecimal.valueOf(2), mc))
                        .divide(BigDecimal.valueOf(9801), mc);

                BigInteger factorial4k = factorial(4 * k);
                BigInteger factorialK4 = factorial(k).pow(4);

                BigDecimal b = new BigDecimal(factorial4k)
                        .multiply(BigDecimal.valueOf(1103 + 26390L * k));

                BigDecimal c = new BigDecimal(factorialK4)
                        .multiply(BigDecimal.valueOf(396).pow(4 * k, mc));

                BigDecimal term = a.multiply(b, mc)
                        .divide(c, mc);

                sum = sum.add(term, mc);

                // Update progress
                progress = progress.add(progressStep);
                int percent = progress.intValue();

                // 模拟SSE进度更新（实际SSE需要WebSocket或HTTP流）
                if (k % 10 == 0 || k == maxK) {
                    System.err.println(String.format("[MathTool] Pi calculation progress: %d%% (k=%d/%d)",
                            Math.min(percent, 100), k, maxK));
                }
            }

            BigDecimal pi = BigDecimal.ONE.divide(sum, mc);
            String piString = pi.setScale(prec, RoundingMode.HALF_UP).toString();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("precision", prec);
            response.put("pi", piString);
            response.put("length", piString.length());
            response.put("method", "Ramanujan's formula");
            response.put("iterations", maxK + 1);

            return response;

        } catch (Exception e) {
            return Map.of(
                    "error", "Failed to calculate pi",
                    "message", e.getMessage(),
                    "stackTrace", e.getStackTrace()
            );
        }
    }

    /**
     * Calculate pi with streaming progress updates (for SSE/WebSocket)
     *
     * @param prec Number of digits after the decimal point
     * @param stream Whether to stream progress updates
     * @return Pi calculation with optional streaming
     */
    @MCPTool(name = "calc_pi_stream",
            description = "Calculate pi with streaming progress updates")
    public Map<String, Object> calcPiStream(
            @ToolParam(description = "How many digits after the dot")
            int prec,
            @ToolParam(description = "Whether to stream progress updates", defaultValue = "false")
            boolean stream) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("precision", prec);
        result.put("streaming", stream);
        result.put("status", "started");

        // 在实际的SSE/WebSocket实现中，这里会建立流式连接
        // 这个示例返回立即结果，但标记为支持流式
        if (stream) {
            result.put("message", "Streaming mode requested - in a real SSE implementation, " +
                    "this would establish a streaming connection");
            result.put("stream_url", "/api/pi/stream/" + prec);
        }

        // 计算pi（非流式版本）
        Map<String, Object> piResult = calcPi(prec);
        result.putAll(piResult);
        result.put("status", "completed");

        return result;
    }

    /**
     * Mathematical constant e calculation
     *
     * @param prec Number of digits after the decimal point
     * @return Mathematical constant e
     */
    @MCPTool(name = "calc_e",
            description = "Calculate mathematical constant e")
    public Map<String, Object> calcE(
            @ToolParam(description = "How many digits after the dot")
            int prec) {

        try {
            MathContext mc = new MathContext(prec + 10);
            BigDecimal e = BigDecimal.ZERO;
            BigDecimal term = BigDecimal.ONE;

            for (int n = 0; n < 1000; n++) {
                if (n > 0) {
                    term = term.divide(BigDecimal.valueOf(n), mc);
                }

                BigDecimal previous = e;
                e = e.add(term, mc);

                // Check for convergence
                if (e.subtract(previous, mc).abs().compareTo(
                        BigDecimal.ONE.movePointLeft(prec + 5)) < 0) {
                    break;
                }
            }

            String eString = e.setScale(prec, RoundingMode.HALF_UP).toString();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("precision", prec);
            response.put("e", eString);
            response.put("length", eString.length());
            response.put("method", "Series expansion: e = Σ(1/n!)");

            return response;

        } catch (Exception e) {
            return Map.of(
                    "error", "Failed to calculate e",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * Calculate golden ratio (phi)
     *
     * @param prec Number of digits after the decimal point
     * @return Golden ratio phi
     */
    @MCPTool(name = "calc_golden_ratio",
            description = "Calculate golden ratio (phi)")
    public Map<String, Object> calcGoldenRatio(
            @ToolParam(description = "How many digits after the dot")
            int prec) {

        try {
            MathContext mc = new MathContext(prec + 10);
            BigDecimal sqrt5 = sqrt(BigDecimal.valueOf(5), mc);
            BigDecimal phi = BigDecimal.ONE.add(sqrt5, mc)
                    .divide(BigDecimal.valueOf(2), mc);

            String phiString = phi.setScale(prec, RoundingMode.HALF_UP).toString();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("precision", prec);
            response.put("phi", phiString);
            response.put("length", phiString.length());
            response.put("formula", "φ = (1 + √5) / 2");

            return response;

        } catch (Exception e) {
            return Map.of(
                    "error", "Failed to calculate golden ratio",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * Calculate multiple mathematical constants at once
     *
     * @param prec Number of digits after the decimal point
     * @return Multiple mathematical constants
     */
    @MCPTool(name = "math_constants",
            description = "Calculate multiple mathematical constants")
    public Map<String, Object> mathConstants(
            @ToolParam(description = "How many digits after the dot")
            int prec) {

        Map<String, Object> constants = new LinkedHashMap<>();
        constants.put("precision", prec);
        constants.put("timestamp", System.currentTimeMillis());

        // Calculate pi
        Map<String, Object> pi = calcPi(Math.min(prec, 100)); // Limit pi precision for performance
        constants.put("pi", pi.get("pi"));

        // Calculate e
        Map<String, Object> e = calcE(prec);
        constants.put("e", e.get("e"));

        // Calculate golden ratio
        Map<String, Object> phi = calcGoldenRatio(prec);
        constants.put("phi", phi.get("phi"));

        // Additional constants
        MathContext mc = new MathContext(prec + 10);

        // Euler-Mascheroni constant (approximation)
        BigDecimal gamma = BigDecimal.valueOf(0.5772156649015328606065120900824024310421);
        constants.put("gamma", gamma.setScale(Math.min(prec, 40), RoundingMode.HALF_UP).toString());

        // √2
        BigDecimal sqrt2 = sqrt(BigDecimal.valueOf(2), mc);
        constants.put("sqrt2", sqrt2.setScale(prec, RoundingMode.HALF_UP).toString());

        // √3
        BigDecimal sqrt3 = sqrt(BigDecimal.valueOf(3), mc);
        constants.put("sqrt3", sqrt3.setScale(prec, RoundingMode.HALF_UP).toString());

        return constants;
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

        int maxIterations = 50;
        for (int i = 0; i < maxIterations; i++) {
            x0 = x1;
            x1 = value.divide(x0, mc);
            x1 = x1.add(x0, mc);
            x1 = x1.divide(BigDecimal.valueOf(2), mc);

            // Check for convergence
            if (x0.subtract(x1, mc).abs().compareTo(
                    BigDecimal.ONE.movePointLeft(mc.getPrecision())) < 0) {
                break;
            }
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
     * Fibonacci sequence generator with streaming capability
     *
     * @param n Number of Fibonacci numbers to generate
     * @param stream Whether to stream results
     * @return Fibonacci sequence
     */
    @MCPTool(name = "fibonacci_sequence",
            description = "Generate Fibonacci sequence")
    public Map<String, Object> fibonacciSequence(
            @ToolParam(description = "Number of Fibonacci numbers to generate")
            int n,
            @ToolParam(description = "Whether to stream results", defaultValue = "false")
            boolean stream) {

        if (n <= 0) {
            return Map.of("error", "n must be positive");
        }

        if (n > 1000) {
            return Map.of("error", "n must be <= 1000 for performance reasons");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", n);
        result.put("streaming", stream);

        List<String> sequence = new ArrayList<>();

        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;

        if (n >= 1) sequence.add(a.toString());
        if (n >= 2) sequence.add(b.toString());

        for (int i = 3; i <= n; i++) {
            BigInteger temp = b;
            b = b.add(a);
            a = temp;
            sequence.add(b.toString());

            // 模拟流式输出
            if (stream && i % 10 == 0) {
                System.err.println(String.format("[MathTool] Generated Fibonacci number %d/%d", i, n));
            }
        }

        result.put("sequence", sequence);

        // 添加一些元数据
        if (n >= 2) {
            BigDecimal ratio = new BigDecimal(b)
                    .divide(new BigDecimal(a), 20, RoundingMode.HALF_UP);
            result.put("last_ratio", ratio.toString());
            result.put("approaches_golden_ratio",
                    Math.abs(ratio.doubleValue() - 1.618033988749894) < 0.0001);
        }

        return result;
    }

    /**
     * Prime number checker with batch processing
     *
     * @param numbers Comma-separated list of numbers to check
     * @param batchSize Batch size for processing (for streaming)
     * @return Prime check results
     */
    @MCPTool(name = "check_primes",
            description = "Check if numbers are prime")
    public Map<String, Object> checkPrimes(
            @ToolParam(description = "Comma-separated list of numbers to check")
            String numbers,
            @ToolParam(description = "Batch size for processing", defaultValue = "10")
            int batchSize) {

        String[] numberStrs = numbers.split(",");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_numbers", numberStrs.length);
        result.put("batch_size", batchSize);

        List<Map<String, Object>> checks = new ArrayList<>();
        int primeCount = 0;

        for (int i = 0; i < numberStrs.length; i++) {
            try {
                long num = Long.parseLong(numberStrs[i].trim());
                boolean isPrime = isPrime(num);

                Map<String, Object> check = new LinkedHashMap<>();
                check.put("number", num);
                check.put("is_prime", isPrime);

                if (!isPrime && num > 1) {
                    // Find a divisor
                    long divisor = findDivisor(num);
                    check.put("divisor", divisor);
                }

                checks.add(check);

                if (isPrime) primeCount++;

                // 模拟批处理进度
                if (batchSize > 0 && (i + 1) % batchSize == 0) {
                    System.err.println(String.format("[MathTool] Processed %d/%d numbers",
                            i + 1, numberStrs.length));
                }

            } catch (NumberFormatException e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("input", numberStrs[i]);
                error.put("error", "Invalid number format");
                checks.add(error);
            }
        }

        result.put("prime_count", primeCount);
        result.put("composite_count", checks.size() - primeCount);
        result.put("checks", checks);

        return result;
    }

    private boolean isPrime(long n) {
        if (n <= 1) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        long limit = (long) Math.sqrt(n);
        for (long i = 5; i <= limit; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    private long findDivisor(long n) {
        if (n % 2 == 0) return 2;
        if (n % 3 == 0) return 3;

        long limit = (long) Math.sqrt(n);
        for (long i = 5; i <= limit; i += 6) {
            if (n % i == 0) return i;
            if (n % (i + 2) == 0) return i + 2;
        }
        return n; // n is prime (shouldn't reach here)
    }

    /**
     * Main method to start the MCP server
     *
     * @param args Command line arguments
     */
    @EnableMcpServer(mode = "web")  // 使用web模式，支持SSE
    public static void main(String[] args) {
        McpServer.start();
    }
}