package com.skishop.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProcessedEvent is a Querydsl query type for ProcessedEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProcessedEvent extends EntityPathBase<ProcessedEvent> {

    private static final long serialVersionUID = -1330591256L;

    public static final QProcessedEvent processedEvent = new QProcessedEvent("processedEvent");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath eventData = createString("eventData");

    public final StringPath eventId = createString("eventId");

    public final StringPath eventType = createString("eventType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isSuccess = createBoolean("isSuccess");

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final StringPath processingNode = createString("processingNode");

    public final NumberPath<Long> processingTimeMs = createNumber("processingTimeMs", Long.class);

    public final StringPath sagaId = createString("sagaId");

    public final ComparablePath<java.util.UUID> userId = createComparable("userId", java.util.UUID.class);

    public QProcessedEvent(String variable) {
        super(ProcessedEvent.class, forVariable(variable));
    }

    public QProcessedEvent(Path<? extends ProcessedEvent> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProcessedEvent(PathMetadata metadata) {
        super(ProcessedEvent.class, metadata);
    }

}

