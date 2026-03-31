package com.example.workflowcommerce.controller;

import com.example.workflowcommerce.dto.ShippingCreateRequest;
import com.example.workflowcommerce.dto.ShippingResponse;
import com.example.workflowcommerce.dto.ShippingStatusUpdateRequest;
import com.example.workflowcommerce.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/calculate-cost")
    public ResponseEntity<?> calculateShippingCost(@RequestBody Map<String, String> request) {
        String shippingMethod = request.get("shippingMethod");
        String destination = request.get("destination");
        BigDecimal cost = shippingService.calculateShippingCost(shippingMethod, destination);
        return ResponseEntity.ok(Map.of("cost", cost));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createShipping(
            @PathVariable Long orderId,
            @RequestBody ShippingCreateRequest request) {
        try {
            ShippingResponse response = shippingService.createShipping(orderId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update-status/{shippingId}")
    public ResponseEntity<?> updateShippingStatus(
            @PathVariable Long shippingId,
            @RequestBody ShippingStatusUpdateRequest request) {
        try {
            ShippingResponse response = shippingService.updateShippingStatus(shippingId, request.getStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<ShippingResponse>> getAllShippings() {
        return ResponseEntity.ok(shippingService.getAllShippings());
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/my/{orderId}")
    public ResponseEntity<?> getMyShipping(@PathVariable Long orderId, Authentication authentication) {
        try {
            String username = authentication.getName();
            ShippingResponse response = shippingService.getShippingByOrderId(orderId, username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
