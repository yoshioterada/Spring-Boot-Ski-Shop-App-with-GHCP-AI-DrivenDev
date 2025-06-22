package com.skishop.user.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * ユーザー登録イベントペイロード
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationPayload {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("additionalAttributes")
    private Map<String, String> additionalAttributes;
}
