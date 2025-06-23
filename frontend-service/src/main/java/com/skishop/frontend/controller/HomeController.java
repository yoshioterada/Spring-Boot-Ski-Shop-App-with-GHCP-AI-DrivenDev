package com.skishop.frontend.controller;

import com.skishop.frontend.dto.CategoryDto;
import com.skishop.frontend.dto.ProductDto;
import com.skishop.frontend.dto.ProductSearchResponse;
import com.skishop.frontend.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * ホームページとメインページのコントローラー
 */
@Controller
public class HomeController {

    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * ホームページ表示
     */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        try {
            // おすすめ商品を取得（同期実行）
            ProductSearchResponse featuredResponse = productService.getProducts(0, 8, "rating", null, null)
                    .block();
            ProductSearchResponse newResponse = productService.getProducts(0, 8, "createdAt", null, null)
                    .block();
            List<CategoryDto> categoryList = productService.getCategories()
                    .block();

            // モデルに追加（nullチェック付き）
            model.addAttribute("featuredProducts", 
                featuredResponse != null && featuredResponse.products() != null ? 
                    featuredResponse.products() : new ArrayList<>());
            model.addAttribute("newProducts", 
                newResponse != null && newResponse.products() != null ? 
                    newResponse.products() : new ArrayList<>());
            model.addAttribute("categories", 
                categoryList != null ? categoryList : new ArrayList<>());

        } catch (Exception e) {
            // エラーが発生した場合のフォールバック
            model.addAttribute("featuredProducts", new ArrayList<>());
            model.addAttribute("newProducts", new ArrayList<>());
            model.addAttribute("categories", new ArrayList<>());
        }

        model.addAttribute("pageTitle", "Azure SkiShop - あなたのスキーライフをサポート");
        model.addAttribute("isHomePage", true);
        model.addAttribute("isAdminPage", false);
        
        // Alert variables initialization
        model.addAttribute("success", "");
        model.addAttribute("error", "");
        model.addAttribute("info", "");
        model.addAttribute("warning", "");

        return "index";
    }

    /**
     * 商品一覧ページ
     */
    @GetMapping("/products")
    public String products(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            Model model) {

        try {
            // Monoを同期的に実行し、結果を取得
            ProductSearchResponse response = productService.getProducts(page, size, sort, category, brand)
                    .block(); // 同期実行
            
            List<CategoryDto> categoryList = productService.getCategories()
                    .block(); // 同期実行

            // レスポンスがnullでない場合のみ属性を設定
            if (response != null) {
                model.addAttribute("products", response.products() != null ? response.products() : new ArrayList<>());
                model.addAttribute("totalPages", (response.totalCount() + size - 1) / size);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalProducts", response.totalCount());
            } else {
                // APIからデータが取得できない場合のデフォルト値
                model.addAttribute("products", new ArrayList<>());
                model.addAttribute("totalPages", 0);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalProducts", 0);
            }

            // カテゴリもnullチェック
            model.addAttribute("categories", categoryList != null ? categoryList : new ArrayList<>());

        } catch (Exception e) {
            // エラーが発生した場合のフォールバック
            model.addAttribute("products", new ArrayList<>());
            model.addAttribute("categories", new ArrayList<>());
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalProducts", 0);
        }

        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("currentSort", sort);
        model.addAttribute("pageTitle", "商品一覧 - Azure SkiShop");
        model.addAttribute("isAdminPage", false);
        
        // Alert variables initialization
        model.addAttribute("success", "");
        model.addAttribute("error", "");
        model.addAttribute("info", "");
        model.addAttribute("warning", "");

        return "products/list";
    }

    /**
     * 商品詳細ページ
     */
    @GetMapping("/products/{id}")
    public String productDetail(@org.springframework.web.bind.annotation.PathVariable String id, Model model) {
        try {
            // 同期実行で商品詳細を取得
            ProductDto product = productService.getProduct(id).block();
            List<ProductDto> relatedProducts = productService.getRelatedProducts(id).block();

            if (product != null) {
                model.addAttribute("product", product);
                model.addAttribute("pageTitle", product.name() + " - Azure SkiShop");
            } else {
                model.addAttribute("pageTitle", "商品詳細 - Azure SkiShop");
            }

            model.addAttribute("relatedProducts", 
                relatedProducts != null ? relatedProducts : new ArrayList<>());

        } catch (Exception e) {
            // エラーが発生した場合のフォールバック
            model.addAttribute("relatedProducts", new ArrayList<>());
            model.addAttribute("pageTitle", "商品詳細 - Azure SkiShop");
        }

        model.addAttribute("isAdminPage", false);
        
        // Alert variables initialization
        model.addAttribute("success", "");
        model.addAttribute("error", "");
        model.addAttribute("info", "");
        model.addAttribute("warning", "");
        
        return "products/detail";
    }

    /**
     * 商品検索
     */
    @GetMapping("/search")
    public String search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        try {
            // 同期実行で検索結果を取得
            ProductSearchResponse response = productService.searchProducts(q, page, size).block();

            if (response != null) {
                model.addAttribute("products", response.products() != null ? response.products() : new ArrayList<>());
                model.addAttribute("totalPages", (response.totalCount() + size - 1) / size);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalProducts", response.totalCount());
            } else {
                // 検索結果が取得できない場合のデフォルト値
                model.addAttribute("products", new ArrayList<>());
                model.addAttribute("totalPages", 0);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalProducts", 0);
            }

        } catch (Exception e) {
            // エラーが発生した場合のフォールバック
            model.addAttribute("products", new ArrayList<>());
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalProducts", 0);
        }

        model.addAttribute("searchQuery", q);
        model.addAttribute("pageTitle", "検索結果: " + q + " - Azure SkiShop");
        model.addAttribute("isAdminPage", false);
        
        // Alert variables initialization
        model.addAttribute("success", "");
        model.addAttribute("error", "");
        model.addAttribute("info", "");
        model.addAttribute("warning", "");

        return "products/search-results";
    }

    /**
     * ログインページ
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "ログイン - Azure SkiShop");
        model.addAttribute("isAdminPage", false);
        
        // Alert variables initialization
        model.addAttribute("success", "");
        model.addAttribute("error", "");
        model.addAttribute("info", "");
        model.addAttribute("warning", "");
        
        return "auth/login";
    }
}
