package com.skishop.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User DTO
 * 
 * ユーザー情報のデータ転送オブジェクト
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean emailVerified;
    private Boolean isActive;
    private Boolean accountLocked;
    private Instant lastLogin;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> roles;
    private Boolean mfaEnabled;
}
