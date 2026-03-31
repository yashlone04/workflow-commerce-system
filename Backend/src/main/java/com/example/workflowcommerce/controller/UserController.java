package com.example.workflowcommerce.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.model.ERole;
import com.example.workflowcommerce.model.Role;
import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.payload.request.UserRequest;
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.payload.response.UserResponse;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.repository.RoleRepository;
import com.example.workflowcommerce.repository.UserRepository;
import com.example.workflowcommerce.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder encoder;

    // USER: Get own profile
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(mapToUserResponse(user));
    }

    // ADMIN: Get all users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String email) {

        List<User> users;

        if (email != null && !email.isEmpty()) {
            users = userRepository.findByEmailContainingIgnoreCase(email);
        } else if (status != null) {
            users = userRepository.findByStatus(status);
        } else {
            users = userRepository.findAll();
        }

        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponseWithOrderCount)
                .collect(Collectors.toList());

        return ResponseEntity.ok(userResponses);
    }

    // ADMIN: Get user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(mapToUserResponseWithOrderCount(user));
    }

    // ADMIN: Create new user
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest userRequest) {
        // Check for duplicate username
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user
        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setPhoneNumber(userRequest.getPhoneNumber());
        user.setStatus(userRequest.isStatus());

        // Set password (generate random if not provided)
        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            user.setPassword(encoder.encode(userRequest.getPassword()));
        } else {
            // Generate a default password - user should change it
            user.setPassword(encoder.encode("changeme123"));
        }

        // Set default role as USER
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User created successfully!"));
    }

    // ADMIN: Update user
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(userRequest.getEmail()) &&
                userRepository.existsByEmail(userRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Update user details
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setPhoneNumber(userRequest.getPhoneNumber());
        user.setEmail(userRequest.getEmail());
        user.setStatus(userRequest.isStatus());
        user.setUpdatedAt(LocalDateTime.now());

        // Update password only if provided
        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            user.setPassword(encoder.encode(userRequest.getPassword()));
        }

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User updated successfully!"));
    }

    // ADMIN: Deactivate user (soft delete)
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if already inactive
        if (!user.getStatus()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: User is already deactivated!"));
        }

        user.setStatus(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User deactivated successfully!"));
    }

    // Helper method to map User to UserResponse
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        response.setRoles(roles);

        return response;
    }

    // Helper method to map User to UserResponse with order count
    private UserResponse mapToUserResponseWithOrderCount(User user) {
        UserResponse response = mapToUserResponse(user);

        // Get order count for this user
        Long orderCount = orderRepository.countByUserId(user.getId());
        response.setOrderCount(orderCount);

        return response;
    }
}
