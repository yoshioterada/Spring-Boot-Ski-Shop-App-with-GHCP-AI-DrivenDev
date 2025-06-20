package com.skishop.inventory.controller;

import com.skishop.inventory.dto.CategoryDTO;
import com.skishop.inventory.dto.ProductDTO;
import com.skishop.inventory.dto.request.CategoryCreateRequest;
import com.skishop.inventory.dto.request.CategoryUpdateRequest;
import com.skishop.inventory.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * カテゴリ管理 API コントローラー
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Category API", description = "商品カテゴリ管理API")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * カテゴリ一覧取得
     */
    @GetMapping
    @Operation(summary = "カテゴリ一覧取得", description = "全カテゴリの一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "カテゴリ一覧の取得成功")
    })
    public ResponseEntity<Page<CategoryDTO>> getCategories(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "検索キーワード") @RequestParam(required = false) String search) {
        
        log.info("Getting categories list with search: {}", search);
        Page<CategoryDTO> categories = categoryService.getCategories(pageable, search);
        return ResponseEntity.ok(categories);
    }

    /**
     * カテゴリ詳細取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "カテゴリ詳細取得", description = "指定IDのカテゴリ詳細情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "カテゴリ詳細の取得成功"),
        @ApiResponse(responseCode = "404", description = "カテゴリが見つかりません")
    })
    public ResponseEntity<CategoryDTO> getCategoryById(
            @Parameter(description = "カテゴリID") @PathVariable String id) {
        
        log.info("Getting category detail for id: {}", id);
        CategoryDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * カテゴリに属する商品一覧取得
     */
    @GetMapping("/{id}/products")
    @Operation(summary = "カテゴリ商品一覧取得", description = "指定カテゴリに属する商品一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "商品一覧の取得成功"),
        @ApiResponse(responseCode = "404", description = "カテゴリが見つかりません")
    })
    public ResponseEntity<Page<ProductDTO>> getProductsByCategory(
            @Parameter(description = "カテゴリID") @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "在庫あり商品のみ") @RequestParam(defaultValue = "false") boolean inStockOnly) {
        
        log.info("Getting products for category: {}, inStockOnly: {}", id, inStockOnly);
        Page<ProductDTO> products = categoryService.getProductsByCategory(id, pageable, inStockOnly);
        return ResponseEntity.ok(products);
    }

    /**
     * カテゴリ作成
     */
    @PostMapping
    @Operation(summary = "カテゴリ作成", description = "新しいカテゴリを作成します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "カテゴリ作成成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です"),
        @ApiResponse(responseCode = "409", description = "既に存在するカテゴリ名です")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<CategoryDTO> createCategory(
            @Parameter(description = "カテゴリ作成リクエスト") @Valid @RequestBody CategoryCreateRequest request) {
        
        log.info("Creating new category: {}", request.getName());
        CategoryDTO category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    /**
     * カテゴリ更新
     */
    @PutMapping("/{id}")
    @Operation(summary = "カテゴリ更新", description = "指定カテゴリの情報を更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "カテゴリ更新成功"),
        @ApiResponse(responseCode = "404", description = "カテゴリが見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<CategoryDTO> updateCategory(
            @Parameter(description = "カテゴリID") @PathVariable String id,
            @Parameter(description = "カテゴリ更新リクエスト") @Valid @RequestBody CategoryUpdateRequest request) {
        
        log.info("Updating category: {}", id);
        CategoryDTO category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }

    /**
     * カテゴリ削除
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "カテゴリ削除", description = "指定カテゴリを削除します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "カテゴリ削除成功"),
        @ApiResponse(responseCode = "404", description = "カテゴリが見つかりません"),
        @ApiResponse(responseCode = "400", description = "商品が紐づいているため削除できません"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "カテゴリID") @PathVariable String id) {
        
        log.info("Deleting category: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
