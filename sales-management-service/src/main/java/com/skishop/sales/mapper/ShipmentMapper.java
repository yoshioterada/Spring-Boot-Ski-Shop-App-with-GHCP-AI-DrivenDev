package com.skishop.sales.mapper;

import com.skishop.sales.dto.response.ShipmentResponse;
import com.skishop.sales.entity.jpa.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 配送マッパー
 */
@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    /**
     * 配送エンティティをレスポンスDTOに変換
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "status", target = "status")
    ShipmentResponse toResponse(Shipment shipment);

    /**
     * 配送先住所エンティティをレスポンスDTOに変換
     */
    ShipmentResponse.ShippingAddressResponse toShippingAddressResponse(Shipment.ShippingAddress shippingAddress);
}
