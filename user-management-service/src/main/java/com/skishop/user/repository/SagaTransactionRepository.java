package com.skishop.user.repository;

import com.skishop.user.entity.SagaTransaction;
import com.skishop.user.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SagaTransactionリポジトリ
 * Sagaパターンの状態管理とクエリ操作
 */
@Repository
public interface SagaTransactionRepository extends JpaRepository<SagaTransaction, String> {

    /**
     * 相関IDでSagaトランザクションを検索
     */
    Optional<SagaTransaction> findByCorrelationId(String correlationId);

    /**
     * 元のイベントIDでSagaトランザクションを検索
     */
    Optional<SagaTransaction> findByOriginalEventId(String originalEventId);

    /**
     * ユーザーIDでSagaトランザクションを検索
     */
    List<SagaTransaction> findByUserId(String userId);

    /**
     * ステータスでSagaトランザクションを検索
     */
    List<SagaTransaction> findByStatus(SagaStatus status);

    /**
     * 特定のステータス一覧でSagaトランザクションを検索
     */
    List<SagaTransaction> findByStatusIn(List<SagaStatus> statuses);

    /**
     * タイムアウトしたSagaトランザクションを検索
     */
    @Query("SELECT s FROM SagaTransaction s WHERE s.timeoutAt < :currentTime AND s.status NOT IN :terminalStatuses")
    List<SagaTransaction> findTimedOutSagas(
        @Param("currentTime") LocalDateTime currentTime,
        @Param("terminalStatuses") List<SagaStatus> terminalStatuses
    );

    /**
     * リトライ対象のSagaトランザクションを検索
     */
    @Query("SELECT s FROM SagaTransaction s WHERE s.status = :status AND s.retryCount < s.maxRetryCount AND (s.timeoutAt IS NULL OR s.timeoutAt > :currentTime)")
    List<SagaTransaction> findRetryableSagas(
        @Param("status") SagaStatus status,
        @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * 特定の期間のSagaトランザクションを検索
     */
    @Query("SELECT s FROM SagaTransaction s WHERE s.createdAt BETWEEN :startTime AND :endTime")
    List<SagaTransaction> findByCreatedAtBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * イベントタイプ別の統計情報取得
     */
    @Query("SELECT s.eventType, s.status, COUNT(s) FROM SagaTransaction s GROUP BY s.eventType, s.status")
    List<Object[]> findStatisticsByEventTypeAndStatus();

    /**
     * 実行中のSaga数をカウント
     */
    @Query("SELECT COUNT(s) FROM SagaTransaction s WHERE s.status IN :activeStatuses")
    Long countActiveSagas(@Param("activeStatuses") List<SagaStatus> activeStatuses);

    /**
     * 平均処理時間を計算
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (s.processingEndTime - s.processingStartTime)) * 1000) FROM SagaTransaction s WHERE s.processingStartTime IS NOT NULL AND s.processingEndTime IS NOT NULL AND s.eventType = :eventType")
    Double findAverageProcessingTimeByEventType(@Param("eventType") String eventType);

    /**
     * ユーザーIDとイベントタイプで最新のSagaを検索
     */
    Optional<SagaTransaction> findTopByUserIdAndEventTypeOrderByCreatedAtDesc(String userId, String eventType);

    /**
     * 古いSagaトランザクションをクリーンアップ用に検索
     */
    @Query("SELECT s FROM SagaTransaction s WHERE s.createdAt < :cutoffTime AND s.status IN :terminalStatuses")
    List<SagaTransaction> findOldCompletedSagas(
        @Param("cutoffTime") LocalDateTime cutoffTime,
        @Param("terminalStatuses") List<SagaStatus> terminalStatuses
    );

    /**
     * 失敗したSagaの詳細情報を検索
     */
    @Query("SELECT s FROM SagaTransaction s WHERE s.status IN :failureStatuses AND s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<SagaTransaction> findRecentFailedSagas(
        @Param("failureStatuses") List<SagaStatus> failureStatuses,
        @Param("since") LocalDateTime since
    );
}
