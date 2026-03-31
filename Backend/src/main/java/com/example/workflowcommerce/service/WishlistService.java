package com.example.workflowcommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workflowcommerce.model.Cart;
import com.example.workflowcommerce.model.CartItem;
import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.model.Wishlist;
import com.example.workflowcommerce.model.WishlistItem;
import com.example.workflowcommerce.repository.CartItemRepository;
import com.example.workflowcommerce.repository.CartRepository;
import com.example.workflowcommerce.repository.ProductRepository;
import com.example.workflowcommerce.repository.WishlistItemRepository;
import com.example.workflowcommerce.repository.WishlistRepository;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Transactional
    public WishlistItem addToWishlist(User user, Long productId) {
        // Validate product exists and is active
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        Product product = productOpt.get();
        if (!product.getStatus()) {
            throw new RuntimeException("Product is not available");
        }

        // Get or create wishlist for user
        Wishlist wishlist = wishlistRepository.findByUser(user)
            .orElseGet(() -> {
                Wishlist newWishlist = new Wishlist();
                newWishlist.setUser(user);
                return wishlistRepository.save(newWishlist);
            });

        // Check if product already exists in wishlist
        if (wishlistItemRepository.existsByWishlistAndProduct(wishlist, product)) {
            throw new RuntimeException("Product already exists in wishlist");
        }

        // Create wishlist item
        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setWishlist(wishlist);
        wishlistItem.setProduct(product);

        return wishlistItemRepository.save(wishlistItem);
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlistItems(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElse(null);
        
        if (wishlist == null) {
            return List.of();
        }
        
        return wishlistItemRepository.findByWishlist(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long itemId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        WishlistItem item = wishlistItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        // Verify the item belongs to the user's wishlist
        if (!item.getWishlist().getWishlistId().equals(wishlist.getWishlistId())) {
            throw new RuntimeException("Unauthorized access to wishlist item");
        }

        wishlistItemRepository.delete(item);
    }

    @Transactional
    public void moveToCart(Long userId, Long itemId) {
        // Get user's wishlist and cart
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        Cart cart = cartRepository.findByUser_Id(userId)
            .orElseGet(() -> {
                Optional<User> userOpt = wishlistRepository.findById(wishlist.getWishlistId())
                    .map(Wishlist::getUser);
                if (userOpt.isEmpty()) {
                    throw new RuntimeException("User not found");
                }
                Cart newCart = new Cart();
                newCart.setUser(userOpt.get());
                return cartRepository.save(newCart);
            });

        // Get the wishlist item
        WishlistItem wishlistItem = wishlistItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        // Verify ownership
        if (!wishlistItem.getWishlist().getWishlistId().equals(wishlist.getWishlistId())) {
            throw new RuntimeException("Unauthorized access to wishlist item");
        }

        Product product = wishlistItem.getProduct();

        // Validate inventory
        if (product.getInventoryCount() == null || product.getInventoryCount() < 1) {
            throw new RuntimeException("Product is out of stock");
        }

        // Check if product already exists in cart
        Optional<CartItem> existingCartItem = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingCartItem.isPresent()) {
            // Update quantity
            CartItem cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + 1;
            
            if (product.getInventoryCount() < newQuantity) {
                throw new RuntimeException("Insufficient inventory. Available: " + product.getInventoryCount());
            }
            
            cartItem.setQuantity(newQuantity);
            cartItem.setTotalPrice(product.getPrice().multiply(new java.math.BigDecimal(newQuantity)));
            cartItemRepository.save(cartItem);
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(1);
            cartItem.setTotalPrice(product.getPrice());
            cartItemRepository.save(cartItem);
        }

        // Remove from wishlist
        wishlistItemRepository.delete(wishlistItem);
    }

    @Transactional(readOnly = true)
    public boolean isProductInWishlist(Long userId, Long productId) {
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUserId(userId);
        if (wishlistOpt.isEmpty()) {
            return false;
        }

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return false;
        }

        return wishlistItemRepository.existsByWishlistAndProduct(wishlistOpt.get(), productOpt.get());
    }

    @Transactional(readOnly = true)
    public long getWishlistItemCount(Long userId) {
        return wishlistRepository.findByUserId(userId)
            .map(wishlistItemRepository::countByWishlist)
            .orElse(0L);
    }
}
