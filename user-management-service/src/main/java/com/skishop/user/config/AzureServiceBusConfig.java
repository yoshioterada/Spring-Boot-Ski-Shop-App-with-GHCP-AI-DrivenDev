package com.skishop.user.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Azure Service Bus 設定クラス
 * 
 * 本番環境でのイベント伝播にAzure Service Busを使用するための設定
 * 管理されたIDを使用したセキュアな認証を実装
 */
@Configuration
@ConditionalOnProperty(
    prefix = "skishop.runtime.azure-servicebus", 
    name = "enabled", 
    havingValue = "true"
)
@Profile({"production", "staging"})
@Slf4j
public class AzureServiceBusConfig {

    @Value("${spring.cloud.azure.servicebus.namespace}")
    private String serviceBusNamespace;

    @Value("${skishop.runtime.azure-servicebus.topic-name}")
    private String topicName;

    @Value("${skishop.runtime.azure-servicebus.subscription-name}")
    private String subscriptionName;

    @Value("${skishop.runtime.azure-servicebus.status-feedback-topic}")
    private String statusFeedbackTopic;

    @Value("${spring.cloud.azure.servicebus.connection-string:}")
    private String connectionString;

    /**
     * イベント受信用 ServiceBus Processor Client
     * 認証サービスからのイベントを受信して処理
     */
    @Bean
    public ServiceBusProcessorClient eventProcessorClient() {
        log.info("Creating Azure Service Bus Processor Client for subscription: {}", subscriptionName);
        
        ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder();
        
        // 接続文字列が設定されている場合は使用、そうでなければ管理されたIDを使用
        if (connectionString != null && !connectionString.isEmpty()) {
            clientBuilder.connectionString(connectionString);
            log.debug("Using connection string for Service Bus authentication");
        } else {
            clientBuilder
                .fullyQualifiedNamespace(serviceBusNamespace + ".servicebus.windows.net")
                .credential(new DefaultAzureCredentialBuilder().build());
            log.debug("Using Managed Identity for Service Bus authentication");
        }
        
        return clientBuilder
            .processor()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .processMessage(message -> {
                log.debug("Received event message: {}", message.getBody().toString());
                // メッセージ処理は EventConsumerService に委譲される
            })
            .processError(context -> {
                log.error("Error processing event message: {}", 
                    context.getException().getMessage(), context.getException());
            })
            .buildProcessorClient();
    }

    /**
     * ステータスフィードバック送信用 ServiceBus Sender Client
     * 認証サービスへのステータス更新送信に使用
     */
    @Bean
    public ServiceBusSenderClient statusFeedbackSenderClient() {
        log.info("Creating Azure Service Bus Status Feedback Sender Client for topic: {}", 
            statusFeedbackTopic);
        
        ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder();
        
        if (connectionString != null && !connectionString.isEmpty()) {
            clientBuilder.connectionString(connectionString);
        } else {
            clientBuilder
                .fullyQualifiedNamespace(serviceBusNamespace + ".servicebus.windows.net")
                .credential(new DefaultAzureCredentialBuilder().build());
        }
        
        return clientBuilder
            .sender()
            .topicName(statusFeedbackTopic)
            .buildClient();
    }
}
