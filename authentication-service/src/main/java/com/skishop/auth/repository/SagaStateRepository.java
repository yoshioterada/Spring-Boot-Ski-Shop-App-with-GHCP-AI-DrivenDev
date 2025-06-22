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
    
    // 監視機能用のメソッド
    
    /**
     * ステータス別カウント
     */
    long countByStatus(String status);
    
    /**
     * Sagaステータス別カウント
     */
    long countBySagaStatus(SagaStatus sagaStatus);
    
    /**
     * Sagaタイプと開始時間で検索
     */
    List<SagaState> findBySagaTypeAndStartTimeAfter(String sagaType, Instant startTime);
    
    /**
     * Sagaタイプで検索（ページング対応）
     */
    org.springframework.data.domain.Page<SagaState> findBySagaType(String sagaType, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Sagaステータスで検索（ページング対応）
     */
    org.springframework.data.domain.Page<SagaState> findBySagaStatus(SagaStatus sagaStatus, org.springframework.data.domain.Pageable pageable);
    
    /**
     * SagaタイプとSagaステータスで検索（ページング対応）
     */
    org.springframework.data.domain.Page<SagaState> findBySagaTypeAndSagaStatus(String sagaType, SagaStatus sagaStatus, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 指定された複数のSagaステータスと最終ハートビート時間で検索
     */
    List<SagaState> findBySagaStatusInAndLastHeartbeatBefore(List<SagaStatus> sagaStatuses, Instant lastHeartbeat);
    
    /**
     * 作成時間以降と特定のSagaステータスでカウント
     */
    long countByCreatedAtAfterAndSagaStatus(java.time.LocalDateTime createdAt, SagaStatus sagaStatus);
    
    /**
     * 指定された時刻以降に完了したSagaの数を取得
     */
    @Query("SELECT COUNT(s) FROM SagaState s WHERE s.sagaStatus = 'SAGA_COMPLETED' AND s.endTime >= :since")
    long countCompletedAfter(@Param("since") Instant since);
    
    /**
     * 指定された時刻以降に失敗したSagaの数を取得
     */
    @Query("SELECT COUNT(s) FROM SagaState s WHERE s.sagaStatus = 'SAGA_FAILED' AND s.endTime >= :since")
    long countFailedAfter(@Param("since") Instant since);
    
    /**
     * 平均実行時間を取得（ミリ秒）
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (s.endTime - s.startTime)) * 1000) FROM SagaState s WHERE s.endTime IS NOT NULL AND s.startTime IS NOT NULL")
    Double getAverageExecutionDuration();
}
