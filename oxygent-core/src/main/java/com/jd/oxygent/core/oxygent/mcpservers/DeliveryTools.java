package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.*;

/**
 * Delivery management service providing delivery information and available delivery methods.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class DeliveryTools {

    private static final Map<String, Map<String, Object>> DELIVERY_INFO;

    static {
        // Delivery for ORDER001
        Map<String, Object> delivery1 = new LinkedHashMap<>();
        delivery1.put("order_id", "ORDER001");
        delivery1.put("delivery_method", "Standard Delivery");
        delivery1.put("delivery_address", "Street A, District A, City A");
        delivery1.put("delivery_phone", "xxx****xxxx");
        delivery1.put("delivery_time_slot", "09:00-18:00");
        delivery1.put("special_instructions", "Please deliver to the front desk");
        delivery1.put("delivery_fee", 0);
        delivery1.put("estimated_delivery", "2024-01-17 18:00:00");
        delivery1.put("delivery_status", "In Transit");

        // Delivery for ORDER002
        Map<String, Object> delivery2 = new LinkedHashMap<>();
        delivery2.put("order_id", "ORDER002");
        delivery2.put("delivery_method", "Next Day Delivery");
        delivery2.put("delivery_address", "Street X, Commercial Area D, City C");
        delivery2.put("delivery_phone", "xxx****xxxx");
        delivery2.put("delivery_time_slot", "10:00-12:00");
        delivery2.put("special_instructions", "Recipient signature required");
        delivery2.put("delivery_fee", 15);
        delivery2.put("estimated_delivery", "2024-01-16 12:00:00");
        delivery2.put("delivery_status", "Delivered");

        DELIVERY_INFO = Map.of(
                "ORDER001", delivery1,
                "ORDER002", delivery2
        );
    }

    private static final List<String> MAJOR_CITIES = List.of(
            "Province A",
            "Province B",
            "Province C",
            "Province D",
            "Province E",
            "Province F",
            "Province G",
            "Province H"
    );

    /**
     * Retrieve delivery information based on order ID
     *
     * @param orderId The order ID to query
     * @return Delivery information if the order exists, otherwise an error message
     */
    @MCPTool(name = "get_delivery_info",
            description = "Retrieve delivery information based on order ID")
    public Map<String, Object> getDeliveryInfo(
            @ToolParam(description = "Order ID")
            String orderId) {

        if (DELIVERY_INFO.containsKey(orderId)) {
            return new LinkedHashMap<>(DELIVERY_INFO.get(orderId));
        } else {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", String.format("Delivery information for order ID %s does not exist.", orderId));
            return errorResponse;
        }
    }

    /**
     * Get available delivery methods based on city and package weight
     *
     * @param city Recipient city
     * @param weight Package weight (kg), default is 1.0
     * @return Available delivery methods with details for the specified city and weight
     */
    @MCPTool(name = "get_delivery_methods",
            description = "Get available delivery methods based on city and package weight")
    public Map<String, Object> getDeliveryMethods(
            @ToolParam(description = "Recipient city")
            String city,
            @ToolParam(description = "Package weight (kg)", defaultValue = "1.0")
            double weight) {

        List<Map<String, Object>> allMethods = new ArrayList<>();

        // Standard Delivery
        Map<String, Object> standardMethod = new LinkedHashMap<>();
        standardMethod.put("method", "Standard Delivery");
        standardMethod.put("description", "Delivered within 3-5 business days");
        standardMethod.put("fee", weight <= 5 ? 0 : (weight - 5) * 2);
        standardMethod.put("available", true);
        allMethods.add(standardMethod);

        // Express Delivery
        Map<String, Object> expressMethod = new LinkedHashMap<>();
        expressMethod.put("method", "Express Delivery");
        expressMethod.put("description", "Delivered within 1-2 business days");
        expressMethod.put("fee", 15 + (weight <= 3 ? 0 : (weight - 3) * 3));
        expressMethod.put("available", MAJOR_CITIES.contains(city));
        allMethods.add(expressMethod);

        // Next Day Delivery
        Map<String, Object> nextDayMethod = new LinkedHashMap<>();
        nextDayMethod.put("method", "Next Day Delivery");
        nextDayMethod.put("description", "Delivered the next business day (weekdays only)");
        nextDayMethod.put("fee", 25 + (weight <= 2 ? 0 : (weight - 2) * 5));
        nextDayMethod.put("available", MAJOR_CITIES.subList(0, 4).contains(city));
        allMethods.add(nextDayMethod);

        // Same Day Delivery
        Map<String, Object> sameDayMethod = new LinkedHashMap<>();
        sameDayMethod.put("method", "Same Day Delivery");
        sameDayMethod.put("description", "Delivered on the same day (limited areas only)");
        sameDayMethod.put("fee", 35.0);
        sameDayMethod.put("available", List.of("Province A", "Province B").contains(city) && weight <= 3);
        allMethods.add(sameDayMethod);

        // Filter only available methods
        List<Map<String, Object>> availableMethods = new ArrayList<>();
        for (Map<String, Object> method : allMethods) {
            if ((boolean) method.get("available")) {
                Map<String, Object> availableMethod = new LinkedHashMap<>(method);
                // Format fee to 2 decimal places
                double fee = (double) availableMethod.get("fee");
                availableMethod.put("fee_formatted", String.format("%.2f", fee));
                availableMethods.add(availableMethod);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("city", city);
        response.put("weight", weight);
        response.put("available_methods", availableMethods);
        response.put("total_available_methods", availableMethods.size());

        if (availableMethods.isEmpty()) {
            response.put("message", String.format("No delivery methods available for city: %s with weight: %.1f kg", city, weight));
        }

        return response;
    }

    /**
     * Get all delivery records
     *
     * @return List of all delivery records
     */
    @MCPTool(name = "get_all_deliveries",
            description = "Get all delivery records")
    public List<Map<String, Object>> getAllDeliveries() {
        List<Map<String, Object>> allDeliveries = new ArrayList<>();
        for (Map<String, Object> delivery : DELIVERY_INFO.values()) {
            allDeliveries.add(new LinkedHashMap<>(delivery));
        }
        return allDeliveries;
    }

    /**
     * Get deliveries by status
     *
     * @param status Delivery status to filter by (e.g., "In Transit", "Delivered", "Pending")
     * @return List of deliveries with the specified status
     */
    @MCPTool(name = "get_deliveries_by_status",
            description = "Get deliveries by status")
    public List<Map<String, Object>> getDeliveriesByStatus(
            @ToolParam(description = "Delivery status (e.g., In Transit, Delivered, Pending)")
            String status) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> delivery : DELIVERY_INFO.values()) {
            if (status.equals(delivery.get("delivery_status"))) {
                result.add(new LinkedHashMap<>(delivery));
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No deliveries found with status: " + status);
            return List.of(message);
        }

        return result;
    }

    /**
     * Get deliveries by delivery method
     *
     * @param deliveryMethod Delivery method to filter by (e.g., "Standard Delivery", "Next Day Delivery")
     * @return List of deliveries with the specified delivery method
     */
    @MCPTool(name = "get_deliveries_by_method",
            description = "Get deliveries by delivery method")
    public List<Map<String, Object>> getDeliveriesByMethod(
            @ToolParam(description = "Delivery method (e.g., Standard Delivery, Next Day Delivery)")
            String deliveryMethod) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> delivery : DELIVERY_INFO.values()) {
            if (deliveryMethod.equals(delivery.get("delivery_method"))) {
                result.add(new LinkedHashMap<>(delivery));
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No deliveries found with delivery method: " + deliveryMethod);
            return List.of(message);
        }

        return result;
    }

    /**
     * Get delivery statistics
     *
     * @return Delivery statistics including total deliveries, status distribution, etc.
     */
    @MCPTool(name = "get_delivery_statistics",
            description = "Get delivery statistics")
    public Map<String, Object> getDeliveryStatistics() {
        int totalDeliveries = DELIVERY_INFO.size();
        double totalDeliveryFees = 0.0;
        double maxDeliveryFee = Double.MIN_VALUE;
        double minDeliveryFee = Double.MAX_VALUE;

        Map<String, Integer> statusCount = new HashMap<>();
        Map<String, Integer> methodCount = new HashMap<>();
        Map<String, Double> methodTotalFees = new HashMap<>();

        for (Map<String, Object> delivery : DELIVERY_INFO.values()) {
            double fee = ((Number) delivery.get("delivery_fee")).doubleValue();
            totalDeliveryFees += fee;
            maxDeliveryFee = Math.max(maxDeliveryFee, fee);
            minDeliveryFee = Math.min(minDeliveryFee, fee);

            String status = (String) delivery.get("delivery_status");
            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);

            String method = (String) delivery.get("delivery_method");
            methodCount.put(method, methodCount.getOrDefault(method, 0) + 1);
            methodTotalFees.put(method, methodTotalFees.getOrDefault(method, 0.0) + fee);
        }

        double averageDeliveryFee = totalDeliveries > 0 ? totalDeliveryFees / totalDeliveries : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_deliveries", totalDeliveries);
        stats.put("total_delivery_fees", totalDeliveryFees);
        stats.put("average_delivery_fee", averageDeliveryFee);
        stats.put("max_delivery_fee", maxDeliveryFee);
        stats.put("min_delivery_fee", minDeliveryFee);
        stats.put("status_distribution", statusCount);
        stats.put("method_distribution", methodCount);
        stats.put("method_total_fees", methodTotalFees);

        // Calculate average fee per method
        Map<String, Double> methodAverageFees = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : methodCount.entrySet()) {
            String method = entry.getKey();
            int count = entry.getValue();
            double totalFee = methodTotalFees.get(method);
            methodAverageFees.put(method, totalFee / count);
        }
        stats.put("method_average_fees", methodAverageFees);

        return stats;
    }

    /**
     * Get deliveries with special instructions
     *
     * @return List of deliveries that have special instructions
     */
    @MCPTool(name = "get_deliveries_with_special_instructions",
            description = "Get deliveries with special instructions")
    public List<Map<String, Object>> getDeliveriesWithSpecialInstructions() {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> delivery : DELIVERY_INFO.values()) {
            String instructions = (String) delivery.get("special_instructions");
            if (instructions != null && !instructions.trim().isEmpty() && !instructions.equals("None")) {
                result.add(new LinkedHashMap<>(delivery));
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No deliveries found with special instructions");
            return List.of(message);
        }

        return result;
    }

    /**
     * Estimate delivery time based on method and distance
     *
     * @param deliveryMethod Delivery method (e.g., "Standard Delivery", "Express Delivery")
     * @param distance Distance in kilometers
     * @return Estimated delivery time and details
     */
    @MCPTool(name = "estimate_delivery_time",
            description = "Estimate delivery time based on method and distance")
    public Map<String, Object> estimateDeliveryTime(
            @ToolParam(description = "Delivery method (e.g., Standard Delivery, Express Delivery)")
            String deliveryMethod,
            @ToolParam(description = "Distance in kilometers")
            double distance) {

        Map<String, Object> estimation = new LinkedHashMap<>();
        estimation.put("delivery_method", deliveryMethod);
        estimation.put("distance_km", distance);

        switch (deliveryMethod) {
            case "Standard Delivery":
                double standardDays = Math.max(3, distance / 100); // Minimum 3 days
                estimation.put("estimated_days", Math.ceil(standardDays));
                estimation.put("description", String.format("Estimated %d business days for %.1f km",
                        (int) Math.ceil(standardDays), distance));
                break;

            case "Express Delivery":
                double expressDays = Math.max(1, distance / 200); // Minimum 1 day
                estimation.put("estimated_days", Math.ceil(expressDays));
                estimation.put("description", String.format("Estimated %d business days for %.1f km",
                        (int) Math.ceil(expressDays), distance));
                break;

            case "Next Day Delivery":
                if (distance <= 300) {
                    estimation.put("estimated_days", 1);
                    estimation.put("description", "Next business day delivery available");
                } else {
                    estimation.put("estimated_days", 2);
                    estimation.put("description", "2 business days for distances over 300 km");
                }
                break;

            case "Same Day Delivery":
                if (distance <= 50) {
                    estimation.put("estimated_days", 0);
                    estimation.put("description", "Same day delivery available");
                } else {
                    estimation.put("estimated_days", 1);
                    estimation.put("description", "Next day delivery for distances over 50 km");
                }
                break;

            default:
                estimation.put("error", "Invalid delivery method");
                estimation.put("description", "Please use a valid delivery method");
        }

        return estimation;
    }

    /**
     * Search deliveries by address keyword
     *
     * @param keyword Address keyword to search for
     * @return List of deliveries matching the address keyword
     */
    @MCPTool(name = "search_deliveries_by_address",
            description = "Search deliveries by address keyword")
    public List<Map<String, Object>> searchDeliveriesByAddress(
            @ToolParam(description = "Address keyword to search for")
            String keyword) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> delivery : DELIVERY_INFO.values()) {
            String address = (String) delivery.get("delivery_address");
            if (address.toLowerCase().contains(keyword.toLowerCase())) {
                result.add(new LinkedHashMap<>(delivery));
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No deliveries found with address containing: " + keyword);
            return List.of(message);
        }

        return result;
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