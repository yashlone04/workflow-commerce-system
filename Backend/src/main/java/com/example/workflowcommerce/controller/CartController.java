package com.example.workflowcommerce.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.model.Cart;
import com.example.workflowcommerce.model.CartItem;
import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.repository.UserRepository;
import com.example.workflowcommerce.security.services.UserDetailsImpl;
import com.example.workflowcommerce.service.CartService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    // USER: Add item to cart
    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddToCartRequest request, 
                                        Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            Optional<User> userOpt = userRepository.findById(userDetails.getId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("User not found"));
            }

            CartItem cartItem = cartService.addToCart(
                userOpt.get(), 
                request.getProductId(), 
                request.getQuantity()
            );

            return ResponseEntity.ok(new MessageResponse("Product added to cart successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // USER: Update cart item quantity
    @PutMapping("/update")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateCartItem(@Valid @RequestBody UpdateCartRequest request,
                                            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            CartItem updatedItem = cartService.updateCartItem(
                userDetails.getId(),
                request.getItemId(),
                request.getQuantity()
            );

            if (updatedItem == null) {
                return ResponseEntity.ok(new MessageResponse("Item removed from cart"));
            }

            return ResponseEntity.ok(new MessageResponse("Cart updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // USER: Remove item from cart
    @DeleteMapping("/remove/{itemId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeFromCart(@PathVariable Long itemId,
                                            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            cartService.removeFromCart(userDetails.getId(), itemId);
            return ResponseEntity.ok(new MessageResponse("Item removed from cart"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // USER: Get my cart
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyCart(Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            List<CartItem> items = cartService.getCartItems(userDetails.getId());
            BigDecimal total = cartService.calculateCartTotal(items);

            // Build response
            CartResponse response = new CartResponse();
            response.setItems(items.stream().map(this::mapToCartItemResponse).toList());
            response.setTotalAmount(total);
            response.setItemCount(items.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // ADMIN: Get all carts
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCarts() {
        try {
            List<Cart> carts = cartService.getAllCarts();
            
            List<AdminCartResponse> response = carts.stream().map(cart -> {
                AdminCartResponse item = new AdminCartResponse();
                item.setCartId(cart.getCartId());
                item.setCustomerName(cart.getUser().getUsername());
                item.setItemCount(cart.getCartItems().size());
                
                BigDecimal total = cart.getCartItems().stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                item.setTotalValue(total);
                item.setUpdatedAt(cart.getUpdatedAt());
                
                return item;
            }).toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getProductId());
        response.setProductName(item.getProduct().getProductName());
        response.setPrice(item.getProduct().getPrice());
        response.setQuantity(item.getQuantity());
        response.setTotalPrice(item.getTotalPrice());
        return response;
    }

    // Inner classes for requests and responses
    public static class AddToCartRequest {
        @NotNull
        private Long productId;
        
        @NotNull
        @Min(1)
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class UpdateCartRequest {
        @NotNull
        private Long itemId;
        
        @NotNull
        @Min(0)
        private Integer quantity;

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class CartResponse {
        private List<CartItemResponse> items;
        private BigDecimal totalAmount;
        private int itemCount;

        public List<CartItemResponse> getItems() { return items; }
        public void setItems(List<CartItemResponse> items) { this.items = items; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    }

    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }

    public static class AdminCartResponse {
        private Long cartId;
        private String customerName;
        private int itemCount;
        private BigDecimal totalValue;
        private java.time.LocalDateTime updatedAt;

        public Long getCartId() { return cartId; }
        public void setCartId(Long cartId) { this.cartId = cartId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
