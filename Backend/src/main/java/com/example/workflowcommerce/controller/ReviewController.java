package com.example.workflowcommerce.controller;

import com.example.workflowcommerce.dto.ReviewRequest;
import com.example.workflowcommerce.dto.ReviewResponse;
import com.example.workflowcommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/add/{productId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            ReviewResponse response = reviewService.addReview(productId, authentication.getName(), request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            ReviewResponse response = reviewService.updateReview(reviewId, authentication.getName(), request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{reviewId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        try {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            reviewService.deleteReview(reviewId, authentication.getName(), isAdmin);
            return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // MODERATING
    @PutMapping("/approve/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveReview(@PathVariable Long reviewId) {
        try {
            ReviewResponse response = reviewService.moderateReview(reviewId, true);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/reject/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectReview(@PathVariable Long reviewId) {
        try {
            ReviewResponse response = reviewService.moderateReview(reviewId, false);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProductReviews(@PathVariable Long productId) {
        List<ReviewResponse> reviews = reviewService.getProductReviews(productId);
        
        double avgRating = 0.0;
        if (!reviews.isEmpty()) {
            avgRating = reviews.stream().mapToInt(ReviewResponse::getRating).average().orElse(0.0);
        }
        
        return ResponseEntity.ok(Map.of(
                "averageRating", Math.round(avgRating * 10.0) / 10.0,
                "totalReviews", reviews.size(),
                "reviews", reviews
        ));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(Authentication authentication) {
        return ResponseEntity.ok(reviewService.getMyReviews(authentication.getName()));
    }
}
