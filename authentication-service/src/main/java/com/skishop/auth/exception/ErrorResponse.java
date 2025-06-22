package com.skishop.auth.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Error Response DTO
 * 
 * エラーレスポンスのデータ転送オブジェクト
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
    
    // Default constructor
    public ErrorResponse() {}
    
    // Manual builder method since Lombok may not be working properly
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
    
    // Manual builder class
    public static class ErrorResponseBuilder {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> validationErrors;
        
        public ErrorResponseBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public ErrorResponseBuilder status(int status) { this.status = status; return this; }
        public ErrorResponseBuilder error(String error) { this.error = error; return this; }
        public ErrorResponseBuilder message(String message) { this.message = message; return this; }
        public ErrorResponseBuilder path(String path) { this.path = path; return this; }
        public ErrorResponseBuilder validationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; return this; }
        
        public ErrorResponse build() {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.timestamp = this.timestamp;
            errorResponse.status = this.status;
            errorResponse.error = this.error;
            errorResponse.message = this.message;
            errorResponse.path = this.path;
            errorResponse.validationErrors = this.validationErrors;
            return errorResponse;
        }
    }
}
