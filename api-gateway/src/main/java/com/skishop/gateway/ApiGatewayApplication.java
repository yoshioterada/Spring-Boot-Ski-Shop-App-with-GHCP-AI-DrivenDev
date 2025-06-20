package com.skishop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

/**
 * API Gateway Application
 * 
 * <p>スキーショップのマイクロサービス群への統一アクセスポイント</p>
 * 
 * <h3>主要機能:</h3>
 * <ul>
 *   <li>リクエストルーティング - 各マイクロサービスへの動的ルーティング</li>
 *   <li>認証・認可 - JWT認証とOAuth2による統一認証</li>
 *   <li>レート制限 - Redis基盤のレート制限機能</li>
 *   <li>サーキットブレーカー - 障害時のフォールバック処理</li>
 *   <li>ロギング・メトリクス - 分散トレーシングと監視</li>
 * </ul>
 * 
 * <p>Java 21の最新機能を活用した設定管理とルート定義</p>
 * 
 * @since 1.0.0
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * カスタムルート設定
     * 
     * <p>Java 21のvar型推論とText Blocksを活用した動的ルート設定</p>
     * <p>デフォルトのapplication.ymlルート設定に加え、
     * 動的ルート設定やフィルタリングロジックを定義</p>
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // ユーザー管理サービス - 認証必須、高レート制限
            .route("user-service", r -> r.path("/api/users/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("user-service")
                        .setFallbackUri("forward:/fallback/user"))
                    .retry(retryConfig -> retryConfig.setRetries(3))
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("${app.services.user-management:http://localhost:8081}"))
            
            // 認証サービス - 重要度最高、高可用性
            .route("auth-service", r -> r.path("/api/auth/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("auth-service")
                        .setFallbackUri("forward:/fallback/auth"))
                    .retry(retryConfig -> retryConfig.setRetries(2)))
                .uri("${app.services.authentication:http://localhost:8080}"))
            
            // 在庫管理サービス - 商品データの中核
            .route("inventory-service", r -> r.path("/api/products/**", "/api/inventory/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("inventory-service")
                        .setFallbackUri("forward:/fallback/inventory"))
                    .retry(retryConfig -> retryConfig.setRetries(3)))
                .uri("${app.services.inventory-management:http://localhost:8082}"))
            
            // 販売管理サービス - ビジネスクリティカル
            .route("sales-service", r -> r.path("/api/orders/**", "/api/reports/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("sales-service")
                        .setFallbackUri("forward:/fallback/sales"))
                    .retry(retryConfig -> retryConfig.setRetries(3)))
                .uri("${app.services.sales-management:http://localhost:8083}"))
            
            // 支払い・カートサービス - セキュリティ重要
            .route("payment-cart-service", r -> r.path("/api/cart/**", "/api/payments/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("payment-cart-service")
                        .setFallbackUri("forward:/fallback/payment"))
                    .retry(retryConfig -> retryConfig.setRetries(2)))
                .uri("${app.services.payment-cart:http://localhost:8084}"))
            
            // ポイントサービス
            .route("point-service", r -> r.path("/api/points/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("point-service")
                        .setFallbackUri("forward:/fallback/point")))
                .uri("${app.services.point:http://localhost:8085}"))
            
            // クーポンサービス
            .route("coupon-service", r -> r.path("/api/coupons/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("coupon-service")
                        .setFallbackUri("forward:/fallback/coupon")))
                .uri("${app.services.coupon:http://localhost:8086}"))
            
            // AIサポートサービス - AI機能、レート制限必須
            .route("ai-support-service", r -> r.path("/api/recommendations/**", "/api/search/**", "/api/chat/**", "/api/analytics/**")
                .filters(f -> f
                    .circuitBreaker(c -> c.setName("ai-support-service")
                        .setFallbackUri("forward:/fallback/ai"))
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("${app.services.ai-support:http://localhost:8087}"))
            
            .build();
    }

    /**
     * Redis レート制限設定
     * 
     * <p>Java 21のレコード型を使った設定例（将来拡張予定）</p>
     * <p>10 requests/秒、バースト20、1秒間隔で補充</p>
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // Java 21の将来バージョンではレコード型での設定を検討
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * ユーザーキー解決設定
     * 
     * <p>Java 21のvar型推論、switch式、パターンマッチングを活用</p>
     * <p>認証トークンからユーザーIDを抽出してレート制限キーとして使用</p>
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            var headers = exchange.getRequest().getHeaders();
            var userIdHeader = headers.getFirst("X-User-ID");
            
            // Java 21のnull安全なチェックとswitch式の組み合わせ
            return switch (userIdHeader) {
                case String userId when !userId.isBlank() -> Mono.just(userId);
                case null, default -> 
                    exchange.getPrincipal()
                        .cast(JwtAuthenticationToken.class)
                        .map(JwtAuthenticationToken::getToken)
                        .map(jwt -> jwt.getClaimAsString("sub"))
                        .switchIfEmpty(Mono.just("anonymous"));
            };
        };
    }
}
