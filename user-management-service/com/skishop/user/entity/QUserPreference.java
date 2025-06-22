package com.skishop.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserPreference is a Querydsl query type for UserPreference
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserPreference extends EntityPathBase<UserPreference> {

    private static final long serialVersionUID = 1634809090L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserPreference userPreference = new QUserPreference("userPreference");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final StringPath prefKey = createString("prefKey");

    public final EnumPath<UserPreference.PreferenceType> prefType = createEnum("prefType", UserPreference.PreferenceType.class);

    public final StringPath prefValue = createString("prefValue");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final QUser user;

    public QUserPreference(String variable) {
        this(UserPreference.class, forVariable(variable), INITS);
    }

    public QUserPreference(Path<? extends UserPreference> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserPreference(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserPreference(PathMetadata metadata, PathInits inits) {
        this(UserPreference.class, metadata, inits);
    }

    public QUserPreference(Class<? extends UserPreference> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

