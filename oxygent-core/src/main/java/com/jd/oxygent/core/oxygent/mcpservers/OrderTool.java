package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Order management service providing order query, user order lookup, and order cancellation.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class OrderTool {

    private static final Map<String, Map<String, Object>> ORDERS;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // Order 001
        Map<String, Object> order1 = new LinkedHashMap<>();
        order1.put("order_id", "ORDER001");
        order1.put("user_id", "USER001");

        List<Map<String, Object>> products1 = new ArrayList<>();
        Map<String, Object> product1 = new LinkedHashMap<>();
        product1.put("product_id", "PROD001");
        product1.put("name", "Product A");
        product1.put("quantity", 1);
        product1.put("price", 5999);
        products1.add(product1);

        order1.put("products", products1);
        order1.put("total", 5999);
        order1.put("status", "Shipped");
        order1.put("create_time", "2024-01-15 10:30:00");
        order1.put("shipping_address", "No. A, Street A, District A, City A");

        // Order 002
        Map<String, Object> order2 = new LinkedHashMap<>();
        order2.put("order_id", "ORDER002");
        order2.put("user_id", "USER002");

        List<Map<String, Object>> products2 = new ArrayList<>();
        Map<String, Object> product2 = new LinkedHashMap<>();
        product2.put("product_id", "PROD002");
        product2.put("name", "Product B");
        product2.put("quantity", 1);
        product2.put("price", 12999);
        products2.add(product2);

        order2.put("products", products2);
        order2.put("total", 12999);
        order2.put("status", "Delivered");
        order2.put("create_time", "2024-01-16 14:20:00");
        order2.put("shipping_address", "No. X, Commercial Street, District D, City C");

        // Order 003
        Map<String, Object> order3 = new LinkedHashMap<>();
        order3.put("order_id", "ORDER003");
        order3.put("user_id", "USER003");

        List<Map<String, Object>> products3 = new ArrayList<>();
        Map<String, Object> product3a = new LinkedHashMap<>();
        product3a.put("product_id", "PROD003");
        product3a.put("name", "Product C");
        product3a.put("quantity", 2);
        product3a.put("price", 899);
        products3.add(product3a);

        Map<String, Object> product3b = new LinkedHashMap<>();
        product3b.put("product_id", "PROD001");
        product3b.put("name", "Product A");
        product3b.put("quantity", 1);
        product3b.put("price", 5999);
        products3.add(product3b);

        order3.put("products", products3);
        order3.put("total", 7797); // 2 * 899 + 5999 = 7797
        order3.put("status", "Pending Payment");
        order3.put("create_time", "2024-01-17 09:00:00");
        order3.put("shipping_address", "No. B, Street B, District B, City B");

        ORDERS = Map.of(
                "ORDER001", order1,
                "ORDER002", order2,
                "ORDER003", order3
        );
    }

    /**
     * Query order details by order ID
     *
     * @param orderId Order ID to query
     * @return Order details if found, otherwise an error message
     */
    @MCPTool(name = "query_order",
            description = "Query order details by order ID")
    public Map<String, Object> queryOrder(
            @ToolParam(description = "Order ID")
            String orderId) {

        Map<String, Object> order = ORDERS.get(orderId);
        if (order == null) {
            return Map.of("error", String.format("Order ID %s does not exist.", orderId));
        }

        // Return a deep copy to avoid modification of original data
        return deepCopyOrder(order);
    }

    /**
     * Query all orders for a specific user
     *
     * @param userId User ID
     * @return A list of orders associated with the user
     */
    @MCPTool(name = "query_user_orders",
            description = "Query all orders for a specific user")
    public List<Map<String, Object>> queryUserOrders(
            @ToolParam(description = "User ID")
            String userId) {

        List<Map<String, Object>> userOrders = new ArrayList<>();
        for (Map<String, Object> order : ORDERS.values()) {
            if (userId.equals(order.get("user_id"))) {
                userOrders.add(deepCopyOrder(order));
            }
        }

        if (userOrders.isEmpty()) {
            return List.of(Map.of("message", String.format("user %s has no orders", userId)));
        }

        return userOrders;
    }

    /**
     * Cancel a specific order with reason and record the cancellation time
     *
     * @param orderId Order ID to cancel
     * @param reason Cancellation reason
     * @return Cancellation status and details
     */
    @MCPTool(name = "cancel_order",
            description = "Cancel a specific order with reason and record the cancellation time")
    public Map<String, Object> cancelOrder(
            @ToolParam(description = "Order ID")
            String orderId,
            @ToolParam(description = "Cancellation reason")
            String reason) {

        if (!ORDERS.containsKey(orderId)) {
            return Map.of(
                    "success", false,
                    "message", String.format("Order ID %s does not exist.", orderId)
            );
        }

        Map<String, Object> order = ORDERS.get(orderId);
        String status = (String) order.get("status");

        // Check if order can be cancelled
        if (status.equals("Processing") || status.equals("Pending Payment")) {
            // Update order status
            order.put("status", "Cancelled");
            order.put("cancel_reason", reason);

            String cancelTime = LocalDateTime.now().format(TIME_FORMATTER);
            order.put("cancel_time", cancelTime);

            // Extract product IDs
            List<String> productIds = new ArrayList<>();
            List<Map<String, Object>> products = (List<Map<String, Object>>) order.get("products");
            for (Map<String, Object> product : products) {
                productIds.add((String) product.get("product_id"));
            }

            return Map.of(
                    "success", true,
                    "message", String.format("Order %s has been successfully cancelled. Reason: %s", orderId, reason),
                    "cancel_time", cancelTime,
                    "products", deepCopyProducts(products),
                    "product_id", productIds
            );
        } else {
            return Map.of(
                    "success", false,
                    "message", String.format("Order %s cannot be cancelled due to its current status: %s.",
                            orderId, status)
            );
        }
    }

    /**
     * Get all orders
     *
     * @return List of all orders
     */
    @MCPTool(name = "get_all_orders",
            description = "Get all orders")
    public List<Map<String, Object>> getAllOrders() {
        List<Map<String, Object>> allOrders = new ArrayList<>();
        for (Map<String, Object> order : ORDERS.values()) {
            allOrders.add(deepCopyOrder(order));
        }
        return allOrders;
    }

    /**
     * Get orders by status
     *
     * @param status Order status to filter by
     * @return List of orders with the specified status
     */
    @MCPTool(name = "get_orders_by_status",
            description = "Get orders by status")
    public List<Map<String, Object>> getOrdersByStatus(
            @ToolParam(description = "Order status (e.g., Shipped, Delivered, Pending Payment)")
            String status) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> order : ORDERS.values()) {
            if (status.equals(order.get("status"))) {
                result.add(deepCopyOrder(order));
            }
        }

        if (result.isEmpty()) {
            return List.of(Map.of("message", "No orders found with status: " + status));
        }

        return result;
    }

    /**
     * Get orders within a date range
     *
     * @param startDate Start date in format "YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS"
     * @param endDate End date in format "YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS"
     * @return List of orders within the date range
     */
    @MCPTool(name = "get_orders_by_date_range",
            description = "Get orders within a date range")
    public List<Map<String, Object>> getOrdersByDateRange(
            @ToolParam(description = "Start date (YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)")
            String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)")
            String endDate) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> order : ORDERS.values()) {
            String createTime = (String) order.get("create_time");
            if (createTime.compareTo(startDate) >= 0 && createTime.compareTo(endDate) <= 0) {
                result.add(deepCopyOrder(order));
            }
        }

        if (result.isEmpty()) {
            return List.of(Map.of("message",
                    String.format("No orders found between %s and %s", startDate, endDate)));
        }

        return result;
    }

    /**
     * Get orders above a certain total amount
     *
     * @param minAmount Minimum total amount threshold
     * @return List of orders with total amount greater than or equal to minAmount
     */
    @MCPTool(name = "get_orders_by_total_amount",
            description = "Get orders above a certain total amount")
    public List<Map<String, Object>> getOrdersByTotalAmount(
            @ToolParam(description = "Minimum total amount")
            int minAmount) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> order : ORDERS.values()) {
            int total = (int) order.get("total");
            if (total >= minAmount) {
                result.add(deepCopyOrder(order));
            }
        }

        if (result.isEmpty()) {
            return List.of(Map.of("message",
                    String.format("No orders found with total amount >= %d", minAmount)));
        }

        return result;
    }

    /**
     * Get order statistics
     *
     * @return Order statistics including total count, total revenue, average order value, etc.
     */
    @MCPTool(name = "get_order_statistics",
            description = "Get order statistics")
    public Map<String, Object> getOrderStatistics() {
        int totalOrders = ORDERS.size();
        int totalRevenue = 0;
        int maxOrderValue = Integer.MIN_VALUE;
        int minOrderValue = Integer.MAX_VALUE;

        Map<String, Integer> statusCount = new HashMap<>();
        Map<String, Integer> userOrderCount = new HashMap<>();

        for (Map<String, Object> order : ORDERS.values()) {
            int total = (int) order.get("total");
            totalRevenue += total;
            maxOrderValue = Math.max(maxOrderValue, total);
            minOrderValue = Math.min(minOrderValue, total);

            String status = (String) order.get("status");
            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);

            String userId = (String) order.get("user_id");
            userOrderCount.put(userId, userOrderCount.getOrDefault(userId, 0) + 1);
        }

        double averageOrderValue = totalOrders > 0 ? (double) totalRevenue / totalOrders : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_orders", totalOrders);
        stats.put("total_revenue", totalRevenue);
        stats.put("average_order_value", averageOrderValue);
        stats.put("max_order_value", maxOrderValue);
        stats.put("min_order_value", minOrderValue);
        stats.put("currency", "CNY");
        stats.put("order_status_distribution", statusCount);
        stats.put("user_order_distribution", userOrderCount);

        return stats;
    }

    /**
     * Create a new order
     *
     * @param userId User ID
     * @param products List of products (JSON string or structured data)
     * @param shippingAddress Shipping address
     * @return Newly created order details
     */
    @MCPTool(name = "create_order",
            description = "Create a new order")
    public Map<String, Object> createOrder(
            @ToolParam(description = "User ID")
            String userId,
            @ToolParam(description = "List of products with product_id, quantity, and price")
            List<Map<String, Object>> products,
            @ToolParam(description = "Shipping address")
            String shippingAddress) {

        // Generate new order ID
        String newOrderId = String.format("ORDER%03d", ORDERS.size() + 1);

        // Calculate total
        int total = 0;
        for (Map<String, Object> product : products) {
            int quantity = (int) product.get("quantity");
            int price = (int) product.get("price");
            total += quantity * price;
        }

        // Create new order
        Map<String, Object> newOrder = new LinkedHashMap<>();
        newOrder.put("order_id", newOrderId);
        newOrder.put("user_id", userId);
        newOrder.put("products", deepCopyProducts(products));
        newOrder.put("total", total);
        newOrder.put("status", "Pending Payment");
        newOrder.put("create_time", LocalDateTime.now().format(TIME_FORMATTER));
        newOrder.put("shipping_address", shippingAddress);

        // Note: In a real implementation, you would save this to a database
        // For demo purposes, we'll just return the order without saving

        return Map.of(
                "success", true,
                "message", "Order created successfully (demo mode - not saved)",
                "order", newOrder
        );
    }

    /**
     * Deep copy an order to avoid modifying the original data
     */
    private Map<String, Object> deepCopyOrder(Map<String, Object> order) {
        Map<String, Object> copy = new LinkedHashMap<>(order);

        // Deep copy products list
        if (order.containsKey("products")) {
            List<Map<String, Object>> products = (List<Map<String, Object>>) order.get("products");
            copy.put("products", deepCopyProducts(products));
        }

        return copy;
    }

    /**
     * Deep copy a list of products
     */
    private List<Map<String, Object>> deepCopyProducts(List<Map<String, Object>> products) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> product : products) {
            copy.add(new LinkedHashMap<>(product));
        }
        return copy;
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