package com.example.workflowcommerce.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.model.WishlistItem;
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.repository.UserRepository;
import com.example.workflowcommerce.security.services.UserDetailsImpl;
import com.example.workflowcommerce.service.WishlistService;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add/{productId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addToWishlist(@PathVariable Long productId, Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findById(userDetails.getId());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("User not found"));
            }

            wishlistService.addToWishlist(userOpt.get(), productId);
            return ResponseEntity.ok(new MessageResponse("Product added to wishlist successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyWishlist(Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<WishlistItem> items = wishlistService.getWishlistItems(userDetails.getId());
            
            List<WishlistItemResponse> response = items.stream()
                .map(this::mapToWishlistItemResponse)
                .toList();
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{itemId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long itemId, Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            wishlistService.removeFromWishlist(userDetails.getId(), itemId);
            return ResponseEntity.ok(new MessageResponse("Item removed from wishlist"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/move-to-cart/{itemId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> moveToCart(@PathVariable Long itemId, Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            wishlistService.moveToCart(userDetails.getId(), itemId);
            return ResponseEntity.ok(new MessageResponse("Item moved to cart successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> checkProductInWishlist(@PathVariable Long productId, Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            boolean inWishlist = wishlistService.isProductInWishlist(userDetails.getId(), productId);
            return ResponseEntity.ok(new WishlistCheckResponse(inWishlist));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private WishlistItemResponse mapToWishlistItemResponse(WishlistItem item) {
        WishlistItemResponse response = new WishlistItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getProductId());
        response.setProductName(item.getProduct().getProductName());
        response.setPrice(item.getProduct().getPrice());
        response.setInStock(item.getProduct().getInventoryCount() != null && item.getProduct().getInventoryCount() > 0);
        response.setInventoryCount(item.getProduct().getInventoryCount());
        response.setAddedAt(item.getCreatedAt());
        return response;
    }

    public static class WishlistItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private java.math.BigDecimal price;
        private boolean inStock;
        private Integer inventoryCount;
        private java.time.LocalDateTime addedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
        public Integer getInventoryCount() { return inventoryCount; }
        public void setInventoryCount(Integer inventoryCount) { this.inventoryCount = inventoryCount; }
        public java.time.LocalDateTime getAddedAt() { return addedAt; }
        public void setAddedAt(java.time.LocalDateTime addedAt) { this.addedAt = addedAt; }
    }

    public static class WishlistCheckResponse {
        private boolean inWishlist;

        public WishlistCheckResponse(boolean inWishlist) {
            this.inWishlist = inWishlist;
        }

        public boolean isInWishlist() { return inWishlist; }
        public void setInWishlist(boolean inWishlist) { this.inWishlist = inWishlist; }
    }
}
