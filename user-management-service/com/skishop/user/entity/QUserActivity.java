package com.skishop.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserActivity is a Querydsl query type for UserActivity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserActivity extends EntityPathBase<UserActivity> {

    private static final long serialVersionUID = 2029930742L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserActivity userActivity = new QUserActivity("userActivity");

    public final EnumPath<UserActivity.ActivityType> activityType = createEnum("activityType", UserActivity.ActivityType.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final StringPath metadata = createString("metadata");

    public final QUser user;

    public final StringPath userAgent = createString("userAgent");

    public QUserActivity(String variable) {
        this(UserActivity.class, forVariable(variable), INITS);
    }

    public QUserActivity(Path<? extends UserActivity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserActivity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserActivity(PathMetadata metadata, PathInits inits) {
        this(UserActivity.class, metadata, inits);
    }

    public QUserActivity(Class<? extends UserActivity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

