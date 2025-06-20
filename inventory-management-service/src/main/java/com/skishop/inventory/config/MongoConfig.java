package com.skishop.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * MongoDB設定（Java 21モダン記法使用）
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "skishop_inventory";
    }

    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        // Java 21のメソッド参照と関数型プログラミングを活用
        return new MongoCustomConversions(List.of(
            localDateTimeToZonedDateTimeConverter(),
            zonedDateTimeToLocalDateTimeConverter()
        ));
    }

    /**
     * LocalDateTime → ZonedDateTime コンバーター（関数型）
     */
    private Converter<LocalDateTime, ZonedDateTime> localDateTimeToZonedDateTimeConverter() {
        return source -> source.atZone(ZoneId.systemDefault());
    }

    /**
     * ZonedDateTime → LocalDateTime コンバーター（関数型）
     */
    private Converter<ZonedDateTime, LocalDateTime> zonedDateTimeToLocalDateTimeConverter() {
        return ZonedDateTime::toLocalDateTime;
    }
}
