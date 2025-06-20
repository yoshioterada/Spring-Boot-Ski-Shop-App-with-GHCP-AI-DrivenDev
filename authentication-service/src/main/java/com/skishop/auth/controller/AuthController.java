package com.skishop.auth.controller;

import com.skishop.auth.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 認証とMicrosoft Graph API呼び出しのコントローラー
 * Microsoft Entra ID Spring Boot Starterを使用した実装
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final GraphService graphService;

    /**
     * ホームページ
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * ホームページ（認証後）
     */
    @GetMapping("/home")
    public String homeAuthenticated(@AuthenticationPrincipal OidcUser principal, Model model) {
        if (principal != null) {
            model.addAttribute("userName", principal.getFullName());
            model.addAttribute("userEmail", principal.getEmail());
        }
        return "home";
    }

    /**
     * IDトークンの詳細を表示
     */
    @GetMapping("/token_details")
    public String tokenDetails(@AuthenticationPrincipal OidcUser principal, Model model) {
        if (principal != null) {
            Map<String, Object> claims = principal.getIdToken().getClaims();
            model.addAttribute("claims", claims);
            model.addAttribute("idToken", principal.getIdToken().getTokenValue());
        }
        return "token_details";
    }

    /**
     * Microsoft Graph APIを呼び出してユーザー情報を取得
     */
    @GetMapping("/call_graph")
    public String callGraph(Authentication authentication, Model model) {
        try {
            Map<String, Object> userDetails = graphService.getUserDetails(authentication);
            
            if (userDetails != null) {
                model.addAttribute("user", userDetails);
                model.addAttribute("displayName", userDetails.get("displayName"));
                model.addAttribute("mail", userDetails.get("mail"));
                model.addAttribute("jobTitle", userDetails.get("jobTitle"));
                model.addAttribute("mobilePhone", userDetails.get("mobilePhone"));
                model.addAttribute("officeLocation", userDetails.get("officeLocation"));
                
                log.info("Successfully retrieved user details from Microsoft Graph: {}", 
                    userDetails.get("displayName"));
            } else {
                model.addAttribute("error", "Unable to retrieve user details from Microsoft Graph");
            }
        } catch (Exception e) {
            log.error("Error calling Microsoft Graph API: {}", e.getMessage());
            model.addAttribute("error", "Error retrieving user details: " + e.getMessage());
        }
        
        return "call_graph";
    }

    /**
     * ユーザープロファイル
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser principal, Model model) {
        if (principal != null) {
            model.addAttribute("principal", principal);
            model.addAttribute("name", principal.getFullName());
            model.addAttribute("email", principal.getEmail());
            model.addAttribute("preferredUsername", principal.getPreferredUsername());
        }
        return "profile";
    }

    /**
     * REST API エンドポイント：ユーザー情報
     */
    @GetMapping("/api/user/me")
    @ResponseBody
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return Map.of("error", "User not authenticated");
        }
        
        return Map.of(
            "name", principal.getFullName(),
            "email", principal.getEmail(),
            "preferredUsername", principal.getPreferredUsername(),
            "subject", principal.getSubject(),
            "authorities", principal.getAuthorities()
        );
    }

    /**
     * REST API エンドポイント：Microsoft Graph ユーザー情報
     */
    @GetMapping("/api/graph/user")
    @ResponseBody
    public Map<String, Object> getGraphUserInfo(Authentication authentication) {
        try {
            Map<String, Object> userDetails = graphService.getUserDetails(authentication);
            if (userDetails != null) {
                return userDetails;
            } else {
                return Map.of("error", "Unable to retrieve user details from Microsoft Graph");
            }
        } catch (Exception e) {
            log.error("Error calling Microsoft Graph API: {}", e.getMessage());
            return Map.of("error", "Failed to retrieve user details from Microsoft Graph: " + e.getMessage());
        }
    }

    /**
     * ログイン失敗ページ
     */
    @GetMapping("/login")
    public String login(Model model, String error) {
        if (error != null) {
            model.addAttribute("error", "Authentication failed. Please try again.");
        }
        return "login";
    }
}
