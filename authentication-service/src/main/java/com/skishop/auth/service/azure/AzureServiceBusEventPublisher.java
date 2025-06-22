package com.skishop.auth.service.azure;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.auth.dto.EventDto;
import com.skishop.auth.exception.EventSerializationException;
import com.skishop.auth.exception.EventPublishingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure Service Bus イベント発行サービス
 * 
 * 本番環境でのイベント伝播にAzure Service Busを使用
 * Managedアイデンティティによるセキュアな認証
 * 自動リトライ、デッドレターキュー対応
 */
@Service
@ConditionalOnProperty(
    prefix = "skishop.runtime.azure-servicebus", 
    name = "enabled", 
    havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class AzureServiceBusEventPublisher {

    private final ServiceBusSenderClient eventSenderClient;
    private final ObjectMapper objectMapper;

    /**
     * イベントをAzure Service Busトピックに発行
     * 
     * @param event 発行するイベント
     * @throws RuntimeException 発行に失敗した場合
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishEvent(EventDto event) {
        try {
            log.debug("Publishing event to Azure Service Bus: eventId={}, eventType={}, sagaId={}", 
                event.getEventId(), event.getEventType(), event.getSagaId());

            // イベントをJSONにシリアライズ
            String jsonPayload = objectMapper.writeValueAsString(event);
            
            // Service Busメッセージを作成
            ServiceBusMessage message = new ServiceBusMessage(jsonPayload)
                .setContentType("application/json")
                .setCorrelationId(event.getCorrelationId())
                .setMessageId(event.getEventId())
                .setTimeToLive(Duration.ofMinutes(30)); // 30分でタイムアウト

            // メッセージプロパティを設定（フィルタリング用）
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventType", event.getEventType());
            properties.put("producer", event.getProducer());
            properties.put("sagaId", event.getSagaId());
            properties.put("version", event.getVersion());
            
            message.getApplicationProperties().putAll(properties);

            // メッセージを送信
            eventSenderClient.sendMessage(message);
            
            log.info("Successfully published event to Azure Service Bus: eventId={}, eventType={}", 
                event.getEventId(), event.getEventType());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON: eventId={}", event.getEventId(), e);
            throw new EventSerializationException("Event serialization failed", e);
        } catch (Exception e) {
            log.error("Failed to publish event to Azure Service Bus: eventId={}, eventType={}", 
                event.getEventId(), event.getEventType(), e);
            throw new EventPublishingException("Event publishing failed", e);
        }
    }

    /**
     * バッチでイベントを発行（パフォーマンス向上）
     * 
     * @param events 発行するイベントのリスト
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishEvents(Iterable<EventDto> events) {
        try {
            log.debug("Publishing batch of events to Azure Service Bus");

            for (EventDto event : events) {
                publishEvent(event);
            }
            
            log.info("Successfully published batch of events to Azure Service Bus");

        } catch (Exception e) {
            log.error("Failed to publish batch of events to Azure Service Bus", e);
            throw new EventPublishingException("Batch event publishing failed", e);
        }
    }

    /**
     * Service Bus接続の健全性をチェック
     * 
     * @return 接続が健全な場合はtrue
     */
    public boolean isHealthy() {
        try {
            // 軽量なヘルステストメッセージを送信
            ServiceBusMessage testMessage = new ServiceBusMessage("health-check")
                .setContentType("text/plain")
                .setTimeToLive(Duration.ofSeconds(10));
            
            eventSenderClient.sendMessage(testMessage);
            return true;
        } catch (Exception e) {
            log.warn("Azure Service Bus health check failed", e);
            return false;
        }
    }
}
