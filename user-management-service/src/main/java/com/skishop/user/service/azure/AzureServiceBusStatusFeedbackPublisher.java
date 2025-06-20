package com.skishop.user.service.azure;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.dto.EventDto;
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
 * Azure Service Bus ステータスフィードバック送信サービス
 * 
 * 認証サービスへのステータス更新をAzure Service Busで送信
 * 自動リトライとエラーハンドリング機能付き
 */
@Service
@ConditionalOnProperty(
    prefix = "skishop.runtime.azure-servicebus", 
    name = "enabled", 
    havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class AzureServiceBusStatusFeedbackPublisher {

    private final ServiceBusSenderClient statusFeedbackSenderClient;
    private final ObjectMapper objectMapper;

    /**
     * ステータスフィードバックをAzure Service Busトピックに送信
     * 
     * @param statusEvent 送信するステータスイベント
     * @throws RuntimeException 送信に失敗した場合
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishStatusFeedback(EventDto statusEvent) {
        try {
            log.debug("Publishing status feedback to Azure Service Bus: eventId={}, eventType={}, sagaId={}", 
                statusEvent.getEventId(), statusEvent.getEventType(), statusEvent.getSagaId());

            // イベントをJSONにシリアライズ
            String jsonPayload = objectMapper.writeValueAsString(statusEvent);
            
            // Service Busメッセージを作成
            ServiceBusMessage message = new ServiceBusMessage(jsonPayload)
                .setContentType("application/json")
                .setCorrelationId(statusEvent.getCorrelationId())
                .setMessageId(statusEvent.getEventId())
                .setTimeToLive(Duration.ofMinutes(15)); // 15分でタイムアウト

            // メッセージプロパティを設定（フィルタリング用）
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventType", statusEvent.getEventType());
            properties.put("producer", statusEvent.getProducer());
            properties.put("sagaId", statusEvent.getSagaId());
            properties.put("feedbackType", "status");
            properties.put("version", statusEvent.getVersion());
            
            message.getApplicationProperties().putAll(properties);

            // メッセージを送信
            statusFeedbackSenderClient.sendMessage(message);
            
            log.info("Successfully published status feedback to Azure Service Bus: eventId={}, eventType={}", 
                statusEvent.getEventId(), statusEvent.getEventType());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize status feedback to JSON: eventId={}", statusEvent.getEventId(), e);
            throw new RuntimeException("Status feedback serialization failed", e);
        } catch (Exception e) {
            log.error("Failed to publish status feedback to Azure Service Bus: eventId={}, eventType={}", 
                statusEvent.getEventId(), statusEvent.getEventType(), e);
            throw new RuntimeException("Status feedback publishing failed", e);
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
            ServiceBusMessage testMessage = new ServiceBusMessage("status-feedback-health-check")
                .setContentType("text/plain")
                .setTimeToLive(Duration.ofSeconds(10));
            
            statusFeedbackSenderClient.sendMessage(testMessage);
            return true;
        } catch (Exception e) {
            log.warn("Azure Service Bus status feedback health check failed", e);
            return false;
        }
    }
}
