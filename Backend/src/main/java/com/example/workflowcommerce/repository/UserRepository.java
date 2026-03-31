package com.example.workflowcommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.workflowcommerce.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
  Boolean existsByUsername(String username);
  Boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);
  List<User> findByStatus(boolean status);
  List<User> findByEmailContainingIgnoreCase(String email);
}
