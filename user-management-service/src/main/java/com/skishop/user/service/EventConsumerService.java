package com.skishop.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.config.SkishopRuntimeProperties;
import com.skishop.user.dto.EventDto;
import com.skishop.user.entity.ProcessedEvent;
import com.skishop.user.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * イベント受信サービス
 * 他のマイクロサービスからのイベントを受信し処理する
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumerService implements MessageListener {

    private final SkishopRuntimeProperties runtimeProperties;
    private final ProcessedEventRepository processedEventRepository;
    private final UserService userService;
    private final StatusFeedbackPublishingService statusFeedbackService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            
            log.info("Received event from channel: {}, body: {}", channel, body);
            
            // チャンネル名から イベントタイプを取得
            String eventType = extractEventTypeFromChannel(channel);
            
            if ("user_registered".equals(eventType)) {
                handleUserRegisteredEvent(body);
            } else if ("user_deleted".equals(eventType)) {
                handleUserDeletedEvent(body);
            } else {
                log.warn("Unknown event type: {} from channel: {}", eventType, channel);
            }
            
        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
        }
    }

    /**
     * ユーザー登録イベントを処理
     */
    @Transactional
    public void handleUserRegisteredEvent(String eventJson) {
        long startTime = System.currentTimeMillis();
        String sagaId = null;
        String originalEventId = null;
        String userId = null;
        
        try {
            EventDto event = objectMapper.readValue(eventJson, EventDto.class);
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            sagaId = event.getSagaId();
            originalEventId = event.getEventId();
            userId = (String) payload.get("userId");
            String email = (String) payload.get("email");
            String firstName = (String) payload.get("firstName");
            String lastName = (String) payload.get("lastName");
            String phoneNumber = (String) payload.get("phoneNumber");
            
            // 重複処理チェック
            if (isEventAlreadyProcessed(sagaId)) {
                log.info("Event already processed, skipping: {}", sagaId);
                return;
            }

            log.info("Processing user registration event for user: {} (saga: {})", userId, sagaId);

            // ユーザー管理サービス側での処理
            userService.handleUserRegisteredFromAuth(
                UUID.fromString(userId),
                email,
                firstName,
                lastName,
                phoneNumber
            );

            // 処理済みイベントとして記録
            markEventAsProcessed(sagaId, "USER_REGISTERED", UUID.fromString(userId), true, null);
            
            // 成功ステータスをAuthentication-serviceに送信
            long processingTime = System.currentTimeMillis() - startTime;
            statusFeedbackService.publishSuccessStatus(sagaId, userId, originalEventId, processingTime);
            
            log.info("Successfully processed user registration event for user: {} (saga: {})", userId, sagaId);
            
        } catch (Exception e) {
            log.error("Failed to handle user registration event (saga: {}): {}", sagaId, e.getMessage(), e);
            
            // 失敗イベントとして記録
            if (sagaId != null && userId != null) {
                markEventAsProcessed(sagaId, "USER_REGISTERED", UUID.fromString(userId), false, e.getMessage());
                
                // 失敗ステータスをAuthentication-serviceに送信
                long processingTime = System.currentTimeMillis() - startTime;
                statusFeedbackService.publishFailureStatus(sagaId, userId, originalEventId, e.getMessage(), processingTime);
            }
            
            throw new RuntimeException("Failed to process user registration event", e);
        }
    }

    /**
     * ユーザー削除イベントを処理
     */
    @Transactional
    public void handleUserDeletedEvent(String eventJson) {
        long startTime = System.currentTimeMillis();
        String sagaId = null;
        String originalEventId = null;
        String userId = null;
        
        try {
            EventDto event = objectMapper.readValue(eventJson, EventDto.class);
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            sagaId = event.getSagaId();
            originalEventId = event.getEventId();
            userId = (String) payload.get("userId");
            String reason = (String) payload.get("reason");
            
            // 重複処理チェック
            if (isEventAlreadyProcessed(sagaId)) {
                log.info("Event already processed, skipping: {}", sagaId);
                return;
            }

            log.info("Processing user deletion event for user: {} (saga: {})", userId, sagaId);

            // ユーザー管理サービス側での処理
            userService.handleUserDeletedFromAuth(
                UUID.fromString(userId),
                reason
            );

            // 処理済みイベントとして記録
            markEventAsProcessed(sagaId, "USER_DELETED", UUID.fromString(userId), true, null);
            
            // 成功ステータスをAuthentication-serviceに送信
            long processingTime = System.currentTimeMillis() - startTime;
            statusFeedbackService.publishSuccessStatus(sagaId, userId, originalEventId, processingTime);
            
            log.info("Successfully processed user deletion event for user: {} (saga: {})", userId, sagaId);
            
        } catch (Exception e) {
            log.error("Failed to handle user deletion event (saga: {}): {}", sagaId, e.getMessage(), e);
            
            // 失敗イベントとして記録
            if (sagaId != null && userId != null) {
                markEventAsProcessed(sagaId, "USER_DELETED", UUID.fromString(userId), false, e.getMessage());
                
                // 失敗ステータスをAuthentication-serviceに送信
                long processingTime = System.currentTimeMillis() - startTime;
                statusFeedbackService.publishFailureStatus(sagaId, userId, originalEventId, e.getMessage(), processingTime);
            }
            
            throw new RuntimeException("Failed to process user deletion event", e);
        }
    }

    /**
     * イベントが既に処理済みかチェック
     */
    private boolean isEventAlreadyProcessed(String sagaId) {
        return processedEventRepository.existsBySagaId(sagaId);
    }

    /**
     * イベントを処理済みとして記録
     */
    private void markEventAsProcessed(String sagaId, String eventType, UUID userId, boolean success, String errorMessage) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
            .sagaId(sagaId)
            .eventType(eventType)
            .userId(userId)
            .success(success)
            .errorMessage(errorMessage)
            .processedAt(LocalDateTime.now())
            .build();
        
        processedEventRepository.save(processedEvent);
    }

    /**
     * チャンネル名からイベントタイプを抽出
     */
    private String extractEventTypeFromChannel(String channel) {
        // Format: "skishop:events:user_registered"
        String[] parts = channel.split(":");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }

}
