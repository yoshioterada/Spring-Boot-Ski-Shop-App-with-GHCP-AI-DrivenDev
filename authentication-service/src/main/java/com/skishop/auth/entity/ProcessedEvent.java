package com.skishop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 処理済みイベントを記録するエンティティ（冪等性保証）
 */
@Entity
@Table(name = "processed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    
    @Id
    @Column(name = "event_id")
    private String eventId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;
    
    @Column(name = "success", nullable = false)
    @Builder.Default
    private boolean success = false;
    
    @Column(name = "processing_time", nullable = false)
    private long processingTime;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
