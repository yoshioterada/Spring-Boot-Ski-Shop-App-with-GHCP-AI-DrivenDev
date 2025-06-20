package com.skishop.payment.controller;

import com.skishop.payment.dto.*;
import com.skishop.payment.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Valid @RequestBody AddCartItemRequest request,
            Authentication authentication) {
        
        log.info("Adding item to cart for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        CartResponse response = cartService.addItemToCart(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Item added to cart successfully"));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        
        log.info("Updating cart item: {} for user: {}", itemId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        CartResponse response = cartService.updateCartItem(userId, itemId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Cart item updated successfully"));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable UUID itemId,
            Authentication authentication) {
        
        log.info("Removing cart item: {} for user: {}", itemId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        cartService.removeCartItem(userId, itemId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Cart item removed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        log.debug("Getting cart for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        CartResponse response = cartService.getCartByUserId(userId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Cart retrieved successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        log.info("Clearing cart for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        cartService.clearCart(userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully"));
    }
}
