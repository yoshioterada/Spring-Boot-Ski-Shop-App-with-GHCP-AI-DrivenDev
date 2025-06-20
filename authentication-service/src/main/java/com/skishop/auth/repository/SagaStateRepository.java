package com.skishop.auth.repository;

import com.skishop.auth.entity.SagaState;
import com.skishop.auth.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Sagaの状態を管理するリポジトリ
 */
@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {
    
    /**
     * Saga IDでSagaを検索
     */
    Optional<SagaState> findBySagaId(String sagaId);
    
    /**
     * エンティティIDでSagaを検索
     */
    List<SagaState> findByEntityId(String entityId);
    
    /**
     * ユーザーIDとイベントタイプで検索
     */
    @Query("SELECT s FROM SagaState s WHERE s.userId = :userId AND s.eventType = :eventType")
    Optional<SagaState> findByUserIdAndEventType(@Param("userId") java.util.UUID userId, @Param("eventType") String eventType);
    
    /**
     * Sagaタイプと状態で検索
     */
    List<SagaState> findBySagaTypeAndState(String sagaType, String state);
    
    /**
     * 古いSagaを検索（タイムアウト処理用）
     */
    @Query("SELECT s FROM SagaState s WHERE s.createdAt < :cutoffTime AND s.state NOT IN ('SAGA_COMPLETED', 'SAGA_FAILED', 'SAGA_TIMEOUT')")
    List<SagaState> findStaleStates(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * アクティブなSagaの数をカウント
     */
    @Query("SELECT COUNT(s) FROM SagaState s WHERE s.state NOT IN ('SAGA_COMPLETED', 'SAGA_FAILED', 'SAGA_TIMEOUT')")
    long countActiveSagas();
    
    /**
     * 特定の状態のSagaを検索
     */
    List<SagaState> findByState(String state);
    
    /**
     * タイムアウトしたSagaを検索（補償処理用）
     */
    List<SagaState> findBySagaStatusAndTimeoutAtBefore(SagaStatus sagaStatus, Instant timeoutBefore);
    
    /**
     * 補償が必要なSagaを検索
     */
    @Query("SELECT s FROM SagaState s WHERE s.sagaStatus = :sagaStatus AND s.errorReason IS NOT NULL")
    List<SagaState> findBySagaStatusAndErrorReasonIsNotNull(@Param("sagaStatus") SagaStatus sagaStatus);
    
    /**
     * 指定されたステータス以外のSagaを検索
     */
    List<SagaState> findBySagaStatusNotIn(List<SagaStatus> statuses);
    
    /**
     * 指定時間以降に開始されたSagaを検索
     */
    List<SagaState> findByStartTimeAfter(Instant startTime);
    
    /**
     * 完了したSagaで終了時間があるものを検索
     */
    List<SagaState> findBySagaStatusAndEndTimeIsNotNull(SagaStatus sagaStatus);
}
