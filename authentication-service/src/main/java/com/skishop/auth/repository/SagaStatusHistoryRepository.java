package com.skishop.auth.repository;

import com.skishop.auth.entity.SagaStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sagaステータス履歴のリポジトリ
 */
@Repository
public interface SagaStatusHistoryRepository extends JpaRepository<SagaStatusHistory, Long> {
    
    /**
     * 指定されたSagaIDの履歴を時系列順で取得
     */
    List<SagaStatusHistory> findBySagaIdOrderByTransitionTimeAsc(String sagaId);
    
    /**
     * 指定期間内の状態遷移履歴を取得
     */
    List<SagaStatusHistory> findByTransitionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * エラーが発生した状態遷移を取得
     */
    List<SagaStatusHistory> findByErrorMessageIsNotNullOrderByTransitionTimeDesc();
    
    /**
     * 指定されたステータスへの遷移履歴を取得
     */
    List<SagaStatusHistory> findByToStatusOrderByTransitionTimeDesc(String toStatus);
    
    /**
     * Sagaの状態遷移統計を取得
     */
    @Query("SELECT h.toStatus, COUNT(h) FROM SagaStatusHistory h WHERE h.transitionTime >= :since GROUP BY h.toStatus")
    List<Object[]> getStatusTransitionStats(@Param("since") LocalDateTime since);
}
