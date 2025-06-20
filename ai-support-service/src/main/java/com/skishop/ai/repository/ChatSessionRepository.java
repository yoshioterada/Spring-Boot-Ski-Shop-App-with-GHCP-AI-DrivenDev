package com.skishop.ai.repository;

import com.skishop.ai.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    @Query("{'userId': ?0}")
    List<ChatSession> findByUserId(String userId);

    @Query("{'userId': ?0, 'status': ?1}")
    List<ChatSession> findByUserIdAndStatus(String userId, String status);

    @Query("{'conversationId': ?0}")
    Optional<ChatSession> findByConversationId(String conversationId);

    @Query("{'sessionType': ?0}")
    List<ChatSession> findBySessionType(String sessionType);

    @Query("{'startedAt': {$gte: ?0, $lte: ?1}}")
    List<ChatSession> findByStartedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("{'userId': ?0, 'sessionType': ?1, 'status': 'ACTIVE'}")
    Optional<ChatSession> findActiveSessionByUserIdAndType(String userId, String sessionType);

    @Query("{'userId': ?0, 'status': 'ACTIVE'}")
    List<ChatSession> findActiveSessionsByUserId(String userId);
}
