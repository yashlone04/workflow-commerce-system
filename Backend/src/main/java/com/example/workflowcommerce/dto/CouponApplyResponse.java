package com.example.workflowcommerce.dto;

import java.math.BigDecimal;

public class CouponApplyResponse {
    private BigDecimal discountAmount;
    private BigDecimal newTotal;
    private String message;

    public CouponApplyResponse(BigDecimal discountAmount, BigDecimal newTotal, String message) {
        this.discountAmount = discountAmount;
        this.newTotal = newTotal;
        this.message = message;
    }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getNewTotal() { return newTotal; }
    public void setNewTotal(BigDecimal newTotal) { this.newTotal = newTotal; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
