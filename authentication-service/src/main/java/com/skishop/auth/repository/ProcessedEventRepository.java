package com.skishop.auth.repository;

import com.skishop.auth.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 処理済みイベントのリポジトリ
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    
    /**
     * イベントタイプで検索
     */
    List<ProcessedEvent> findByEventType(String eventType);
    
    /**
     * 成功/失敗で検索
     */
    List<ProcessedEvent> findBySuccess(boolean success);
    
    /**
     * 古い処理済みイベントを削除するためのクエリ
     */
    @Query("SELECT p FROM ProcessedEvent p WHERE p.createdAt < :cutoffTime")
    List<ProcessedEvent> findOldProcessedEvents(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * 特定期間の処理済みイベント数をカウント
     */
    @Query("SELECT COUNT(p) FROM ProcessedEvent p WHERE p.createdAt >= :startTime AND p.createdAt <= :endTime AND p.success = :success")
    long countByPeriodAndSuccess(@Param("startTime") Instant startTime, 
                                @Param("endTime") Instant endTime, 
                                @Param("success") boolean success);
}
