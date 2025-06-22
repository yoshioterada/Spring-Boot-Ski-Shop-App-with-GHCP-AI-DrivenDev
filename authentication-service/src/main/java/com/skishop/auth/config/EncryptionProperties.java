package com.skishop.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 暗号化設定プロパティ
 */
@ConfigurationProperties(prefix = "encryption")
@Data
@Component
public class EncryptionProperties {
    private String password = System.getenv("ENCRYPTION_PASSWORD");
}
