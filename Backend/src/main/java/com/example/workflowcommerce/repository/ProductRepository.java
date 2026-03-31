package com.example.workflowcommerce.repository;

import com.example.workflowcommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p.category.category_id, COUNT(p) FROM Product p WHERE p.status = true GROUP BY p.category.category_id")
    List<Object[]> countActiveByCategory();

    boolean existsBySku(String sku);

    List<Product> findByStatusTrue();
}
