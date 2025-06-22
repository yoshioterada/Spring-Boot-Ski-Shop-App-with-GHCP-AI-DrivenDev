package com.skishop.user.service.health;

import com.skishop.user.config.SkishopRuntimeProperties;
import com.skishop.user.service.azure.AzureServiceBusEventReceiver;
import com.skishop.user.service.azure.AzureServiceBusStatusFeedbackPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * イベントシステム健全性インジケータ
 * イベント伝播システムの健全性を監視
 */
@Component
@Slf4j
public class EventSystemHealthIndicator implements HealthIndicator {

    private final SkishopRuntimeProperties runtimeProperties;
    
    @Autowired(required = false)
    private AzureServiceBusEventReceiver eventReceiver;
    
    @Autowired(required = false)
    private AzureServiceBusStatusFeedbackPublisher statusFeedbackPublisher;

    public EventSystemHealthIndicator(SkishopRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        
        // イベント伝播が有効かチェック
        if (!runtimeProperties.isEventPropagationEnabled()) {
            details.put("eventPropagation", "disabled");
            return Health.up()
                .withDetail("status", "Event propagation is disabled")
                .withDetails(details)
                .build();
        }
        
        // ブローカータイプを確認
        details.put("brokerType", runtimeProperties.getEventBrokerType());
        details.put("environment", runtimeProperties.getEnvironment());
        
        boolean isAzureServiceBusEnabled = isAzureServiceBusEnabled();
        details.put("azureServiceBusEnabled", isAzureServiceBusEnabled);
        
        // Azure Service Busが有効な場合はその健全性をチェック
        if (isAzureServiceBusEnabled) {
            boolean receiverHealthy = checkEventReceiverHealth();
            boolean publisherHealthy = checkStatusFeedbackPublisherHealth();
            
            details.put("eventReceiverHealthy", receiverHealthy);
            details.put("statusFeedbackPublisherHealthy", publisherHealthy);
            
            if (receiverHealthy && publisherHealthy) {
                return Health.up()
                    .withDetail("status", "Azure Service Bus connections are healthy")
                    .withDetails(details)
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "Azure Service Bus connections are unhealthy")
                    .withDetails(details)
                    .build();
            }
        }
        
        // Redis（デフォルト）の場合
        return Health.up()
            .withDetail("status", "Default event broker is active")
            .withDetails(details)
            .build();
    }
    
    /**
     * Azure Service Busが有効かどうかを確認
     */
    private boolean isAzureServiceBusEnabled() {
        return runtimeProperties.getAzureServicebus() != null && 
               runtimeProperties.getAzureServicebus().isEnabled();
    }
    
    /**
     * イベントレシーバーの健全性をチェック
     */
    private boolean checkEventReceiverHealth() {
        if (eventReceiver == null) {
            log.warn("Azure Service Bus Event Receiver is not available");
            return false;
        }
        
        try {
            return eventReceiver.isHealthy();
        } catch (Exception e) {
            log.error("Failed to check Azure Service Bus Event Receiver health", e);
            return false;
        }
    }
    
    /**
     * ステータスフィードバックパブリッシャーの健全性をチェック
     */
    private boolean checkStatusFeedbackPublisherHealth() {
        if (statusFeedbackPublisher == null) {
            log.warn("Azure Service Bus Status Feedback Publisher is not available");
            return false;
        }
        
        try {
            return statusFeedbackPublisher.isHealthy();
        } catch (Exception e) {
            log.error("Failed to check Azure Service Bus Status Feedback Publisher health", e);
            return false;
        }
    }
}
