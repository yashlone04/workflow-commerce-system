package com.example.workflowcommerce.payload.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized API Response wrapper
 * Provides consistent response format across all endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private int status;
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, int status, String code, String message, T data, LocalDateTime timestamp) {
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    // Static factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "SUCCESS", "Operation completed successfully", 
                data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, "SUCCESS", message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, "CREATED", "Resource created successfully", 
                data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(int status, String code, String message) {
        return new ApiResponse<>(false, status, code, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(int status, String code, String message, T data) {
        return new ApiResponse<>(false, status, code, message, data, LocalDateTime.now());
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
