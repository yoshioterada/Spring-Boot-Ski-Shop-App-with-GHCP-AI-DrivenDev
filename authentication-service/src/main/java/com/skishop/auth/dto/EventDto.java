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
    private Map<String, Object> payload;
    
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
    
    // Manual getter methods since Lombok may not be working properly
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getTimestamp() { return timestamp; }
    public String getVersion() { return version; }
    public String getProducer() { return producer; }
    public Map<String, Object> getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public String getSagaId() { return sagaId; }
    public int getRetry() { return retry; }
    
    // Manual setter methods since Lombok may not be working properly
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public void setVersion(String version) { this.version = version; }
    public void setProducer(String producer) { this.producer = producer; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public void setRetry(int retry) { this.retry = retry; }

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
        private String version;
        private String producer;
        private Map<String, Object> payload;
        private String correlationId;
        private String sagaId;
        private int retry = 0;
        
        public EventDtoBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public EventDtoBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public EventDtoBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public EventDtoBuilder version(String version) { this.version = version; return this; }
        public EventDtoBuilder producer(String producer) { this.producer = producer; return this; }
        public EventDtoBuilder payload(Map<String, Object> payload) { this.payload = payload; return this; }
        public EventDtoBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public EventDtoBuilder sagaId(String sagaId) { this.sagaId = sagaId; return this; }
        public EventDtoBuilder retry(int retry) { this.retry = retry; return this; }
        
        public EventDto build() {
            EventDto eventDto = new EventDto();
            eventDto.eventId = this.eventId;
            eventDto.eventType = this.eventType;
            eventDto.timestamp = this.timestamp;
            eventDto.version = this.version;
            eventDto.producer = this.producer;
            eventDto.payload = this.payload;
            eventDto.correlationId = this.correlationId;
            eventDto.sagaId = this.sagaId;
            eventDto.retry = this.retry;
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
