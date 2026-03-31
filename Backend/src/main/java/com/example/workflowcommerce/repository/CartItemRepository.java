package com.example.workflowcommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.Cart;
import com.example.workflowcommerce.model.CartItem;
import com.example.workflowcommerce.model.Product;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    void deleteByCart(Cart cart);

    long countByCart(Cart cart);
}
