package com.skishop.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.auth.config.SkishopRuntimeProperties;
import com.skishop.auth.dto.EventDto;
import com.skishop.auth.entity.SagaState;
import com.skishop.auth.enums.SagaStatus;
import com.skishop.auth.enums.UserRegistrationStatus;
import com.skishop.auth.enums.UserDeletionStatus;
import com.skishop.auth.repository.SagaStateRepository;
import com.skishop.auth.service.compensation.CompensationService;
import com.skishop.auth.service.azure.AzureServiceBusEventPublisher;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * イベント発行サービス
 * 認証関連のイベントを他のマイクロサービスに通知
 */
@Service
@RequiredArgsConstructor
public class EventPublishingService {

    private final SkishopRuntimeProperties runtimeProperties;
    private final SagaStateRepository sagaStateRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CompensationService compensationService;
    
    // Manual log field since Lombok @Slf4j may not be working properly
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EventPublishingService.class);
    
    // Azure Service Bus Publisher - オプショナル（本番環境でのみ利用可能）
    @Autowired(required = false)
    private AzureServiceBusEventPublisher azureServiceBusEventPublisher;

    /**
     * ユーザー登録イベントを発行
     */
    @Transactional
    @Retry(name = "event-publishing")
    public String publishUserRegisteredEvent(UUID userId, String username, String email, String firstName, String lastName, String phoneNumber) {
        if (!runtimeProperties.isEventPropagationEnabled()) {
            log.debug("Event propagation is disabled, skipping user registration event for user {}", userId);
            return null;
        }

        String sagaId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        try {
            // Create saga state
            SagaState sagaState = SagaState.builder()
                .sagaId(sagaId)
                .eventType("USER_REGISTERED")
                .sagaType("USER_REGISTRATION")
                .userId(userId)
                .status(UserRegistrationStatus.PENDING_REGISTRATION.name())
                .sagaStatus(SagaStatus.SAGA_STARTED)
                .correlationId(correlationId)
                .originalEventId(eventId)
                .startTime(Instant.now())
                .timeoutAt(Instant.now().plusSeconds(runtimeProperties.getEventTimeoutMs() / 1000))
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            sagaStateRepository.save(sagaState);
            
            // Update status to ACCOUNT_CREATED (assuming account creation is successful)
            updateSagaStatus(sagaId, UserRegistrationStatus.ACCOUNT_CREATED, SagaStatus.SAGA_IN_PROGRESS);

            // Create event payload with complete schema
            Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "email", email,
                "firstName", firstName != null ? firstName : "",
                "lastName", lastName != null ? lastName : "",
                "phoneNumber", phoneNumber != null ? phoneNumber : "",
                "status", "PENDING_VERIFICATION",
                "createdAt", Instant.now().toString(),
                "additionalAttributes", Map.of("username", username)
            );
            
            EventDto event = EventDto.builder()
                .eventId(eventId)
                .eventType("USER_REGISTERED")
                .timestamp(Instant.now())
                .version("1.0")
                .producer("authentication-service")
                .payload(payload)
                .correlationId(correlationId)
                .sagaId(sagaId)
                .retry(0)
                .build();

            // Publish to Redis/message broker
            publishEventToRedis("USER_REGISTERED", event);
            
            // Update saga state to EVENT_PUBLISHED
            updateSagaStatus(sagaId, UserRegistrationStatus.EVENT_PUBLISHED, SagaStatus.SAGA_IN_PROGRESS);
            
            log.info("Successfully published user registration event for user {} with saga {}", userId, sagaId);
            return sagaId;
            
        } catch (Exception e) {
            log.error("Failed to publish user registration event for user {}: {}", userId, e.getMessage(), e);
            // Update saga state to FAILED
            updateSagaStatus(sagaId, UserRegistrationStatus.EVENT_PUBLISH_FAILED, SagaStatus.SAGA_FAILED);
            throw new RuntimeException("Failed to publish user registration event", e);
        }
    }

    /**
     * ユーザー削除イベントを発行
     */
    @Transactional
    @Retry(name = "event-publishing")
    public String publishUserDeletedEvent(UUID userId, String reason) {
        if (!runtimeProperties.isEventPropagationEnabled()) {
            log.debug("Event propagation is disabled, skipping user deletion event for user {}", userId);
            return null;
        }

        String sagaId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        
        try {
            // Create saga state
            SagaState sagaState = SagaState.builder()
                .sagaId(sagaId)
                .eventType("USER_DELETED")
                .sagaType("USER_DELETION")
                .userId(userId)
                .status(UserDeletionStatus.PENDING_DELETION.name())
                .sagaStatus(SagaStatus.SAGA_STARTED)
                .correlationId(correlationId)
                .originalEventId(eventId)
                .startTime(Instant.now())
                .timeoutAt(Instant.now().plusSeconds(runtimeProperties.getEventTimeoutMs() / 1000))
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            sagaStateRepository.save(sagaState);
            
            // Update status to DELETION_AUTHORIZED -> ACCOUNT_SOFT_DELETED
            updateSagaStatus(sagaId, UserDeletionStatus.DELETION_AUTHORIZED, SagaStatus.SAGA_IN_PROGRESS);
            updateSagaStatus(sagaId, UserDeletionStatus.ACCOUNT_SOFT_DELETED, SagaStatus.SAGA_IN_PROGRESS);

            // Create event payload with complete schema
            Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "reason", reason,
                "deletedAt", Instant.now().toString()
            );
            
            EventDto event = EventDto.builder()
                .eventId(eventId)
                .eventType("USER_DELETED")
                .timestamp(Instant.now())
                .version("1.0")
                .producer("authentication-service")
                .payload(payload)
                .correlationId(correlationId)
                .sagaId(sagaId)
                .retry(0)
                .build();

            // Publish to Redis/message broker
            publishEventToRedis("USER_DELETED", event);
            
            // Update saga state to DELETION_EVENT_PUBLISHED
            updateSagaStatus(sagaId, UserDeletionStatus.DELETION_EVENT_PUBLISHED, SagaStatus.SAGA_IN_PROGRESS);
            
            log.info("Successfully published user deletion event for user {} with saga {}", userId, sagaId);
            return sagaId;
            
        } catch (Exception e) {
            log.error("Failed to publish user deletion event for user {}: {}", userId, e.getMessage(), e);
            // Update saga state to FAILED
            updateSagaStatus(sagaId, UserDeletionStatus.DELETION_EVENT_PUBLISH_FAILED, SagaStatus.SAGA_FAILED);
            throw new RuntimeException("Failed to publish user deletion event", e);
        }
    }

    /**
     * イベントを適切なメッセージブローカーに発行
     */
    private void publishEventToRedis(String eventType, EventDto event) {
        try {
            // Azure Service Bus が有効で利用可能な場合は Azure Service Bus を使用
            if (isAzureServiceBusEnabled() && azureServiceBusEventPublisher != null) {
                azureServiceBusEventPublisher.publishEvent(event);
                log.debug("Published event to Azure Service Bus: eventType={}", eventType);
            } else if ("redis".equals(runtimeProperties.getEventBrokerType())) {
                // Redis を使用
                String channel = getRedisChannel(eventType);
                String eventJson = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(channel, eventJson);
                log.debug("Published event to Redis channel: {}", channel);
            } else {
                // フォールバック: Redis を使用
                log.warn("Event broker type {} not supported, using Redis fallback", 
                    runtimeProperties.getEventBrokerType());
                String channel = getRedisChannel(eventType);
                String eventJson = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(channel, eventJson);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish event: eventType={}, error={}", eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Azure Service Bus が有効かどうかを確認
     */
    private boolean isAzureServiceBusEnabled() {
        try {
            // Spring Profileと設定プロパティで判定
            return runtimeProperties.getEnvironment() != null && 
                   ("production".equals(runtimeProperties.getEnvironment()) || 
                    "staging".equals(runtimeProperties.getEnvironment()));
        } catch (Exception e) {
            log.debug("Azure Service Bus availability check failed", e);
            return false;
        }
    }

    /**
     * Redisチャンネル名を取得
     */
    private String getRedisChannel(String eventType) {
        return String.format("%s:events:%s", 
            runtimeProperties.getEventRedisKeyPrefix(), 
            eventType.toLowerCase());
    }

    /**
     * Saga状態とステータスを更新
     */
    private void updateSagaStatus(String sagaId, Enum<?> status, SagaStatus sagaStatus) {
        try {
            sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
                saga.setStatus(status.name());
                saga.setSagaStatus(sagaStatus);
                saga.setUpdatedAt(LocalDateTime.now());
                saga.setLastHeartbeat(Instant.now());
                
                if (sagaStatus == SagaStatus.SAGA_COMPLETED || sagaStatus == SagaStatus.SAGA_FAILED) {
                    saga.setEndTime(Instant.now());
                }
                
                sagaStateRepository.save(saga);
            });
        } catch (Exception e) {
            log.error("Failed to update saga status for {}: {}", sagaId, e.getMessage());
        }
    }
    
    /**
     * Saga状態を安全に更新（後方互換性のため）
     */
    private void updateSagaStateSafely(String sagaId, String status) {
        try {
            sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
                saga.setStatus(status);
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
            });
        } catch (Exception e) {
            log.error("Failed to update saga state for {}: {}", sagaId, e.getMessage());
        }
    }
    
    /**
     * 補償処理を実行
     */
    @Transactional
    public void compensateFailedSaga(String sagaId, String reason) {
        try {
            log.info("Starting compensation for saga: {} due to: {}", sagaId, reason);
            
            boolean success = compensationService.executeCompensation(sagaId, reason);
            
            if (success) {
                log.info("Compensation completed successfully for saga: {}", sagaId);
            } else {
                log.error("Compensation failed for saga: {}", sagaId);
            }
            
        } catch (Exception e) {
            log.error("Failed to execute compensation for saga {}: {}", sagaId, e.getMessage(), e);
            throw new RuntimeException("Compensation execution failed", e);
        }
    }

    /**
     * 汎用的なユーザーイベント発行メソッド（後方互換性のため）
     */
    @Transactional
    public String publishUserEvent(String eventType, UUID userId, String details) {
        if ("USER_REGISTERED".equals(eventType)) {
            // 基本的なユーザー登録イベントを発行
            return publishUserRegisteredEvent(userId, "unknown", "unknown@example.com", "Unknown", "User", null);
        } else if ("USER_DELETED".equals(eventType)) {
            return publishUserDeletedEvent(userId, details != null ? details : "User account deleted");
        } else {
            log.warn("Unknown event type: {}, skipping event publication", eventType);
            return null;
        }
    }

}