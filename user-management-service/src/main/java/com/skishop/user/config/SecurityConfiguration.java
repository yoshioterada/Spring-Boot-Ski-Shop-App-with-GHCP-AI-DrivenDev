package com.skishop.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * セキュリティ設定クラス
 * 暗号化とマスキング機能の設定
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableEncryptableProperties
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    private final SecurityProperties securityProperties;

    /**
     * 文字列暗号化Bean
     */
    @Bean
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        
        SecurityProperties.Encryption encryptionProps = securityProperties.getEncryption();
        
        encryptor.setPassword(encryptionProps.getPassword());
        encryptor.setAlgorithm(encryptionProps.getAlgorithm());
        encryptor.setKeyObtentionIterations(encryptionProps.getKeyObtentionIterations());
        encryptor.setPoolSize(encryptionProps.getPoolSize());
        encryptor.setProviderName(encryptionProps.getProviderName());
        encryptor.setSaltGenerator(new org.jasypt.salt.RandomSaltGenerator());
        encryptor.setStringOutputType(encryptionProps.getStringOutputType());
        
        return encryptor;
    }

    /**
     * 設定セキュリティマネージャー
     */
    @Bean
    public ConfigurationSecurityManager configurationSecurityManager() {
        return new ConfigurationSecurityManager(securityProperties);
    }

    /**
     * 設定値セキュリティ管理クラス
     */
    public static class ConfigurationSecurityManager {
        
        private final SecurityProperties securityProperties;
        
        public ConfigurationSecurityManager(SecurityProperties securityProperties) {
            this.securityProperties = securityProperties;
        }
        
        /**
         * 機密値のマスキング
         */
        public String maskSensitiveValue(String key, String value) {
            if (!securityProperties.getConfigurationMasking().isEnabled()) {
                return value;
            }
            
            if (isSensitiveKey(key)) {
                return maskValue(value, 
                    securityProperties.getConfigurationMasking().getVisibleChars(),
                    securityProperties.getConfigurationMasking().getMaskPattern());
            }
            return value;
        }
        
        /**
         * キーが機密情報かどうかの判定
         */
        private boolean isSensitiveKey(String key) {
            if (key == null) return false;
            
            String lowerKey = key.toLowerCase();
            Set<String> sensitiveKeys = securityProperties.getConfigurationMasking().getSensitiveKeys();
            
            return sensitiveKeys.stream().anyMatch(lowerKey::contains);
        }
        
        /**
         * 値のマスキング処理
         */
        private String maskValue(String value, int visibleChars, String maskPattern) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            
            if (value.length() <= visibleChars) {
                return maskPattern.repeat(value.length());
            }
            
            return value.substring(0, visibleChars) + maskPattern.repeat(value.length() - visibleChars);
        }
        
        /**
         * 設定の安全なログ出力
         */
        public void logConfigurationSafely(Map<String, String> config) {
            config.forEach((key, value) -> {
                String maskedValue = maskSensitiveValue(key, value);
                log.info("設定: {}={}", key, maskedValue);
            });
        }
    }
}
