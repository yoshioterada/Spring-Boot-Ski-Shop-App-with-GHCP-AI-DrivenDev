package com.skishop.frontend.controller;

import com.skishop.frontend.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * AI相談チャット機能のコントローラー
 */
@Controller
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * AI相談チャット画面表示
     */
    @GetMapping("/ai-chat")
    public String aiChat(Model model) {
        model.addAttribute("pageTitle", "AI相談チャット - Azure SkiShop");
        model.addAttribute("isAdminPage", false);
        
        // Alert variables initialization
        model.addAttribute("success", "");
        model.addAttribute("error", "");
        model.addAttribute("info", "");
        model.addAttribute("warning", "");
        
        return "ai-chat/chat";
    }

    /**
     * AI相談チャットメッセージ送信
     */
    @PostMapping("/api/ai-chat/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            // ChatMessageRequest形式でリクエストを受け取る
            String userId = (String) request.get("userId");
            String content = (String) request.get("content");
            String conversationId = (String) request.get("conversationId");
            String sessionId = (String) request.get("sessionId");
            
            // 下位互換性のため"message"フィールドもサポート
            if (content == null) {
                content = (String) request.get("message");
            }

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "メッセージが空です"));
            }

            // デフォルトユーザーIDを設定
            if (userId == null || userId.trim().isEmpty()) {
                userId = "anonymous-user";
            }

            // AIサポートサービスのAPIを呼び出し
            Map<String, Object> response = aiChatService.sendMessage(userId, content, conversationId, sessionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("AI chat message processing failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "申し訳ございません。現在AIサービスが利用できません。しばらく後にもう一度お試しください。",
                    "message", "申し訳ございませんが、現在AIアシスタントが利用できません。お急ぎの場合は、商品ページから直接商品をご覧いただくか、お電話でお問い合わせください。",
                    "content", "申し訳ございませんが、現在AIアシスタントが利用できません。お急ぎの場合は、商品ページから直接商品をご覧いただくか、お電話でお問い合わせください。"
                ));
        }
    }

    /**
     * 会話履歴取得
     */
    @GetMapping("/api/ai-chat/conversations")
    @ResponseBody
    public ResponseEntity<?> getConversations(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ユーザーIDが必要です"));
            }

            Map<String, Object> conversations = aiChatService.getConversations(userId);
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            log.error("Failed to get conversations", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "会話履歴の取得に失敗しました"));
        }
    }
}
