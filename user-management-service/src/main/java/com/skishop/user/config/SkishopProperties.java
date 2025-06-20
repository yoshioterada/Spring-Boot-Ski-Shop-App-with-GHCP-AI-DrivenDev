package com.skishop.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * スキーショップのアプリケーション設定
 */
@Configuration
@ConfigurationProperties(prefix = "skishop")
@Data
public class SkishopProperties {
    
    /**
     * 認証機能設定
     */
    private AuthFunc authfunc = new AuthFunc();
    
    /**
     * ユーザー管理設定
     */
    private User user = new User();
    
    @Data
    public static class AuthFunc {
        /**
         * 認証機能有効フラグ
         */
        private boolean enable = true;
    }
    
    @Data
    public static class User {
        private Password password = new Password();
        private EmailVerification emailVerification = new EmailVerification();
        private AccountLock accountLock = new AccountLock();
        private Session session = new Session();
        private Notification notification = new Notification();
        
        @Data
        public static class Password {
            private int minLength = 8;
            private int maxLength = 100;
            private boolean requireUppercase = true;
            private boolean requireLowercase = true;
            private boolean requireDigits = true;
            private boolean requireSpecialChars = true;
        }
        
        @Data
        public static class EmailVerification {
            private int tokenValidity = 24;
            private String baseUrl = "http://localhost:3000";
        }
        
        @Data
        public static class AccountLock {
            private int maxFailedAttempts = 5;
            private int lockDuration = 30;
        }
        
        @Data
        public static class Session {
            private int timeout = 30;
            private int maxConcurrent = 1;
        }
        
        @Data
        public static class Notification {
            private Email email = new Email();
            
            @Data
            public static class Email {
                private boolean enabled = true;
                private String from = "noreply@skishop.com";
                private Smtp smtp = new Smtp();
                
                @Data
                public static class Smtp {
                    private String host = "localhost";
                    private int port = 587;
                    private String username = "";
                    private String password = "";
                    private boolean auth = true;
                    private boolean starttls = true;
                }
            }
        }
    }
}
