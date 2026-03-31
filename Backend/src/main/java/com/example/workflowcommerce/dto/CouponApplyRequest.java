package com.example.workflowcommerce.dto;

import java.math.BigDecimal;

public class CouponApplyRequest {
    private String couponCode;
    private BigDecimal cartTotal;

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public BigDecimal getCartTotal() { return cartTotal; }
    public void setCartTotal(BigDecimal cartTotal) { this.cartTotal = cartTotal; }
}
