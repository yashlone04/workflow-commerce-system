package com.example.workflowcommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    List<Payment> findByPaymentStatus(String paymentStatus);

    List<Payment> findByOrder_User_Id(Long userId);

    boolean existsByOrder(Order order);
}
