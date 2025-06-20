package com.skishop.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.config.SkishopRuntimeProperties;
import com.skishop.user.dto.EventDto;
import com.skishop.user.service.azure.AzureServiceBusStatusFeedbackPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication-serviceへのステータスフィードバックを送信するサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatusFeedbackPublishingService {

    private final SkishopRuntimeProperties runtimeProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Azure Service Bus Publisher - オプショナル（本番環境でのみ利用可能）
    @Autowired(required = false)
    private AzureServiceBusStatusFeedbackPublisher azureServiceBusStatusFeedbackPublisher;

    /**
     * 処理成功ステータスを送信
     */
    @Transactional
    public void publishSuccessStatus(String sagaId, String userId, String originalEventId, long processingTimeMs) {
        publishStatusFeedback(sagaId, userId, originalEventId, "SUCCESS", null, processingTimeMs);
    }

    /**
     * 処理失敗ステータスを送信
     */
    @Transactional
    public void publishFailureStatus(String sagaId, String userId, String originalEventId, String reason, long processingTimeMs) {
        publishStatusFeedback(sagaId, userId, originalEventId, "FAILED", reason, processingTimeMs);
    }

    /**
     * ステータスフィードバックイベントを発行
     */
    private void publishStatusFeedback(String sagaId, String userId, String originalEventId, String status, String reason, long processingTimeMs) {
        if (!runtimeProperties.isEventPropagationEnabled()) {
            log.debug("Event propagation is disabled, skipping status feedback for saga {}", sagaId);
            return;
        }

        try {
            String eventId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();

            // Create status feedback payload
            Map<String, Object> payload = Map.of(
                "userId", userId,
                "originalEventId", originalEventId,
                "status", status,
                "reason", reason != null ? reason : "",
                "processingTime", processingTimeMs
            );

            // Create event with complete schema
            EventDto event = EventDto.builder()
                .eventId(eventId)
                .eventType("USER_MANAGEMENT_STATUS")
                .timestamp(Instant.now())
                .version("1.0")
                .producer("user-management-service")
                .payload(payload)
                .correlationId(correlationId)
                .sagaId(sagaId)
                .retry(0)
                .build();

            // Publish to Redis/message broker
            publishEventToRedis("USER_MANAGEMENT_STATUS", event);

            log.info("Successfully published status feedback for saga {}: {}", sagaId, status);

        } catch (Exception e) {
            log.error("Failed to publish status feedback for saga {}: {}", sagaId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish status feedback", e);
        }
    }

    /**
     * イベントを適切なメッセージブローカーに発行
     */
    private void publishEventToRedis(String eventType, EventDto event) {
        try {
            // Azure Service Bus が有効で利用可能な場合は Azure Service Bus を使用
            if (isAzureServiceBusEnabled() && azureServiceBusStatusFeedbackPublisher != null) {
                azureServiceBusStatusFeedbackPublisher.publishStatusFeedback(event);
                log.debug("Published status feedback to Azure Service Bus: eventType={}", eventType);
            } else if ("redis".equals(runtimeProperties.getEventBrokerType())) {
                // Redis を使用
                String channel = getRedisChannel(eventType);
                String eventJson = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(channel, eventJson);
                log.debug("Published status feedback to Redis channel: {}", channel);
            } else {
                // フォールバック: Redis を使用
                log.warn("Event broker type {} not supported, using Redis fallback", 
                    runtimeProperties.getEventBrokerType());
                String channel = getRedisChannel(eventType);
                String eventJson = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(channel, eventJson);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish status feedback: eventType={}, error={}", eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to publish status feedback", e);
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
                redisTemplate.convertAndSend(channel, eventJson);
            }

        } catch (Exception e) {
            log.error("Failed to publish status feedback to Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish status feedback", e);
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
}
