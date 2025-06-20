package com.skishop.auth.service.azure;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.auth.dto.EventDto;
import com.skishop.auth.service.StatusFeedbackService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Azure Service Bus ステータスフィードバック受信サービス
 * 
 * 他サービスからのステータス更新をAzure Service Busで受信
 * メッセージの自動デシリアライゼーションとエラーハンドリング
 */
@Service
@ConditionalOnProperty(
    prefix = "skishop.runtime.azure-servicebus", 
    name = "enabled", 
    havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class AzureServiceBusStatusFeedbackReceiver {

    private final ServiceBusProcessorClient statusFeedbackProcessorClient;
    private final StatusFeedbackService statusFeedbackService;
    private final ObjectMapper objectMapper;

    /**
     * Service Bus Processor を開始
     */
    @PostConstruct
    public void startReceiving() {
        log.info("Starting Azure Service Bus Status Feedback Processor");
        
        // メッセージ処理ハンドラーを設定
        statusFeedbackProcessorClient.start();
        
        log.info("Azure Service Bus Status Feedback Processor started successfully");
    }

    /**
     * Service Bus Processor を停止
     */
    @PreDestroy
    public void stopReceiving() {
        log.info("Stopping Azure Service Bus Status Feedback Processor");
        
        if (statusFeedbackProcessorClient != null) {
            statusFeedbackProcessorClient.close();
        }
        
        log.info("Azure Service Bus Status Feedback Processor stopped");
    }

    /**
     * Service Busメッセージを処理
     * 
     * @param context Service Busメッセージコンテキスト
     */
    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        
        try {
            log.debug("Processing status feedback message: messageId={}, correlationId={}", 
                message.getMessageId(), message.getCorrelationId());

            // メッセージボディをEventDtoにデシリアライゼーション
            String messageBody = message.getBody().toString();
            EventDto statusEvent = objectMapper.readValue(messageBody, EventDto.class);

            // ステータスフィードバックサービスで処理
            statusFeedbackService.processStatusFeedback(statusEvent);

            // メッセージを完了
            context.complete();
            
            log.info("Successfully processed status feedback message: eventId={}, eventType={}", 
                statusEvent.getEventId(), statusEvent.getEventType());

        } catch (Exception e) {
            log.error("Failed to process status feedback message: messageId={}, error={}", 
                message.getMessageId(), e.getMessage(), e);
            
            // メッセージをデッドレターキューに送信
            context.deadLetter(new DeadLetterOptions()
                .setDeadLetterErrorDescription("Failed to process status feedback")
                .setDeadLetterReason("ProcessingError"));
        }
    }

    /**
     * Service Busエラーを処理
     * 
     * @param context エラーコンテキスト
     */
    public void processError(ServiceBusErrorContext context) {
        log.error("Azure Service Bus error occurred: {}", 
            context.getException().getMessage(), context.getException());
        
        // エラーメトリクスを記録
        // TODO: メトリクス収集の実装
    }

    /**
     * プロセッサーの健全性をチェック
     * 
     * @return プロセッサーが稼働中の場合はtrue
     */
    public boolean isHealthy() {
        return statusFeedbackProcessorClient != null && 
               statusFeedbackProcessorClient.isRunning();
    }
}
