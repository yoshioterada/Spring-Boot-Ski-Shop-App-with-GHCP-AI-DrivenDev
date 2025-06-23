package com.skishop.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Azure SkiShop Frontend Service Application
 * 
 * フロントエンドサービスのメインエントリポイント
 * 顧客向けWebサイトと管理者向け管理画面を提供する
 */
@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
@EnableCaching
public class FrontendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendServiceApplication.class, args);
    }
}
