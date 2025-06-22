package com.skishop.user.service;

import com.skishop.user.dto.event.EventDto;
import com.skishop.user.service.event.EventHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * イベント受信サービス
 * 他のマイクロサービスからのイベントを受信し処理する
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumerService implements MessageListener {

    private final EventHandlerService eventHandlerService;

    /**
     * Azure Service Busから受信したイベントを処理
     */
    public void processEvent(EventDto<?> event) {
        try {
            log.info("Processing event: eventId={}, eventType={}", 
                event.getEventId(), event.getEventType());
            
            String eventType = event.getEventType();
            String eventData = event.getPayload() != null ? event.getPayload().toString() : "{}";
            
            if ("user_registered".equals(eventType)) {
                eventHandlerService.handleUserRegisteredEvent(eventData);
            } else if ("user_deleted".equals(eventType)) {
                eventHandlerService.handleUserDeletedEvent(eventData);
            } else {
                log.warn("Unknown event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Failed to process event: eventId={}, error={}", 
                event.getEventId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            
            log.info("Received event from channel: {}, body: {}", channel, body);
            
            // チャンネル名から イベントタイプを取得
            String eventType = extractEventTypeFromChannel(channel);
            
            if ("user_registered".equals(eventType)) {
                eventHandlerService.handleUserRegisteredEvent(body);
            } else if ("user_deleted".equals(eventType)) {
                eventHandlerService.handleUserDeletedEvent(body);
            } else {
                log.warn("Unknown event type: {} from channel: {}", eventType, channel);
            }
            
        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
        }
    }

    /**
     * チャンネル名からイベントタイプを抽出
     */
    private String extractEventTypeFromChannel(String channel) {
        if (channel.contains("USER_REGISTERED")) {
            return "user_registered";
        } else if (channel.contains("USER_DELETED")) {
            return "user_deleted";
        }
        return "unknown";
    }
}
