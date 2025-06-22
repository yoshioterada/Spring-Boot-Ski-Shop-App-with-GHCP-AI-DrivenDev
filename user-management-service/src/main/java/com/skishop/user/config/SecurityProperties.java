package com.skishop.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * セキュリティ設定プロパティ
 * 機密情報のマスキングと暗号化設定
 */
@Configuration
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

    /**
     * 暗号化設定
     */
    private Encryption encryption = new Encryption();

    /**
     * ログマスキング設定
     */
    private LogMasking logMasking = new LogMasking();

    /**
     * 設定マスキング設定
     */
    private ConfigurationMasking configurationMasking = new ConfigurationMasking();

    @Data
    public static class Encryption {
        private String password = System.getenv("ENCRYPTION_PASSWORD");
        private String algorithm = "PBEWITHHMACSHA512ANDAES_256";
        private int keyObtentionIterations = 1000;
        private int poolSize = 4;
        private String providerName = "SunJCE";
        private String saltGeneratorClassName = "org.jasypt.salt.RandomSaltGenerator";
        private String stringOutputType = "base64";
    }

    @Data
    public static class LogMasking {
        private boolean enabled = true;
        private Set<String> sensitiveFields = Set.of(
            "password", "secret", "key", "token", "credential", 
            "authorization", "cookie", "session"
        );
        private String maskPattern = "****";
        private int visibleChars = 4;
    }

    @Data
    public static class ConfigurationMasking {
        private boolean enabled = true;
        private Set<String> sensitiveKeys = Set.of(
            "password", "secret", "key", "token", "credential",
            "connection-string", "url", "uri"
        );
        private String maskPattern = "****";
        private int visibleChars = 4;
    }
}
