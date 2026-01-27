package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Product information service providing product lookup and category filtering tools.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class ProductTool {

    private static final Map<String, String> NAME_TO_ID = Map.of(
            "Product A", "PROD001",
            "Product B", "PROD002",
            "Product C", "PROD003"
    );

    private static final Map<String, Map<String, Object>> PRODUCTS;

    static {
        Map<String, Object> productA = new HashMap<>();
        productA.put("product_id", "PROD001");
        productA.put("name", "Product A");
        productA.put("price", 5999);
        productA.put("description", "High-end smart device equipped with an advanced processor and multifunctional camera system.");
        productA.put("category", "Electronics");
        productA.put("brand", "Brand A");
        productA.put("rating", 4.8);
        productA.put("reviews_count", 15420);

        Map<String, Object> productB = new HashMap<>();
        productB.put("product_id", "PROD002");
        productB.put("name", "Product B");
        productB.put("price", 12999);
        productB.put("description", "Professional-grade portable device with a high-performance chip, high-definition display, and large storage capacity.");
        productB.put("category", "Computing Devices");
        productB.put("brand", "Brand A");
        productB.put("rating", 4.9);
        productB.put("reviews_count", 8760);

        Map<String, Object> productC = new HashMap<>();
        productC.put("product_id", "PROD003");
        productC.put("name", "Product C");
        productC.put("price", 899);
        productC.put("description", "Practical and cost-effective product suitable for daily use.");
        productC.put("category", "Accessories");
        productC.put("brand", "Brand B");
        productC.put("rating", 4.5);
        productC.put("reviews_count", 23150);

        PRODUCTS = Map.of(
                "PROD001", productA,
                "PROD002", productB,
                "PROD003", productC
        );
    }

    /**
     * Get product information by product ID or name
     */
    @MCPTool(name = "get_product_info",
            description = "Get product information by product ID or product name")
    public Map<String, Object> getProductInfo(
            @ToolParam(description = "Product ID or product name")
            String productId) {

        // Check if input is product name
        if (NAME_TO_ID.containsKey(productId)) {
            productId = NAME_TO_ID.get(productId);
        }

        Map<String, Object> product = PRODUCTS.get(productId);
        if (product == null) {
            return Map.of("error", "Product not found");
        }

        // Return a copy to avoid modification of original data
        return new HashMap<>(product);
    }

    /**
     * Get product list by category
     */
    @MCPTool(name = "get_products_by_category",
            description = "Get product list by category")
    public List<Map<String, Object>> getProductsByCategory(
            @ToolParam(description = "Product category")
            String category) {

        List<Map<String, Object>> result = PRODUCTS.values().stream()
                .filter(product -> category.equals(product.get("category")))
                .map(HashMap::new)  // Create a copy of each product map
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return List.of(Map.of("message", "No products found in this category"));
        }

        return result;
    }

    /**
     * Get all products
     */
    @MCPTool(name = "get_all_products",
            description = "Get all available products")
    public List<Map<String, Object>> getAllProducts() {
        return PRODUCTS.values().stream()
                .map(HashMap::new)  // Create a copy of each product map
                .collect(Collectors.toList());
    }

    /**
     * Search products by keyword in name or description
     */
    @MCPTool(name = "search_products",
            description = "Search products by keyword in name or description")
    public List<Map<String, Object>> searchProducts(
            @ToolParam(description = "Search keyword")
            String keyword) {

        String lowerKeyword = keyword.toLowerCase();

        List<Map<String, Object>> result = PRODUCTS.values().stream()
                .filter(product ->
                        ((String) product.get("name")).toLowerCase().contains(lowerKeyword) ||
                                ((String) product.get("description")).toLowerCase().contains(lowerKeyword))
                .map(HashMap::new)  // Create a copy of each product map
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return List.of(Map.of("message", "No products found matching: " + keyword));
        }

        return result;
    }

    /**
     * Get products filtered by price range
     */
    @MCPTool(name = "get_products_by_price_range",
            description = "Get products within a specific price range")
    public List<Map<String, Object>> getProductsByPriceRange(
            @ToolParam(description = "Minimum price")
            int minPrice,
            @ToolParam(description = "Maximum price")
            int maxPrice) {

        List<Map<String, Object>> result = PRODUCTS.values().stream()
                .filter(product -> {
                    int price = (int) product.get("price");
                    return price >= minPrice && price <= maxPrice;
                })
                .map(HashMap::new)  // Create a copy of each product map
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return List.of(Map.of("message",
                    String.format("No products found in price range %d - %d", minPrice, maxPrice)));
        }

        return result;
    }

    /**
     * Main method to start the MCP server
     */
    @EnableMcpServer(mode = "stdio")
    public static void main(String[] args) {
        McpServer.start();
    }
}