package com.skishop.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -892491577L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final SetPath<UserActivity, QUserActivity> activities = this.<UserActivity, QUserActivity>createSet("activities", UserActivity.class, QUserActivity.class, PathInits.DIRECT2);

    public final DatePath<java.time.LocalDate> birthDate = createDate("birthDate", java.time.LocalDate.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final BooleanPath emailVerified = createBoolean("emailVerified");

    public final StringPath firstName = createString("firstName");

    public final EnumPath<User.Gender> gender = createEnum("gender", User.Gender.class);

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final StringPath lastName = createString("lastName");

    public final StringPath passwordHash = createString("passwordHash");

    public final StringPath phoneNumber = createString("phoneNumber");

    public final BooleanPath phoneVerified = createBoolean("phoneVerified");

    public final SetPath<UserPreference, QUserPreference> preferences = this.<UserPreference, QUserPreference>createSet("preferences", UserPreference.class, QUserPreference.class, PathInits.DIRECT2);

    public final QRole role;

    public final EnumPath<User.UserStatus> status = createEnum("status", User.UserStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.role = inits.isInitialized("role") ? new QRole(forProperty("role")) : null;
    }

}

