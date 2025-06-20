package com.skishop.inventory.service;

import com.skishop.inventory.dto.CategoryDTO;
import com.skishop.inventory.dto.ProductDTO;
import com.skishop.inventory.dto.request.CategoryCreateRequest;
import com.skishop.inventory.dto.request.CategoryUpdateRequest;
import com.skishop.inventory.entity.mongo.Category;
import com.skishop.inventory.repository.mongo.CategoryRepository;
import com.skishop.inventory.mapper.CategoryMapper;
import com.skishop.inventory.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * カテゴリサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * 全カテゴリ取得
     */
    @Cacheable(value = "categories")
    public List<CategoryDTO> findAll() {
        log.debug("全カテゴリ取得");
        List<Category> categories = categoryRepository.findByActiveTrueOrderByPathAsc();
        return categories.stream()
            .map(categoryMapper::toDTO)
            .toList();
    }

    /**
     * ページネーション付きカテゴリ取得
     */
    public Page<CategoryDTO> getCategories(Pageable pageable, String searchKeyword) {
        log.debug("ページネーション付きカテゴリ取得: keyword={}", searchKeyword);
        // モック実装
        List<CategoryDTO> mockCategories = findAll();
        return Page.empty();
    }

    /**
     * カテゴリをIDで取得
     */
    public CategoryDTO getCategoryById(String id) {
        log.debug("カテゴリをIDで取得: {}", id);
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: " + id));
        return categoryMapper.toDTO(category);
    }

    /**
     * カテゴリ別の商品取得
     */
    public Page<ProductDTO> getProductsByCategory(String categoryId, Pageable pageable, boolean includeInactive) {
        log.debug("カテゴリ別の商品取得: categoryId={}, includeInactive={}", categoryId, includeInactive);
        // モック実装
        return Page.empty();
    }

    /**
     * カテゴリ作成（リクエストから）
     */
    @Transactional
    @CacheEvict(value = {"categories", "rootCategories"}, allEntries = true)
    public CategoryDTO createCategory(CategoryCreateRequest request) {
        log.info("カテゴリ作成開始 - 名前: {}", request.name());
        
        // リクエストからCategoryDTOを作成
        CategoryDTO categoryDTO = new CategoryDTO(
            null,
            request.name(),
            request.description(),
            request.parentId(),
            null,
            List.of(),
            0,
            request.name(),
            true,
            null,
            null,
            0L
        );
        
        return create(categoryDTO);
    }

    /**
     * カテゴリ作成
     */
    @Transactional
    @CacheEvict(value = {"categories", "rootCategories", "childCategories", "categoryHierarchy"}, allEntries = true)
    public CategoryDTO create(CategoryDTO categoryDTO) {
        log.info("カテゴリ作成開始 - 名前: {}", categoryDTO.name());

        Category category = categoryMapper.toEntity(categoryDTO);
        
        // 親カテゴリ情報設定
        if (categoryDTO.parentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("親カテゴリが見つかりません: " + categoryDTO.parentId()));
            
            category.setLevel(parent.getLevel() + 1);
            category.setPath(parent.getPath() + "/" + category.getName());
        } else {
            category.setLevel(0);
            category.setPath(category.getName());
        }

        category.prePersist();
        category = categoryRepository.save(category);

        log.info("カテゴリ作成完了 - ID: {}, 名前: {}", category.getId(), category.getName());
        return categoryMapper.toDTO(category);
    }

    /**
     * カテゴリ更新
     */
    @Transactional
    @CacheEvict(value = {"categories", "rootCategories"}, allEntries = true)
    public CategoryDTO updateCategory(String id, CategoryUpdateRequest request) {
        log.info("カテゴリ更新開始 - ID: {}, 名前: {}", id, request.name());
        
        Category existingCategory = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: " + id));
        
        // 基本フィールドのみ更新（エンティティに存在するフィールドのみ）
        existingCategory.setName(request.name());
        existingCategory.setDescription(request.description());
        existingCategory.setParentId(request.parentId());
        
        Category savedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toDTO(savedCategory);
    }

    /**
     * カテゴリ削除
     */
    @Transactional
    @CacheEvict(value = {"categories", "rootCategories"}, allEntries = true)
    public void deleteCategory(String id) {
        log.info("カテゴリ削除 - ID: {}", id);
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: " + id));
        
        // 論理削除
        category.setActive(false);
        categoryRepository.save(category);
        
        log.info("カテゴリ削除完了 - ID: {}", id);
    }

    /**
     * カテゴリ詳細取得
     */
    @Cacheable(value = "category", key = "#id")
    public CategoryDTO findById(String id) {
        log.debug("カテゴリ詳細取得 - ID: {}", id);
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: " + id));
        
        return enrichCategoryWithChildren(category);
    }

    /**
     * 子カテゴリ取得
     */
    @Cacheable(value = "childCategories", key = "#parentId")
    public List<CategoryDTO> findByParentId(String parentId) {
        log.debug("子カテゴリ取得 - 親ID: {}", parentId);
        List<Category> categories = categoryRepository.findByParentIdAndActiveTrueOrderByName(parentId);
        return categories.stream()
            .map(categoryMapper::toDTO)
            .toList();
    }

    /**
     * カテゴリ階層取得
     */
    @Cacheable(value = "categoryHierarchy", key = "#parentPath")
    public List<CategoryDTO> findCategoryHierarchy(String parentPath) {
        log.debug("カテゴリ階層取得 - パス: {}", parentPath);
        List<Category> categories = categoryRepository.findCategoryHierarchy("^" + parentPath);
        return categories.stream()
            .map(categoryMapper::toDTO)
            .toList();
    }

    /**
     * カテゴリに子カテゴリ情報を付加
     */
    private CategoryDTO enrichCategoryWithChildren(Category category) {
        CategoryDTO dto = categoryMapper.toDTO(category);
        
        // 子カテゴリ取得
        List<Category> children = categoryRepository.findByParentIdAndActiveTrueOrderByName(category.getId());
        List<CategoryDTO> childrenDTOs = children.stream()
            .map(categoryMapper::toDTO)
            .toList();
        
        // 子カテゴリを含む新しいDTOを作成
        return new CategoryDTO(
            dto.id(),
            dto.name(),
            dto.description(),
            dto.parentId(),
            dto.parent(),
            childrenDTOs,
            dto.level(),
            dto.path(),
            dto.active(),
            dto.createdAt(),
            dto.updatedAt(),
            dto.productCount()
        );
    }
}
