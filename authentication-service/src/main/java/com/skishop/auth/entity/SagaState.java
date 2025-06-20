package com.skishop.auth.entity;

import com.skishop.auth.enums.SagaStatus;
import com.skishop.auth.enums.UserRegistrationStatus;
import com.skishop.auth.enums.UserDeletionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sagaの状態を管理するエンティティ
 */
@Entity
@Table(name = "saga_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "saga_id", unique = true, nullable = false)
    private String sagaId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "status", nullable = false)
    private String status; // 詳細なステータス（UserRegistrationStatus/UserDeletionStatus）
    
    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    @Builder.Default
    private SagaStatus sagaStatus = SagaStatus.SAGA_STARTED; // Saga全体のステータス
    
    @Column(name = "saga_type", nullable = false)
    private String sagaType; // USER_REGISTRATION, USER_DELETION
    
    @Column(name = "correlation_id")
    private String correlationId;
    
    @Column(name = "original_event_id")
    private String originalEventId;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "start_time", nullable = false)
    @Builder.Default
    private Instant startTime = Instant.now();
    
    @Column(name = "end_time")
    private Instant endTime;
    
    @Column(name = "timeout_at")
    private Instant timeoutAt;
    
    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;
    
    @Column(name = "data", columnDefinition = "TEXT")
    private String data; // JSON形式でデータを保存
    
    @Column(name = "error_reason")
    private String errorReason;
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // Manual getter and setter methods since Lombok may not be working properly
    
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public SagaStatus getSagaStatus() { return sagaStatus; }
    public void setSagaStatus(SagaStatus sagaStatus) { this.sagaStatus = sagaStatus; }
    
    public String getSagaType() { return sagaType; }
    public void setSagaType(String sagaType) { this.sagaType = sagaType; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public Instant getTimeoutAt() { return timeoutAt; }
    public void setTimeoutAt(Instant timeoutAt) { this.timeoutAt = timeoutAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    
    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    
    // Static builder method
    public static SagaStateBuilder builder() {
        return new SagaStateBuilder();
    }
    
    // Inner Builder class
    public static class SagaStateBuilder {
        private String sagaId;
        private String eventType;
        private UUID userId;
        private String status;
        private SagaStatus sagaStatus = SagaStatus.SAGA_STARTED;
        private String sagaType;
        private String correlationId;
        private String originalEventId;
        private String state;
        private String entityId;
        private Instant startTime = Instant.now();
        private Instant endTime;
        private Instant timeoutAt;
        private Instant lastHeartbeat;
        private String data;
        private String errorReason;
        private int retryCount = 0;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public SagaStateBuilder sagaId(String sagaId) { this.sagaId = sagaId; return this; }
        public SagaStateBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public SagaStateBuilder userId(UUID userId) { this.userId = userId; return this; }
        public SagaStateBuilder status(String status) { this.status = status; return this; }
        public SagaStateBuilder sagaStatus(SagaStatus sagaStatus) { this.sagaStatus = sagaStatus; return this; }
        public SagaStateBuilder sagaType(String sagaType) { this.sagaType = sagaType; return this; }
        public SagaStateBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public SagaStateBuilder originalEventId(String originalEventId) { this.originalEventId = originalEventId; return this; }
        public SagaStateBuilder state(String state) { this.state = state; return this; }
        public SagaStateBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public SagaStateBuilder startTime(Instant startTime) { this.startTime = startTime; return this; }
        public SagaStateBuilder endTime(Instant endTime) { this.endTime = endTime; return this; }
        public SagaStateBuilder timeoutAt(Instant timeoutAt) { this.timeoutAt = timeoutAt; return this; }
        public SagaStateBuilder lastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; return this; }
        public SagaStateBuilder data(String data) { this.data = data; return this; }
        public SagaStateBuilder errorReason(String errorReason) { this.errorReason = errorReason; return this; }
        public SagaStateBuilder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public SagaStateBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SagaStateBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public SagaState build() {
            SagaState sagaState = new SagaState();
            sagaState.sagaId = this.sagaId;
            sagaState.eventType = this.eventType;
            sagaState.userId = this.userId;
            sagaState.status = this.status;
            sagaState.sagaStatus = this.sagaStatus;
            sagaState.sagaType = this.sagaType;
            sagaState.correlationId = this.correlationId;
            sagaState.originalEventId = this.originalEventId;
            sagaState.state = this.state;
            sagaState.entityId = this.entityId;
            sagaState.startTime = this.startTime;
            sagaState.endTime = this.endTime;
            sagaState.timeoutAt = this.timeoutAt;
            sagaState.lastHeartbeat = this.lastHeartbeat;
            sagaState.data = this.data;
            sagaState.errorReason = this.errorReason;
            sagaState.retryCount = this.retryCount;
            sagaState.createdAt = this.createdAt;
            sagaState.updatedAt = this.updatedAt;
            return sagaState;
        }
    }
}
