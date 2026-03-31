package com.example.workflowcommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = "category_name")
})
public class Category {
    public Category() {}
    public Long getCategory_id() { return this.category_id; }
    public String getCategory_name() { return this.category_name; }
    public void setCategory_name(String category_name) { this.category_name = category_name; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public boolean getStatus() { return this.status; }
    public void setStatus(boolean status) { this.status = status; }
    public long getProductCount() { return this.productCount; }
    public void setProductCount(long productCount) { this.productCount = productCount; }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long category_id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "category_name", nullable = false)
    private String category_name;

    @Size(max = 300)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    private LocalDateTime updated_at;

    private boolean status = true; // true = active, false = inactive (soft delete)

    @Transient
    private long productCount;

    public Category(String category_name, String description) {
        this.category_name = category_name;
        this.description = description;
        this.status = true;
    }
}
