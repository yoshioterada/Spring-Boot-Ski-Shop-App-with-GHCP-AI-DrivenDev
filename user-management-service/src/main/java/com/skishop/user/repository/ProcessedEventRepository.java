package com.skishop.user.repository;

import com.skishop.user.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    /**
     * Saga IDで処理済みイベントの存在確認
     */
    boolean existsBySagaId(String sagaId);
    
    /**
     * 古い処理済みイベントを削除（クリーンアップ用）
     */
    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :cutoffDate")
    int deleteOldProcessedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 特定の期間内の処理済みイベント数をカウント
     */
    @Query("SELECT COUNT(p) FROM ProcessedEvent p WHERE p.processedAt >= :fromDate AND p.processedAt < :toDate")
    long countProcessedEventsBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
