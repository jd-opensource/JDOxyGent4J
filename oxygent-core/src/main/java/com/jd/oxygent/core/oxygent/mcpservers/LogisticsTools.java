package com.jd.oxygent.core.oxygent.mcpservers;


import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.*;

/**
 * Logistics tracking service providing package tracking and order lookup.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class LogisticsTools {

    private static final Map<String, String> COURIER_COMPANIES = Map.of(
            "JD", "JD Logistics"
    );

    private static final Map<String, Map<String, Object>> TRACKING_DATA;

    static {
        // Package 001 - JD1234567890
        Map<String, Object> package1 = new LinkedHashMap<>();
        package1.put("tracking_number", "JD1234567890");
        package1.put("courier_company", "JD");
        package1.put("order_id", "ORDER001");

        Map<String, Object> sender1 = new LinkedHashMap<>();
        sender1.put("name", "Seller A");
        sender1.put("address", "Tech Park, District A, Province A");
        sender1.put("phone", "xxx-xxx-xxxx");
        package1.put("sender", sender1);

        Map<String, Object> receiver1 = new LinkedHashMap<>();
        receiver1.put("name", "Recipient A");
        receiver1.put("address", "No. A, Street A, District A, City A");
        receiver1.put("phone", "xxx****xxxx");
        package1.put("receiver", receiver1);

        package1.put("status", "In Transit");
        package1.put("estimated_delivery", "2024-01-17 18:00:00");

        List<Map<String, Object>> history1 = new ArrayList<>();

        Map<String, Object> history1_1 = new LinkedHashMap<>();
        history1_1.put("time", "2024-01-15 14:30:00");
        history1_1.put("location", "District A, City A, Province A");
        history1_1.put("status", "Shipped");
        history1_1.put("description", "Your package has been shipped from [City A District A Branch].");
        history1.add(history1_1);

        Map<String, Object> history1_2 = new LinkedHashMap<>();
        history1_2.put("time", "2024-01-15 18:45:00");
        history1_2.put("location", "City A, Province A");
        history1_2.put("status", "In Transit");
        history1_2.put("description", "Package arrived at [City A Transfer Center].");
        history1.add(history1_2);

        Map<String, Object> history1_3 = new LinkedHashMap<>();
        history1_3.put("time", "2024-01-16 08:30:00");
        history1_3.put("location", "City B");
        history1_3.put("status", "In Transit");
        history1_3.put("description", "Package arrived at [City B Transfer Center].");
        history1.add(history1_3);

        Map<String, Object> history1_4 = new LinkedHashMap<>();
        history1_4.put("time", "2024-01-16 15:20:00");
        history1_4.put("location", "District B, City B");
        history1_4.put("status", "Out for Delivery");
        history1_4.put("description", "Package arrived at [City B District B Branch], delivery is being arranged.");
        history1.add(history1_4);

        package1.put("tracking_history", history1);

        // Package 002 - JD1234567891
        Map<String, Object> package2 = new LinkedHashMap<>();
        package2.put("tracking_number", "JD1234567891");
        package2.put("courier_company", "JD");
        package2.put("order_id", "ORDER002");

        Map<String, Object> sender2 = new LinkedHashMap<>();
        sender2.put("name", "Seller B");
        sender2.put("address", "Tech Park, District C, City C");
        sender2.put("phone", "xxx-xxx-xxxx");
        package2.put("sender", sender2);

        Map<String, Object> receiver2 = new LinkedHashMap<>();
        receiver2.put("name", "Recipient B");
        receiver2.put("address", "No. X, Commercial Street, District D, City C");
        receiver2.put("phone", "xxx****xxxx");
        package2.put("receiver", receiver2);

        package2.put("status", "Delivered");
        package2.put("estimated_delivery", "2024-01-16 12:00:00");
        package2.put("actual_delivery", "2024-01-16 11:30:00");

        List<Map<String, Object>> history2 = new ArrayList<>();

        Map<String, Object> history2_1 = new LinkedHashMap<>();
        history2_1.put("time", "2024-01-15 20:00:00");
        history2_1.put("location", "District C, City C");
        history2_1.put("status", "Shipped");
        history2_1.put("description", "Your item has left the warehouse.");
        history2.add(history2_1);

        Map<String, Object> history2_2 = new LinkedHashMap<>();
        history2_2.put("time", "2024-01-16 06:00:00");
        history2_2.put("location", "City C");
        history2_2.put("status", "In Transit");
        history2_2.put("description", "Item has arrived at the delivery station.");
        history2.add(history2_2);

        Map<String, Object> history2_3 = new LinkedHashMap<>();
        history2_3.put("time", "2024-01-16 09:30:00");
        history2_3.put("location", "District D, City C");
        history2_3.put("status", "Out for Delivery");
        history2_3.put("description", "Courier [Courier A xxx****xxxx] is delivering your package.");
        history2.add(history2_3);

        Map<String, Object> history2_4 = new LinkedHashMap<>();
        history2_4.put("time", "2024-01-16 11:30:00");
        history2_4.put("location", "No. X, Commercial Street, District D, City C");
        history2_4.put("status", "Delivered");
        history2_4.put("description", "Your package has been delivered. Signed by: Recipient.");
        history2.add(history2_4);

        package2.put("tracking_history", history2);

        TRACKING_DATA = Map.of(
                "JD1234567890", package1,
                "JD1234567891", package2
        );
    }

    /**
     * Retrieve logistics tracking information by tracking number
     *
     * @param trackingNumber The tracking number of the package
     * @return Tracking information if found, otherwise an error message with suggestion
     */
    @MCPTool(name = "track_package",
            description = "Retrieve logistics tracking information by tracking number")
    public Map<String, Object> trackPackage(
            @ToolParam(description = "Tracking number")
            String trackingNumber) {

        if (TRACKING_DATA.containsKey(trackingNumber)) {
            Map<String, Object> trackingInfo = new LinkedHashMap<>(TRACKING_DATA.get(trackingNumber));

            // Add courier company name
            String companyCode = (String) trackingInfo.get("courier_company");
            String companyName = COURIER_COMPANIES.getOrDefault(companyCode, "Unknown courier company");
            trackingInfo.put("courier_name", companyName);

            return trackingInfo;
        } else {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", String.format("Tracking number %s does not exist or is not yet in the system.", trackingNumber));
            errorResponse.put("suggestion", "Please check the tracking number for accuracy or try again later.");
            return errorResponse;
        }
    }

    /**
     * Retrieve logistics tracking information by order ID
     *
     * @param orderId The order ID to query
     * @return List of tracking information dictionaries if found, otherwise an error message
     */
    @MCPTool(name = "track_by_order",
            description = "Retrieve logistics tracking information by order ID")
    public List<Map<String, Object>> trackByOrder(
            @ToolParam(description = "Order ID")
            String orderId) {

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> trackingInfo : TRACKING_DATA.values()) {
            if (orderId.equals(trackingInfo.get("order_id"))) {
                Map<String, Object> info = new LinkedHashMap<>(trackingInfo);

                // Add courier company name
                String companyCode = (String) info.get("courier_company");
                String companyName = COURIER_COMPANIES.getOrDefault(companyCode, "Unknown courier company");
                info.put("courier_name", companyName);

                results.add(info);
            }
        }

        if (results.isEmpty()) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", String.format("No tracking information found for order ID %s.", orderId));
            return List.of(errorResponse);
        }

        return results;
    }

    /**
     * Get all tracking records
     *
     * @return List of all tracking records
     */
    @MCPTool(name = "get_all_tracking_records",
            description = "Get all tracking records")
    public List<Map<String, Object>> getAllTrackingRecords() {
        List<Map<String, Object>> allRecords = new ArrayList<>();

        for (Map<String, Object> trackingInfo : TRACKING_DATA.values()) {
            Map<String, Object> info = new LinkedHashMap<>(trackingInfo);

            // Add courier company name
            String companyCode = (String) info.get("courier_company");
            String companyName = COURIER_COMPANIES.getOrDefault(companyCode, "Unknown courier company");
            info.put("courier_name", companyName);

            allRecords.add(info);
        }

        return allRecords;
    }

    /**
     * Get tracking records by status
     *
     * @param status Package status to filter by (e.g., "In Transit", "Delivered", "Out for Delivery")
     * @return List of tracking records with the specified status
     */
    @MCPTool(name = "get_tracking_by_status",
            description = "Get tracking records by status")
    public List<Map<String, Object>> getTrackingByStatus(
            @ToolParam(description = "Package status (e.g., In Transit, Delivered, Out for Delivery)")
            String status) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> trackingInfo : TRACKING_DATA.values()) {
            if (status.equals(trackingInfo.get("status"))) {
                Map<String, Object> info = new LinkedHashMap<>(trackingInfo);

                // Add courier company name
                String companyCode = (String) info.get("courier_company");
                String companyName = COURIER_COMPANIES.getOrDefault(companyCode, "Unknown courier company");
                info.put("courier_name", companyName);

                result.add(info);
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No tracking records found with status: " + status);
            return List.of(message);
        }

        return result;
    }

    /**
     * Get tracking records by courier company
     *
     * @param courierCompany Courier company code (e.g., "JD")
     * @return List of tracking records for the specified courier company
     */
    @MCPTool(name = "get_tracking_by_courier",
            description = "Get tracking records by courier company")
    public List<Map<String, Object>> getTrackingByCourier(
            @ToolParam(description = "Courier company code (e.g., JD)")
            String courierCompany) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> trackingInfo : TRACKING_DATA.values()) {
            if (courierCompany.equals(trackingInfo.get("courier_company"))) {
                Map<String, Object> info = new LinkedHashMap<>(trackingInfo);

                // Add courier company name
                String companyName = COURIER_COMPANIES.getOrDefault(courierCompany, "Unknown courier company");
                info.put("courier_name", companyName);

                result.add(info);
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No tracking records found for courier company: " + courierCompany);
            return List.of(message);
        }

        return result;
    }

    /**
     * Get package statistics
     *
     * @return Package statistics including total count, status distribution, etc.
     */
    @MCPTool(name = "get_package_statistics",
            description = "Get package statistics")
    public Map<String, Object> getPackageStatistics() {
        int totalCount = TRACKING_DATA.size();

        // Count by status
        Map<String, Integer> statusCount = new HashMap<>();
        Map<String, Integer> courierCount = new HashMap<>();

        for (Map<String, Object> trackingInfo : TRACKING_DATA.values()) {
            String status = (String) trackingInfo.get("status");
            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);

            String courier = (String) trackingInfo.get("courier_company");
            courierCount.put(courier, courierCount.getOrDefault(courier, 0) + 1);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_packages", totalCount);
        stats.put("status_distribution", statusCount);
        stats.put("courier_distribution", courierCount);

        // Add courier names to distribution
        Map<String, Object> courierDistributionWithNames = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : courierCount.entrySet()) {
            String companyName = COURIER_COMPANIES.getOrDefault(entry.getKey(), entry.getKey() + " (Unknown)");
            courierDistributionWithNames.put(companyName, entry.getValue());
        }
        stats.put("courier_distribution_with_names", courierDistributionWithNames);

        return stats;
    }

    /**
     * Search tracking records by recipient name
     *
     * @param recipientName Recipient name to search for
     * @return List of tracking records for the specified recipient
     */
    @MCPTool(name = "search_by_recipient",
            description = "Search tracking records by recipient name")
    public List<Map<String, Object>> searchByRecipient(
            @ToolParam(description = "Recipient name to search for")
            String recipientName) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> trackingInfo : TRACKING_DATA.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> receiver = (Map<String, Object>) trackingInfo.get("receiver");
            String receiverName = (String) receiver.get("name");

            if (receiverName.toLowerCase().contains(recipientName.toLowerCase())) {
                Map<String, Object> info = new LinkedHashMap<>(trackingInfo);

                // Add courier company name
                String companyCode = (String) info.get("courier_company");
                String companyName = COURIER_COMPANIES.getOrDefault(companyCode, "Unknown courier company");
                info.put("courier_name", companyName);

                result.add(info);
            }
        }

        if (result.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No tracking records found for recipient: " + recipientName);
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