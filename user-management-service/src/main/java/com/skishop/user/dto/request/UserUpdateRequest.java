package com.skishop.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * ユーザー更新リクエストDTO
 */
public record UserUpdateRequest(
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,
    
    @Email(message = "Invalid email format")
    String email,
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phoneNumber,
    
    LocalDate birthDate,
    
    String gender
) {
}
