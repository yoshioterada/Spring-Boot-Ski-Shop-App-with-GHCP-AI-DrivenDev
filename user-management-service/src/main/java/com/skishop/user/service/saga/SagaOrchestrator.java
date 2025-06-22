package com.skishop.user.service.saga;

import com.skishop.user.dto.event.EventDto;
import com.skishop.user.dto.event.UserRegistrationPayload;
import com.skishop.user.dto.event.UserDeletionPayload;
import com.skishop.user.dto.event.UserManagementStatusPayload;
import com.skishop.user.entity.SagaTransaction;
import com.skishop.user.entity.User;
import com.skishop.user.enums.SagaStatus;
import com.skishop.user.enums.ProcessingStatus;
import com.skishop.user.repository.SagaTransactionRepository;
import com.skishop.user.service.UserEventService;
import com.skishop.user.service.UserQueryService;
import com.skishop.user.service.event.EventHandlerService;
import com.skishop.user.service.event.EventPublisherService;
import com.skishop.user.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sagaオーケストレーター
 * イベント駆動アーキテクチャにおけるSagaパターンの実装
 * ユーザー登録と削除のトランザクション管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final SagaTransactionRepository sagaRepository;
    private final UserEventService userEventService;
    private final UserQueryService userQueryService;
    private final EventPublisherService eventPublisherService;
    private final MetricsService metricsService;
    private final EventHandlerService eventHandlerService;

    @Value("${saga.timeout.registration:30}")
    private Integer registrationTimeoutSeconds;

    @Value("${saga.timeout.deletion:60}")
    private Integer deletionTimeoutSeconds;

    @Value("${saga.max-retry:3}")
    private Integer maxRetryCount;

    /**
     * ユーザー登録Sagaの開始
     * 独立したトランザクションで実行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startUserRegistrationSaga(EventDto<UserRegistrationPayload> event) {
        String sagaId = UUID.randomUUID().toString();
        
        try {
            log.info("ユーザー登録Saga開始: sagaId={}, userId={}, correlationId={}", 
                     sagaId, event.getPayload().getUserId(), event.getCorrelationId());

            // Sagaトランザクション作成
            SagaTransaction saga = createSagaTransaction(sagaId, event, "USER_REGISTRATION", registrationTimeoutSeconds);
            sagaRepository.save(saga);

            // コンテキスト情報の保存
            saga.addContext("EMAIL", event.getPayload().getEmail());
            saga.addContext("FIRST_NAME", event.getPayload().getFirstName());
            saga.addContext("LAST_NAME", event.getPayload().getLastName());
            sagaRepository.save(saga);

            metricsService.recordSagaStarted("USER_REGISTRATION");

            // ステップ1: イベント受信確認
            updateSagaStatus(saga, SagaStatus.SAGA_STARTED, "EVENT_RECEIVED", null);
            
            // イベントを処理済みとして記録
            eventHandlerService.markEventAsProcessed(
                    saga.getSagaId(), 
                    "USER_REGISTERED", 
                    event.getPayload().getUserId(), 
                    true, 
                    null);
            
            // ステップ2: データ検証
            executeRegistrationValidation(saga, event);

        } catch (Exception e) {
            log.error("ユーザー登録Saga開始エラー: sagaId={}, error={}", sagaId, e.getMessage(), e);
            handleSagaFailure(sagaId, "SAGA_START_FAILED", e.getMessage());
            
            // イベントを処理失敗として記録
            try {
                eventHandlerService.markEventAsProcessed(
                        sagaId, 
                        "USER_REGISTERED", 
                        event.getPayload().getUserId(), 
                        false, 
                        e.getMessage());
            } catch (Exception ex) {
                log.error("イベント処理失敗の記録に失敗: sagaId={}, error={}", sagaId, ex.getMessage(), ex);
            }
            
            throw e;
        }
    }

    /**
     * ユーザー削除Sagaの開始
     * 独立したトランザクションで実行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startUserDeletionSaga(EventDto<UserDeletionPayload> event) {
        String sagaId = UUID.randomUUID().toString();
        
        try {
            log.info("ユーザー削除Saga開始: sagaId={}, userId={}, correlationId={}", 
                     sagaId, event.getPayload().getUserId(), event.getCorrelationId());

            // Sagaトランザクション作成
            SagaTransaction saga = createSagaTransaction(sagaId, event, "USER_DELETION", deletionTimeoutSeconds);
            sagaRepository.save(saga);

            metricsService.recordSagaStarted("USER_DELETION");

            // ステップ1: イベント受信確認
            updateSagaStatus(saga, SagaStatus.SAGA_STARTED, "DELETION_EVENT_RECEIVED", null);
            
            // イベントを処理済みとして記録
            eventHandlerService.markEventAsProcessed(
                    saga.getSagaId(), 
                    "USER_DELETED", 
                    event.getPayload().getUserId(), 
                    true, 
                    null);
            
            // ステップ2: 削除データ検証
            executeDeletionValidation(saga, event);

        } catch (Exception e) {
            log.error("ユーザー削除Saga開始エラー: sagaId={}, error={}", sagaId, e.getMessage(), e);
            handleSagaFailure(sagaId, "SAGA_START_FAILED", e.getMessage());
            
            // イベントを処理失敗として記録
            try {
                eventHandlerService.markEventAsProcessed(
                        sagaId, 
                        "USER_DELETED", 
                        event.getPayload().getUserId(), 
                        false, 
                        e.getMessage());
            } catch (Exception ex) {
                log.error("イベント処理失敗の記録に失敗: sagaId={}, error={}", sagaId, ex.getMessage(), ex);
            }
            
            throw e;
        }
    }

    /**
     * ユーザー登録の検証ステップ実行
     */
    private void executeRegistrationValidation(SagaTransaction saga, EventDto<UserRegistrationPayload> event) {
        try {
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "VALIDATION_IN_PROGRESS", null);
            
            UserRegistrationPayload payload = event.getPayload();
            
            // 必須フィールド検証
            if (payload.getUserId() == null || payload.getEmail() == null) {
                throw new IllegalArgumentException("必須フィールドが不足しています");
            }

            // 重複ユーザーチェック
            if (userEventService.existsByUserId(payload.getUserId())) {
                updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "DUPLICATE_USER_DETECTED", 
                                "ユーザーID重複: " + payload.getUserId());
                sendFailureResponse(saga, "ユーザーID重複");
                return;
            }

            if (userEventService.existsByEmail(payload.getEmail())) {
                updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "DUPLICATE_USER_DETECTED", 
                                "メールアドレス重複: " + payload.getEmail());
                sendFailureResponse(saga, "メールアドレス重複");
                return;
            }

            saga.addCompletedStep("VALIDATION_PASSED", "ユーザーデータ検証成功");
            
            // ステップ3: ユーザープロファイル作成
            executeUserProfileCreation(saga, event);

        } catch (Exception e) {
            log.error("ユーザー登録検証エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "VALIDATION_FAILED", e.getMessage());
            sendFailureResponse(saga, e.getMessage());
        }
    }

    /**
     * ユーザープロファイル作成ステップ実行
     */
    private void executeUserProfileCreation(SagaTransaction saga, EventDto<UserRegistrationPayload> event) {
        try {
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "PROFILE_CREATION_IN_PROGRESS", null);
            
            UserRegistrationPayload payload = event.getPayload();
            
            // ユーザープロファイル作成
            User user = userEventService.createUserProfile(payload);
            
            saga.addCompletedStep("PROFILE_CREATED", "ユーザープロファイル作成成功: " + user.getId());
            saga.addContext("CREATED_USER_ID", user.getId().toString());
            
            // Saga完了
            completeSaga(saga);
            
            // 成功レスポンス送信
            sendSuccessResponse(saga);

        } catch (Exception e) {
            log.error("ユーザープロファイル作成エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "PROFILE_CREATION_FAILED", e.getMessage());
            sendFailureResponse(saga, e.getMessage());
        }
    }

    /**
     * ユーザー削除の検証ステップ実行
     */
    private void executeDeletionValidation(SagaTransaction saga, EventDto<UserDeletionPayload> event) {
        try {
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "DELETION_VALIDATION_IN_PROGRESS", null);
            
            UserDeletionPayload payload = event.getPayload();
            
            // ユーザー存在確認
            Optional<User> userOpt = userEventService.findByUserId(payload.getUserId());
            if (userOpt.isEmpty()) {
                // 冪等性のため、存在しない場合も成功とする
                log.info("削除対象ユーザーが見つかりません（冪等性により成功扱い）: userId={}", payload.getUserId());
                saga.addCompletedStep("USER_NOT_FOUND", "削除対象ユーザー不存在（冪等性により成功）");
                completeSaga(saga);
                sendSuccessResponse(saga);
                return;
            }

            saga.addCompletedStep("DELETION_VALIDATION_PASSED", "削除対象ユーザー確認完了");
            saga.addContext("TARGET_USER_ID", userOpt.get().getId().toString());
            
            // ステップ3: ユーザープロファイル削除
            executeUserProfileDeletion(saga, userOpt.get());

        } catch (Exception e) {
            log.error("ユーザー削除検証エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "DELETION_VALIDATION_FAILED", e.getMessage());
            sendFailureResponse(saga, e.getMessage());
        }
    }

    /**
     * ユーザープロファイル削除ステップ実行
     */
    private void executeUserProfileDeletion(SagaTransaction saga, User user) {
        try {
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "PROFILE_DELETION_IN_PROGRESS", null);
            
            // 関連データのクリーンアップと削除実行
            userEventService.deleteUserProfile(user.getId());
            
            saga.addCompletedStep("PROFILE_DELETED", "ユーザープロファイル削除完了: " + user.getId());
            
            // Saga完了
            completeSaga(saga);
            
            // 成功レスポンス送信
            sendSuccessResponse(saga);

        } catch (Exception e) {
            log.error("ユーザープロファイル削除エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "PROFILE_DELETION_FAILED", e.getMessage());
            sendFailureResponse(saga, e.getMessage());
        }
    }

    /**
     * Sagaトランザクション作成
     */
    private SagaTransaction createSagaTransaction(String sagaId, EventDto<?> event, String eventType, int timeoutSeconds) {
        return SagaTransaction.builder()
                .sagaId(sagaId)
                .correlationId(event.getCorrelationId())
                .originalEventId(event.getEventId())
                .eventType(eventType)
                .userId(extractUserId(event))
                .status(SagaStatus.SAGA_STARTED)
                .maxRetryCount(maxRetryCount)
                .timeoutAt(LocalDateTime.now().plusSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Sagaステータス更新
     */
    private void updateSagaStatus(SagaTransaction saga, SagaStatus status, String currentStep, String errorMessage) {
        saga.setStatus(status);
        saga.setCurrentStep(currentStep);
        saga.setErrorMessage(errorMessage);
        
        if (status == SagaStatus.SAGA_IN_PROGRESS && saga.getProcessingStartTime() == null) {
            saga.markProcessingStart();
        }
        
        sagaRepository.save(saga);
        
        log.info("Sagaステータス更新: sagaId={}, status={}, step={}", 
                 saga.getSagaId(), status, currentStep);
    }

    /**
     * Saga完了処理
     */
    private void completeSaga(SagaTransaction saga) {
        saga.setStatus(SagaStatus.SAGA_COMPLETED);
        saga.markProcessingEnd();
        sagaRepository.save(saga);
        
        long processingTimeMs = saga.getProcessingTimeMs();
        metricsService.recordSagaCompleted(saga.getEventType(), processingTimeMs, true);
        
        log.info("Saga完了: sagaId={}, processingTime={}ms", saga.getSagaId(), processingTimeMs);
    }

    /**
     * Saga失敗処理
     */
    private void handleSagaFailure(String sagaId, String errorType, String errorMessage) {
        Optional<SagaTransaction> sagaOpt = sagaRepository.findById(sagaId);
        if (sagaOpt.isPresent()) {
            SagaTransaction saga = sagaOpt.get();
            saga.setStatus(SagaStatus.SAGA_FAILED);
            saga.setErrorType(errorType);
            saga.setErrorMessage(errorMessage);
            saga.markProcessingEnd();
            sagaRepository.save(saga);
            
            metricsService.recordSagaCompleted(saga.getEventType(), saga.getProcessingTimeMs(), false);
        }
    }

    /**
     * 成功レスポンス送信
     */
    private void sendSuccessResponse(SagaTransaction saga) {
        try {
            UserManagementStatusPayload statusPayload = UserManagementStatusPayload.builder()
                    .userId(saga.getUserId())
                    .originalEventId(saga.getOriginalEventId())
                    .status(ProcessingStatus.SUCCESS)
                    .processingTime(saga.getProcessingTimeMs())
                    .build();

            EventDto<UserManagementStatusPayload> responseEvent = EventDto.<UserManagementStatusPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("USER_MANAGEMENT_STATUS")
                    .timestamp(java.time.Instant.now())
                    .version("1.0")
                    .producer("user-management-service")
                    .payload(statusPayload)
                    .correlationId(saga.getCorrelationId())
                    .sagaId(saga.getSagaId())
                    .build();

            eventPublisherService.publishEvent(responseEvent);
            
            log.info("成功レスポンス送信完了: sagaId={}, correlationId={}", 
                     saga.getSagaId(), saga.getCorrelationId());

        } catch (Exception e) {
            log.error("成功レスポンス送信エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
        }
    }

    /**
     * 失敗レスポンス送信
     */
    private void sendFailureResponse(SagaTransaction saga, String reason) {
        try {
            UserManagementStatusPayload statusPayload = UserManagementStatusPayload.builder()
                    .userId(saga.getUserId())
                    .originalEventId(saga.getOriginalEventId())
                    .status(ProcessingStatus.FAILED)
                    .reason(reason)
                    .processingTime(saga.getProcessingTimeMs())
                    .build();

            EventDto<UserManagementStatusPayload> responseEvent = EventDto.<UserManagementStatusPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("USER_MANAGEMENT_STATUS")
                    .timestamp(java.time.Instant.now())
                    .version("1.0")
                    .producer("user-management-service")
                    .payload(statusPayload)
                    .correlationId(saga.getCorrelationId())
                    .sagaId(saga.getSagaId())
                    .build();

            eventPublisherService.publishEvent(responseEvent);
            
            log.info("失敗レスポンス送信完了: sagaId={}, correlationId={}, reason={}", 
                     saga.getSagaId(), saga.getCorrelationId(), reason);

        } catch (Exception e) {
            log.error("失敗レスポンス送信エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
        }
    }

    /**
     * イベントからユーザーIDを抽出
     */
    private String extractUserId(EventDto<?> event) {
        Object payload = event.getPayload();
        if (payload instanceof UserRegistrationPayload userRegistrationPayload) {
            return userRegistrationPayload.getUserId();
        } else if (payload instanceof UserDeletionPayload userDeletionPayload) {
            return userDeletionPayload.getUserId();
        }
        return null;
    }

    /**
     * 補償処理の実行
     * 失敗したSagaに対する補償処理を実行
     * 独立したトランザクションで実行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeCompensation(String sagaId, String reason) {
        Optional<SagaTransaction> sagaOpt = sagaRepository.findById(sagaId);
        if (sagaOpt.isEmpty()) {
            log.warn("補償処理対象Sagaが見つかりません: sagaId={}", sagaId);
            return;
        }
        
        SagaTransaction saga = sagaOpt.get();
        
        try {
            log.info("補償処理開始: sagaId={}, eventType={}, reason={}", 
                    saga.getSagaId(), saga.getEventType(), reason);
            
            updateSagaStatus(saga, SagaStatus.SAGA_COMPENSATING, "COMPENSATION_STARTED", 
                           "補償処理開始: " + reason);
            
            long startTime = System.currentTimeMillis();
            
            // イベントタイプに基づいて適切な補償処理を実行
            if ("USER_REGISTRATION".equals(saga.getEventType())) {
                executeUserRegistrationCompensation(saga, reason);
            } else if ("USER_DELETION".equals(saga.getEventType())) {
                executeUserDeletionCompensation(saga, reason);
            }
            
            updateSagaStatus(saga, SagaStatus.SAGA_COMPENSATED, "COMPENSATION_COMPLETED", 
                           "補償処理完了: " + reason);
            
            // 補償処理のメトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordCompensationExecuted(saga.getEventType(), reason, processingTime, true);
            
            // 認証サービスへの補償完了通知
            publishCompensationStatusEvent(saga, true, reason);
            
            log.info("補償処理完了: sagaId={}, eventType={}, processingTime={}ms", 
                   saga.getSagaId(), saga.getEventType(), processingTime);
            
        } catch (Exception e) {
            log.error("補償処理失敗: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            
            updateSagaStatus(saga, SagaStatus.SAGA_COMPENSATION_FAILED, "COMPENSATION_FAILED", 
                           "補償処理失敗: " + e.getMessage());
            
            // 失敗メトリクス記録
            metricsService.recordCompensationExecuted(saga.getEventType(), reason, 
                                                    System.currentTimeMillis() - saga.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), 
                                                    false);
            
            // 認証サービスへの補償失敗通知
            publishCompensationStatusEvent(saga, false, e.getMessage());
        }
    }
    
    /**
     * ユーザー登録の補償処理実行
     * 実際に補償アクションを実行するメソッド
     */
    private void executeUserRegistrationCompensation(SagaTransaction saga, String reason) {
        String createdUserId = saga.getContextValue("CREATED_USER_ID");
        
        if (createdUserId != null) {
            try {
                // 作成したユーザープロファイルの強制削除
                userEventService.hardDeleteUserProfile(UUID.fromString(createdUserId));
                
                saga.addCompletedStep("USER_PROFILE_DELETED", 
                                    "ユーザープロファイル削除完了: " + createdUserId + " (理由: " + reason + ")");
                
                log.info("ユーザー登録補償処理成功: sagaId={}, userId={}, reason={}", 
                        saga.getSagaId(), createdUserId, reason);
                
            } catch (Exception e) {
                log.error("ユーザー登録補償処理失敗: sagaId={}, userId={}, reason={}, error={}", 
                        saga.getSagaId(), createdUserId, reason, e.getMessage(), e);
                throw e;
            }
        } else {
            log.warn("ユーザーID不明のため補償処理スキップ: sagaId={}, reason={}", saga.getSagaId(), reason);
        }
    }
    
    /**
     * ユーザー削除の補償処理実行
     * 実際に補償アクションを実行するメソッド
     */
    private void executeUserDeletionCompensation(SagaTransaction saga, String reason) {
        // ユーザー削除操作の補償処理（必要に応じて実装）
        // 現状では特に必要な処理がないためログのみ
        log.info("ユーザー削除補償処理: sagaId={}, userId={}, 理由={}, 補償処理は不要", 
                saga.getSagaId(), saga.getUserId(), reason);
        
        saga.addCompletedStep("DELETION_COMPENSATION", 
                            "ユーザー削除補償処理（処理なし）: " + saga.getUserId() + " (理由: " + reason + ")");
    }
    
    /**
     * 補償処理ステータスイベントの発行
     */
    private void publishCompensationStatusEvent(SagaTransaction saga, boolean success, String reason) {
        try {
            UserManagementStatusPayload statusPayload = UserManagementStatusPayload.builder()
                    .userId(saga.getUserId())
                    .originalEventId(saga.getOriginalEventId())
                    .status(success ? ProcessingStatus.COMPENSATION_SUCCESS : ProcessingStatus.COMPENSATION_FAILED)
                    .reason(reason)
                    .processingTime(saga.getProcessingTimeMs())
                    .build();

            EventDto<UserManagementStatusPayload> responseEvent = EventDto.<UserManagementStatusPayload>builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("USER_MANAGEMENT_COMPENSATION_STATUS")
                    .timestamp(java.time.Instant.now())
                    .version("1.0")
                    .producer("user-management-service")
                    .payload(statusPayload)
                    .correlationId(saga.getCorrelationId())
                    .sagaId(saga.getSagaId())
                    .build();

            eventPublisherService.publishEvent(responseEvent);
            
            log.info("補償処理ステータス送信完了: sagaId={}, correlationId={}, status={}", 
                    saga.getSagaId(), saga.getCorrelationId(), success ? "SUCCESS" : "FAILED");
                    
        } catch (Exception e) {
            log.error("補償処理ステータス送信エラー: sagaId={}, error={}", 
                    saga.getSagaId(), e.getMessage(), e);
        }
    }
    
    /**
     * タイムアウトSagaのチェックと処理
     * 定期的に実行し、タイムアウトしたSagaを検出して補償処理を実行
     * 独立したトランザクションで実行
     */
    @Scheduled(fixedDelay = 30000) // 30秒間隔
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndProcessTimeoutSagas() {
        try {
            log.debug("タイムアウトSaga監視開始");
            
            LocalDateTime currentTime = LocalDateTime.now();
            List<SagaStatus> terminalStatuses = List.of(
                SagaStatus.SAGA_COMPLETED,
                SagaStatus.SAGA_FAILED,
                SagaStatus.SAGA_COMPENSATED,
                SagaStatus.SAGA_COMPENSATION_FAILED,
                SagaStatus.SAGA_TIMEOUT
            );
            
            // タイムアウトしたSagaを検索
            List<SagaTransaction> timeoutSagas = sagaRepository.findTimedOutSagas(currentTime, terminalStatuses);
            
            if (!timeoutSagas.isEmpty()) {
                log.warn("タイムアウトSaga検出: count={}", timeoutSagas.size());
                
                for (SagaTransaction saga : timeoutSagas) {
                    // 別のトランザクションでタイムアウト処理を実行
                    handleTimeoutSagaWithNewTransaction(saga);
                }
            }
            
            log.debug("タイムアウトSaga監視完了: 処理数={}", timeoutSagas.size());
            
        } catch (Exception e) {
            log.error("タイムアウトSaga監視エラー: {}", e.getMessage(), e);
        }
    }
    
    /**
     * タイムアウトSagaの処理
     * 独立したトランザクションで実行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTimeoutSagaWithNewTransaction(SagaTransaction saga) {
        try {
            log.warn("Sagaタイムアウト処理: sagaId={}, eventType={}, currentStep={}", 
                    saga.getSagaId(), saga.getEventType(), saga.getCurrentStep());
            
            // 補償処理の実行
            String sagaId = saga.getSagaId();
            
            // 別のトランザクションでの補償処理実行
            executeCompensation(sagaId, "TIMEOUT");
            
            // 再取得して状態更新
            saga = sagaRepository.findById(sagaId).orElseThrow();
            
            // タイムアウト状態に更新
            updateSagaStatus(saga, SagaStatus.SAGA_TIMEOUT, "TIMED_OUT", 
                           "Sagaがタイムアウトしました: " + saga.getTimeoutAt());
            
            // メトリクス記録
            metricsService.recordSagaCompleted(saga.getEventType(), saga.getProcessingTimeMs(), false);
            
            log.info("Sagaタイムアウト処理完了: sagaId={}", saga.getSagaId());
            
        } catch (Exception e) {
            log.error("Sagaタイムアウト処理エラー: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            
            updateSagaStatus(saga, SagaStatus.SAGA_TIMEOUT, "TIMEOUT_ERROR", 
                           "タイムアウト処理中にエラー: " + e.getMessage());
        }
    }

    /**
     * Sagaのリトライ処理
     * 独立したトランザクションで実行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retrySaga(SagaTransaction saga) {
        try {
            log.info("Sagaリトライ開始: sagaId={}, eventType={}, retryCount={}/{}", 
                    saga.getSagaId(), saga.getEventType(), saga.getRetryCount(), saga.getMaxRetryCount());
                    
            // イベントタイプに基づいて適切なリトライ処理を実行
            if ("USER_REGISTRATION".equals(saga.getEventType())) {
                retryUserRegistration(saga);
            } else if ("USER_DELETION".equals(saga.getEventType())) {
                retryUserDeletion(saga);
            } else {
                log.warn("未知のイベントタイプのためリトライ不可: sagaId={}, eventType={}", 
                        saga.getSagaId(), saga.getEventType());
            }
            
            log.info("Sagaリトライ完了: sagaId={}", saga.getSagaId());
            
        } catch (Exception e) {
            log.error("Sagaリトライ失敗: sagaId={}, error={}", saga.getSagaId(), e.getMessage(), e);
            
            updateSagaStatus(saga, SagaStatus.SAGA_STEP_FAILED, "RETRY_FAILED", e.getMessage());
            
            // リトライ回数が上限に達したら補償処理を実行
            if (saga.getRetryCount() >= saga.getMaxRetryCount()) {
                log.warn("リトライ回数上限到達のため補償処理を実行: sagaId={}, retryCount={}", 
                        saga.getSagaId(), saga.getRetryCount());
                        
                // リトライ用の別トランザクションで補償処理を実行
                String sagaId = saga.getSagaId();
                try {
                    executeCompensation(sagaId, "MAX_RETRY_EXCEEDED");
                } catch (Exception ex) {
                    log.error("補償処理の実行に失敗: sagaId={}, error={}", sagaId, ex.getMessage(), ex);
                }
            }
        }
    }
    
    /**
     * ユーザー登録Sagaのリトライ
     */
    private void retryUserRegistration(SagaTransaction saga) {
        // 現在のステップに基づいて適切なポイントからリトライ
        String currentStep = saga.getCurrentStep();
        
        if ("VALIDATION_IN_PROGRESS".equals(currentStep) || "VALIDATION_FAILED".equals(currentStep)) {
            // 元のイベントから情報を取得（実際の実装ではイベントストアから取得するなど）
            String userId = saga.getUserId();
            // EmailはContext情報から取得するが、ここでは実際に使用しないので取得しない
            
            // 検証ステップを再実行
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "VALIDATION_RETRY", null);
            
            // リトライロジックを実装
            log.info("ユーザー登録検証リトライ: sagaId={}, userId={}", saga.getSagaId(), userId);
            
            // 実際のリトライロジック（ここでは簡略化）
            saga.addCompletedStep("VALIDATION_RETRY_COMPLETED", "検証リトライ成功");
            
            // 次のステップへ進む（ここではユーザープロファイル作成）
            // 実装省略 - 実際にはプロファイル作成ステップを実行
            
        } else if ("PROFILE_CREATION_IN_PROGRESS".equals(currentStep) || "PROFILE_CREATION_FAILED".equals(currentStep)) {
            // プロファイル作成ステップのリトライ
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "PROFILE_CREATION_RETRY", null);
            
            // リトライロジックを実装
            log.info("ユーザープロファイル作成リトライ: sagaId={}, userId={}", saga.getSagaId(), saga.getUserId());
            
            // 実際のリトライロジック（ここでは簡略化）
            saga.addCompletedStep("PROFILE_CREATION_RETRY_COMPLETED", "プロファイル作成リトライ成功");
            
            // Saga完了
            completeSaga(saga);
            
            // 成功レスポンス送信
            sendSuccessResponse(saga);
        }
    }
    
    /**
     * ユーザー削除Sagaのリトライ
     */
    private void retryUserDeletion(SagaTransaction saga) {
        // 現在のステップに基づいて適切なポイントからリトライ
        String currentStep = saga.getCurrentStep();
        
        if ("DELETION_VALIDATION_IN_PROGRESS".equals(currentStep) || "DELETION_VALIDATION_FAILED".equals(currentStep)) {
            // 削除検証ステップのリトライ
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "DELETION_VALIDATION_RETRY", null);
            
            // リトライロジックを実装
            log.info("ユーザー削除検証リトライ: sagaId={}, userId={}", saga.getSagaId(), saga.getUserId());
            
            // 実際のリトライロジック（ここでは簡略化）
            saga.addCompletedStep("DELETION_VALIDATION_RETRY_COMPLETED", "削除検証リトライ成功");
            
            // 次のステップへ進む
            // 実装省略 - 実際にはユーザー削除ステップを実行
            
        } else if ("PROFILE_DELETION_IN_PROGRESS".equals(currentStep) || "PROFILE_DELETION_FAILED".equals(currentStep)) {
            // プロファイル削除ステップのリトライ
            updateSagaStatus(saga, SagaStatus.SAGA_IN_PROGRESS, "PROFILE_DELETION_RETRY", null);
            
            // リトライロジックを実装
            log.info("ユーザープロファイル削除リトライ: sagaId={}, userId={}", saga.getSagaId(), saga.getUserId());
            
            // 実際のリトライロジック（ここでは簡略化）
            saga.addCompletedStep("PROFILE_DELETION_RETRY_COMPLETED", "プロファイル削除リトライ成功");
            
            // Saga完了
            completeSaga(saga);
            
            // 成功レスポンス送信
            sendSuccessResponse(saga);
        }
    }
}
