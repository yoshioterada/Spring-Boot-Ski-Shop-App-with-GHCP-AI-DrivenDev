package com.skishop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sagaの状態遷移履歴を記録するエンティティ
 */
@Entity
@Table(name = "saga_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaStatusHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "saga_id", nullable = false)
    private String sagaId;
    
    @Column(name = "from_status")
    private String fromStatus;
    
    @Column(name = "to_status", nullable = false)
    private String toStatus;
    
    @Column(name = "saga_status_from")
    private String sagaStatusFrom;
    
    @Column(name = "saga_status_to", nullable = false)
    private String sagaStatusTo;
    
    @Column(name = "transition_time", nullable = false)
    private LocalDateTime transitionTime;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
}
