package com.skishop.inventory.service;

import com.skishop.inventory.dto.ProductDTO;
import com.skishop.inventory.dto.CategoryDTO;
import com.skishop.inventory.dto.request.ProductCreateRequest;
import com.skishop.inventory.entity.mongo.Product;
import com.skishop.inventory.entity.jpa.Inventory;
import com.skishop.inventory.entity.jpa.Price;
import com.skishop.inventory.repository.mongo.ProductRepository;
import com.skishop.inventory.repository.jpa.InventoryRepository;
import com.skishop.inventory.repository.jpa.PriceRepository;
import com.skishop.inventory.mapper.ProductMapper;
import com.skishop.inventory.exception.ResourceNotFoundException;
import com.skishop.inventory.exception.DuplicateResourceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PriceRepository priceRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final EventPublisherService eventPublisherService;

    /**
     * 商品一覧取得
     */
    @Cacheable(value = "products", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductDTO> findAll(Pageable pageable) {
        log.debug("商品一覧取得 - ページ: {}", pageable);
        Page<Product> products = productRepository.findByActiveTrue(pageable);
        return products.map(this::enrichProductWithDetails);
    }

    /**
     * 商品検索
     */
    @Cacheable(value = "productSearch", key = "#keyword + '_' + #pageable.pageNumber")
    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        log.debug("商品検索 - キーワード: {}, ページ: {}", keyword, pageable);
        Page<Product> products = productRepository.searchProducts(keyword, pageable);
        return products.map(this::enrichProductWithDetails);
    }

    /**
     * 商品詳細取得
     */
    @Cacheable(value = "product", key = "#id")
    public ProductDTO findById(String id) {
        log.debug("商品詳細取得 - ID: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("商品が見つかりません: " + id));
        
        return enrichProductWithDetails(product);
    }

    /**
     * SKUで商品取得
     */
    @Cacheable(value = "productBySku", key = "#sku")
    public ProductDTO findBySku(String sku) {
        log.debug("SKUで商品取得 - SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
            .orElseThrow(() -> new ResourceNotFoundException("商品が見つかりません: " + sku));
        
        return enrichProductWithDetails(product);
    }

    /**
     * カテゴリ別商品取得
     */
    @Cacheable(value = "productsByCategory", key = "#categoryId + '_' + #pageable.pageNumber")
    public Page<ProductDTO> findByCategory(String categoryId, Pageable pageable) {
        log.debug("カテゴリ別商品取得 - カテゴリID: {}, ページ: {}", categoryId, pageable);
        Page<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
        return products.map(this::enrichProductWithDetails);
    }

    /**
     * 商品作成
     */
    @Transactional
    @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    public ProductDTO createProduct(ProductCreateRequest request) {
        log.info("商品作成開始 - SKU: {}", request.getSku());

        // SKU重複チェック
        if (productRepository.existsBySkuAndActiveTrue(request.getSku())) {
            throw new DuplicateResourceException("SKUが既に存在します: " + request.getSku());
        }

        // カテゴリ存在チェック
        categoryService.findById(request.getCategoryId());

        // 商品エンティティ作成
        Product product = productMapper.toEntity(request);
        product.prePersist();
        product = productRepository.save(product);

        // 価格情報作成
        Price price = Price.builder()
            .productId(product.getId())
            .regularPrice(request.getPrice().getRegularPrice())
            .salePrice(request.getPrice().getSalePrice())
            .saleStartDate(request.getPrice().getSaleStartDate())
            .saleEndDate(request.getPrice().getSaleEndDate())
            .currencyCode(request.getPrice().getCurrencyCode())
            .build();
        price.prePersist();
        priceRepository.save(price);

        // 在庫情報作成
        Inventory inventory = Inventory.builder()
            .productId(product.getId())
            .quantity(request.getInventory().getQuantity())
            .locationCode(request.getInventory().getLocationCode())
            .build();
        inventory.prePersist();
        inventoryRepository.save(inventory);

        // イベント発行
        eventPublisherService.publishProductCreatedEvent(product.getId());

        log.info("商品作成完了 - ID: {}, SKU: {}", product.getId(), product.getSku());
        return enrichProductWithDetails(product);
    }

    /**
     * 商品にその他の詳細情報を付加
     */
    private ProductDTO enrichProductWithDetails(Product product) {
        // カテゴリ情報取得
        CategoryDTO category = null;
        if (product.getCategoryId() != null) {
            try {
                category = categoryService.findById(product.getCategoryId());
            } catch (ResourceNotFoundException e) {
                log.warn("カテゴリが見つかりません - ID: {}", product.getCategoryId());
            }
        }

        // 価格情報取得（Java 21のOptional改善記法）
        ProductDTO.PriceInfoDTO priceInfo = priceRepository.findByProductIdAndIsActiveTrue(product.getId())
            .map(p -> new ProductDTO.PriceInfoDTO(
                p.getRegularPrice(),
                p.getSalePrice(),
                p.getCurrentPrice(),
                p.getCurrencyCode(),
                p.isOnSale(),
                p.getSaleStartDate(),
                p.getSaleEndDate()
            ))
            .orElse(null);

        // 在庫情報取得（Java 21のOptional改善記法）
        ProductDTO.InventoryInfoDTO inventoryInfo = inventoryRepository.findByProductId(product.getId())
            .map(inv -> new ProductDTO.InventoryInfoDTO(
                inv.getStatus().name(),
                inv.getQuantity(),
                inv.getAvailableQuantity(),
                inv.getLocationCode()
            ))
            .orElse(null);

        // 商品DTOを作成（recordのコンストラクタを使用）
        return new ProductDTO(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getBrand(),
            product.getAttributes(),
            product.getTags(),
            category,
            priceInfo,
            inventoryInfo,
            null, // images - 必要に応じて実装
            null, // imageUrl - 必要に応じて実装
            product.getActive(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    /**
     * 複数商品の詳細情報を一括取得
     */
    public List<ProductDTO> findByIds(List<String> ids) {
        log.debug("複数商品取得 - IDs: {}", ids);
        List<Product> products = productRepository.findByIdInAndActiveTrue(ids);
        return products.stream()
            .map(this::enrichProductWithDetails)
            .toList();
    }
}
