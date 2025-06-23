package com.skishop.frontend.controller;

import com.skishop.frontend.dto.AddToCartRequest;
import com.skishop.frontend.dto.CartDto;
import com.skishop.frontend.service.CartService;
import com.skishop.frontend.service.CartService.CartServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * カート関連のコントローラー
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    
    @Value("${app.skishop.auth.enabled:true}")
    private boolean authEnabled;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * ユーザーIDを取得（認証有無に関わらず）
     */
    private String getUserId(Authentication auth) {
        if (authEnabled && auth != null) {
            return auth.getName();
        }
        // テスト用のデフォルトユーザーID
        return "test-user-1";
    }

    /**
     * カート表示
     */
    @GetMapping
    public String viewCart(Authentication auth, Model model) {
        String userId = getUserId(auth);
        try {
            CartDto cart = cartService.getCart(userId);
            if (cart != null) {
                model.addAttribute("cart", cart);
            }
        } catch (CartServiceUnavailableException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "カートの取得中に予期しないエラーが発生しました。");
        }

        model.addAttribute("pageTitle", "カート - Azure SkiShop");
        return "cart/view";
    }

    /**
     * カートにアイテム追加
     */
    @PostMapping("/add")
    public String addToCart(
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        String userId = getUserId(auth);
        AddToCartRequest request = new AddToCartRequest(productId, quantity);

        try {
            cartService.addToCart(userId, request);
            redirectAttributes.addFlashAttribute("success", "商品をカートに追加しました");
        } catch (CartServiceUnavailableException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "カートへの追加中に予期しないエラーが発生しました。");
        }

        return "redirect:/cart";
    }

    /**
     * カートアイテム更新
     */
    @PostMapping("/update")
    public String updateCartItem(
            @RequestParam String itemId,
            @RequestParam int quantity,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        String userId = getUserId(auth);

        try {
            cartService.updateCartItem(userId, itemId, quantity);
            redirectAttributes.addFlashAttribute("success", "カートを更新しました");
        } catch (CartServiceUnavailableException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "カートの更新中に予期しないエラーが発生しました。");
        }

        return "redirect:/cart";
    }

    /**
     * カートアイテム削除
     */
    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam String itemId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        String userId = getUserId(auth);

        try {
            cartService.removeFromCart(userId, itemId);
            redirectAttributes.addFlashAttribute("success", "商品をカートから削除しました");
        } catch (CartServiceUnavailableException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除中に予期しないエラーが発生しました。");
        }

        return "redirect:/cart";
    }

    /**
     * カートクリア
     */
    @PostMapping("/clear")
    public String clearCart(Authentication auth, RedirectAttributes redirectAttributes) {
        String userId = getUserId(auth);

        try {
            cartService.clearCart(userId);
            redirectAttributes.addFlashAttribute("success", "カートをクリアしました");
        } catch (CartServiceUnavailableException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "カートのクリア中に予期しないエラーが発生しました。");
        }

        return "redirect:/cart";
    }

    /**
     * AJAX: カートアイテム数取得
     */
    @GetMapping("/count")
    @ResponseBody
    public Object getCartItemCount(Authentication auth) {
        String userId = getUserId(auth);
        try {
            CartDto cart = cartService.getCart(userId);
            int count = cart != null ? cart.itemCount() : 0;
            return java.util.Map.of("count", count);
        } catch (Exception e) {
            return java.util.Map.of("count", 0);
        }
    }
}
