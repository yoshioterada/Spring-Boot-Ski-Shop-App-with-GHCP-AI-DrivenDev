package com.skishop.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * セキュリティ設定
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final SkishopProperties skishopProperties;

    /**
     * パスワードエンコーダー
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 認証機能有効時のセキュリティフィルターチェーン
     */
    @Bean
    @ConditionalOnProperty(name = "skishop.authfunc.enable", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securityFilterChainWithAuth(HttpSecurity http) throws Exception {
        log.info("Security Configuration: Authentication ENABLED");
        
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 公開エンドポイント
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/health").permitAll()
                        // ユーザー登録は認証不要
                        .requestMatchers("/users/register").permitAll()
                        .requestMatchers("/users/verify-email").permitAll()
                        .requestMatchers("/users/forgot-password").permitAll()
                        .requestMatchers("/users/reset-password").permitAll()
                        // その他は認証必須
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                            // JWT設定は必要に応じて追加
                        }))
                .build();
    }

    /**
     * 認証機能無効時のセキュリティフィルターチェーン
     */
    @Bean
    @ConditionalOnProperty(name = "skishop.authfunc.enable", havingValue = "false")
    public SecurityFilterChain securityFilterChainWithoutAuth(HttpSecurity http) throws Exception {
        log.info("Security Configuration: Authentication DISABLED (Development Mode)");
        
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 全てのエンドポイントを許可（開発用）
                        .anyRequest().permitAll())
                .build();
    }
}
