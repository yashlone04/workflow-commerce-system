package com.example.workflowcommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.model.Cart;
import com.example.workflowcommerce.model.CartItem;
import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.repository.CartItemRepository;
import com.example.workflowcommerce.repository.CartRepository;
import com.example.workflowcommerce.repository.ProductRepository;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public CartItem addToCart(User user, Long productId, Integer quantity) {
        // Validate product exists and is active
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        Product product = productOpt.get();
        if (!product.getStatus()) {
            throw new RuntimeException("Product is not available");
        }

        // Validate stock
        if (product.getInventoryCount() == null || product.getInventoryCount() < quantity) {
            throw new RuntimeException("Insufficient inventory. Available: " + 
                (product.getInventoryCount() != null ? product.getInventoryCount() : 0));
        }

        // Get or create cart for user
        Cart cart = getOrCreateCart(user);

        // Check if product already in cart
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);
        
        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            // Update existing item quantity
            cartItem = existingItemOpt.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            // Validate new quantity against inventory
            if (product.getInventoryCount() < newQuantity) {
                throw new RuntimeException("Insufficient inventory. Available: " + product.getInventoryCount());
            }
            
            cartItem.setQuantity(newQuantity);
            cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            // Create new cart item
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        return cartItemRepository.save(cartItem);
    }

    @Transactional
    public CartItem updateCartItem(Long userId, Long itemId, Integer newQuantity) {
        // Validate quantity
        if (newQuantity < 0) {
            throw new RuntimeException("Quantity cannot be negative");
        }

        // Get user's cart
        Cart cart = cartRepository.findByUser_Id(userId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Find cart item
        CartItem cartItem = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Verify item belongs to user's cart
        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        // If quantity is 0, remove item
        if (newQuantity == 0) {
            cartItemRepository.delete(cartItem);
            return null;
        }

        // Validate stock
        Product product = cartItem.getProduct();
        if (product.getInventoryCount() == null || product.getInventoryCount() < newQuantity) {
            throw new RuntimeException("Insufficient inventory. Available: " + 
                (product.getInventoryCount() != null ? product.getInventoryCount() : 0));
        }

        // Update item
        cartItem.setQuantity(newQuantity);
        cartItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));

        return cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeFromCart(Long userId, Long itemId) {
        // Get user's cart
        Cart cart = cartRepository.findByUser_Id(userId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Find cart item
        CartItem cartItem = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Verify item belongs to user's cart
        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        cartItemRepository.delete(cartItem);
    }

    @Transactional(readOnly = true)
    public Cart getCartByUser(Long userId) {
        return cartRepository.findByUser_Id(userId)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Long userId) {
        Cart cart = cartRepository.findByUser_Id(userId).orElse(null);
        if (cart == null) {
            return List.of();
        }
        return cartItemRepository.findByCart(cart);
    }

    @Transactional(readOnly = true)
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUser_Id(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCart(cart);
        }
    }

    private Cart getOrCreateCart(User user) {
        Optional<Cart> existingCart = cartRepository.findByUser(user);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        Cart newCart = new Cart();
        newCart.setUser(user);
        return cartRepository.save(newCart);
    }

    public BigDecimal calculateCartTotal(List<CartItem> items) {
        return items.stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
