package com.skishop.auth.service;

import com.skishop.auth.entity.SecurityLog;
import com.skishop.auth.entity.User;
import com.skishop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * セキュリティログサービス
 * 認証関連のセキュリティイベントをログに記録
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SecurityLogService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    /**
     * 認証試行ログを記録
     */
    public void logAuthenticationAttempt(UUID userId, String eventType, String ipAddress, 
                                       String userAgent, String details) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            SecurityLog securityLog = SecurityLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .eventType(eventType)
                .eventDetails(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status("COMPLETED")
                .build();

            entityManager.persist(securityLog);
            log.info("Security log recorded: {} for user {}", eventType, userId);
            
        } catch (Exception e) {
            log.error("Failed to record security log for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * セキュリティイベントログを記録（汎用）
     */
    public void logSecurityEvent(UUID userId, String eventType, String ipAddress, 
                               String userAgent, String details, String severity) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            SecurityLog securityLog = SecurityLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .eventType(eventType)
                .eventDetails(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(severity)
                .build();

            entityManager.persist(securityLog);
            log.info("Security event logged: {} for user {} with severity {}", eventType, userId, severity);
            
        } catch (Exception e) {
            log.error("Failed to record security event for user {}: {}", userId, e.getMessage());
        }
    }
}
