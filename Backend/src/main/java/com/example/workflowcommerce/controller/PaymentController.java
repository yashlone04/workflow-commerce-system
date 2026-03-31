package com.example.workflowcommerce.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.Payment;
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.security.services.UserDetailsImpl;
import com.example.workflowcommerce.service.PaymentService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    // USER: Process Payment for Order
    @PostMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> processPayment(@PathVariable Long orderId, 
                                            @Valid @RequestBody PaymentRequest paymentRequest,
                                            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();

            // Find order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Order not found."));
            }

            Order order = orderOpt.get();

            // Verify order belongs to current user
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(new MessageResponse("You can only pay for your own orders."));
            }

            // Check if order is in Pending status
            if (!"Pending".equals(order.getOrderStatus())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Only pending orders can be paid."));
            }

            // Check if payment already exists
            if (paymentService.hasPaymentForOrder(order)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Payment already processed for this order."));
            }

            // Process payment
            Payment payment = paymentService.processPayment(order, paymentRequest.getPaymentMethod());

            if ("COMPLETED".equals(payment.getPaymentStatus())) {
                return ResponseEntity.ok(new MessageResponse("Payment successful! Order status updated to Paid."));
            } else {
                return ResponseEntity.ok(new MessageResponse("Payment failed. Please try again."));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error processing payment: " + e.getMessage()));
        }
    }

    // ADMIN: Get All Payments
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPayments(@RequestParam(required = false) String status) {
        try {
            List<Payment> payments;
            if (status != null && !status.isEmpty()) {
                payments = paymentService.getPaymentsByStatus(status);
            } else {
                payments = paymentService.getAllPayments();
            }

            // Return payment summaries to avoid circular references
            List<?> paymentSummaries = payments.stream().map(payment -> {
                return new Object() {
                    public final Long paymentId = payment.getPaymentId();
                    public final Long orderId = payment.getOrder().getOrderId();
                    public final String customerName = payment.getOrder().getUser().getUsername();
                    public final Double amount = payment.getAmount().doubleValue();
                    public final String paymentMethod = payment.getPaymentMethod();
                    public final String paymentStatus = payment.getPaymentStatus();
                    public final java.time.LocalDateTime createdAt = payment.getCreatedAt();
                };
            }).toList();

            return ResponseEntity.ok(paymentSummaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error fetching payments: " + e.getMessage()));
        }
    }

    // ADMIN: Refund Payment
    @PutMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refundPayment(@PathVariable Long paymentId) {
        try {
            Optional<Payment> paymentOpt = paymentService.getPaymentById(paymentId);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Payment not found."));
            }

            Payment payment = paymentOpt.get();

            String status = payment.getPaymentStatus();
            if (!"Paid".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(status)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Only paid/completed payments can be refunded."));
            }

            paymentService.refundPayment(paymentId);
            return ResponseEntity.ok(new MessageResponse("Payment refunded successfully. Order status updated to Cancelled."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error refunding payment: " + e.getMessage()));
        }
    }

    // Inner class for payment request
    public static class PaymentRequest {
        private String paymentMethod;

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }
}
