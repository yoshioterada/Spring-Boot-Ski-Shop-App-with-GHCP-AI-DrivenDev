package com.skishop.payment.repository;

import com.skishop.payment.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndExpiresAtAfter(UUID userId, LocalDateTime currentTime);

    Optional<Cart> findByUserId(UUID userId);

    @Query("""
           SELECT c FROM Cart c 
           WHERE c.expiresAt < :currentTime
           """)
    List<Cart> findExpiredCarts(@Param("currentTime") LocalDateTime currentTime);

    void deleteByUserIdAndExpiresAtBefore(UUID userId, LocalDateTime expirationTime);

    @Query("""
           SELECT COUNT(ci) 
           FROM CartItem ci 
           WHERE ci.cart.id = :cartId
           """)
    int countItemsByCartId(@Param("cartId") UUID cartId);

    @Query("""
           SELECT c FROM Cart c 
           JOIN FETCH c.items 
           WHERE c.userId = :userId
           """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") UUID userId);
}
