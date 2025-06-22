package com.skishop.user.mapper;

import com.skishop.user.dto.UserRegistrationRequest;
import com.skishop.user.dto.response.UserResponse;
import com.skishop.user.dto.request.UserCreateRequest;
import com.skishop.user.dto.request.UserUpdateRequest;
import com.skishop.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * ユーザーエンティティとDTOの変換を行うMapStructマッパー
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * エンティティをレスポンスDTOに変換
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "gender", target = "gender")
    @Mapping(target = "roles", expression = "java(user.getRole() != null ? java.util.Set.of(user.getRole().getName()) : java.util.Set.of())")
    @Mapping(target = "username", source = "email")
    UserResponse toResponse(User user);

    /**
     * 登録リクエストDTOをエンティティに変換
     * パスワードは別途暗号化が必要なため除外
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRegistrationRequest request);

    /**
     * ユーザー作成リクエストDTOをエンティティに変換
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    User toEntity(UserCreateRequest request);

    /**
     * ユーザー更新リクエストDTOで既存エンティティを更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget User user, UserUpdateRequest request);
}
