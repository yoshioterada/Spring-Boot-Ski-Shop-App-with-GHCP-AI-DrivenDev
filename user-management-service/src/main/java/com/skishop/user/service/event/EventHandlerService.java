package com.skishop.user.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.dto.event.EventDto;
import com.skishop.user.dto.event.UserDeletionPayload;
import com.skishop.user.dto.event.UserRegistrationPayload;
import com.skishop.user.entity.ProcessedEvent;
import com.skishop.user.exception.EventProcessingException;
import com.skishop.user.repository.ProcessedEventRepository;
import com.skishop.user.service.UserEventService;
import com.skishop.user.service.StatusFeedbackPublishingService;
import com.skishop.user.service.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * イベント処理ハンドラー
 * トランザクション管理を適切に行うため分離
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventHandlerService {

    private final ProcessedEventRepository processedEventRepository;
    private final UserEventService userEventService;
    private final StatusFeedbackPublishingService statusFeedbackService;
    private final SagaOrchestrator sagaOrchestrator;
    private final ObjectMapper objectMapper;

    /**
     * ユーザー登録イベントを処理
     * イベント処理とSagaオーケストレーションを明確に分離
     * トランザクション境界を適切に設定
     */
    public void handleUserRegisteredEvent(String eventJson) {
        EventDto<UserRegistrationPayload> event = null;
        
        try {
            // イベントの解析（トランザクション外で実行）
            event = parseAndValidateEvent(eventJson);
            
            // 冪等性チェック（読み取り専用トランザクション）
            if (isEventAlreadyProcessed(event.getSagaId())) {
                log.info("イベントは既に処理済み、スキップします: {}", event.getSagaId());
                return;
            }
            
            log.info("ユーザー登録イベント処理開始: userId={}, sagaId={}", 
                    event.getPayload().getUserId(), event.getSagaId());
            
            // Sagaオーケストレーションを開始（独立したトランザクション）
            // SagaOrchestratorは内部で新しいトランザクションを開始する
            sagaOrchestrator.startUserRegistrationSaga(event);
            
        } catch (Exception e) {
            log.error("ユーザー登録イベント処理エラー: {}", e.getMessage(), e);
            
            // イベント処理失敗を記録（独立したトランザクションで実行）
            if (event != null) {
                try {
                    markEventAsProcessed(event.getSagaId(), 
                            "USER_REGISTERED", 
                            event.getPayload().getUserId(), 
                            false, 
                            e.getMessage());
                } catch (Exception ex) {
                    log.error("イベント処理失敗の記録に失敗: sagaId={}, error={}", 
                            event.getSagaId(), ex.getMessage(), ex);
                }
            }
            
            throw new EventProcessingException("ユーザー登録イベント処理に失敗しました", e);
        }
    }

    /**
     * ユーザー削除イベントを処理
     * イベント処理とSagaオーケストレーションを明確に分離
     * トランザクション境界を適切に設定
     */
    public void handleUserDeletedEvent(String eventJson) {
        EventDto<UserDeletionPayload> event = null;
        
        try {
            // イベントの解析（トランザクション外で実行）
            event = parseAndValidateEvent(eventJson);
            
            // 冪等性チェック（読み取り専用トランザクション）
            if (isEventAlreadyProcessed(event.getSagaId())) {
                log.info("イベントは既に処理済み、スキップします: {}", event.getSagaId());
                return;
            }
            
            log.info("ユーザー削除イベント処理開始: userId={}, sagaId={}", 
                    event.getPayload().getUserId(), event.getSagaId());
            
            // Sagaオーケストレーションを開始（独立したトランザクション）
            // SagaOrchestratorは内部で新しいトランザクションを開始する
            sagaOrchestrator.startUserDeletionSaga(event);
            
        } catch (Exception e) {
            log.error("ユーザー削除イベント処理エラー: {}", e.getMessage(), e);
            
            // イベント処理失敗を記録（独立したトランザクションで実行）
            if (event != null) {
                try {
                    markEventAsProcessed(event.getSagaId(), 
                            "USER_DELETED", 
                            event.getPayload().getUserId(), 
                            false, 
                            e.getMessage());
                } catch (Exception ex) {
                    log.error("イベント処理失敗の記録に失敗: sagaId={}, error={}", 
                            event.getSagaId(), ex.getMessage(), ex);
                }
            }
            
            throw new EventProcessingException("ユーザー削除イベント処理に失敗しました", e);
        }
    }

    /**
     * イベントが既に処理済みかチェック（読み取り専用トランザクション）
     */
    @Transactional(readOnly = true)
    public boolean isEventAlreadyProcessed(String sagaId) {
        return processedEventRepository.existsBySagaId(sagaId);
    }

    /**
     * イベントを処理済みとして記録（独立したトランザクション）
     */
    @Transactional
    public void markEventAsProcessed(String sagaId, String eventType, String userId, boolean success, String errorMessage) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
            .sagaId(sagaId)
            .eventType(eventType)
            .userId(UUID.fromString(userId))
            .isSuccess(success)
            .errorMessage(errorMessage)
            .processedAt(LocalDateTime.now())
            .build();
        
        processedEventRepository.save(processedEvent);
        log.debug("イベント処理状態を記録: sagaId={}, eventType={}, success={}", 
                sagaId, eventType, success);
    }
    
    /**
     * イベントの解析と検証（ジェネリック）
     */
    @SuppressWarnings("unchecked")
    private <T> EventDto<T> parseAndValidateEvent(String eventJson) {
        try {
            EventDto<T> event = objectMapper.readValue(eventJson, EventDto.class);
            validateEventSchema(event);
            return event;
        } catch (Exception e) {
            log.error("イベントJSONの解析に失敗: {}", e.getMessage(), e);
            throw new EventProcessingException("不正なイベント形式", e);
        }
    }
    
    /**
     * イベントスキーマの検証
     */
    private void validateEventSchema(EventDto<?> event) {
        if (event.getEventId() == null || event.getEventType() == null || 
            event.getPayload() == null || event.getSagaId() == null) {
            throw new IllegalArgumentException("必須イベントフィールドが不足しています");
        }
    }
}
