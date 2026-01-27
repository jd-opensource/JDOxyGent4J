package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.*;

/**
 * Payment service providing payment status query and payment methods information.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class PaymentTool {

    private static final Map<String, String> ORDER_TO_PAYMENT = Map.of(
            "ORDER001", "PAY001",
            "ORDER002", "PAY002"
    );

    private static final Map<String, Map<String, Object>> PAYMENTS;

    static {
        // Payment 001
        Map<String, Object> payment1 = new LinkedHashMap<>();
        payment1.put("payment_id", "PAY001");
        payment1.put("order_id", "ORDER001");
        payment1.put("amount", 5999.00);
        payment1.put("payment_method", "Pay method A");
        payment1.put("status", "Paid");
        payment1.put("transaction_id", "2024011510300001");
        payment1.put("create_time", "2024-01-15 10:30:00");
        payment1.put("paid_time", "2024-01-15 10:30:15");

        // Payment 002
        Map<String, Object> payment2 = new LinkedHashMap<>();
        payment2.put("payment_id", "PAY002");
        payment2.put("order_id", "ORDER002");
        payment2.put("amount", 12999.00);
        payment2.put("payment_method", "Pay method B");
        payment2.put("status", "Paid");
        payment2.put("transaction_id", "2024011510310002");
        payment2.put("create_time", "2024-01-16 14:20:00");
        payment2.put("paid_time", "2024-01-16 14:20:20");

        PAYMENTS = Map.of(
                "PAY001", payment1,
                "PAY002", payment2
        );
    }

    /**
     * Query payment status by payment ID or order ID
     *
     * @param id Payment ID or Order ID
     * @return Payment information or error message
     */
    @MCPTool(name = "query_payment_status",
            description = "Query payment status by payment ID or order ID")
    public Map<String, Object> queryPaymentStatus(
            @ToolParam(description = "Payment ID or Order ID")
            String id) {

        // If not directly a payment ID, check if it's an order ID
        if (!PAYMENTS.containsKey(id)) {
            if (!ORDER_TO_PAYMENT.containsKey(id)) {
                return Map.of("error", "Payment record or order does not exist");
            }
            id = ORDER_TO_PAYMENT.get(id); // Convert order ID to payment ID
        }

        Map<String, Object> payment = PAYMENTS.get(id);
        if (payment == null) {
            return Map.of("error", "Payment record not found");
        }

        // Return a copy to avoid modification of original data
        return new LinkedHashMap<>(payment);
    }

    /**
     * Get supported payment methods
     *
     * @return List of supported payment methods with details
     */
    @MCPTool(name = "get_payment_methods",
            description = "Get supported pay methods")
    public List<Map<String, Object>> getPaymentMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();

        // Method A
        Map<String, Object> methodA = new LinkedHashMap<>();
        methodA.put("method", "payment method A");
        methodA.put("fee_rate", 0.006);
        methodA.put("max_amount", 50000);
        methodA.put("description", "Balances/Bank Cards");
        methods.add(methodA);

        // Method B
        Map<String, Object> methodB = new LinkedHashMap<>();
        methodB.put("method", "payment method B");
        methodB.put("fee_rate", 0.006);
        methodB.put("max_amount", 50000);
        methodB.put("description", "Cash/Bank Cards");
        methods.add(methodB);

        // Method C
        Map<String, Object> methodC = new LinkedHashMap<>();
        methodC.put("method", "payment method C");
        methodC.put("fee_rate", 0.008);
        methodC.put("max_amount", 100000);
        methodC.put("description", "Bank Cards/Credit Cards");
        methods.add(methodC);

        return methods;
    }

    /**
     * Get all payment records
     *
     * @return List of all payment records
     */
    @MCPTool(name = "get_all_payments",
            description = "Get all payment records")
    public List<Map<String, Object>> getAllPayments() {
        List<Map<String, Object>> allPayments = new ArrayList<>();
        for (Map<String, Object> payment : PAYMENTS.values()) {
            allPayments.add(new LinkedHashMap<>(payment));
        }
        return allPayments;
    }

    /**
     * Get payments by status
     *
     * @param status Payment status to filter by (e.g., "Paid", "Pending", "Failed")
     * @return List of payments with the specified status
     */
    @MCPTool(name = "get_payments_by_status",
            description = "Get payments by status")
    public List<Map<String, Object>> getPaymentsByStatus(
            @ToolParam(description = "Payment status (e.g., Paid, Pending, Failed)")
            String status) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> payment : PAYMENTS.values()) {
            if (status.equals(payment.get("status"))) {
                result.add(new LinkedHashMap<>(payment));
            }
        }

        if (result.isEmpty()) {
            return List.of(Map.of("message", "No payments found with status: " + status));
        }

        return result;
    }

    /**
     * Get payments within a date range
     *
     * @param startDate Start date in format "YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS"
     * @param endDate End date in format "YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS"
     * @return List of payments within the date range
     */
    @MCPTool(name = "get_payments_by_date_range",
            description = "Get payments within a date range")
    public List<Map<String, Object>> getPaymentsByDateRange(
            @ToolParam(description = "Start date (YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)")
            String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)")
            String endDate) {

        List<Map<String, Object>> result = new ArrayList<>();

        // Note: In a real implementation, you would parse and compare dates
        // This is a simplified version that just checks if create_time is within range
        for (Map<String, Object> payment : PAYMENTS.values()) {
            String createTime = (String) payment.get("create_time");
            // Simple string comparison (for demo purposes)
            if (createTime.compareTo(startDate) >= 0 && createTime.compareTo(endDate) <= 0) {
                result.add(new LinkedHashMap<>(payment));
            }
        }

        if (result.isEmpty()) {
            return List.of(Map.of("message",
                    String.format("No payments found between %s and %s", startDate, endDate)));
        }

        return result;
    }

    /**
     * Get payments above a certain amount
     *
     * @param minAmount Minimum amount threshold
     * @return List of payments with amount greater than or equal to minAmount
     */
    @MCPTool(name = "get_payments_by_amount",
            description = "Get payments above a certain amount")
    public List<Map<String, Object>> getPaymentsByAmount(
            @ToolParam(description = "Minimum amount")
            double minAmount) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> payment : PAYMENTS.values()) {
            double amount = (double) payment.get("amount");
            if (amount >= minAmount) {
                result.add(new LinkedHashMap<>(payment));
            }
        }

        if (result.isEmpty()) {
            return List.of(Map.of("message",
                    String.format("No payments found with amount >= %.2f", minAmount)));
        }

        return result;
    }

    /**
     * Get payment statistics
     *
     * @return Payment statistics including total count, total amount, average amount, etc.
     */
    @MCPTool(name = "get_payment_statistics",
            description = "Get payment statistics")
    public Map<String, Object> getPaymentStatistics() {
        int totalCount = PAYMENTS.size();
        double totalAmount = 0.0;
        double maxAmount = Double.MIN_VALUE;
        double minAmount = Double.MAX_VALUE;

        for (Map<String, Object> payment : PAYMENTS.values()) {
            double amount = (double) payment.get("amount");
            totalAmount += amount;
            maxAmount = Math.max(maxAmount, amount);
            minAmount = Math.min(minAmount, amount);
        }

        double averageAmount = totalCount > 0 ? totalAmount / totalCount : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_payments", totalCount);
        stats.put("total_amount", totalAmount);
        stats.put("average_amount", averageAmount);
        stats.put("max_amount", maxAmount);
        stats.put("min_amount", minAmount);
        stats.put("currency", "CNY");

        // Count by payment method
        Map<String, Integer> methodCount = new HashMap<>();
        for (Map<String, Object> payment : PAYMENTS.values()) {
            String method = (String) payment.get("payment_method");
            methodCount.put(method, methodCount.getOrDefault(method, 0) + 1);
        }
        stats.put("payment_method_distribution", methodCount);

        return stats;
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