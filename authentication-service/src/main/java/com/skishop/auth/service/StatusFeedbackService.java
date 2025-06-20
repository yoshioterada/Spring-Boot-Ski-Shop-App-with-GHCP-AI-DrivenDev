package com.skishop.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.auth.dto.EventDto;
import com.skishop.auth.entity.SagaState;
import com.skishop.auth.enums.SagaStatus;
import com.skishop.auth.enums.UserRegistrationStatus;
import com.skishop.auth.enums.UserDeletionStatus;
import com.skishop.auth.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * User-management-serviceからのステータス応答を受信・処理するサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatusFeedbackService implements MessageListener {

    private final SagaStateRepository sagaStateRepository;
    private final EventPublishingService eventPublishingService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            
            log.info("Received status feedback from channel: {}, body: {}", channel, body);
            
            // チャンネル名からイベントタイプを取得
            String eventType = extractEventTypeFromChannel(channel);
            
            if ("user_management_status".equals(eventType)) {
                handleUserManagementStatusEvent(body);
            } else {
                log.warn("Unknown status event type: {} from channel: {}", eventType, channel);
            }
            
        } catch (Exception e) {
            log.error("Failed to process status feedback: {}", e.getMessage(), e);
        }
    }

    /**
     * User-management-serviceからのステータス応答を処理
     */
    @Transactional
    public void handleUserManagementStatusEvent(String eventJson) {
        try {
            EventDto event = objectMapper.readValue(eventJson, EventDto.class);
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String sagaId = event.getSagaId();
            String originalEventId = (String) payload.get("originalEventId");
            String status = (String) payload.get("status");
            String reason = (String) payload.get("reason");
            Long processingTime = payload.get("processingTime") != null ? 
                ((Number) payload.get("processingTime")).longValue() : null;
            
            log.info("Processing status feedback for saga: {}, status: {}", sagaId, status);

            // Saga状態を更新
            sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
                updateSagaBasedOnStatus(saga, status, reason, processingTime);
            });
            
            log.info("Successfully processed status feedback for saga: {}", sagaId);
            
        } catch (Exception e) {
            log.error("Failed to handle user management status event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process status feedback", e);
        }
    }

    /**
     * ステータスに基づいてSaga状態を更新
     */
    private void updateSagaBasedOnStatus(SagaState saga, String status, String reason, Long processingTime) {
        try {
            if ("SUCCESS".equals(status)) {
                handleSuccessStatus(saga, processingTime);
            } else if ("FAILED".equals(status)) {
                handleFailedStatus(saga, reason, processingTime);
            } else {
                log.warn("Unknown status received: {} for saga: {}", status, saga.getSagaId());
            }
        } catch (Exception e) {
            log.error("Failed to update saga status for {}: {}", saga.getSagaId(), e.getMessage(), e);
        }
    }

    /**
     * 成功ステータスの処理
     */
    private void handleSuccessStatus(SagaState saga, Long processingTime) {
        if ("USER_REGISTRATION".equals(saga.getSagaType())) {
            saga.setStatus(UserRegistrationStatus.REGISTRATION_COMPLETED.name());
            saga.setSagaStatus(SagaStatus.SAGA_COMPLETED);
        } else if ("USER_DELETION".equals(saga.getSagaType())) {
            saga.setStatus(UserDeletionStatus.DELETION_COMPLETED.name());
            saga.setSagaStatus(SagaStatus.SAGA_COMPLETED);
        }
        
        saga.setEndTime(Instant.now());
        saga.setUpdatedAt(LocalDateTime.now());
        
        // 処理時間を記録（オプション）
        if (processingTime != null) {
            saga.setData(String.format("{\"processingTime\": %d}", processingTime));
        }
        
        sagaStateRepository.save(saga);
        
        log.info("Saga completed successfully: {} (type: {})", saga.getSagaId(), saga.getSagaType());
    }

    /**
     * 失敗ステータスの処理
     */
    private void handleFailedStatus(SagaState saga, String reason, Long processingTime) {
        if ("USER_REGISTRATION".equals(saga.getSagaType())) {
            saga.setStatus(UserRegistrationStatus.REGISTRATION_FAILED.name());
            saga.setSagaStatus(SagaStatus.SAGA_FAILED);
            
            // 補償処理が必要かどうかを判断
            if (shouldTriggerCompensation(saga)) {
                saga.setStatus(UserRegistrationStatus.COMPENSATION_REQUIRED.name());
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATING);
                
                // 補償処理を非同期で実行
                triggerCompensationAsync(saga.getSagaId(), reason);
            }
            
        } else if ("USER_DELETION".equals(saga.getSagaType())) {
            saga.setStatus(UserDeletionStatus.DELETION_FAILED.name());
            saga.setSagaStatus(SagaStatus.SAGA_FAILED);
            
            // 削除の巻き戻しが必要かどうかを判断
            if (shouldTriggerRollback(saga)) {
                saga.setStatus(UserDeletionStatus.DELETION_ROLLBACK_REQUIRED.name());
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATING);
                
                // 巻き戻し処理を非同期で実行
                triggerRollbackAsync(saga.getSagaId(), reason);
            }
        }
        
        saga.setErrorReason(reason);
        saga.setEndTime(Instant.now());
        saga.setUpdatedAt(LocalDateTime.now());
        
        sagaStateRepository.save(saga);
        
        log.warn("Saga failed: {} (type: {}, reason: {})", saga.getSagaId(), saga.getSagaType(), reason);
    }

    /**
     * 補償処理が必要かどうかを判断
     */
    private boolean shouldTriggerCompensation(SagaState saga) {
        // 既にアカウントが作成されている場合は補償処理が必要
        return UserRegistrationStatus.ACCOUNT_CREATED.name().equals(saga.getStatus()) ||
               UserRegistrationStatus.EVENT_PUBLISHED.name().equals(saga.getStatus());
    }

    /**
     * 巻き戻し処理が必要かどうかを判断
     */
    private boolean shouldTriggerRollback(SagaState saga) {
        // 既にアカウントが論理削除されている場合は巻き戻しが必要
        return UserDeletionStatus.ACCOUNT_SOFT_DELETED.name().equals(saga.getStatus()) ||
               UserDeletionStatus.DELETION_EVENT_PUBLISHED.name().equals(saga.getStatus());
    }

    /**
     * 補償処理を非同期で実行
     */
    private void triggerCompensationAsync(String sagaId, String reason) {
        // 実際の実装では@Async メソッドを使用して非同期実行
        try {
            eventPublishingService.compensateFailedSaga(sagaId, reason);
        } catch (Exception e) {
            log.error("Failed to trigger compensation for saga {}: {}", sagaId, e.getMessage(), e);
        }
    }

    /**
     * 巻き戻し処理を非同期で実行
     */
    private void triggerRollbackAsync(String sagaId, String reason) {
        try {
            // ユーザーアカウントの復旧処理
            rollbackUserDeletion(sagaId, reason);
        } catch (Exception e) {
            log.error("Failed to trigger rollback for saga {}: {}", sagaId, e.getMessage(), e);
        }
    }

    /**
     * ユーザー削除の巻き戻し処理
     */
    private void rollbackUserDeletion(String sagaId, String reason) {
        sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
            try {
                // 実際のロールバック処理（例：ユーザーアカウントの復旧）
                // この部分は具体的なビジネスロジックに依存
                
                saga.setStatus(UserDeletionStatus.DELETION_ROLLED_BACK.name());
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATED);
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
                
                log.info("Rollback completed for saga: {}", sagaId);
                
            } catch (Exception e) {
                saga.setSagaStatus(SagaStatus.SAGA_COMPENSATION_FAILED);
                saga.setErrorReason("Rollback failed: " + e.getMessage());
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
                
                log.error("Rollback failed for saga {}: {}", sagaId, e.getMessage(), e);
            }
        });
    }

    /**
     * チャンネル名からイベントタイプを抽出
     */
    private String extractEventTypeFromChannel(String channel) {
        // チャンネル形式: "skishop:events:user_management_status"
        String[] parts = channel.split(":");
        return parts.length >= 3 ? parts[2] : "unknown";
    }
    
    /**
     * Azure Service Busからのステータスフィードバック処理
     * AzureServiceBusStatusFeedbackReceiverから呼び出される
     */
    public void processStatusFeedback(EventDto eventDto) {
        try {
            log.info("Processing status feedback event: eventType={}, sagaId={}", 
                eventDto.getEventType(), eventDto.getSagaId());
                
            if (eventDto.getPayload() != null) {
                String eventJson = objectMapper.writeValueAsString(eventDto.getPayload());
                handleUserManagementStatusEvent(eventJson);
            } else {
                log.warn("Received status feedback event with null payload: eventId={}", eventDto.getEventId());
            }
            
        } catch (Exception e) {
            log.error("Failed to process status feedback: eventId={}, error={}", 
                eventDto.getEventId(), e.getMessage(), e);
        }
    }
}
