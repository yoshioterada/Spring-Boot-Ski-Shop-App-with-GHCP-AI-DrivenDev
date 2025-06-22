package com.skishop.auth.service.monitoring;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 包括的なメトリクス収集サービス
 * システム全体のパフォーマンスと健全性を監視
 */
@Component
@Slf4j
public class ComprehensiveMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // タイマー
    private final Timer eventPublishingTimer;
    private final Timer eventProcessingTimer;
    private final Timer sagaExecutionTimer;
    private final Timer databaseOperationTimer;
    
    // カウンター
    // 動的に作成するため、フィールドは不要
    
    // ゲージ
    private final AtomicLong activeSagaCount = new AtomicLong(0);
    private final AtomicLong pendingEventCount = new AtomicLong(0);
    private final AtomicLong failedEventCount = new AtomicLong(0);
    
    // 分布サマリー
    private final DistributionSummary payloadSizeDistribution;
    private final DistributionSummary sagaDurationDistribution;
    
    public ComprehensiveMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // タイマー初期化
        this.eventPublishingTimer = Timer.builder("events.publishing.duration")
            .description("イベント発行にかかる時間")
            .register(meterRegistry);
            
        this.eventProcessingTimer = Timer.builder("events.processing.duration")
            .description("イベント処理にかかる時間")
            .register(meterRegistry);
            
        this.sagaExecutionTimer = Timer.builder("saga.execution.duration")
            .description("Saga実行時間")
            .register(meterRegistry);
            
        this.databaseOperationTimer = Timer.builder("database.operation.duration")
            .description("データベース操作時間")
            .register(meterRegistry);
        // ゲージ登録
        Gauge.builder("saga.active.count", activeSagaCount, AtomicLong::get)
            .description("アクティブなSaga数")
            .register(meterRegistry);
            
        Gauge.builder("events.pending.count", pendingEventCount, AtomicLong::get)
            .description("処理待ちイベント数")
            .register(meterRegistry);
            
        Gauge.builder("events.failed.count", failedEventCount, AtomicLong::get)
            .description("失敗したイベント数")
            .register(meterRegistry);
            
        // 分布サマリー初期化
        this.payloadSizeDistribution = DistributionSummary.builder("events.payload.size")
            .description("イベントペイロードサイズ分布")
            .baseUnit("bytes")
            .register(meterRegistry);
            
        this.sagaDurationDistribution = DistributionSummary.builder("saga.duration.distribution")
            .description("Saga実行時間分布")
            .baseUnit("milliseconds")
            .register(meterRegistry);
    }
    
    // イベント発行メトリクス
    public void recordEventPublished(String eventType, String environment, long durationMs, int payloadSize) {
        eventPublishingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("events.success.total")
            .tag("eventType", eventType)
            .tag("environment", environment)
            .register(meterRegistry)
            .increment();
            
        payloadSizeDistribution.record(payloadSize);
        
        log.debug("Recorded event published: type={}, env={}, duration={}ms, size={}bytes", 
                 eventType, environment, durationMs, payloadSize);
    }
    
    public void recordEventFailure(String eventType, String environment, long durationMs, String errorType, String errorMessage) {
        eventPublishingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("events.failure.total")
            .tag("eventType", eventType)
            .tag("environment", environment)
            .tag("errorType", errorType)
            .register(meterRegistry)
            .increment();
            
        failedEventCount.incrementAndGet();
        
        // エラー詳細をカスタムメトリクスとして記録
        Counter.builder("events.errors.detail")
            .description("エラー詳細")
            .tag("errorType", errorType)
            .tag("errorMessage", truncateErrorMessage(errorMessage))
            .register(meterRegistry)
            .increment();
            
        log.warn("Recorded event failure: type={}, env={}, duration={}ms, error={}", 
                eventType, environment, durationMs, errorType);
    }
    
    public void recordEventProcessed(String eventType, String environment, long processingTimeMs) {
        eventProcessingTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
        pendingEventCount.decrementAndGet();
        
        log.debug("Recorded event processed: type={}, env={}, processingTime={}ms", 
                 eventType, environment, processingTimeMs);
    }
    
    // Sagaメトリクス
    public void recordSagaStarted(String sagaType) {
        activeSagaCount.incrementAndGet();
        
        Counter.builder("saga.started.total")
            .tag("sagaType", sagaType)
            .register(meterRegistry)
            .increment();
        
        log.debug("Recorded saga started: type={}, active={}", sagaType, activeSagaCount.get());
    }
    
    public void recordSagaCompleted(String sagaType, long durationMs, boolean success) {
        activeSagaCount.decrementAndGet();
        sagaDurationDistribution.record(durationMs);
        
        if (success) {
            Counter.builder("saga.completed.total")
                .tag("sagaType", sagaType)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
        } else {
            Counter.builder("saga.failed.total")
                .tag("sagaType", sagaType)
                .tag("status", "failed")
                .register(meterRegistry)
                .increment();
        }
        
        sagaExecutionTimer.record(durationMs, TimeUnit.MILLISECONDS);
            
        log.info("Recorded saga completed: type={}, duration={}ms, success={}, active={}", 
                sagaType, durationMs, success, activeSagaCount.get());
    }
    
    public void recordCompensationExecuted(String sagaType, String compensationType, long durationMs, boolean success) {
        Counter.builder("compensation.executed.total")
            .tag("sagaType", sagaType)
            .tag("compensationType", compensationType)
            .tag("status", success ? "success" : "failed")
            .register(meterRegistry)
            .increment();
        
        Timer.builder("compensation.execution.duration")
            .description("補償処理実行時間")
            .tag("sagaType", sagaType)
            .tag("compensationType", compensationType)
            .tag("status", success ? "success" : "failed")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
            
        log.info("Recorded compensation executed: saga={}, compensation={}, duration={}ms, success={}", 
                sagaType, compensationType, durationMs, success);
    }
    
    // データベースメトリクス
    public void recordDatabaseOperation(String operationType, long durationMs, boolean success) {
        databaseOperationTimer.record(durationMs, TimeUnit.MILLISECONDS);
            
        Counter.builder("database.operations.total")
            .description("データベース操作数")
            .tag("operation", operationType)
            .tag("status", success ? "success" : "failed")
            .register(meterRegistry)
            .increment();
            
        log.debug("Recorded database operation: operation={}, duration={}ms, success={}", 
                 operationType, durationMs, success);
    }
    
    // カスタムメトリクス記録
    public void recordCustomMetric(String metricName, double value, Tags tags) {
        Gauge.builder(metricName, () -> value)
            .tags(tags)
            .register(meterRegistry);
            
        log.debug("Recorded custom metric: name={}, value={}, tags={}", metricName, value, tags);
    }
    
    public void incrementCustomCounter(String counterName, Tags tags) {
        Counter.builder(counterName)
            .tags(tags)
            .register(meterRegistry)
            .increment();
            
        log.debug("Incremented custom counter: name={}, tags={}", counterName, tags);
    }
    
    // ユーティリティメソッド
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) return "unknown";
        return errorMessage.length() > 100 ? errorMessage.substring(0, 100) + "..." : errorMessage;
    }
    
    // 現在の状況取得メソッド
    public long getActiveSagaCount() {
        return activeSagaCount.get();
    }
    
    public long getPendingEventCount() {
        return pendingEventCount.get();
    }
    
    public long getFailedEventCount() {
        return failedEventCount.get();
    }
    
    public void incrementPendingEventCount() {
        pendingEventCount.incrementAndGet();
    }
    
    public void decrementPendingEventCount() {
        pendingEventCount.decrementAndGet();
    }
    
    public void resetFailedEventCount() {
        failedEventCount.set(0);
    }
}
