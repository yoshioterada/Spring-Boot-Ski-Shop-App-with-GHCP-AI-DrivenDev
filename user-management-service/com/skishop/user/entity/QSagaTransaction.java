package com.skishop.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSagaTransaction is a Querydsl query type for SagaTransaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSagaTransaction extends EntityPathBase<SagaTransaction> {

    private static final long serialVersionUID = -1700368486L;

    public static final QSagaTransaction sagaTransaction = new QSagaTransaction("sagaTransaction");

    public final MapPath<String, String, StringPath> completedSteps = this.<String, String, StringPath>createMap("completedSteps", String.class, String.class, StringPath.class);

    public final MapPath<String, String, StringPath> context = this.<String, String, StringPath>createMap("context", String.class, String.class, StringPath.class);

    public final StringPath correlationId = createString("correlationId");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath currentStep = createString("currentStep");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath errorType = createString("errorType");

    public final StringPath eventType = createString("eventType");

    public final NumberPath<Integer> maxRetryCount = createNumber("maxRetryCount", Integer.class);

    public final StringPath originalEventId = createString("originalEventId");

    public final DateTimePath<java.time.LocalDateTime> processingEndTime = createDateTime("processingEndTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> processingStartTime = createDateTime("processingStartTime", java.time.LocalDateTime.class);

    public final NumberPath<Integer> retryCount = createNumber("retryCount", Integer.class);

    public final StringPath sagaId = createString("sagaId");

    public final EnumPath<com.skishop.user.enums.SagaStatus> status = createEnum("status", com.skishop.user.enums.SagaStatus.class);

    public final DateTimePath<java.time.LocalDateTime> timeoutAt = createDateTime("timeoutAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath userId = createString("userId");

    public QSagaTransaction(String variable) {
        super(SagaTransaction.class, forVariable(variable));
    }

    public QSagaTransaction(Path<? extends SagaTransaction> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSagaTransaction(PathMetadata metadata) {
        super(SagaTransaction.class, metadata);
    }

}

