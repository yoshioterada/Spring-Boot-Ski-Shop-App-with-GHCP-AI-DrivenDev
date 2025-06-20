package com.skishop.auth.service.scheduler;

import com.skishop.auth.service.compensation.CompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Sagaのタイムアウト監視とクリーンアップを行うスケジューラ
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "skishop.runtime.event-propagation-enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class SagaTimeoutScheduler {

    private final CompensationService compensationService;

    /**
     * 30秒ごとにタイムアウトしたSagaをチェック
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void checkSagaTimeouts() {
        try {
            log.debug("Checking for timed out sagas...");
            compensationService.checkAndHandleTimeouts();
        } catch (Exception e) {
            log.error("Error during saga timeout check: {}", e.getMessage(), e);
        }
    }

    /**
     * 5分ごとにSagaの健全性をチェック
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void performSagaHealthCheck() {
        try {
            log.debug("Performing saga health check...");
            // 追加の健全性チェックロジックをここに実装
            // 例：長時間stuckしているSaga、異常な状態のSagaなど
        } catch (Exception e) {
            log.error("Error during saga health check: {}", e.getMessage(), e);
        }
    }
}
