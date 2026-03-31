package com.example.workflowcommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.event.PaymentCompletedEvent;
import com.example.workflowcommerce.event.PaymentRefundedEvent;
import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.Payment;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.repository.PaymentRepository;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public Payment processPayment(Order order, String paymentMethod) {
        // Check if there's already a completed payment
        Optional<Payment> existingPayment = paymentRepository.findByOrder(order);
        if (existingPayment.isPresent() && "COMPLETED".equals(existingPayment.get().getPaymentStatus())) {
            return existingPayment.get(); // Return existing completed payment
        }

        Payment payment;
        if (existingPayment.isPresent()) {
            // Update existing PENDING payment instead of deleting
            payment = existingPayment.get();
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentStatus("COMPLETED");
        } else {
            // Create new payment if none exists
            payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentStatus("COMPLETED");
        }
        
        // Update order status to Paid
        order.setOrderStatus("Paid");
        orderRepository.save(order);

        Payment savedPayment = paymentRepository.save(payment);

        // Publish event - workflow transition will happen AFTER this transaction commits
        eventPublisher.publishEvent(new PaymentCompletedEvent(this, order.getOrderId()));

        return savedPayment;
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found");
        }

        Payment payment = paymentOpt.get();

        if (!"COMPLETED".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("Only completed payments can be refunded");
        }

        // Update payment status to Refunded
        payment.setPaymentStatus("REFUNDED");
        
        // Update order status to Refunded (matching workflow state)
        Order order = payment.getOrder();
        order.setOrderStatus("Refunded");
        orderRepository.save(order);

        Payment savedPayment = paymentRepository.save(payment);

        // Publish event - workflow transition will happen AFTER this transaction commits
        eventPublisher.publishEvent(new PaymentRefundedEvent(this, order.getOrderId()));

        return savedPayment;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByPaymentStatus(status);
    }

    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Optional<Payment> getPaymentByOrder(Order order) {
        return paymentRepository.findByOrder(order);
    }

    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByOrder_User_Id(userId);
    }

    public boolean hasPaymentForOrder(Order order) {
        Optional<Payment> payment = paymentRepository.findByOrder(order);
        return payment.isPresent() && "COMPLETED".equals(payment.get().getPaymentStatus());
    }
}
