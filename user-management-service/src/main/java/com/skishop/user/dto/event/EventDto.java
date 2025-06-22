package com.skishop.user.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * イベントDTO統合版（ジェネリック型対応）
 * 設計仕様書に従ったイベントスキーマ実装
 * 型安全性を確保したペイロード処理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDto<T> {
    
    /**
     * イベント固有のID（UUID文字列）
     */
    @JsonProperty("eventId")
    private String eventId;
    
    /**
     * イベントタイプ
     * 例: USER_REGISTERED, USER_DELETED, USER_MANAGEMENT_STATUS
     */
    @JsonProperty("eventType")
    private String eventType;
    
    /**
     * イベント発生タイムスタンプ（ISO-8601形式）
     */
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    /**
     * イベントスキーマバージョン
     */
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0";
    
    /**
     * イベント発行元サービス
     * 例: authentication-service, user-management-service
     */
    @JsonProperty("producer")
    private String producer;
    
    /**
     * イベントペイロード（ジェネリック型で型安全性確保）
     */
    @JsonProperty("payload")
    private T payload;
    
    /**
     * 関連するリクエストの追跡ID（UUID文字列）
     */
    @JsonProperty("correlationId")
    private String correlationId;
    
    /**
     * Saga取引ID（UUID文字列）
     */
    @JsonProperty("sagaId")
    private String sagaId;
    
    /**
     * リトライ回数
     */
    @JsonProperty("retry")
    @Builder.Default
    private Integer retry = 0;
    
    /**
     * 追加メタデータ（オプション）
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
