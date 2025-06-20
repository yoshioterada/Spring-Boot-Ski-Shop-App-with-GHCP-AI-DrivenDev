package com.skishop.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

/**
 * Microsoft Entra ID セキュリティ設定
 * Microsoft Entra ID Spring Boot Starterを使用した認証・認可設定
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.protect.authenticated:/token_details,/call_graph,/profile,/admin/**}")
    private String[] protectedRoutes;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // カスタムセキュリティ設定
        http.authorizeHttpRequests(authz -> authz
                // 保護されたルート
                .requestMatchers(protectedRoutes).authenticated()
                // 管理者専用ルート
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // API エンドポイント
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/user/**").authenticated()
                // 公開ルート
                .requestMatchers("/", "/home", "/public/**", "/health", "/actuator/health").permitAll()
                // その他はすべて認証必要
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/azure")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                )
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/actuator/**")
            )
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            );

        return http.build();
    }

    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
