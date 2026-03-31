package com.example.workflowcommerce.repository;

import com.example.workflowcommerce.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductProductIdAndStatusTrue(Long productId);
    List<Review> findByCustomerId(Long customerId);
    boolean existsByProductProductIdAndCustomerId(Long productId, Long customerId);
    Optional<Review> findByProductProductIdAndCustomerId(Long productId, Long customerId);
}
