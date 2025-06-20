package com.skishop.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 処理済みイベントを記録するエンティティ
 * 重複処理を防ぐためのイデンポテンシー管理
 */
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_saga_id", columnList = "saga_id", unique = true),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "saga_id", unique = true, nullable = false)
    private String sagaId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    @Column(name = "processing_node")
    private String processingNode; // どのノードで処理されたか
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs; // 処理時間
}
