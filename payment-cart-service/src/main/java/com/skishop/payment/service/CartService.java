package com.skishop.payment.service;

import com.skishop.payment.dto.*;
import com.skishop.payment.entity.Cart;

import java.util.UUID;

public interface CartService {

    CartResponse addItemToCart(UUID userId, AddCartItemRequest request);

    CartResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request);

    void removeCartItem(UUID userId, UUID itemId);

    CartResponse getCartByUserId(UUID userId);

    void clearCart(UUID userId);

    Cart getOrCreateCart(UUID userId);

    void validateCartForPayment(UUID cartId);

    void expireCart(UUID cartId);

    void cleanupExpiredCarts();
}
