package com.skishop.ai.integration;

import com.skishop.ai.service.CustomerSupportAssistant;
import com.skishop.ai.service.ProductRecommendationAssistant;
import com.skishop.ai.service.SearchEnhancementAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LangChain4j + Azure OpenAI Integration Test
 * 
 * Azure OpenAI環境変数が設定されている場合のみ実行されるテスト
 * 実際のAzure OpenAIサービスとの接続をテストします
 */
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {
    "azure.openai.api-key=test-key",
    "azure.openai.endpoint=https://test.openai.azure.com/",
    "azure.openai.chat-deployment-name=gpt-4",
    "azure.openai.embedding-deployment-name=text-embedding-3-small"
})
class LangChain4jIntegrationTest {

    @Autowired
    private CustomerSupportAssistant customerSupportAssistant;
    
    @Autowired
    private ProductRecommendationAssistant productRecommendationAssistant;
    
    @Autowired
    private SearchEnhancementAssistant searchEnhancementAssistant;

    @Test
    void contextLoads() {
        // Spring ContextがLangChain4j設定を含めて正常に起動することを確認
        assertThat(customerSupportAssistant).isNotNull();
        assertThat(productRecommendationAssistant).isNotNull();
        assertThat(searchEnhancementAssistant).isNotNull();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
    void testCustomerSupportAssistant() {
        // 実際のAzure OpenAI接続が利用可能な場合のみ実行
        String response = customerSupportAssistant.chat("Hello, I need help with ski equipment.");
        
        assertThat(response)
            .isNotNull()
            .isNotBlank()
            .containsAnyOf("ski", "equipment", "help", "assistance");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
    void testProductRecommendation() {
        // 実際のAzure OpenAI接続が利用可能な場合のみ実行
        String recommendation = productRecommendationAssistant.generateRecommendations(
            "I'm looking for beginner-friendly skis",
            "Beginner skier, height 170cm, budget $500",
            "Ski catalog with various models"
        );
        
        assertThat(recommendation)
            .isNotNull()
            .isNotBlank();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
    void testSearchEnhancement() {
        // 実際のAzure OpenAI接続が利用可能な場合のみ実行
        String enhancedQuery = searchEnhancementAssistant.performSemanticSearch(
            "ski boots",
            "product catalog",
            "user context"
        );
        
        assertThat(enhancedQuery)
            .isNotNull()
            .isNotBlank()
            .contains("ski");
    }
}
