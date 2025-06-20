package com.skishop.auth.entity;

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
    private String status; // INITIATED, PUBLISHED, PROCESSING, COMPLETED, FAILED
    
    @Column(name = "saga_type")
    private String sagaType;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "start_time")
    private Long startTime;
    
    @Column(name = "end_time")
    private Long endTime;
    
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
}
