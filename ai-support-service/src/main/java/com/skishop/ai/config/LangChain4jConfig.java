package com.skishop.ai.config;

import com.skishop.ai.service.CustomerSupportAssistant;
import com.skishop.ai.service.ProductRecommendationAssistant;
import com.skishop.ai.service.SearchEnhancementAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 1.1.0 Configuration for Azure OpenAI
 * 
 * <p>この設定クラスは最新のLangChain4j 1.1.0のAPIを使用してAzure OpenAIとの接続を提供します。</p>
 * 
 * <h3>Azure OpenAI Serviceのガイドライン：</h3>
 * <ul>
 *   <li>API Key認証を使用 (プロダクション環境ではManaged Identityを推奨)</li>
 *   <li>serviceVersionパラメータを使用 (apiVersionから変更)</li>
 *   <li>エラーハンドリングと再試行機能を含む</li>
 *   <li>セキュアな設定管理</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Configuration
public class LangChain4jConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${azure.openai.api-key}")
    private String apiKey;

    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.deployment-name}")
    private String chatDeploymentName;

    @Value("${azure.openai.embedding-deployment-name}")
    private String embeddingDeploymentName;

    @Value("${azure.openai.api-version:2024-02-15-preview}")
    private String serviceVersion;

    @Value("${azure.openai.temperature:0.7}")
    private Double temperature;

    @Value("${azure.openai.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${azure.openai.timeout:60s}")
    private Duration timeout;

    @Value("${azure.openai.max-retries:3}")
    private Integer maxRetries;

    /**
     * Azure OpenAI Chat Model Configuration
     * 
     * <p>LangChain4j 1.1.0の最新APIを使用してChatModelを設定</p>
     * <p>セキュリティのためにログは無効化</p>
     * 
     * @return 設定されたChatModelインスタンス
     */
    @Bean
    @Primary
    public ChatModel chatLanguageModel() {
        logger.info("Configuring Azure OpenAI Chat Model with deployment: {}", chatDeploymentName);
        
        return AzureOpenAiChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(chatDeploymentName)
                .serviceVersion(serviceVersion)  // LangChain4j 1.1.0ではapiVersionからserviceVersionに変更
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequestsAndResponses(false)  // プロダクション環境ではfalseを推奨
                .build();
    }

    /**
     * Azure OpenAI Embedding Model Configuration
     * 
     * <p>テキストの埋め込み表現を生成するためのモデル設定</p>
     * 
     * @return 設定されたEmbeddingModelインスタンス
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        logger.info("Configuring Azure OpenAI Embedding Model with deployment: {}", embeddingDeploymentName);
        
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(embeddingDeploymentName)
                .serviceVersion(serviceVersion)  // LangChain4j 1.1.0ではapiVersionからserviceVersionに変更
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequestsAndResponses(false)  // セキュリティのため無効化
                .build();
    }

    /**
     * Chat Memory Store Configuration
     * 
     * <p>会話履歴を保存するためのインメモリストア</p>
     * 
     * @return ChatMemoryStoreインスタンス
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    /**
     * Message Window Chat Memory Configuration
     * 
     * <p>会話の履歴管理のための設定</p>
     * 
     * @return MessageWindowChatMemoryインスタンス
     */
    @Bean
    public MessageWindowChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)  // 最大保持メッセージ数
                .chatMemoryStore(chatMemoryStore())
                .build();
    }

    /**
     * Customer Support AI Service
     * 
     * <p>LangChain4j 1.1.0のAiServicesを使用してカスタマーサポート用AIサービスを生成</p>
     * 
     * @return CustomerSupportAssistantインスタンス
     */
    @Bean
    public CustomerSupportAssistant customerSupportAssistant() {
        logger.info("Creating Customer Support Assistant with LangChain4j 1.1.0");
        
        return AiServices.builder(CustomerSupportAssistant.class)
                .chatModel(chatLanguageModel())
                .chatMemory(chatMemory())
                .build();
    }

    /**
     * Product Recommendation AI Service
     * 
     * <p>商品推奨機能のためのAIサービス</p>
     * 
     * @return ProductRecommendationAssistantインスタンス
     */
    @Bean
    public ProductRecommendationAssistant productRecommendationAssistant() {
        logger.info("Creating Product Recommendation Assistant with LangChain4j 1.1.0");
        
        return AiServices.builder(ProductRecommendationAssistant.class)
                .chatModel(chatLanguageModel())
                .build();
    }

    /**
     * Search Enhancement AI Service
     * 
     * <p>検索拡張機能のためのAIサービス</p>
     * 
     * @return SearchEnhancementAssistantインスタンス
     */
    @Bean
    public SearchEnhancementAssistant searchEnhancementAssistant() {
        logger.info("Creating Search Enhancement Assistant with LangChain4j 1.1.0");
        
        return AiServices.builder(SearchEnhancementAssistant.class)
                .chatModel(chatLanguageModel())
                .build();
    }
}
