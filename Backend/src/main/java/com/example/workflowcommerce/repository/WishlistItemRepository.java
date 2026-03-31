package com.example.workflowcommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.model.Wishlist;
import com.example.workflowcommerce.model.WishlistItem;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByWishlist(Wishlist wishlist);
    Optional<WishlistItem> findByWishlistAndProduct(Wishlist wishlist, Product product);
    boolean existsByWishlistAndProduct(Wishlist wishlist, Product product);
    void deleteByWishlist(Wishlist wishlist);
    long countByWishlist(Wishlist wishlist);
}
