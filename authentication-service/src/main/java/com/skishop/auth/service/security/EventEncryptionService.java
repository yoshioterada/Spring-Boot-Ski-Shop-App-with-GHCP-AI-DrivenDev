package com.skishop.auth.service.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;

/**
 * イベントデータ暗号化サービス
 * 
 * 機密データを含むイベントペイロードの暗号化・復号化
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventEncryptionService {
    
    private final StringEncryptor encryptor;
    private final ObjectMapper objectMapper;
    
    /**
     * ペイロードを暗号化
     */
    public String encryptPayload(Object payload) throws JsonProcessingException {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String encrypted = encryptor.encrypt(json);
            log.debug("Payload encrypted successfully, original size: {}, encrypted size: {}", 
                     json.length(), encrypted.length());
            return encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt payload: {}", e.getMessage(), e);
            throw new RuntimeException("Payload encryption failed", e);
        }
    }
    
    /**
     * ペイロードを復号化
     */
    public <T> T decryptPayload(String encryptedPayload, Class<T> clazz) 
            throws JsonProcessingException {
        try {
            String decrypted = encryptor.decrypt(encryptedPayload);
            T result = objectMapper.readValue(decrypted, clazz);
            log.debug("Payload decrypted successfully for type: {}", clazz.getSimpleName());
            return result;
        } catch (Exception e) {
            log.error("Failed to decrypt payload for type {}: {}", clazz.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Payload decryption failed", e);
        }
    }
    
    /**
     * 機密情報をマスキング
     */
    public String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "*".repeat(data != null ? data.length() : 0);
        }
        return data.substring(0, 2) + "*".repeat(data.length() - 4) + data.substring(data.length() - 2);
    }
    
    /**
     * ペイロードが暗号化済みかチェック
     */
    public boolean isEncrypted(String payload) {
        try {
            // 暗号化されたデータは通常Base64エンコードされており、JSON形式ではない
            objectMapper.readTree(payload);
            return false; // JSONとして解析できた場合は暗号化されていない
        } catch (JsonProcessingException e) {
            return true; // JSON解析に失敗した場合は暗号化されている可能性が高い
        }
    }
}
