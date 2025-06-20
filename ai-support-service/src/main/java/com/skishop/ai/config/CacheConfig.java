package com.skishop.ai.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for Spring Boot application.
 * 
 * This configuration provides a simple cache manager for Spring Boot's caching functionality.
 * Uses ConcurrentMapCacheManager for development and testing - for production,
 * consider using Redis or other distributed cache solutions.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Creates a cache manager bean.
     * 
     * Using ConcurrentMapCacheManager which is suitable for development and testing.
     * For production environments, consider using:
     * - RedisCacheManager for distributed caching
     * - EhCacheCacheManager for local caching with persistence
     * - CaffeineCacheManager for high-performance local caching
     * 
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "aiResponses",     // Cache for AI model responses
            "userProfiles",    // Cache for user profiles
            "productCatalog",  // Cache for product information
            "chatSessions"     // Cache for chat sessions
        );
    }
}
