package com.skishop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API共通レスポンス形式
 */
public record ApiResponse<T>(
    @JsonProperty("success")
    boolean success,
    
    @JsonProperty("data")
    T data,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("errorCode")
    String errorCode
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Success", null);
    }
    
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }
    
    public boolean isSuccess() {
        return success;
    }
}
