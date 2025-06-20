package com.skishop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Login Request DTO
 * 
 * ログイン要求のデータ転送オブジェクト
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
    
    /**
     * MFAコード（任意）
     */
    private String mfaCode;
    
    private Boolean rememberMe = false;
    
    private DeviceInfo deviceInfo;
    
    @Data
    public static class DeviceInfo {
        private String deviceType;
        private String deviceName;
        private String ipAddress;
        private String userAgent;
    }
}
