package com.skishop.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

/**
 * Microsoft Graph API サービス
 * Graph APIを使用してユーザー情報を取得
 */
@Service
@Slf4j
public class GraphService {

    /**
     * Microsoft Graph APIを使用してユーザー詳細を取得
     * 現在は仮実装（ダミーデータを返す）
     */
    public Map<String, Object> getUserDetails(Authentication authentication) {
        try {
            log.info("Retrieving user details for: {}", authentication.getName());
            
            // TODO: 実際のMicrosoft Graph API呼び出しを実装
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("displayName", authentication.getName());
            userDetails.put("mail", authentication.getName());
            userDetails.put("jobTitle", "User");
            userDetails.put("mobilePhone", "");
            userDetails.put("officeLocation", "");
            
            log.info("Successfully retrieved user details (mock data)");
            return userDetails;
            
        } catch (Exception e) {
            log.error("Error retrieving user details: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Microsoft Graph APIを使用してプロフィール写真を取得
     * 現在は仮実装
     */
    public byte[] getUserPhoto(Authentication authentication) {
        try {
            log.info("Retrieving user photo for: {}", authentication.getName());
            // TODO: 実際のMicrosoft Graph API呼び出しを実装
            return null;
        } catch (Exception e) {
            log.error("Error retrieving user photo: {}", e.getMessage());
            return null;
        }
    }
}
