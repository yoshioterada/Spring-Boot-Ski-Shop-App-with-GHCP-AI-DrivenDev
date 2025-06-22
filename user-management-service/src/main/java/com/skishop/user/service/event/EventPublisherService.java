package com.skishop.user.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.dto.event.EventDto;
import com.skishop.user.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * イベントパブリッシャーサービス
 * Kafka経由でのイベント発行
 * 非同期処理とエラーハンドリング、フォールバック機能を実装
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    @Value("${kafka.topics.user-management-status:user-management-status}")
    private String statusTopic;

    @Value("${kafka.producer.retry.max:3}")
    private Integer maxRetryCount;

    @Value("${kafka.producer.timeout:10000}")
    private Long timeoutMs;

    /**
     * イベント発行（非同期）
     */
    public CompletableFuture<Boolean> publishEvent(EventDto<?> event) {
        long startTime = System.currentTimeMillis();
        
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String topic = determineTopicByEventType(event.getEventType());
            String key = generatePartitionKey(event);

            log.info("イベント発行開始: eventId={}, eventType={}, topic={}", 
                     event.getEventId(), event.getEventType(), topic);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
            
            return future.handle((result, throwable) -> {
                long processingTime = System.currentTimeMillis() - startTime;
                
                if (throwable != null) {
                    log.error("イベント発行失敗: eventId={}, topic={}, error={}", 
                             event.getEventId(), topic, throwable.getMessage(), throwable);
                    
                    metricsService.recordEventFailure(event.getEventType(), "production", processingTime, 
                                                     throwable.getClass().getSimpleName(), throwable.getMessage());
                    return false;
                } else {
                    log.info("イベント発行成功: eventId={}, topic={}, partition={}, offset={}, processingTime={}ms", 
                             event.getEventId(), topic, 
                             result.getRecordMetadata().partition(), 
                             result.getRecordMetadata().offset(),
                             processingTime);
                    
                    metricsService.recordEventSuccessful(event.getEventType(), "production", eventJson.length());
                    return true;
                }
            });

        } catch (Exception e) {
            log.error("イベント発行エラー: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordEventFailure(event.getEventType(), "production", processingTime, 
                                             e.getClass().getSimpleName(), e.getMessage());
            
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * イベント発行（同期・ヘルスチェック用）
     */
    public boolean publishEventSync(String eventType, Object payload, String correlationId) {
        try {
            EventDto<Object> event = EventDto.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .eventType(eventType)
                    .timestamp(java.time.Instant.now())
                    .version("1.0")
                    .producer("user-management-service")
                    .payload(payload)
                    .correlationId(correlationId)
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            String topic = determineTopicByEventType(eventType);
            String key = generatePartitionKey(event);

            SendResult<String, String> result = kafkaTemplate.send(topic, key, eventJson).get();
            
            log.debug("同期イベント発行成功: eventType={}, topic={}, partition={}, offset={}", 
                     eventType, topic, 
                     result.getRecordMetadata().partition(), 
                     result.getRecordMetadata().offset());
            
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("同期イベント発行中断: eventType={}", eventType, e);
            return false;
        } catch (Exception e) {
            log.error("同期イベント発行エラー: eventType={}, error={}", eventType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * リトライ付きイベント発行
     */
    public CompletableFuture<Boolean> publishEventWithRetry(EventDto<?> event) {
        return publishEventWithRetry(event, 0);
    }

    private CompletableFuture<Boolean> publishEventWithRetry(EventDto<?> event, int retryCount) {
        return publishEvent(event).thenCompose(success -> {
            if (Boolean.FALSE.equals(success) && retryCount < maxRetryCount) {
                log.warn("イベント発行リトライ: eventId={}, retryCount={}/{}", 
                         event.getEventId(), retryCount + 1, maxRetryCount);
                
                // 指数バックオフ
                try {
                    Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return CompletableFuture.completedFuture(false);
                }
                
                return publishEventWithRetry(event, retryCount + 1);
            }
            return CompletableFuture.completedFuture(success);
        });
    }

    /**
     * バルクイベント発行
     */
    public CompletableFuture<Boolean> publishEventsInBatch(java.util.List<EventDto<?>> events) {
        log.info("バルクイベント発行開始: eventCount={}", events.size());
        
        java.util.List<CompletableFuture<Boolean>> futures = events.stream()
                .map(this::publishEvent)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .allMatch(future -> {
                            try {
                                return future.get();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.error("バルクイベント処理中断", e);
                                return false;
                            } catch (Exception e) {
                                log.error("バルクイベント結果取得エラー", e);
                                return false;
                            }
                        }));
    }

    /**
     * イベントタイプに基づくトピック決定
     */
    private String determineTopicByEventType(String eventType) {
        return switch (eventType) {
            case "USER_MANAGEMENT_STATUS" -> statusTopic;
            case "HEALTH_CHECK" -> "health-check";
            default -> "default-events";
        };
    }

    /**
     * パーティション分散のためのキー生成
     */
    private String generatePartitionKey(EventDto<?> event) {
        // ユーザーIDベースでパーティション分散
        String userId = extractUserId(event);
        if (userId != null) {
            return userId;
        }
        
        // フォールバック: correlationIdまたはeventId
        return event.getCorrelationId() != null ? event.getCorrelationId() : event.getEventId();
    }

    /**
     * イベントからユーザーIDを抽出
     */
    private String extractUserId(EventDto<?> event) {
        Object payload = event.getPayload();
        if (payload instanceof com.skishop.user.dto.event.UserManagementStatusPayload statusPayload) {
            return statusPayload.getUserId();
        }
        return null;
    }

    /**
     * Kafkaテンプレート設定確認
     */
    public boolean isKafkaAvailable() {
        try {
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor("test-topic");
            return true;
        } catch (Exception e) {
            log.warn("Kafka接続確認失敗: {}", e.getMessage());
            return false;
        }
    }
}
