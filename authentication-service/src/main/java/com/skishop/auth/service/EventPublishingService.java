package com.skishop.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.auth.config.SkishopRuntimeProperties;
import com.skishop.auth.entity.SagaState;
import com.skishop.auth.repository.SagaStateRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * イベント発行サービス
 * 認証関連のイベントを他のマイクロサービスに通知
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublishingService {

    private final SkishopRuntimeProperties runtimeProperties;
    private final SagaStateRepository sagaStateRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * ユーザー登録イベントを発行
     */
    @Transactional
    @Retry(name = "event-publishing")
    public void publishUserRegisteredEvent(UUID userId, String username, String email) {
        if (!runtimeProperties.isEventPropagationEnabled()) {
            log.debug("Event propagation is disabled, skipping user registration event for user {}", userId);
            return;
        }

        String sagaId = UUID.randomUUID().toString();
        String eventType = "USER_REGISTERED";
        
        try {
            // Create saga state
            SagaState sagaState = new SagaState();
            sagaState.setSagaId(sagaId);
            sagaState.setEventType(eventType);
            sagaState.setUserId(userId);
            sagaState.setStatus("INITIATED");
            sagaState.setCreatedAt(LocalDateTime.now());
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            // Create event payload
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                .sagaId(sagaId)
                .userId(userId)
                .username(username)
                .email(email)
                .timestamp(System.currentTimeMillis())
                .source("authentication-service")
                .build();

            // Publish to Redis/message broker
            publishEventToRedis(eventType, event);
            
            // Update saga state to PUBLISHED
            sagaState.setStatus("PUBLISHED");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);
            
            log.info("Successfully published user registration event for user {} with saga {}", userId, sagaId);
            
        } catch (Exception e) {
            log.error("Failed to publish user registration event for user {}: {}", userId, e.getMessage(), e);
            // Update saga state to FAILED
            updateSagaStateSafely(sagaId, "FAILED");
            throw new RuntimeException("Failed to publish user registration event", e);
        }
    }

    /**
     * ユーザー削除イベントを発行
     */
    @Transactional
    @Retry(name = "event-publishing")
    public void publishUserDeletedEvent(UUID userId, String reason) {
        if (!runtimeProperties.isEventPropagationEnabled()) {
            log.debug("Event propagation is disabled, skipping user deletion event for user {}", userId);
            return;
        }

        String sagaId = UUID.randomUUID().toString();
        String eventType = "USER_DELETED";
        
        try {
            // Create saga state
            SagaState sagaState = new SagaState();
            sagaState.setSagaId(sagaId);
            sagaState.setEventType(eventType);
            sagaState.setUserId(userId);
            sagaState.setStatus("INITIATED");
            sagaState.setCreatedAt(LocalDateTime.now());
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);

            // Create event payload
            UserDeletedEvent event = UserDeletedEvent.builder()
                .sagaId(sagaId)
                .userId(userId)
                .reason(reason)
                .timestamp(System.currentTimeMillis())
                .source("authentication-service")
                .build();

            // Publish to Redis/message broker
            publishEventToRedis(eventType, event);
            
            // Update saga state to PUBLISHED
            sagaState.setStatus("PUBLISHED");
            sagaState.setUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);
            
            log.info("Successfully published user deletion event for user {} with saga {}", userId, sagaId);
            
        } catch (Exception e) {
            log.error("Failed to publish user deletion event for user {}: {}", userId, e.getMessage(), e);
            // Update saga state to FAILED
            updateSagaStateSafely(sagaId, "FAILED");
            throw new RuntimeException("Failed to publish user deletion event", e);
        }
    }

    /**
     * イベントをRedisに発行
     */
    private void publishEventToRedis(String eventType, Object event) {
        try {
            String channel = getRedisChannel(eventType);
            String eventJson = objectMapper.writeValueAsString(event);
            
            if (runtimeProperties.getEventBrokerType().equals("redis")) {
                redisTemplate.convertAndSend(channel, eventJson);
                log.debug("Published event to Redis channel: {}", channel);
            } else {
                // For other message brokers (Azure Service Bus, etc.)
                log.warn("Event broker type {} not implemented yet, using Redis fallback", 
                    runtimeProperties.getEventBrokerType());
                redisTemplate.convertAndSend(channel, eventJson);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish event to Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
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
     * Saga状態を安全に更新
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
     * ユーザー登録イベントのデータクラス
     */
    public static class UserRegisteredEvent {
        private String sagaId;
        private UUID userId;
        private String username;
        private String email;
        private long timestamp;
        private String source;

        public static UserRegisteredEventBuilder builder() {
            return new UserRegisteredEventBuilder();
        }

        public static class UserRegisteredEventBuilder {
            private String sagaId;
            private UUID userId;
            private String username;
            private String email;
            private long timestamp;
            private String source;

            public UserRegisteredEventBuilder sagaId(String sagaId) {
                this.sagaId = sagaId;
                return this;
            }

            public UserRegisteredEventBuilder userId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public UserRegisteredEventBuilder username(String username) {
                this.username = username;
                return this;
            }

            public UserRegisteredEventBuilder email(String email) {
                this.email = email;
                return this;
            }

            public UserRegisteredEventBuilder timestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public UserRegisteredEventBuilder source(String source) {
                this.source = source;
                return this;
            }

            public UserRegisteredEvent build() {
                UserRegisteredEvent event = new UserRegisteredEvent();
                event.sagaId = this.sagaId;
                event.userId = this.userId;
                event.username = this.username;
                event.email = this.email;
                event.timestamp = this.timestamp;
                event.source = this.source;
                return event;
            }
        }

        // Getters
        public String getSagaId() { return sagaId; }
        public UUID getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public long getTimestamp() { return timestamp; }
        public String getSource() { return source; }
    }

    /**
     * ユーザー削除イベントのデータクラス
     */
    public static class UserDeletedEvent {
        private String sagaId;
        private UUID userId;
        private String reason;
        private long timestamp;
        private String source;

        public static UserDeletedEventBuilder builder() {
            return new UserDeletedEventBuilder();
        }

        public static class UserDeletedEventBuilder {
            private String sagaId;
            private UUID userId;
            private String reason;
            private long timestamp;
            private String source;

            public UserDeletedEventBuilder sagaId(String sagaId) {
                this.sagaId = sagaId;
                return this;
            }

            public UserDeletedEventBuilder userId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public UserDeletedEventBuilder reason(String reason) {
                this.reason = reason;
                return this;
            }

            public UserDeletedEventBuilder timestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public UserDeletedEventBuilder source(String source) {
                this.source = source;
                return this;
            }

            public UserDeletedEvent build() {
                UserDeletedEvent event = new UserDeletedEvent();
                event.sagaId = this.sagaId;
                event.userId = this.userId;
                event.reason = this.reason;
                event.timestamp = this.timestamp;
                event.source = this.source;
                return event;
            }
        }

        // Getters
        public String getSagaId() { return sagaId; }
        public UUID getUserId() { return userId; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
        public String getSource() { return source; }
    }
}
