package com.skishop.user.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ロール一覧レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleListResponse {

    private List<RoleResponse> roles;
    private int totalCount;
    private int page;
    private int size;
}
