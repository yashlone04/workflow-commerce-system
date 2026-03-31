package com.example.workflowcommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByOrderStatusOrderByCreatedAtDesc(String orderStatus);

    List<Order> findByOrderByCreatedAtDesc();

    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status AND o.status = true")
    Long countByStatus(@Param("status") String status);

    Long countByUserId(Long userId);
}
