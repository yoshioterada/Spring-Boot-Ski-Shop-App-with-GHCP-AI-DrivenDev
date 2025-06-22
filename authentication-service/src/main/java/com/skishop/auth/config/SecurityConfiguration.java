package com.skishop.auth.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.salt.RandomSaltGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * セキュリティ暗号化設定
 * 
 * 機密情報の暗号化とセキュアな設定管理
 */
@Configuration
@EnableConfigurationProperties(EncryptionProperties.class)
public class SecurityConfiguration {
    
    @Bean
    public StringEncryptor stringEncryptor(EncryptionProperties properties) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword(properties.getPassword());
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setKeyObtentionIterations(1000);
        encryptor.setPoolSize(4);
        encryptor.setProviderName("SunJCE");
        encryptor.setSaltGenerator(new RandomSaltGenerator());
        encryptor.setStringOutputType("base64");
        return encryptor;
    }
}
