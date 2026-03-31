package com.example.workflowcommerce.service;

import com.example.workflowcommerce.dto.CouponApplyResponse;
import com.example.workflowcommerce.dto.CouponCreateRequest;
import com.example.workflowcommerce.model.Coupon;
import com.example.workflowcommerce.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Transactional
    public Coupon createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCouponCode(request.getCouponCode())) {
            throw new RuntimeException("Coupon code already exists.");
        }
        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new RuntimeException("Valid To date must be after Valid From date.");
        }
        
        Coupon coupon = new Coupon();
        coupon.setCouponCode(request.getCouponCode());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setStatus(true);
        coupon.setUsageCount(0);

        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateCoupon(Long id, CouponCreateRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (LocalDateTime.now().isAfter(coupon.getValidTo())) {
            throw new RuntimeException("Cannot edit an expired coupon.");
        }

        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new RuntimeException("Valid To date must be after Valid From date.");
        }

        // Do not update the code if it already exists maliciously
        if (!coupon.getCouponCode().equals(request.getCouponCode()) && couponRepository.existsByCouponCode(request.getCouponCode())) {
            throw new RuntimeException("New coupon code already exists.");
        }

        coupon.setCouponCode(request.getCouponCode());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setMinOrderAmount(request.getMinOrderAmount());

        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setStatus(false);
        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public CouponApplyResponse applyCoupon(String couponCode, BigDecimal cartTotal) {
        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Invalid coupon code."));

        if (!coupon.isStatus()) {
            throw new RuntimeException("This coupon is inactive.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new RuntimeException("This coupon is expired or not valid yet.");
        }

        if (coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new RuntimeException("This coupon usage limit has been reached.");
        }

        if (coupon.getMinOrderAmount() != null && cartTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Cart total is less than the minimum order amount for this coupon.");
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if ("Percentage".equalsIgnoreCase(coupon.getDiscountType())) {
            BigDecimal percentage = coupon.getDiscountValue().divide(new BigDecimal("100"));
            discountAmount = cartTotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
        } else if ("Fixed".equalsIgnoreCase(coupon.getDiscountType())) {
            discountAmount = coupon.getDiscountValue();
        }

        // Prevent negative total
        if (discountAmount.compareTo(cartTotal) > 0) {
            discountAmount = cartTotal;
        }

        BigDecimal newTotal = cartTotal.subtract(discountAmount);

        return new CouponApplyResponse(discountAmount, newTotal, "Coupon applied successfully!");
    }

    @Transactional
    public void incrementUsageCount(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) return;

        couponRepository.findByCouponCode(couponCode).ifPresent(coupon -> {
            if (coupon.getUsageCount() < coupon.getUsageLimit()) {
                coupon.setUsageCount(coupon.getUsageCount() + 1);
                couponRepository.save(coupon);
            }
        });
    }
}
