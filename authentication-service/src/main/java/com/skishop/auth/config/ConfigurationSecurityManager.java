package com.skishop.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 設定値のセキュリティマネージャー
 * 機密情報の適切なマスキングとセキュアなログ出力
 */
@Component
@Slf4j
public class ConfigurationSecurityManager {
    
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "secret", "key", "token", "credential", "connectionstring"
    );
    
    /**
     * 機密値のマスキング
     */
    public String maskSensitiveValue(String key, String value) {
        if (value == null) return null;
        
        if (isSensitiveKey(key)) {
            return value.length() > 4 ? 
                value.substring(0, 4) + "*".repeat(value.length() - 4) : 
                "*".repeat(value.length());
        }
        return value;
    }
    
    /**
     * 機密キーかどうかの判定
     */
    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lowerKey = key.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(lowerKey::contains);
    }
    
    /**
     * 設定を安全にログ出力
     */
    public void logConfigurationSafely(Map<String, String> config) {
        config.forEach((key, value) -> {
            String maskedValue = maskSensitiveValue(key, value);
            log.info("設定: {}={}", key, maskedValue);
        });
    }
    
    /**
     * 接続文字列の安全なマスキング
     */
    public String maskConnectionString(String connectionString) {
        if (connectionString == null) return null;
        
        return connectionString
            .replaceAll("Password=[^;]+", "Password=****")
            .replaceAll("password=[^;]+", "password=****")
            .replaceAll("SharedAccessKey=[^;]+", "SharedAccessKey=****")
            .replaceAll("client_secret=[^&]+", "client_secret=****");
    }
    
    /**
     * JWT秘密鍵の安全な検証
     */
    public boolean isValidJwtSecret(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            log.error("JWT secret is null or empty");
            return false;
        }
        
        // 最低256ビット（32文字）を要求
        if (jwtSecret.length() < 32) {
            log.error("JWT secret is too short. Minimum 32 characters required, but got: {}", jwtSecret.length());
            return false;
        }
        
        // 弱いパスワードのチェック
        String lowerSecret = jwtSecret.toLowerCase();
        String[] weakPatterns = {"test", "password", "secret", "123456", "qwerty"};
        
        for (String pattern : weakPatterns) {
            if (lowerSecret.contains(pattern)) {
                log.warn("JWT secret contains weak pattern: {}", pattern);
                return false;
            }
        }
        
        log.info("JWT secret validation passed. Length: {} characters", jwtSecret.length());
        return true;
    }
    
    /**
     * 環境変数の安全な取得
     */
    public String getSecureEnvironmentVariable(String varName, String defaultValue) {
        String value = System.getenv(varName);
        if (value == null || value.trim().isEmpty()) {
            log.warn("Environment variable '{}' is not set, using default", varName);
            return defaultValue;
        }
        
        log.info("Environment variable '{}' loaded successfully", varName);
        return value;
    }
}
