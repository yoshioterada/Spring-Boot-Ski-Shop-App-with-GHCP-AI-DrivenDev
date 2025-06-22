package com.skishop.user.service.saga;

import com.skishop.user.entity.SagaTransaction;
import com.skishop.user.enums.SagaStatus;
import com.skishop.user.repository.SagaTransactionRepository;
import com.skishop.user.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sagaタイムアウト監視サービス
 * 定期的にタイムアウトしたSagaを検知し、適切な処理を実行
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaTimeoutMonitoringService {

    private final SagaTransactionRepository sagaRepository;
    private final MetricsService metricsService;
    private final SagaOrchestrator sagaOrchestrator;

    /**
     * タイムアウトSagaの監視と処理
     * 5分間隔で実行
     */
    @Scheduled(fixedRate = 300000) // 5分
    public void monitorTimeouts() {
        try {
            log.debug("Sagaタイムアウト監視開始");
            
            // 実際の処理はSagaOrchestratorのcheckAndProcessTimeoutSagas()に委譲
            // このメソッドが独立したトランザクションを開始するため、ここでは呼び出しのみ
            sagaOrchestrator.checkAndProcessTimeoutSagas();
            
            log.debug("Sagaタイムアウト監視完了");
            
        } catch (Exception e) {
            log.error("Sagaタイムアウト監視エラー", e);
        }
    }

    /**
     * リトライ可能なSagaの監視と再実行
     * 10分間隔で実行
     */
    @Scheduled(fixedRate = 600000) // 10分
    @Transactional
    public void monitorRetryableSagas() {
        try {
            log.debug("リトライ可能Saga監視開始");
            
            LocalDateTime currentTime = LocalDateTime.now();
            List<SagaTransaction> retryableSagas = sagaRepository.findRetryableSagas(
                SagaStatus.SAGA_STEP_FAILED, currentTime);
            
            if (!retryableSagas.isEmpty()) {
                log.info("リトライ可能Saga検出: count={}", retryableSagas.size());
                
                for (SagaTransaction saga : retryableSagas) {
                    handleRetryableSaga(saga);
                }
            }
            
            log.debug("リトライ可能Saga監視完了: processed={}", retryableSagas.size());
            
        } catch (Exception e) {
            log.error("リトライ可能Saga監視エラー", e);
        }
    }

    /**
     * 古いSagaトランザクションのクリーンアップ
     * 1日1回実行
     */
    @Scheduled(cron = "0 0 2 * * ?") // 毎日2時
    @Transactional
    public void cleanupOldSagas() {
        try {
            log.info("古いSagaクリーンアップ開始");
            
            // 30日前より古い完了済みSagaを削除
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            List<SagaStatus> terminalStatuses = List.of(
                SagaStatus.SAGA_COMPLETED,
                SagaStatus.SAGA_FAILED,
                SagaStatus.SAGA_COMPENSATION_FAILED,
                SagaStatus.SAGA_TIMEOUT
            );

            List<SagaTransaction> oldSagas = sagaRepository.findOldCompletedSagas(cutoffTime, terminalStatuses);
            
            if (!oldSagas.isEmpty()) {
                log.info("古いSaga削除対象: count={}", oldSagas.size());
                sagaRepository.deleteAll(oldSagas);
                log.info("古いSaga削除完了: count={}", oldSagas.size());
            }
            
        } catch (Exception e) {
            log.error("古いSagaクリーンアップエラー", e);
        }
    }

    /**
     * Sagaメトリクス更新
     * 1分間隔で実行
     */
    @Scheduled(fixedRate = 60000) // 1分
    public void updateSagaMetrics() {
        try {
            List<SagaStatus> activeStatuses = List.of(
                SagaStatus.SAGA_STARTED,
                SagaStatus.SAGA_IN_PROGRESS,
                SagaStatus.SAGA_COMPENSATING
            );

            Long activeSagaCount = sagaRepository.countActiveSagas(activeStatuses);
            
            // メトリクス更新（AtomicLong値を直接更新）
            long currentActive = metricsService.getActiveSagaCount();
            if (currentActive != activeSagaCount) {
                log.debug("アクティブSaga数更新: {} -> {}", currentActive, activeSagaCount);
            }
            
        } catch (Exception e) {
            log.error("Sagaメトリクス更新エラー", e);
        }
    }

    /**
     * タイムアウトしたSagaの処理
     */
    private void handleTimeoutSaga(SagaTransaction saga) {
        try {
            log.warn("Sagaタイムアウト処理: sagaId={}, eventType={}, currentStep={}", 
                     saga.getSagaId(), saga.getEventType(), saga.getCurrentStep());

            saga.setStatus(SagaStatus.SAGA_TIMEOUT);
            saga.setErrorType("TIMEOUT");
            saga.setErrorMessage("Sagaタイムアウト: " + saga.getCurrentStep());
            saga.markProcessingEnd();
            
            sagaRepository.save(saga);

            // メトリクス記録
            metricsService.recordSagaCompleted(saga.getEventType(), saga.getProcessingTimeMs(), false);
            
            log.info("Sagaタイムアウト処理完了: sagaId={}", saga.getSagaId());

        } catch (Exception e) {
            log.error("Sagaタイムアウト処理エラー: sagaId={}", saga.getSagaId(), e);
        }
    }

    /**
     * リトライ可能なSagaの処理
     */
    private void handleRetryableSaga(SagaTransaction saga) {
        try {
            if (!saga.canRetry()) {
                log.warn("Sagaリトライ不可: sagaId={}, retryCount={}/{}", 
                         saga.getSagaId(), saga.getRetryCount(), saga.getMaxRetryCount());
                return;
            }

            log.info("Sagaリトライ実行: sagaId={}, retryCount={}/{}", 
                     saga.getSagaId(), saga.getRetryCount() + 1, saga.getMaxRetryCount());

            saga.incrementRetryCount();
            saga.setStatus(SagaStatus.SAGA_IN_PROGRESS);
            saga.setErrorMessage(null);
            saga.setErrorType(null);
            
            sagaRepository.save(saga);

            // リトライ処理をSagaOrchestratorに委譲
            sagaOrchestrator.retrySaga(saga);
            
            log.info("Sagaリトライ設定完了: sagaId={}", saga.getSagaId());

        } catch (Exception e) {
            log.error("Sagaリトライ処理エラー: sagaId={}", saga.getSagaId(), e);
        }
    }

    /**
     * Saga統計情報の出力
     * 1時間間隔で実行
     */
    @Scheduled(fixedRate = 3600000) // 1時間
    public void logSagaStatistics() {
        try {
            List<Object[]> statistics = sagaRepository.findStatisticsByEventTypeAndStatus();
            
            log.info("=== Saga統計情報 ===");
            for (Object[] stat : statistics) {
                String eventType = (String) stat[0];
                SagaStatus status = (SagaStatus) stat[1];
                Long count = (Long) stat[2];
                
                log.info("EventType: {}, Status: {}, Count: {}", eventType, status, count);
            }
            log.info("==================");
            
        } catch (Exception e) {
            log.error("Saga統計情報出力エラー", e);
        }
    }
}
