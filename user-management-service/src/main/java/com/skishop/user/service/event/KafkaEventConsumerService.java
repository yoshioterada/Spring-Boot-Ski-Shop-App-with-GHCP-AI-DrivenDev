package com.skishop.user.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.dto.event.EventDto;
import com.skishop.user.dto.event.UserRegistrationPayload;
import com.skishop.user.dto.event.UserDeletionPayload;
import com.skishop.user.entity.ProcessedEvent;
import com.skishop.user.repository.ProcessedEventRepository;
import com.skishop.user.service.saga.SagaOrchestrator;
import com.skishop.user.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafkaイベントコンシューマーサービス
 * Kafka経由でのイベント受信と処理
 * 冪等性とエラーハンドリングを考慮した実装
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumerService {

    private final SagaOrchestrator sagaOrchestrator;
    private final ProcessedEventRepository processedEventRepository;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    /**
     * ユーザー登録イベントの処理
     */
    @KafkaListener(topics = "${kafka.topics.user-registration:user-registration}")
    @Transactional
    public void handleUserRegistrationEvent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        
        long startTime = System.currentTimeMillis();
        EventDto<UserRegistrationPayload> event = null;
        
        try {
            log.info("ユーザー登録イベント受信: topic={}, partition={}, offset={}", topic, partition, offset);
            
            // JSON -> EventDto変換
            event = objectMapper.readValue(eventJson, 
                objectMapper.getTypeFactory().constructParametricType(EventDto.class, UserRegistrationPayload.class));
            
            // 冪等性チェック
            if (isEventAlreadyProcessed(event.getEventId())) {
                log.info("イベント既処理済み（冪等性）: eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            // イベント処理記録
            recordEventProcessing(event.getEventId(), "USER_REGISTRATION", eventJson);

            // Saga開始
            sagaOrchestrator.startUserRegistrationSaga(event);

            // メトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordEventProcessed("USER_REGISTRATION", "production", processingTime);
            metricsService.recordEventSuccessful("USER_REGISTRATION", "production", eventJson.length());

            ack.acknowledge();
            log.info("ユーザー登録イベント処理完了: eventId={}, processingTime={}ms", 
                     event.getEventId(), processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            String eventId = event != null ? event.getEventId() : "unknown";
            
            log.error("ユーザー登録イベント処理エラー: eventId={}, error={}", eventId, e.getMessage(), e);
            
            metricsService.recordEventFailure("USER_REGISTRATION", "production", processingTime, 
                                             e.getClass().getSimpleName(), e.getMessage());
            
            // エラー時はNACKして再処理を促すか、DLQに送る
            // 設定に応じてackまたは例外を投げる
            ack.acknowledge(); // ここでは無限リトライを避けるためack
        }
    }

    /**
     * ユーザー削除イベントの処理
     */
    @KafkaListener(topics = "${kafka.topics.user-deletion:user-deletion}")
    @Transactional
    public void handleUserDeletionEvent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        
        long startTime = System.currentTimeMillis();
        EventDto<UserDeletionPayload> event = null;
        
        try {
            log.info("ユーザー削除イベント受信: topic={}, partition={}, offset={}", topic, partition, offset);
            
            // JSON -> EventDto変換
            event = objectMapper.readValue(eventJson, 
                objectMapper.getTypeFactory().constructParametricType(EventDto.class, UserDeletionPayload.class));
            
            // 冪等性チェック
            if (isEventAlreadyProcessed(event.getEventId())) {
                log.info("イベント既処理済み（冪等性）: eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            // イベント処理記録
            recordEventProcessing(event.getEventId(), "USER_DELETION", eventJson);

            // Saga開始
            sagaOrchestrator.startUserDeletionSaga(event);

            // メトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordEventProcessed("USER_DELETION", "production", processingTime);
            metricsService.recordEventSuccessful("USER_DELETION", "production", eventJson.length());

            ack.acknowledge();
            log.info("ユーザー削除イベント処理完了: eventId={}, processingTime={}ms", 
                     event.getEventId(), processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            String eventId = event != null ? event.getEventId() : "unknown";
            
            log.error("ユーザー削除イベント処理エラー: eventId={}, error={}", eventId, e.getMessage(), e);
            
            metricsService.recordEventFailure("USER_DELETION", "production", processingTime, 
                                             e.getClass().getSimpleName(), e.getMessage());
            
            ack.acknowledge(); // ここでは無限リトライを避けるためack
        }
    }

    /**
     * 汎用イベントハンドラー（フォールバック）
     */
    @KafkaListener(topics = "${kafka.topics.fallback:fallback-events}")
    @Transactional
    public void handleGenericEvent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        
        try {
            log.info("汎用イベント受信: topic={}, partition={}, offset={}", topic, partition, offset);
            
            // 基本的なEventDto構造を解析
            EventDto<?> event = objectMapper.readValue(eventJson, EventDto.class);
            
            log.warn("未対応のイベントタイプ: eventType={}, eventId={}", 
                     event.getEventType(), event.getEventId());
            
            ack.acknowledge();

        } catch (Exception e) {
            log.error("汎用イベント処理エラー: topic={}, error={}", topic, e.getMessage(), e);
            ack.acknowledge();
        }
    }

    /**
     * イベント重複処理チェック（冪等性保証）
     */
    private boolean isEventAlreadyProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    /**
     * イベント処理記録
     */
    private void recordEventProcessing(String eventId, String eventType, String eventData) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .eventData(eventData)
                .processedAt(LocalDateTime.now())
                .build();
        
        processedEventRepository.save(processedEvent);
        log.debug("イベント処理記録保存: eventId={}, eventType={}", eventId, eventType);
    }
}
