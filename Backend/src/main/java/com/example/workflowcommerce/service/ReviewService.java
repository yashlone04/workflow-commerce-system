package com.example.workflowcommerce.service;

import com.example.workflowcommerce.dto.ReviewRequest;
import com.example.workflowcommerce.dto.ReviewResponse;
import com.example.workflowcommerce.model.Order;
import com.example.workflowcommerce.model.OrderItem;
import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.model.Review;
import com.example.workflowcommerce.model.User;
import com.example.workflowcommerce.repository.OrderRepository;
import com.example.workflowcommerce.repository.ProductRepository;
import com.example.workflowcommerce.repository.ReviewRepository;
import com.example.workflowcommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public ReviewResponse addReview(Long productId, String username, ReviewRequest request) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Validation: No duplicate review
        if (reviewRepository.existsByProductProductIdAndCustomerId(product.getProductId(), customer.getId())) {
            throw new RuntimeException("You have already reviewed this product.");
        }

        // Validation: Verify purchase and Delivered status
        boolean hasPurchasedAndDelivered = false;
        List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(customer.getId());
        for (Order order : userOrders) {
            if ("Delivered".equalsIgnoreCase(order.getOrderStatus())) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getProduct().getProductId().equals(product.getProductId())) {
                        hasPurchasedAndDelivered = true;
                        break;
                    }
                }
            }
            if (hasPurchasedAndDelivered) break;
        }

        if (!hasPurchasedAndDelivered) {
            throw new RuntimeException("You can only review products from delivered orders.");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setCustomer(customer);
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        review.setStatus(false); // Default pending

        return new ReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, String username, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        if (!review.getCustomer().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: You can only update your own reviews.");
        }

        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        review.setStatus(false); // Revert to pending after update

        return new ReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(Long reviewId, String username, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        if (!isAdmin && !review.getCustomer().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: You can only delete your own reviews.");
        }

        reviewRepository.delete(review);
    }

    @Transactional
    public ReviewResponse moderateReview(Long reviewId, boolean approve) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));
        
        review.setStatus(approve);
        return new ReviewResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getProductReviews(Long productId) {
        return reviewRepository.findByProductProductIdAndStatusTrue(productId).stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getMyReviews(String username) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        return reviewRepository.findByCustomerId(customer.getId()).stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toList());
    }
}
