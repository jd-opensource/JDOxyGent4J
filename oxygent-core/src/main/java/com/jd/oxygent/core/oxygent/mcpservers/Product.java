package com.jd.oxygent.core.oxygent.samples.examples.ecommerce;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {
    @JsonProperty("product_id")
    private String productId;
    private String name;
    private int price;
    private String description;
    private String category;
    private String brand;
    private double rating;
    @JsonProperty("reviews_count")
    private int reviewsCount;

    public Product() {
    }

    public Product(String productId, String name, int price, String description, String category, String brand, double rating, int reviewsCount) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
}
