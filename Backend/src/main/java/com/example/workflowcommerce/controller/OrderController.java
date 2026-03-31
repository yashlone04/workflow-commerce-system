package com.example.workflowcommerce.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.OrderItem;
import com.example.workflowcommerce.model.Payment;
import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.model.Shipping;
import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.payload.request.OrderItemRequest;
import com.example.workflowcommerce.payload.request.OrderRequest;
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.repository.PaymentRepository;
import com.example.workflowcommerce.repository.ProductRepository;
import com.example.workflowcommerce.repository.ShippingRepository;
import com.example.workflowcommerce.repository.UserRepository;
import com.example.workflowcommerce.service.CouponService;
import com.example.workflowcommerce.service.OrderService;
import com.example.workflowcommerce.service.workflow.WorkflowIntegrationService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private WorkflowIntegrationService workflowIntegrationService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShippingRepository shippingRepository;

    // USER: Create Order
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            User currentUser = userOpt.get();
            
            // Validate all products are active and have sufficient inventory
            for (OrderItemRequest item : orderRequest.getItems()) {
                if (!orderService.validateProductAvailability(item.getProductId(), item.getQuantity())) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Product " + item.getProductId() + " is not available or insufficient inventory."));
                }
            }

            // Create order
            Order order = new Order();
            order.setUser(currentUser);
            order.setShippingAddress(orderRequest.getShippingAddress());
            order.setOrderStatus("Pending");
            order.setStatus(true);

            // Create order items
            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderItemRequest item : orderRequest.getItems()) {
                Optional<Product> productOpt = productRepository.findById(item.getProductId());
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setPriceAtPurchase(product.getPrice());
                    
                    orderItems.add(orderItem);
                    
                    // Calculate total
                    totalAmount = totalAmount.add(
                            product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                }
            }

            // Validate and apply coupon if present
            if (orderRequest.getCouponCode() != null && !orderRequest.getCouponCode().isBlank()) {
                com.example.workflowcommerce.dto.CouponApplyResponse applyResponse = 
                    couponService.applyCoupon(orderRequest.getCouponCode(), totalAmount);
                totalAmount = applyResponse.getNewTotal();
            }

            order.setTotalAmount(totalAmount);
            order.setOrderItems(orderItems);

            // Save order and items
            orderService.saveOrder(order);
            for (OrderItem item : orderItems) {
                orderService.saveOrderItem(item);
                // Update product inventory
                orderService.updateProductInventory(item.getProduct().getProductId(), item.getQuantity());
            }

            // Increment coupon usage
            if (orderRequest.getCouponCode() != null && !orderRequest.getCouponCode().isBlank()) {
                couponService.incrementUsageCount(orderRequest.getCouponCode());
            }

            // Auto-create Payment record with PENDING status
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(orderRequest.getPaymentMethod() != null ? orderRequest.getPaymentMethod() : "PENDING");
            payment.setPaymentStatus("PENDING");
            paymentRepository.save(payment);

            // Auto-create Shipping record with PENDING status
            Shipping shipping = new Shipping();
            shipping.setOrder(order);
            shipping.setCourierService("To be assigned");
            shipping.setTrackingNumber("TBD-" + order.getOrderId());
            shipping.setShippingStatus("PENDING");
            shipping.setShippingMethod("Standard");
            shipping.setShippingCost(BigDecimal.ZERO);
            shippingRepository.save(shipping);

            // Initialize workflow for the new order
            workflowIntegrationService.onOrderCreated(order.getOrderId(), currentUser.getUsername());

            return ResponseEntity.ok(new MessageResponse(
                "Order placed successfully. Order ID: " + order.getOrderId() + 
                ". Payment and Shipping records created automatically."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error creating order: " + e.getMessage()));
        }
    }

    // USER: Get My Orders
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            User currentUser = userOpt.get();
            List<Order> orders = orderService.getUserOrders(currentUser.getId());
            
            // Return simple order info to avoid circular references
            List<?> orderSummaries = orders.stream().map(order -> {
                return new Object() {
                    public final Long orderId = order.getOrderId();
                    public final String orderStatus = order.getOrderStatus();
                    public final BigDecimal totalAmount = order.getTotalAmount();
                    public final String shippingAddress = order.getShippingAddress();
                    public final LocalDateTime createdAt = order.getCreatedAt();
                };
            }).toList();
            
            return ResponseEntity.ok(orderSummaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error fetching orders: " + e.getMessage()));
        }
    }

    // USER: Cancel Own Order
    @PutMapping("/{id}/cancel/user")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> cancelOwnOrder(@PathVariable Long id, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("User not found"));
            }
            
            User currentUser = userOpt.get();
            Optional<Order> orderOpt = orderService.getUserOrderById(id, currentUser.getId());
            
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Order not found."));
            }

            Order order = orderOpt.get();
            if (!"Pending".equals(order.getOrderStatus())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Only pending orders can be cancelled."));
            }

            order.setOrderStatus("Cancelled");
            order.setStatus(false);
            orderService.saveOrder(order);
            
            // Restore inventory for cancelled order
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    orderService.restoreProductInventory(item.getProduct().getProductId(), item.getQuantity());
                }
            }

            return ResponseEntity.ok(new MessageResponse("Order cancelled successfully."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error cancelling order: " + e.getMessage()));
        }
    }

    // ADMIN: Get All Orders
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            
            // Return simple order info to avoid circular references
            List<?> orderSummaries = orders.stream().map(order -> {
                return new Object() {
                    public final Long orderId = order.getOrderId();
                    public final String orderStatus = order.getOrderStatus();
                    public final BigDecimal totalAmount = order.getTotalAmount();
                    public final String shippingAddress = order.getShippingAddress();
                    public final LocalDateTime createdAt = order.getCreatedAt();
                    public final String username = order.getUser().getUsername();
                    public final Long userId = order.getUser().getId();
                };
            }).toList();
            
            return ResponseEntity.ok(orderSummaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error fetching orders: " + e.getMessage()));
        }
    }

    // ADMIN: Get Orders by Status
    @GetMapping("/filter/{status}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable String status) {
        try {
            List<Order> orders = orderService.getOrdersByStatus(status);
            
            // Return simple order info to avoid circular references
            List<?> orderSummaries = orders.stream().map(order -> {
                return new Object() {
                    public final Long orderId = order.getOrderId();
                    public final String orderStatus = order.getOrderStatus();
                    public final BigDecimal totalAmount = order.getTotalAmount();
                    public final String shippingAddress = order.getShippingAddress();
                    public final LocalDateTime createdAt = order.getCreatedAt();
                    public final String username = order.getUser().getUsername();
                    public final Long userId = order.getUser().getId();
                };
            }).toList();
            
            return ResponseEntity.ok(orderSummaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error fetching orders: " + e.getMessage()));
        }
    }

    // ADMIN: Update Order Status
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody String newStatus) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Order not found."));
            }

            Order order = orderOpt.get();
            order.setOrderStatus(newStatus);
            orderService.saveOrder(order);

            return ResponseEntity.ok(new MessageResponse("Order status updated to: " + newStatus));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error updating order status: " + e.getMessage()));
        }
    }

    // ADMIN: Cancel Any Order
    @PutMapping("/{id}/cancel/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> cancelAnyOrder(@PathVariable Long id) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new MessageResponse("Order not found."));
            }

            Order order = orderOpt.get();
            if ("Shipped".equals(order.getOrderStatus()) || "Delivered".equals(order.getOrderStatus())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Cannot cancel shipped or delivered orders."));
            }

            order.setOrderStatus("Cancelled");
            order.setStatus(false);
            orderService.saveOrder(order);
            
            // Restore inventory for cancelled order
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    orderService.restoreProductInventory(item.getProduct().getProductId(), item.getQuantity());
                }
            }

            return ResponseEntity.ok(new MessageResponse("Order cancelled successfully."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error cancelling order: " + e.getMessage()));
        }
    }
}
