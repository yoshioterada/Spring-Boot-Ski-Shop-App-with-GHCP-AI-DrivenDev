package com.skishop.user.dto;

import com.skishop.user.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ユーザー登録リクエストDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 100, message = "パスワードは8文字以上100文字以内で入力してください")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]",
        message = "パスワードは大文字、小文字、数字、特殊文字を含む必要があります"
    )
    private String password;

    @NotBlank(message = "名前は必須です")
    @Size(max = 100, message = "名前は100文字以内で入力してください")
    private String firstName;

    @NotBlank(message = "姓は必須です")
    @Size(max = 100, message = "姓は100文字以内で入力してください")
    private String lastName;

    @Pattern(
        regexp = "^[0-9\\-()+ ]*$",
        message = "有効な電話番号を入力してください"
    )
    @Size(max = 20, message = "電話番号は20文字以内で入力してください")
    private String phoneNumber;

    @Past(message = "生年月日は過去の日付である必要があります")
    private LocalDate birthDate;

    private User.Gender gender;

    /**
     * パスワード確認用
     */
    @NotBlank(message = "パスワード確認は必須です")
    private String confirmPassword;

    /**
     * パスワードと確認パスワードが一致するかチェック
     */
    public boolean isPasswordMatching() {
        return password != null && confirmPassword != null && password.equals(confirmPassword);
    }
}
