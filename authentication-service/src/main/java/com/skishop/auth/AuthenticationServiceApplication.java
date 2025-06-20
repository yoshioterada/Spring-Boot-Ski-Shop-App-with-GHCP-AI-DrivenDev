package com.skishop.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Authentication Service Main Application
 * 
 * スキーショップ電子商取引プラットフォームの認証・認可サービス
 * - ユーザー認証（ログイン/ログアウト）
 * - JWTトークン生成と検証
 * - OAuth統合（Azure Entra ID、Google、Facebook、LINE）
 * - 多要素認証（MFA）
 * - セッション管理とセキュリティ
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class AuthenticationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationServiceApplication.class, args);
    }
}
