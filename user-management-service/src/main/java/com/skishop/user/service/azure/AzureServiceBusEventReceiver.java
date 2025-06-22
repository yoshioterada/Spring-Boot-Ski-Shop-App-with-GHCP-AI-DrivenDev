package com.skishop.user.service.azure;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.dto.event.EventDto;
import com.skishop.user.service.EventConsumerService;
import com.skishop.user.service.metrics.MetricsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Azure Service Bus イベント受信サービス
 * 
 * 認証サービスからのイベントをAzure Service Busで受信
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
public class AzureServiceBusEventReceiver {

    private final ServiceBusProcessorClient eventProcessorClient;
    private final EventConsumerService eventConsumerService;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    /**
     * Service Bus Processor を開始
     */
    @PostConstruct
    public void startReceiving() {
        log.info("Starting Azure Service Bus Event Processor");
        
        // プロセッサーを開始
        // 注：ハンドラーはAzureServiceBusConfigで設定済み
        if (!eventProcessorClient.isRunning()) {
            eventProcessorClient.start();
            log.info("Azure Service Bus Event Processor started successfully");
        } else {
            log.info("Azure Service Bus Event Processor is already running");
        }
    }

    /**
     * Service Bus Processor を停止
     */
    @PreDestroy
    public void stopReceiving() {
        log.info("Stopping Azure Service Bus Event Processor");
        
        if (eventProcessorClient != null) {
            eventProcessorClient.close();
        }
        
        log.info("Azure Service Bus Event Processor stopped");
    }

    /**
     * Service Busメッセージを処理
     * 
     * @param context Service Busメッセージコンテキスト
     */
    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        long startTime = System.currentTimeMillis();
        String messageId = message.getMessageId();
        String correlationId = message.getCorrelationId();
        
        try {
            log.debug("Processing event message: messageId={}, correlationId={}", 
                messageId, correlationId);

            // メッセージボディをEventDtoにデシリアライゼーション
            String messageBody = message.getBody().toString();
            EventDto<?> event = objectMapper.readValue(messageBody, EventDto.class);

            // イベントコンシューマーサービスで処理
            eventConsumerService.processEvent(event);

            // メッセージを完了
            context.complete();
            
            // 処理時間を計測してメトリクスに記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordEventProcessed(
                event.getEventType(), 
                "azure-servicebus", 
                processingTime);
            
            log.info("Successfully processed event message: eventId={}, eventType={}, processingTime={}ms", 
                event.getEventId(), event.getEventType(), processingTime);

        } catch (Exception e) {
            log.error("Failed to process event message: messageId={}, error={}", 
                messageId, e.getMessage(), e);
            
            // リトライカウントをチェック
            long deliveryCount = message.getDeliveryCount();
            if (deliveryCount >= 3) {
                // 最大リトライ回数に達した場合はデッドレターキューに送信
                DeadLetterOptions options = new DeadLetterOptions()
                    .setDeadLetterReason("MaxRetryExceeded")
                    .setDeadLetterErrorDescription("Maximum retry attempts exceeded: " + e.getMessage());
                context.deadLetter(options);
                
                // メトリクス記録
                metricsService.recordMessageBrokerError(
                    "azure-servicebus", 
                    "MaxRetryExceeded", 
                    "Maximum retry attempts exceeded for message: " + messageId);
                
                log.warn("Message sent to dead letter queue after {} attempts: messageId={}", 
                    deliveryCount, messageId);
            } else {
                // リトライのためにメッセージを放棄
                context.abandon();
                
                log.info("Message abandoned for retry (attempt {}/3): messageId={}", 
                    deliveryCount + 1, messageId);
            }
            
            // エラーメトリクス記録
            String errorType = e.getClass().getSimpleName();
            metricsService.recordEventFailure(
                "event-processing", 
                "azure-servicebus", 
                System.currentTimeMillis() - startTime,
                errorType,
                e.getMessage());
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
        String errorType = context.getException().getClass().getSimpleName();
        String errorMessage = context.getException().getMessage();
        metricsService.recordMessageBrokerError("azure-servicebus", errorType, errorMessage);
    }

    /**
     * プロセッサーの健全性をチェック
     * 
     * @return プロセッサーが稼働中の場合はtrue
     */
    public boolean isHealthy() {
        return eventProcessorClient != null && 
               eventProcessorClient.isRunning();
    }
}
