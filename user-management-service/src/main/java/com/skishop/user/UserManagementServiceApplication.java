package com.skishop.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ユーザー管理サービスのメインアプリケーションクラス
 * 
 * このサービスは以下の機能を提供します：
 * <ul>
 * <li>ユーザー登録と認証</li>
 * <li>ユーザープロファイル管理</li>
 * <li>権限管理</li>
 * <li>ユーザー設定管理</li>
 * <li>アクティビティ追跡</li>
 * </ul>
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableJpaAuditing
public class UserManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementServiceApplication.class, args);
    }
}
