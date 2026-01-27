package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Inventory management service providing stock query, availability check, and restock suggestions.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class InventoryTools {

    private static final Map<String, String> NAME_TO_ID = Map.of(
            "Product A", "PROD001",
            "Product B", "PROD002",
            "Product C", "PROD003"
    );

    private static final Map<String, Map<String, Object>> INVENTORY;

    static {
        // Product 001 - PROD001
        Map<String, Object> product1 = new LinkedHashMap<>();
        product1.put("product_id", "PROD001");
        product1.put("product_name", "Product A");
        product1.put("total_stock", 1500);
        product1.put("available_stock", 1200);
        product1.put("reserved_stock", 300);

        Map<String, Integer> warehouse1 = new LinkedHashMap<>();
        warehouse1.put("Warehouse A", 400);
        warehouse1.put("Warehouse B", 500);
        warehouse1.put("Warehouse C", 300);
        warehouse1.put("Warehouse D", 0);
        product1.put("warehouse_locations", warehouse1);

        product1.put("low_stock_threshold", 100);
        product1.put("last_restock_date", "2024-01-10");
        product1.put("supplier", "Supplier A");

        // Product 002 - PROD002
        Map<String, Object> product2 = new LinkedHashMap<>();
        product2.put("product_id", "PROD002");
        product2.put("product_name", "Product B");
        product2.put("total_stock", 800);
        product2.put("available_stock", 650);
        product2.put("reserved_stock", 150);

        Map<String, Integer> warehouse2 = new LinkedHashMap<>();
        warehouse2.put("Warehouse A", 200);
        warehouse2.put("Warehouse B", 300);
        warehouse2.put("Warehouse C", 150);
        warehouse2.put("Warehouse D", 0);
        product2.put("warehouse_locations", warehouse2);

        product2.put("low_stock_threshold", 50);
        product2.put("last_restock_date", "2024-01-08");
        product2.put("supplier", "Supplier A");

        // Product 003 - PROD003
        Map<String, Object> product3 = new LinkedHashMap<>();
        product3.put("product_id", "PROD003");
        product3.put("product_name", "Product C");
        product3.put("total_stock", 3000);
        product3.put("available_stock", 2800);
        product3.put("reserved_stock", 200);

        Map<String, Integer> warehouse3 = new LinkedHashMap<>();
        warehouse3.put("Warehouse A", 800);
        warehouse3.put("Warehouse B", 1000);
        warehouse3.put("Warehouse C", 700);
        warehouse3.put("Warehouse D", 300);
        product3.put("warehouse_locations", warehouse3);

        product3.put("low_stock_threshold", 500);
        product3.put("last_restock_date", "2024-01-12");
        product3.put("supplier", "Supplier B");

        INVENTORY = Map.of(
                "PROD001", product1,
                "PROD002", product2,
                "PROD003", product3
        );
    }

    /**
     * Query inventory info by product ID or name
     *
     * @param productId Product ID or name
     * @return Detailed inventory info for a specific product
     */
    @MCPTool(name = "check_inventory",
            description = "Query inventory info by product ID or name")
    public Map<String, Object> checkInventory(
            @ToolParam(description = "Product ID or name")
            String productId) {

        String actualProductId = productId;
        if (!INVENTORY.containsKey(productId)) {
            if (NAME_TO_ID.containsKey(productId)) {
                actualProductId = NAME_TO_ID.get(productId);
            } else {
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("error", String.format("Product ID or name %s does not exist.", productId));
                return errorResponse;
            }
        }

        Map<String, Object> inventoryInfo = new LinkedHashMap<>(INVENTORY.get(actualProductId));

        // Add stock status
        int availableStock = (int) inventoryInfo.get("available_stock");
        int lowStockThreshold = (int) inventoryInfo.get("low_stock_threshold");

        if (availableStock <= lowStockThreshold) {
            if (availableStock == 0) {
                inventoryInfo.put("stock_status", "Out of Stock");
            } else {
                inventoryInfo.put("stock_status", "Low Stock Warning");
            }
        } else {
            inventoryInfo.put("stock_status", "Sufficient Stock");
        }

        return inventoryInfo;
    }

    /**
     * Check if the specified quantity of a product is available in stock
     *
     * @param productId Product ID or name
     * @param quantity Required quantity
     * @return Availability status and details
     */
    @MCPTool(name = "check_stock_availability",
            description = "Check if the specified quantity of a product is available in stock")
    public Map<String, Object> checkStockAvailability(
            @ToolParam(description = "Product ID or name")
            String productId,
            @ToolParam(description = "Required quantity")
            int quantity) {

        String actualProductId = productId;
        if (!INVENTORY.containsKey(productId)) {
            if (NAME_TO_ID.containsKey(productId)) {
                actualProductId = NAME_TO_ID.get(productId);
            } else {
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("error", String.format("Product ID or name %s does not exist.", productId));
                return errorResponse;
            }
        }

        Map<String, Object> inventory = INVENTORY.get(actualProductId);
        int availableStock = (int) inventory.get("available_stock");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requested_quantity", quantity);
        response.put("available_stock", availableStock);
        response.put("product_id", actualProductId);
        response.put("product_name", inventory.get("product_name"));

        if (quantity <= availableStock) {
            response.put("available", true);
            response.put("message", String.format("Sufficient stock available to fulfill the order of %d units.", quantity));
        } else {
            response.put("available", false);
            response.put("shortage", quantity - availableStock);
            response.put("message", String.format("Insufficient stock: requested %d, only %d available.", quantity, availableStock));
        }

        return response;
    }

    /**
     * Release reserved stock (called when an order is canceled)
     *
     * @param productId Product ID or name
     * @param quantity Quantity to release
     * @param orderId Order ID
     * @return Release status and updated available stock
     */
    @MCPTool(name = "release_reserved_stock",
            description = "Release reserved stock (called when an order is canceled)")
    public Map<String, Object> releaseReservedStock(
            @ToolParam(description = "Product ID or name")
            String productId,
            @ToolParam(description = "Quantity to release")
            int quantity,
            @ToolParam(description = "Order ID")
            String orderId) {

        String actualProductId = productId;
        if (!INVENTORY.containsKey(productId)) {
            if (NAME_TO_ID.containsKey(productId)) {
                actualProductId = NAME_TO_ID.get(productId);
            } else {
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Product does not exist.");
                return errorResponse;
            }
        }

        Map<String, Object> inventory = INVENTORY.get(actualProductId);
        int reservedStock = (int) inventory.get("reserved_stock");

        if (quantity > reservedStock) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", String.format("Insufficient reserved stock to release %d units.", quantity));
            return errorResponse;
        }

        // Update stock levels
        inventory.put("reserved_stock", reservedStock - quantity);
        inventory.put("available_stock", (int) inventory.get("available_stock") + quantity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", String.format("Released %d units of %s for order %s.",
                quantity, inventory.get("product_name"), orderId));
        response.put("available_stock", inventory.get("available_stock"));
        response.put("reserved_stock", inventory.get("reserved_stock"));
        response.put("product_id", actualProductId);
        response.put("product_name", inventory.get("product_name"));

        return response;
    }

    /**
     * Get a list of all low stock products
     *
     * @return List of products with stock below their threshold
     */
    @MCPTool(name = "get_low_stock_products",
            description = "Get a list of all low stock products")
    public List<Map<String, Object>> getLowStockProducts() {

        List<Map<String, Object>> lowStockItems = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : INVENTORY.entrySet()) {
            Map<String, Object> inventory = entry.getValue();
            int availableStock = (int) inventory.get("available_stock");
            int lowStockThreshold = (int) inventory.get("low_stock_threshold");

            if (availableStock <= lowStockThreshold) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("product_id", entry.getKey());
                item.put("product_name", inventory.get("product_name"));
                item.put("available_stock", availableStock);
                item.put("low_stock_threshold", lowStockThreshold);
                item.put("urgency", availableStock == 0 ? "Critical" : "Warning");

                lowStockItems.add(item);
            }
        }

        if (lowStockItems.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "No products with low stock.");
            return List.of(message);
        }

        return lowStockItems;
    }

    /**
     * Query inventory distribution by warehouse
     *
     * @param warehouse Warehouse name
     * @return Products and their stock in the given warehouse
     */
    @MCPTool(name = "get_inventory_by_warehouse",
            description = "Query inventory distribution by warehouse")
    public Map<String, Object> getInventoryByWarehouse(
            @ToolParam(description = "Warehouse name")
            String warehouse) {

        Map<String, Object> warehouseInventory = new LinkedHashMap<>();
        boolean hasStock = false;

        for (Map.Entry<String, Map<String, Object>> entry : INVENTORY.entrySet()) {
            String productId = entry.getKey();
            Map<String, Object> inventory = entry.getValue();

            @SuppressWarnings("unchecked")
            Map<String, Integer> warehouseLocations = (Map<String, Integer>) inventory.get("warehouse_locations");

            if (warehouseLocations.containsKey(warehouse)) {
                int stock = warehouseLocations.get(warehouse);
                if (stock > 0) {
                    hasStock = true;

                    Map<String, Object> productInfo = new LinkedHashMap<>();
                    productInfo.put("product_name", inventory.get("product_name"));
                    productInfo.put("stock", stock);
                    productInfo.put("total_available", inventory.get("available_stock"));

                    warehouseInventory.put(productId, productInfo);
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("warehouse", warehouse);
        response.put("products", warehouseInventory);
        response.put("query_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (!hasStock) {
            response.put("message", String.format("No stock found in warehouse %s.", warehouse));
        }

        return response;
    }

    /**
     * Get restock suggestions
     *
     * @return Restock suggestions including recommended quantity and urgency
     */
    @MCPTool(name = "get_restock_suggestions",
            description = "Get restock suggestions")
    public List<Map<String, Object>> getRestockSuggestions() {

        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : INVENTORY.entrySet()) {
            String productId = entry.getKey();
            Map<String, Object> inventory = entry.getValue();

            int availableStock = (int) inventory.get("available_stock");
            int lowStockThreshold = (int) inventory.get("low_stock_threshold");
            int totalStock = (int) inventory.get("total_stock");

            if (availableStock <= lowStockThreshold) {
                int suggestedQuantity = Math.max(lowStockThreshold * 3 - totalStock, 0);

                Map<String, Object> suggestion = new LinkedHashMap<>();
                suggestion.put("product_id", productId);
                suggestion.put("product_name", inventory.get("product_name"));
                suggestion.put("current_stock", availableStock);
                suggestion.put("low_stock_threshold", lowStockThreshold);
                suggestion.put("suggested_restock", suggestedQuantity);
                suggestion.put("supplier", inventory.get("supplier"));
                suggestion.put("priority", availableStock <= lowStockThreshold / 2 ? "High" : "Medium");
                suggestion.put("last_restock", inventory.get("last_restock_date"));

                suggestions.add(suggestion);
            }
        }

        if (suggestions.isEmpty()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", "Stock levels are sufficient. No restocking needed.");
            return List.of(message);
        }

        return suggestions;
    }

    /**
     * Get all inventory records
     *
     * @return List of all inventory records
     */
    @MCPTool(name = "get_all_inventory",
            description = "Get all inventory records")
    public List<Map<String, Object>> getAllInventory() {

        List<Map<String, Object>> allInventory = new ArrayList<>();

        for (Map<String, Object> inventory : INVENTORY.values()) {
            Map<String, Object> inventoryCopy = new LinkedHashMap<>(inventory);

            // Add stock status
            int availableStock = (int) inventoryCopy.get("available_stock");
            int lowStockThreshold = (int) inventoryCopy.get("low_stock_threshold");

            if (availableStock <= lowStockThreshold) {
                if (availableStock == 0) {
                    inventoryCopy.put("stock_status", "Out of Stock");
                } else {
                    inventoryCopy.put("stock_status", "Low Stock Warning");
                }
            } else {
                inventoryCopy.put("stock_status", "Sufficient Stock");
            }

            allInventory.add(inventoryCopy);
        }

        return allInventory;
    }

    /**
     * Get inventory statistics
     *
     * @return Inventory statistics including total items, total stock value, etc.
     */
    @MCPTool(name = "get_inventory_statistics",
            description = "Get inventory statistics")
    public Map<String, Object> getInventoryStatistics() {

        int totalProducts = INVENTORY.size();
        int totalStock = 0;
        int totalAvailableStock = 0;
        int totalReservedStock = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        Map<String, Integer> supplierDistribution = new HashMap<>();
        Map<String, Integer> warehouseDistribution = new HashMap<>();

        for (Map<String, Object> inventory : INVENTORY.values()) {
            totalStock += (int) inventory.get("total_stock");
            totalAvailableStock += (int) inventory.get("available_stock");
            totalReservedStock += (int) inventory.get("reserved_stock");

            int availableStock = (int) inventory.get("available_stock");
            int lowStockThreshold = (int) inventory.get("low_stock_threshold");

            if (availableStock <= lowStockThreshold) {
                lowStockCount++;
                if (availableStock == 0) {
                    outOfStockCount++;
                }
            }

            // Supplier distribution
            String supplier = (String) inventory.get("supplier");
            supplierDistribution.put(supplier, supplierDistribution.getOrDefault(supplier, 0) + 1);

            // Warehouse distribution
            @SuppressWarnings("unchecked")
            Map<String, Integer> warehouseLocations = (Map<String, Integer>) inventory.get("warehouse_locations");
            for (Map.Entry<String, Integer> entry : warehouseLocations.entrySet()) {
                String warehouse = entry.getKey();
                int stock = entry.getValue();
                warehouseDistribution.put(warehouse, warehouseDistribution.getOrDefault(warehouse, 0) + stock);
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_products", totalProducts);
        stats.put("total_stock", totalStock);
        stats.put("total_available_stock", totalAvailableStock);
        stats.put("total_reserved_stock", totalReservedStock);
        stats.put("low_stock_count", lowStockCount);
        stats.put("out_of_stock_count", outOfStockCount);
        stats.put("stock_utilization_rate", totalProducts > 0 ? (double) totalAvailableStock / totalStock * 100 : 0.0);
        stats.put("supplier_distribution", supplierDistribution);
        stats.put("warehouse_distribution", warehouseDistribution);
        stats.put("query_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return stats;
    }

    /**
     * Reserve stock for an order
     *
     * @param productId Product ID or name
     * @param quantity Quantity to reserve
     * @param orderId Order ID
     * @return Reservation status and details
     */
    @MCPTool(name = "reserve_stock",
            description = "Reserve stock for an order")
    public Map<String, Object> reserveStock(
            @ToolParam(description = "Product ID or name")
            String productId,
            @ToolParam(description = "Quantity to reserve")
            int quantity,
            @ToolParam(description = "Order ID")
            String orderId) {

        String actualProductId = productId;
        if (!INVENTORY.containsKey(productId)) {
            if (NAME_TO_ID.containsKey(productId)) {
                actualProductId = NAME_TO_ID.get(productId);
            } else {
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Product does not exist.");
                return errorResponse;
            }
        }

        Map<String, Object> inventory = INVENTORY.get(actualProductId);
        int availableStock = (int) inventory.get("available_stock");

        if (quantity > availableStock) {
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", String.format("Insufficient available stock to reserve %d units. Only %d available.",
                    quantity, availableStock));
            return errorResponse;
        }

        // Reserve the stock
        inventory.put("available_stock", availableStock - quantity);
        inventory.put("reserved_stock", (int) inventory.get("reserved_stock") + quantity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", String.format("Reserved %d units of %s for order %s.",
                quantity, inventory.get("product_name"), orderId));
        response.put("available_stock", inventory.get("available_stock"));
        response.put("reserved_stock", inventory.get("reserved_stock"));
        response.put("product_id", actualProductId);
        response.put("product_name", inventory.get("product_name"));
        response.put("order_id", orderId);

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
