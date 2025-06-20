package com.skishop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MFA Verification Request DTO
 * 
 * MFA検証要求のデータ転送オブジェクト
 */
@Data
public class MfaVerificationRequest {
    
    @NotBlank(message = "Temp token is required")
    private String tempToken;
    
    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 8, message = "Code must be between 6 and 8 characters")
    private String mfaCode;
    
    private String sessionId;
    private String backupCode;
    
    // Manual getter methods since Lombok may not be working properly
    public String getSessionId() { return sessionId; }
    public String getMfaCode() { return mfaCode; }
    public String getTempToken() { return tempToken; }
    public String getBackupCode() { return backupCode; }
}
