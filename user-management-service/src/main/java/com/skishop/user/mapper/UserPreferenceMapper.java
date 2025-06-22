package com.skishop.user.mapper;

import com.skishop.user.dto.request.UserPreferenceUpdateRequest;
import com.skishop.user.dto.response.UserPreferenceResponse;
import com.skishop.user.entity.UserPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * UserPreference entity と DTO 間のマッピング
 */
@Mapper(componentModel = "spring")
public interface UserPreferenceMapper {
    
    /**
     * UserPreference エンティティから UserPreferenceResponse への変換
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "key", source = "prefKey")
    @Mapping(target = "value", source = "prefValue")
    @Mapping(target = "category", expression = "java(userPreference.getPrefType() != null ? userPreference.getPrefType().name().toLowerCase() : \"general\")")
    UserPreferenceResponse toResponse(UserPreference userPreference);
    
    /**
     * UserPreferenceUpdateRequest で UserPreference エンティティを更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "prefKey", ignore = true)
    @Mapping(target = "prefValue", source = "value")
    @Mapping(target = "prefType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget UserPreference userPreference, UserPreferenceUpdateRequest request);
}
