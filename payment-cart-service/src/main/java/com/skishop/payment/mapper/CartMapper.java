package com.skishop.payment.mapper;

import com.skishop.payment.dto.CartResponse;
import com.skishop.payment.dto.CartItemResponse;
import com.skishop.payment.entity.Cart;
import com.skishop.payment.entity.CartItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "itemCount", expression = "java(cart.getItems() != null ? cart.getItems().size() : 0)")
    CartResponse toCartResponse(Cart cart);

    List<CartItemResponse> toCartItemResponseList(List<CartItem> cartItems);

    CartItemResponse toCartItemResponse(CartItem cartItem);
}
