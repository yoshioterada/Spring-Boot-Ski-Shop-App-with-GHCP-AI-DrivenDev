package com.skishop.payment.service.impl;

import com.skishop.payment.dto.*;
import com.skishop.payment.entity.Cart;
import com.skishop.payment.entity.CartItem;
import com.skishop.payment.repository.CartRepository;
import com.skishop.payment.repository.CartItemRepository;
import com.skishop.payment.service.CartService;
import com.skishop.payment.mapper.CartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CART_CACHE_KEY_PREFIX = "cart:";
    private static final long CART_CACHE_TTL = 24; // hours

    @Override
    @Transactional
    public CartResponse addItemToCart(UUID userId, AddCartItemRequest request) {
        log.info("Adding item to cart for user: {}", userId);

        Cart cart = getOrCreateCart(userId);
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository
            .findByCartIdAndProductId(cart.getId(), request.productId());

        CartItem cartItem;
        if (existingItem.isPresent()) {
            // Update quantity of existing item
            cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.quantity());
            cartItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            cartItem.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new cart item  
            cartItem = CartItem.builder()
                .cart(cart)
                .productId(request.productId())
                .quantity(request.quantity())
                .unitPrice(BigDecimal.valueOf(10000)) // Default price - would call inventory service
                .productDetails(request.productDetails())
                .build();
            cartItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        cartItemRepository.save(cartItem);
        
        // Update cart total
        updateCartTotal(cart);
        cartRepository.save(cart);

        // Clear cache
        clearCartCache(userId);

        return getCartByUserId(userId);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        log.info("Updating cart item {} for user: {}", itemId, userId);

        Cart cart = getCartByUserIdEntity(userId);
        CartItem cartItem = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        cartItem.setQuantity(request.quantity());
        cartItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())));
        cartItem.setUpdatedAt(LocalDateTime.now());

        cartItemRepository.save(cartItem);

        // Update cart total
        updateCartTotal(cart);
        cartRepository.save(cart);

        // Clear cache
        clearCartCache(userId);

        return getCartByUserId(userId);
    }

    @Override
    @Transactional
    public void removeCartItem(UUID userId, UUID itemId) {
        log.info("Removing cart item {} for user: {}", itemId, userId);

        Cart cart = getCartByUserIdEntity(userId);
        CartItem cartItem = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        cartItemRepository.delete(cartItem);

        // Update cart total
        updateCartTotal(cart);
        cartRepository.save(cart);

        // Clear cache
        clearCartCache(userId);
    }

    @Override
    public CartResponse getCartByUserId(UUID userId) {
        log.debug("Getting cart for user: {}", userId);

        // Try to get from cache first using switch expression
        String cacheKey = CART_CACHE_KEY_PREFIX + userId.toString();
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        
        return switch (cachedData) {
            case CartResponse response -> {
                log.debug("Cart found in cache for user: {}", userId);
                yield response;
            }
            case null, default -> {
                // Get from database
                Cart cart = getCartByUserIdEntity(userId);
                CartResponse response = cartMapper.toCartResponse(cart);
                
                // Cache the result
                redisTemplate.opsForValue().set(cacheKey, response, CART_CACHE_TTL, TimeUnit.HOURS);
                
                yield response;
            }
        };
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);

        Cart cart = getCartByUserIdEntity(userId);
        cartItemRepository.deleteByCartId(cart.getId());

        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Clear cache
        clearCartCache(userId);
    }

    @Override
    @Transactional
    public Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now())
            .orElseGet(() -> createNewCart(userId));
    }

    @Override
    public void validateCartForPayment(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cart has expired");
        }

        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Validate inventory availability (would call inventory service)
        validateInventoryAvailability(items);
    }

    @Override
    @Transactional
    public void expireCart(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.setExpiresAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Clear cache if exists
        clearCartCache(cart.getUserId());
    }

    @Override
    @Transactional
    public void cleanupExpiredCarts() {
        log.info("Cleaning up expired carts");
        
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(LocalDateTime.now());
        
        // Java 21のSequenced Collectionsを活用
        if (!expiredCarts.isEmpty()) {
            expiredCarts.forEach(cart -> {
                cartItemRepository.deleteByCartId(cart.getId());
                cartRepository.delete(cart);
                clearCartCache(cart.getUserId());
            });
        }
        
        log.info("Cleaned up {} expired carts", expiredCarts.size());
    }

    private Cart getCartByUserIdEntity(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
            .orElseThrow(() -> new RuntimeException("Cart not found for user"));
    }

    private Cart createNewCart(UUID userId) {
        Cart cart = Cart.builder()
            .userId(userId)
            .totalAmount(BigDecimal.ZERO)
            .currency("JPY")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        
        return cartRepository.save(cart);
    }

    private void updateCartTotal(Cart cart) {
        Double total = cartItemRepository.calculateTotalByCartId(cart.getId());
        cart.setTotalAmount(total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO);
        cart.setUpdatedAt(LocalDateTime.now());
    }

    private void clearCartCache(UUID userId) {
        String cacheKey = CART_CACHE_KEY_PREFIX + userId.toString();
        redisTemplate.delete(cacheKey);
    }

    private void validateInventoryAvailability(List<CartItem> items) {
        // This would call the inventory service to validate availability
        // For now, just log
        log.info("Validating inventory availability for {} items", items.size());
    }
}
