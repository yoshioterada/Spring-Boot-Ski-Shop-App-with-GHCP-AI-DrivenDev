package com.skishop.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 設計文書に準拠した標準イベントスキーマ
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
     * イベントタイプ (USER_REGISTERED, USER_DELETED)
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

    // Add manual builder methods since Lombok may not be working properly
    // Manual EventDto builder method
    public static EventDtoBuilder builder() {
        return new EventDtoBuilder();
    }

    // Inner Builder class for EventDto
    public static class EventDtoBuilder {
        private String eventId;
        private String eventType;
        private Instant timestamp;
        private String sagaId;
        private String correlationId;
        private String source;
        private String destination;
        private String version;
        private Object payload;
        private Object metadata;
        
        public EventDtoBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public EventDtoBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public EventDtoBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public EventDtoBuilder sagaId(String sagaId) { this.sagaId = sagaId; return this; }
        public EventDtoBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public EventDtoBuilder source(String source) { this.source = source; return this; }
        public EventDtoBuilder destination(String destination) { this.destination = destination; return this; }
        public EventDtoBuilder version(String version) { this.version = version; return this; }
        public EventDtoBuilder payload(Object payload) { this.payload = payload; return this; }
        public EventDtoBuilder metadata(Object metadata) { this.metadata = metadata; return this; }
        
        public EventDto build() {
            EventDto eventDto = new EventDto();
            eventDto.eventId = this.eventId;
            eventDto.eventType = this.eventType;
            eventDto.timestamp = this.timestamp;
            eventDto.sagaId = this.sagaId;
            eventDto.correlationId = this.correlationId;
            eventDto.version = this.version;
            eventDto.payload = this.payload;
            eventDto.producer = this.source; // Map source to producer
            // Skip destination and metadata as they don't exist in EventDto
            return eventDto;
        }
    }
}

/**
 * ユーザー登録イベントペイロード
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserRegistrationPayload {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    private Map<String, String> additionalAttributes;
}

/**
 * ユーザー削除イベントペイロード
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserDeletionPayload {
    private String userId;
    private String reason;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant deletedAt;
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
