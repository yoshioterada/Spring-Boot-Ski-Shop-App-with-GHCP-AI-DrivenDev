package com.skishop.user.dto.response;

import com.skishop.user.entity.User;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * ユーザーレスポンスDTO（統合版）
 * 仕様書準拠のフィールド構成とUUID型IDに統一
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private User.Gender gender;
    private User.UserStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * フルネームを取得
     */
    public String getFullName() {
        return "%s %s".formatted(firstName, lastName);
    }
}
