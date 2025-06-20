package com.skishop.user.dto;

import com.skishop.user.entity.User;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ユーザー更新リクエストDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 100, message = "名前は100文字以内で入力してください")
    private String firstName;

    @Size(max = 100, message = "姓は100文字以内で入力してください")
    private String lastName;

    @Size(max = 20, message = "電話番号は20文字以内で入力してください")
    private String phoneNumber;

    private LocalDate birthDate;

    private User.Gender gender;
}
