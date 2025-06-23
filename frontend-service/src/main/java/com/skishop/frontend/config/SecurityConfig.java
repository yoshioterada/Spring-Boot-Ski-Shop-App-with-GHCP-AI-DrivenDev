package com.skishop.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security Configuration
 * 認証・認可の設定を行う
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.skishop.auth.enabled:true}")
    private boolean authEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!authEnabled) {
            // 認証無効モード（ローカル開発用）
            http
                .authorizeHttpRequests(authz -> authz
                    .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable);
        } else {
            // 認証有効モード（本番用）
            http
                .authorizeHttpRequests(authz -> authz
                    // 公開リソース
                    .requestMatchers(
                        "/", "/home", "/products/**", "/search/**", 
                        "/api/public/**", "/webjars/**", "/css/**", 
                        "/js/**", "/images/**", "/favicon.ico"
                    ).permitAll()
                    // 管理者専用
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    // その他は認証必要
                    .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/login?error=true")
                )
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .deleteCookies("JSESSIONID")
                    .invalidateHttpSession(true)
                )
                .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers("/api/webhook/**")
                )
                .sessionManagement(session -> session
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)
                );
        }
        
        return http.build();
    }
}
