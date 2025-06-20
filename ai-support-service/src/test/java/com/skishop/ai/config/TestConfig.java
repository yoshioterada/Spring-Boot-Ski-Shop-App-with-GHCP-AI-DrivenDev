package com.skishop.ai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration that excludes MongoDB repositories and related beans
 */
@TestConfiguration
@ComponentScan(
    basePackages = "com.skishop.ai",
    excludeFilters = {
        @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, 
                             pattern = "com\\.skishop\\.ai\\.repository\\..*")
    }
)
public class TestConfig {
    // No bean definitions needed - just excluding repositories
}
