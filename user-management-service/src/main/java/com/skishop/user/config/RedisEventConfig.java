package com.skishop.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.user.service.EventConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redisイベント設定
 * Redisを使用したイベント受信の設定
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "skishop.runtime.event-propagation-enabled", havingValue = "true")
public class RedisEventConfig {

    private final SkishopRuntimeProperties runtimeProperties;
    private final EventConsumerService eventConsumerService;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // イベントチャンネルの購読設定
        String keyPrefix = runtimeProperties.getEventRedisKeyPrefix();
        
        // ユーザー登録イベントを購読
        container.addMessageListener(
            new MessageListenerAdapter(eventConsumerService),
            new PatternTopic(keyPrefix + ":events:user_registered")
        );
        
        // ユーザー削除イベントを購読
        container.addMessageListener(
            new MessageListenerAdapter(eventConsumerService),
            new PatternTopic(keyPrefix + ":events:user_deleted")
        );
        
        log.info("Redis message listener container configured for event propagation");
        log.info("Subscribed to channels: {}:events:user_registered, {}:events:user_deleted", 
            keyPrefix, keyPrefix);
        
        return container;
    }
}
