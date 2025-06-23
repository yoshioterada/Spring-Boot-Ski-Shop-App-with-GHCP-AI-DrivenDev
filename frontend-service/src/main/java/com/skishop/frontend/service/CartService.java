package com.skishop.frontend.service;

import com.skishop.frontend.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;
import java.util.Map;

/**
 * カート関連のAPIクライアントサービス（payment-cart-serviceと連携）
 * payment-cart-serviceが利用できない場合は適切なエラーメッセージを表示
 */
@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
    private final RestTemplate restTemplate;
    private final String paymentCartServiceUrl;
    
    // カスタム例外クラス
    public static class CartServiceUnavailableException extends RuntimeException {
        public CartServiceUnavailableException(String message) {
            super(message);
        }
    }

    public CartService(RestTemplate restTemplate, 
                      @Value("${payment-cart-service.base-url:http://localhost:8085}") String paymentCartServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentCartServiceUrl = paymentCartServiceUrl;
    }

    /**
     * カート取得
     */
    public CartDto getCart(String userId) {
        try {
            String url = paymentCartServiceUrl + "/api/v1/cart";
            HttpHeaders headers = createHeaders(userId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ApiResponse<CartDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, 
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<CartDto>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse<CartDto> apiResponse = response.getBody();
                if (apiResponse.isSuccess()) {
                    return apiResponse.data();
                }
            }
            
            return createEmptyCart();
        } catch (ResourceAccessException ex) {
            logger.error("payment-cart-service is not available for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("HTTP error when getting cart for user {}: {} - {}", userId, ex.getStatusCode(), ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (Exception ex) {
            logger.error("Unexpected error when getting cart for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        }
    }

    /**
     * カートにアイテム追加
     */
    public CartDto addToCart(String userId, AddToCartRequest request) {
        try {
            String url = paymentCartServiceUrl + "/api/v1/cart/items";
            HttpHeaders headers = createHeaders(userId);
            
            // payment-cart-serviceのAPI仕様に合わせてリクエストを変換
            AddCartItemRequest cartRequest = new AddCartItemRequest(
                request.getProductIdAsUUID(),
                request.quantity(),
                null // productDetailsは後で設定可能
            );
            
            HttpEntity<AddCartItemRequest> entity = new HttpEntity<>(cartRequest, headers);
            
            ResponseEntity<ApiResponse<CartDto>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<CartDto>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse<CartDto> apiResponse = response.getBody();
                if (apiResponse.isSuccess()) {
                    return apiResponse.data();
                }
            }
            
            logger.warn("Failed to add item to cart for user {}: API returned unsuccessful response", userId);
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (ResourceAccessException ex) {
            logger.warn("payment-cart-service is not available for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("HTTP error when adding item to cart for user {}: {} - {}", userId, ex.getStatusCode(), ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (Exception ex) {
            logger.error("Unexpected error when adding item to cart for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        }
    }

    /**
     * カートアイテム更新
     */
    public CartDto updateCartItem(String userId, String itemId, int quantity) {
        try {
            String url = paymentCartServiceUrl + "/api/v1/cart/items/" + itemId;
            HttpHeaders headers = createHeaders(userId);
            
            UpdateCartItemRequest request = new UpdateCartItemRequest(quantity);
            HttpEntity<UpdateCartItemRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<ApiResponse<CartDto>> response = restTemplate.exchange(
                url, HttpMethod.PUT, entity,
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<CartDto>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse<CartDto> apiResponse = response.getBody();
                if (apiResponse.isSuccess()) {
                    return apiResponse.data();
                }
            }
            
            logger.warn("Failed to update cart item for user {}: API returned unsuccessful response", userId);
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (ResourceAccessException ex) {
            logger.warn("payment-cart-service is not available for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("HTTP error when updating cart item for user {}: {} - {}", userId, ex.getStatusCode(), ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (Exception ex) {
            logger.error("Unexpected error when updating cart item {} for user {}: {}", itemId, userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        }
    }

    /**
     * カートアイテム削除
     */
    public void removeFromCart(String userId, String itemId) {
        try {
            String url = paymentCartServiceUrl + "/api/v1/cart/items/" + itemId;
            HttpHeaders headers = createHeaders(userId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                url, HttpMethod.DELETE, entity,
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<Void>>() {}
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Failed to remove cart item for user {}: API returned unsuccessful response", userId);
                throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
            }
        } catch (ResourceAccessException ex) {
            logger.warn("payment-cart-service is not available for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("HTTP error when removing cart item for user {}: {} - {}", userId, ex.getStatusCode(), ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (Exception ex) {
            logger.error("Unexpected error when removing cart item {} for user {}: {}", itemId, userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        }
    }

    /**
     * カートクリア
     */
    public void clearCart(String userId) {
        try {
            String url = paymentCartServiceUrl + "/api/v1/cart";
            HttpHeaders headers = createHeaders(userId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                url, HttpMethod.DELETE, entity,
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<Void>>() {}
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Failed to clear cart for user {}: API returned unsuccessful response", userId);
                throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
            }
        } catch (ResourceAccessException ex) {
            logger.warn("payment-cart-service is not available for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("HTTP error when clearing cart for user {}: {} - {}", userId, ex.getStatusCode(), ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        } catch (Exception ex) {
            logger.error("Unexpected error when clearing cart for user {}: {}", userId, ex.getMessage());
            throw new CartServiceUnavailableException("現在カートサービスは一時的に利用できません。しばらく時間をおいて再度お試しください。");
        }
    }

    /**
     * HTTPヘッダーを作成（JWT認証や其他必要なヘッダー）
     */
    private HttpHeaders createHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // 今後JWT実装時に、Authorization: Bearer <token>を追加
        // headers.setBearerAuth(jwtToken);
        
        // 今は仮でUser-IDヘッダーを追加（実際の実装ではJWTから取得）
        headers.set("X-User-ID", userId);
        
        return headers;
    }

    /**
     * 空のカートを作成
     */
    private CartDto createEmptyCart() {
        return new CartDto(
            null, // id
            null, // userId
            Collections.emptyList(), // items
            0, // totalAmount
            "JPY", // currency
            0 // itemCount
        );
    }
}
