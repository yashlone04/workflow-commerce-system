package com.example.workflowcommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = "sku")
})
public class Product {
    public Product() {}
    public Long getProductId() { return this.productId; }
    public void setStatus(boolean status) { this.status = status; }
    public boolean getStatus() { return this.status; }
    public void setCategory(Category category) { this.category = category; }
    public Category getCategory() { return this.category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getPrice() { return this.price; }
    public void setInventoryCount(Integer inventoryCount) { this.inventoryCount = inventoryCount; }
    public Integer getInventoryCount() { return this.inventoryCount; }
    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return this.description; }
    public void setSku(String sku) { this.sku = sku; }
    public String getSku() { return this.sku; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductName() { return this.productName; }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Version
    private Long version;

    @NotBlank
    @Size(max = 150)
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Size(max = 500)
    private String description;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotBlank
    @Size(max = 50)
    @Column(name = "sku", unique = true, nullable = false)
    private String sku;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Min(0)
    @Column(name = "inventory_count")
    private Integer inventoryCount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    private LocalDateTime updated_at;

    @Column(nullable = false)
    private boolean status = true; // true = active, false = inactive (soft delete)
}
