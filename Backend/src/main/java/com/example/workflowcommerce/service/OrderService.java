package com.example.workflowcommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.OrderItem;
import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.repository.OrderItemRepository;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.repository.ProductRepository;

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findByOrderByCreatedAtDesc();
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByOrderStatusOrderByCreatedAtDesc(status);
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getUserOrderById(Long orderId, Long userId) {
        return orderRepository.findByOrderIdAndUserId(orderId, userId);
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    public OrderItem saveOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    public boolean validateProductAvailability(Long productId, Integer quantity) {
        Optional<Product> product = productRepository.findById(productId);
        return product.isPresent() && 
               product.get().getStatus() && 
               product.get().getInventoryCount() >= quantity;
    }

    public void updateProductInventory(Long productId, Integer quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setInventoryCount(product.getInventoryCount() - quantity);
            productRepository.save(product);
        }
    }

    public void restoreProductInventory(Long productId, Integer quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setInventoryCount(product.getInventoryCount() + quantity);
            productRepository.save(product);
        }
    }

    public BigDecimal calculateOrderTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
