package com.skishop.user.service.azure;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.dto.EventDto;
import com.skishop.user.service.EventConsumerService;
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

    /**
     * Service Bus Processor を開始
     */
    @PostConstruct
    public void startReceiving() {
        log.info("Starting Azure Service Bus Event Processor");
        
        // メッセージ処理ハンドラーを設定
        eventProcessorClient.start();
        
        log.info("Azure Service Bus Event Processor started successfully");
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
        
        try {
            log.debug("Processing event message: messageId={}, correlationId={}", 
                message.getMessageId(), message.getCorrelationId());

            // メッセージボディをEventDtoにデシリアライゼーション
            String messageBody = message.getBody().toString();
            EventDto event = objectMapper.readValue(messageBody, EventDto.class);

            // イベントコンシューマーサービスで処理
            eventConsumerService.processEvent(event);

            // メッセージを完了
            context.complete();
            
            log.info("Successfully processed event message: eventId={}, eventType={}", 
                event.getEventId(), event.getEventType());

        } catch (Exception e) {
            log.error("Failed to process event message: messageId={}, error={}", 
                message.getMessageId(), e.getMessage(), e);
            
            // リトライカウントをチェック
            int deliveryCount = message.getDeliveryCount();
            if (deliveryCount >= 3) {
                // 最大リトライ回数に達した場合はデッドレターキューに送信
                context.deadLetter(DeadLetterOptions.of()
                    .setErrorDescription("Maximum retry attempts exceeded")
                    .setReason("MaxRetryExceeded"));
            } else {
                // リトライのためにメッセージを放棄
                context.abandon();
            }
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
        return eventProcessorClient != null && 
               eventProcessorClient.isRunning();
    }
}
