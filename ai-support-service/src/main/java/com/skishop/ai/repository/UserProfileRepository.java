package com.skishop.ai.repository;

import com.skishop.ai.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    @Query("{'userId': ?0}")
    Optional<UserProfile> findByUserId(String userId);

    @Query("{'loyaltyTier': ?0}")
    List<UserProfile> findByLoyaltyTier(String loyaltyTier);

    @Query("{'totalSpent': {$gte: ?0}}")
    List<UserProfile> findByTotalSpentGreaterThanEqual(Double minSpent);

    @Query("{'lastActivity': {$gte: ?0}}")
    List<UserProfile> findByLastActivityAfter(LocalDateTime date);

    @Query("{'favoriteCategories': {$in: ?0}}")
    List<UserProfile> findByFavoriteCategoriesIn(List<String> categories);

    @Query("{'totalSpent': {$gte: ?0, $lte: ?1}}")
    List<UserProfile> findByTotalSpentBetween(Double minSpent, Double maxSpent);
}
