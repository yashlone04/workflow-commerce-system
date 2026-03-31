package com.example.workflowcommerce.repository;

import com.example.workflowcommerce.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.category_name = :name")
    Optional<Category> findByCategory_name(@Param("name") String category_name);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.category_name = :name")
    boolean existsByCategory_name(@Param("name") String category_name);

    List<Category> findByStatusTrue();
}
