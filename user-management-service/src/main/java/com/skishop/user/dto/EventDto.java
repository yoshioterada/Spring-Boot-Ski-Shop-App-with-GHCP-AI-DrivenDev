package com.skishop.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 設計文書に準拠した標準イベントスキーマ（User-management-service用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDto {
    
    /**
     * イベント固有のID
     */
    private String eventId;
    
    /**
     * イベントタイプ (USER_MANAGEMENT_STATUS)
     */
    private String eventType;
    
    /**
     * イベント発生タイムスタンプ（ISO-8601形式）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    /**
     * イベントスキーマバージョン
     */
    private String version;
    
    /**
     * イベント発行元サービス
     */
    private String producer;
    
    /**
     * イベントペイロード
     */
    private Object payload;
    
    /**
     * 関連するリクエストの追跡ID
     */
    private String correlationId;
    
    /**
     * Saga取引ID
     */
    private String sagaId;
    
    /**
     * リトライ回数
     */
    @Builder.Default
    private int retry = 0;
}

/**
 * ステータス更新イベントペイロード（User-management-serviceから）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserManagementStatusPayload {
    private String userId;
    private String originalEventId;
    private String status;  // SUCCESS, FAILED
    private String reason;
    private Long processingTime;  // ミリ秒
}
