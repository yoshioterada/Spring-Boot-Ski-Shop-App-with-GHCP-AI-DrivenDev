package com.skishop.user.dto;

import com.skishop.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザー情報レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private User.Gender gender;
    private User.UserStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * フルネームを取得
     */
    public String getFullName() {
        return "%s %s".formatted(firstName, lastName);
    }
}
