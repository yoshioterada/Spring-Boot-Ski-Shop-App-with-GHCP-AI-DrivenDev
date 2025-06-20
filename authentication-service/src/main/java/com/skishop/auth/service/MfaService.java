package com.skishop.auth.service;

import com.skishop.auth.dto.LoginResponse;
import com.skishop.auth.dto.MfaVerificationRequest;
import com.skishop.auth.entity.UserMFA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * MFA（多要素認証）サービス
 * TOTP（Time-based One-Time Password）による二要素認証を実装
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MfaService {

    private final EntityManager entityManager;

    /**
     * MFAコードを検証
     */
    public boolean verifyMfaCode(UUID userId, String code) {
        try {
            // UserMFAエンティティを取得
            UserMFA userMFA = entityManager.createQuery(
                "SELECT um FROM UserMFA um WHERE um.user.id = :userId AND um.isEnabled = true", 
                UserMFA.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (userMFA == null) {
                log.warn("MFA not enabled for user: {}", userId);
                return false;
            }

            // 簡易的なコード検証（実際の実装ではTOTP検証を行う）
            // TODO: TOTPライブラリを使用した検証を実装
            if (code != null && code.length() == 6 && code.matches("\\d{6}")) {
                log.info("MFA code verified for user: {}", userId);
                return true;
            }

            log.warn("Invalid MFA code for user: {}", userId);
            return false;

        } catch (Exception e) {
            log.error("Error verifying MFA code for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * MFAコードを検証（リクエストオブジェクト版）
     */
    public LoginResponse verifyMfaCode(MfaVerificationRequest request, HttpServletRequest httpRequest) {
        // セッションIDからユーザーIDを取得（実装簡略化）
        UUID userId = UUID.fromString(request.getSessionId()); // 実際はセッション管理が必要
        
        boolean isValid = verifyMfaCode(userId, request.getMfaCode());
        
        if (!isValid) {
            return LoginResponse.error("Invalid MFA code");
        }
        
        // MFA検証成功後のトークン生成処理（簡略化）
        // 実際の実装では認証サービスとの連携が必要
        return LoginResponse.success(null, null, null);
    }

    /**
     * MFAを有効化
     */
    public String enableMfa(UUID userId) {
        try {
            // TODO: TOTPライブラリを使用してシークレットキーを生成
            String secretKey = "MOCK_SECRET_KEY"; // 実際の実装では安全に生成する
            
            UserMFA userMFA = UserMFA.builder()
                .id(UUID.randomUUID())
                .secretKey(secretKey)
                .isEnabled(true)
                .build();
            
            entityManager.persist(userMFA);
            log.info("MFA enabled for user: {}", userId);
            
            return secretKey;
            
        } catch (Exception e) {
            log.error("Error enabling MFA for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to enable MFA");
        }
    }

    /**
     * MFAを無効化
     */
    public void disableMfa(UUID userId) {
        try {
            UserMFA userMFA = entityManager.createQuery(
                "SELECT um FROM UserMFA um WHERE um.user.id = :userId", 
                UserMFA.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (userMFA != null) {
                userMFA.setIsEnabled(false);
                entityManager.merge(userMFA);
                log.info("MFA disabled for user: {}", userId);
            }
            
        } catch (Exception e) {
            log.error("Error disabling MFA for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to disable MFA");
        }
    }
}
