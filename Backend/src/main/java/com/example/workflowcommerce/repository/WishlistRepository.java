package com.example.workflowcommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.model.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUser(User user);
    Optional<Wishlist> findByUserId(Long userId);
    boolean existsByUser(User user);
    boolean existsByUserId(Long userId);
}
