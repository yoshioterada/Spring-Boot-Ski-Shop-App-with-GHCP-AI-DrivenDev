package com.skishop.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.config.SkishopRuntimeProperties;
import com.skishop.user.entity.ProcessedEvent;
import com.skishop.user.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        try {
            UserRegisteredEvent event = objectMapper.readValue(eventJson, UserRegisteredEvent.class);
            
            // 重複処理チェック
            if (isEventAlreadyProcessed(event.getSagaId())) {
                log.info("Event already processed, skipping: {}", event.getSagaId());
                return;
            }

            log.info("Processing user registration event for user: {}", event.getUserId());

            // ユーザー管理サービス側での処理
            userService.handleUserRegisteredFromAuth(
                event.getUserId(),
                event.getUsername(),
                event.getEmail()
            );

            // 処理済みイベントとして記録
            markEventAsProcessed(event.getSagaId(), "USER_REGISTERED", event.getUserId());
            
            log.info("Successfully processed user registration event for user: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to handle user registration event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process user registration event", e);
        }
    }

    /**
     * ユーザー削除イベントを処理
     */
    @Transactional
    public void handleUserDeletedEvent(String eventJson) {
        try {
            UserDeletedEvent event = objectMapper.readValue(eventJson, UserDeletedEvent.class);
            
            // 重複処理チェック
            if (isEventAlreadyProcessed(event.getSagaId())) {
                log.info("Event already processed, skipping: {}", event.getSagaId());
                return;
            }

            log.info("Processing user deletion event for user: {}", event.getUserId());

            // ユーザー管理サービス側での処理
            userService.handleUserDeletedFromAuth(
                event.getUserId(),
                event.getReason()
            );

            // 処理済みイベントとして記録
            markEventAsProcessed(event.getSagaId(), "USER_DELETED", event.getUserId());
            
            log.info("Successfully processed user deletion event for user: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to handle user deletion event: {}", e.getMessage(), e);
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
    private void markEventAsProcessed(String sagaId, String eventType, UUID userId) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
            .sagaId(sagaId)
            .eventType(eventType)
            .userId(userId)
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

        // Getters and Setters
        public String getSagaId() { return sagaId; }
        public void setSagaId(String sagaId) { this.sagaId = sagaId; }
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
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

        // Getters and Setters
        public String getSagaId() { return sagaId; }
        public void setSagaId(String sagaId) { this.sagaId = sagaId; }
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
