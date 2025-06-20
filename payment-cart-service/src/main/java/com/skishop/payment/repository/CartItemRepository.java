package com.skishop.payment.repository;

import com.skishop.payment.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    void deleteByCartId(UUID cartId);

    void deleteByCartIdAndProductId(UUID cartId, UUID productId);

    @Query("""
           SELECT SUM(ci.totalPrice) 
           FROM CartItem ci 
           WHERE ci.cart.id = :cartId
           """)
    Double calculateTotalByCartId(@Param("cartId") UUID cartId);

    @Query("""
           SELECT COUNT(ci) 
           FROM CartItem ci 
           WHERE ci.cart.id = :cartId
           """)
    int countByCartId(@Param("cartId") UUID cartId);

    @Query("""
           SELECT ci FROM CartItem ci 
           WHERE ci.cart.userId = :userId
           """)
    List<CartItem> findByCartUserId(@Param("userId") UUID userId);
}
