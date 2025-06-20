package com.skishop.inventory.mapper;

import com.skishop.inventory.dto.ProductDTO;
import com.skishop.inventory.dto.request.ProductCreateRequest;
import com.skishop.inventory.entity.mongo.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 商品マッパー
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    /**
     * エンティティからDTOに変換
     */
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    ProductDTO toDTO(Product entity);

    /**
     * DTOからエンティティに変換
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Product toEntity(ProductDTO dto);

    /**
     * 作成リクエストからエンティティに変換
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Product toEntity(ProductCreateRequest request);
}
