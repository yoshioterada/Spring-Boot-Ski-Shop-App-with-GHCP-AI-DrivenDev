package com.skishop.user.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.skishop.user.service.azure.AzureServiceBusEventReceiver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final SkishopRuntimeProperties runtimeProperties;

    @Value("${spring.cloud.azure.servicebus.connection-string:}")
    private String connectionString;
    
    public AzureServiceBusConfig(SkishopRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    /**
     * イベント受信用 ServiceBus Processor Client
     * 認証サービスからのイベントを受信して処理
     */
    @Bean
    public ServiceBusProcessorClient eventProcessorClient(
            @Autowired ObjectProvider<AzureServiceBusEventReceiver> eventReceiverProvider) {
        String topicName = runtimeProperties.getAzureServicebus().getTopicName();
        String subscriptionName = runtimeProperties.getAzureServicebus().getSubscriptionName();
        int prefetchCount = runtimeProperties.getAzureServicebus().getPrefetchCount();
        int maxConcurrentCalls = runtimeProperties.getAzureServicebus().getMaxConcurrentCalls();
        
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
        
        // 最初はプロセッサをコールバックなしで作成
        // コールバックはAzureServiceBusEventReceiverでインジェクション後に設定される
        return clientBuilder
            .processor()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .disableAutoComplete()  // 明示的な完了またはエラー処理を可能にする
            .maxConcurrentCalls(maxConcurrentCalls)  // 並列処理の制御
            .prefetchCount(prefetchCount)      // パフォーマンス最適化
            .processMessage(context -> {
                // AzureServiceBusEventReceiverがインジェクションされたら、そのメソッドを使用
                AzureServiceBusEventReceiver receiver = eventReceiverProvider.getIfAvailable();
                if (receiver != null) {
                    receiver.processMessage(context);
                } else {
                    log.warn("AzureServiceBusEventReceiver not available, message will not be processed");
                    context.abandon();
                }
            })
            .processError(context -> {
                // AzureServiceBusEventReceiverがインジェクションされたら、そのメソッドを使用
                AzureServiceBusEventReceiver receiver = eventReceiverProvider.getIfAvailable();
                if (receiver != null) {
                    receiver.processError(context);
                } else {
                    log.error("AzureServiceBusEventReceiver not available, error will not be processed: {}", 
                        context.getException().getMessage(), context.getException());
                }
            })
            .buildProcessorClient();
    }

    /**
     * ステータスフィードバック送信用 ServiceBus Sender Client
     * 認証サービスへのステータス更新送信に使用
     */
    @Bean
    public ServiceBusSenderClient statusFeedbackSenderClient() {
        String statusFeedbackTopic = runtimeProperties.getAzureServicebus().getStatusFeedbackTopic();
        
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
