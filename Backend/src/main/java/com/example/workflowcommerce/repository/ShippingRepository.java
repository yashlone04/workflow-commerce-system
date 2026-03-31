package com.example.workflowcommerce.repository;

import com.example.workflowcommerce.model.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingRepository extends JpaRepository<Shipping, Long> {
    Optional<Shipping> findByOrderOrderId(Long orderId);
    boolean existsByOrderOrderId(Long orderId);
}
