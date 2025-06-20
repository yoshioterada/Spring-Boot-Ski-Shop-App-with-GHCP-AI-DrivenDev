package com.skishop.user.mapper;

import com.skishop.user.dto.UserRegistrationRequest;
import com.skishop.user.dto.UserResponse;
import com.skishop.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * ユーザーエンティティとDTOの変換を行うMapStructマッパー
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * エンティティをレスポンスDTOに変換
     */
    @Mapping(source = "role.name", target = "roleName")
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
}
