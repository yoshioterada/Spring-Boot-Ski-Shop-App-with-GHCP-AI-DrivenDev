package com.skishop.sales.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Kafka設定
 * Java 21のモダンな機能を活用した設定クラス
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Kafkaプロデューサー設定
     * Java 21のMap.of()とText Blocksを使用してより読みやすく設定
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.ACKS_CONFIG, "all",
            ProducerConfig.RETRIES_CONFIG, 3,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
            ProducerConfig.LINGER_MS_CONFIG, 10,
            ProducerConfig.BATCH_SIZE_CONFIG, 16384
        ));
    }

    /**
     * KafkaTemplate設定
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        var template = new KafkaTemplate<>(producerFactory());
        
        // Java 21のText Blockを使用したデフォルトトピック設定用の説明文
        var topicDescription = """
            このKafkaTemplateは以下のトピックに対して使用されます:
            - sales.order.created: 注文作成イベント
            - sales.order.updated: 注文更新イベント
            - sales.order.cancelled: 注文キャンセルイベント
            - sales.payment.completed: 支払い完了イベント
            """;
        
        // ログ出力（実際の設定には影響しない）
        log.debug("Kafka topic configuration: {}", topicDescription);
        
        return template;
    }
}
