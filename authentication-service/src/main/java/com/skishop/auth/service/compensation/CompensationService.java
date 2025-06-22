package com.skishop.auth.service.compensation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.auth.entity.SagaState;
import com.skishop.auth.enums.SagaStatus;
import com.skishop.auth.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 補償処理を管理・実行するサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationService {

    private final SagaStateRepository sagaStateRepository;
    private final List<CompensationAction> compensationActions;
    private final ObjectMapper objectMapper;

    /**
     * 指定されたSagaの補償処理を実行
     */
    @Transactional
    public boolean executeCompensation(String sagaId, String failureReason) {
        try {
            Optional<SagaState> sagaOpt = sagaStateRepository.findBySagaId(sagaId);
            
            if (sagaOpt.isEmpty()) {
                log.error("Saga not found for compensation: {}", sagaId);
                return false;
            }

            SagaState saga = sagaOpt.get();
            
            // 補償処理開始の記録
            saga.setSagaStatus(SagaStatus.SAGA_COMPENSATING);
            saga.setErrorReason(failureReason);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);

            log.info("Starting compensation for saga: {} (type: {}, status: {})", 
                sagaId, saga.getSagaType(), saga.getStatus());

            // 補償コンテキストを構築
            CompensationContext context = buildCompensationContext(saga, failureReason);

            // 適用可能な補償アクションを検索・実行
            boolean allSuccessful = executeApplicableCompensations(saga, context);

            // 補償処理結果を記録
            if (allSuccessful) {
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATED);
                saga.setEndTime(Instant.now());
                log.info("Compensation completed successfully for saga: {}", sagaId);
            } else {
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATION_FAILED);
                log.error("Compensation failed for saga: {}", sagaId);
            }

            saga.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);

            return allSuccessful;

        } catch (Exception e) {
            log.error("Failed to execute compensation for saga {}: {}", sagaId, e.getMessage(), e);
            
            // 補償処理失敗を記録
            markCompensationFailed(sagaId, e.getMessage());
            return false;
        }
    }

    /**
     * 補償コンテキストを構築
     */
    private CompensationContext buildCompensationContext(SagaState saga, String failureReason) {
        Map<String, Object> sagaData = new HashMap<>();
        
        try {
            if (saga.getData() != null) {
                sagaData = objectMapper.readValue(saga.getData(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse saga data for compensation: {}", e.getMessage());
        }

        return CompensationContext.builder()
            .sagaId(saga.getSagaId())
            .sagaType(saga.getSagaType())
            .userId(saga.getUserId())
            .failureReason(failureReason)
            .currentStatus(saga.getStatus())
            .sagaData(sagaData)
            .errorTimestamp(System.currentTimeMillis())
            .retryCount(saga.getRetryCount())
            .build();
    }

    /**
     * 適用可能な補償アクションを実行
     */
    private boolean executeApplicableCompensations(SagaState saga, CompensationContext context) {
        // 優先度順でソート
        List<CompensationAction> applicableActions = compensationActions.stream()
            .filter(action -> action.isApplicable(saga.getSagaType(), saga.getStatus()))
            .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
            .toList();

        if (applicableActions.isEmpty()) {
            log.warn("No applicable compensation actions found for saga: {} (type: {}, status: {})", 
                saga.getSagaId(), saga.getSagaType(), saga.getStatus());
            return true; // 補償不要と判断
        }

        boolean allSuccessful = true;

        for (CompensationAction action : applicableActions) {
            try {
                log.info("Executing compensation action: {} for saga: {}", 
                    action.getActionName(), saga.getSagaId());

                boolean success = action.compensate(saga.getSagaId(), context);
                
                if (success) {
                    log.info("Compensation action {} completed successfully for saga: {}", 
                        action.getActionName(), saga.getSagaId());
                } else {
                    log.error("Compensation action {} failed for saga: {}", 
                        action.getActionName(), saga.getSagaId());
                    allSuccessful = false;
                }

            } catch (Exception e) {
                log.error("Exception in compensation action {} for saga {}: {}", 
                    action.getActionName(), saga.getSagaId(), e.getMessage(), e);
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    /**
     * 補償処理失敗を記録
     */
    private void markCompensationFailed(String sagaId, String errorMessage) {
        try {
            sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATION_FAILED);
                saga.setErrorReason("Compensation execution failed: " + errorMessage);
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
            });
        } catch (Exception e) {
            log.error("Failed to mark compensation as failed for saga {}: {}", sagaId, e.getMessage());
        }
    }

    /**
     * 補償が必要かどうかを判定
     */
    public boolean isCompensationRequired(String sagaType, String status) {
        return compensationActions.stream()
            .anyMatch(action -> action.isApplicable(sagaType, status));
    }

    /**
     * Sagaのタイムアウトをチェックし、必要に応じて補償処理を実行
     */
    @Transactional
    public void checkAndHandleTimeouts() {
        try {
            Instant now = Instant.now();
            
            // タイムアウトしたSagaを検索
            List<SagaState> timedOutSagas = sagaStateRepository.findBySagaStatusAndTimeoutAtBefore(
                SagaStatus.SAGA_IN_PROGRESS, now);
            
            for (SagaState saga : timedOutSagas) {
                log.warn("Saga timeout detected: {} (started: {}, timeout: {})", 
                    saga.getSagaId(), saga.getStartTime(), saga.getTimeoutAt());
                
                // タイムアウトステータスに更新
                saga.setSagaStatus(SagaStatus.SAGA_TIMEOUT);
                saga.setErrorReason("Saga execution timeout");
                saga.setEndTime(now);
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
                
                // 補償処理が必要かチェック
                if (isCompensationRequired(saga.getSagaType(), saga.getStatus())) {
                    executeCompensation(saga.getSagaId(), "Saga timeout");
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to check and handle timeouts: {}", e.getMessage(), e);
        }
    }
}
