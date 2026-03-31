package com.example.workflowcommerce.dto;

import com.example.workflowcommerce.model.Review;
import java.time.LocalDateTime;

public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long customerId;
    private String customerName;
    private Integer rating;
    private String reviewText;
    private boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewResponse() {}

    public ReviewResponse(Review review) {
        this.id = review.getId();
        this.productId = review.getProduct().getProductId();
        this.productName = review.getProduct().getProductName();
        this.customerId = review.getCustomer().getId();
        this.customerName = review.getCustomer().getUsername();
        this.rating = review.getRating();
        this.reviewText = review.getReviewText();
        this.status = review.isStatus();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
