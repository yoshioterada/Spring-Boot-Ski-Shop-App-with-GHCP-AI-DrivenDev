package com.skishop.coupon.service;

import com.skishop.coupon.exception.CouponException;
import com.skishop.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCodeGeneratorService {

    private final CouponRepository couponRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateUniqueCode(String pattern) {
        String code;
        int attempts = 0;
        final int maxAttempts = 1000;

        do {
            code = generateCodeFromPattern(pattern);
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new CouponException("Unable to generate unique coupon code after " + maxAttempts + " attempts");
            }
        } while (couponRepository.existsByCode(code));

        return code;
    }

    public List<String> generateBulkCodes(int count, String pattern) {
        log.info("Generating {} bulk codes with pattern: {}", count, pattern);
        
        Set<String> generatedCodes = new HashSet<>();
        int attempts = 0;
        final int maxAttempts = count * 10; // 安全マージン

        while (generatedCodes.size() < count && attempts < maxAttempts) {
            String code = generateCodeFromPattern(pattern);
            
            // データベースとメモリ両方で重複チェック
            if (!generatedCodes.contains(code) && !couponRepository.existsByCode(code)) {
                generatedCodes.add(code);
            }
            
            attempts++;
        }

        if (generatedCodes.size() < count) {
            throw new CouponException(
                String.format("Unable to generate %d unique codes. Generated %d codes after %d attempts", 
                    count, generatedCodes.size(), attempts));
        }

        List<String> result = new ArrayList<>(generatedCodes);
        log.info("Successfully generated {} unique codes", result.size());
        return result;
    }

    private String generateCodeFromPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            // デフォルトパターン
            return generateRandomString(8);
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            
            if (c == '{' && i + 1 < pattern.length()) {
                // プレースホルダーの処理
                int endIndex = pattern.indexOf('}', i);
                if (endIndex != -1) {
                    String placeholder = pattern.substring(i + 1, endIndex);
                    result.append(processPlaceholder(placeholder));
                    i = endIndex + 1;
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        
        return result.toString();
    }

    private String processPlaceholder(String placeholder) {
        if (placeholder.startsWith("random:")) {
            String lengthStr = placeholder.substring(7);
            try {
                int length = Integer.parseInt(lengthStr);
                return generateRandomString(length);
            } catch (NumberFormatException e) {
                log.warn("Invalid random length in pattern: {}", placeholder);
                return generateRandomString(6); // デフォルト長
            }
        } else if (placeholder.equals("timestamp")) {
            return String.valueOf(System.currentTimeMillis() % 100000);
        } else if (placeholder.equals("uuid")) {
            return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else {
            log.warn("Unknown placeholder: {}", placeholder);
            return generateRandomString(6);
        }
    }

    private String generateRandomString(int length) {
        if (length <= 0) {
            length = 6; // デフォルト長
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public boolean validateCodeUniqueness(String code) {
        return !couponRepository.existsByCode(code);
    }
}
