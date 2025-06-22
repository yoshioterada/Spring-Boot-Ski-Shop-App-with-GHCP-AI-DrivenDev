package com.skishop.user.service;

import com.skishop.user.entity.User;
import com.skishop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * ユーザー検索・フィルタリング共通サービス
 * 複雑な検索条件の処理を統一
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    /**
     * ユーザー検索（統一API）
     */
    public Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        log.debug("ユーザー検索実行: criteria={}", criteria);
        
        Specification<User> spec = buildSpecification(criteria);
        Page<User> result = userRepository.findAll(spec, pageable);
        
        log.debug("ユーザー検索結果: totalElements={}, totalPages={}", 
                result.getTotalElements(), result.getTotalPages());
        
        return result;
    }

    /**
     * 検索条件からSpecificationを構築
     */
    public Specification<User> buildSpecification(UserSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 検索キーワード条件
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                Predicate searchPredicate = buildSearchPredicate(root, criteriaBuilder, criteria.getSearch());
                predicates.add(searchPredicate);
            }

            // ステータス条件
            if (criteria.getStatus() != null && !criteria.getStatus().trim().isEmpty()) {
                Predicate statusPredicate = buildStatusPredicate(root, criteriaBuilder, criteria.getStatus());
                predicates.add(statusPredicate);
            }

            // ロール条件
            if (criteria.getRole() != null && !criteria.getRole().trim().isEmpty()) {
                Predicate rolePredicate = buildRolePredicate(root, criteriaBuilder, criteria.getRole());
                predicates.add(rolePredicate);
            }

            // メール検証状態条件
            if (criteria.getEmailVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("emailVerified"), criteria.getEmailVerified()));
            }

            // 電話番号検証状態条件
            if (criteria.getPhoneVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("phoneVerified"), criteria.getPhoneVerified()));
            }

            // 作成日範囲条件
            if (criteria.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAfter()));
            }
            
            if (criteria.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedBefore()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 検索キーワード条件構築
     */
    private Predicate buildSearchPredicate(jakarta.persistence.criteria.Root<User> root, 
                                         jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, 
                                         String search) {
        String searchPattern = "%" + search.toLowerCase() + "%";
        
        return criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), searchPattern)
        );
    }

    /**
     * ステータス条件構築
     */
    private Predicate buildStatusPredicate(jakarta.persistence.criteria.Root<User> root, 
                                         jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, 
                                         String status) {
        try {
            User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
            return criteriaBuilder.equal(root.get("status"), userStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: {}", status);
            // 無効なステータスの場合は条件を無視
            return criteriaBuilder.conjunction();
        }
    }

    /**
     * ロール条件構築
     */
    private Predicate buildRolePredicate(jakarta.persistence.criteria.Root<User> root, 
                                       jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, 
                                       String role) {
        return criteriaBuilder.equal(root.join("role").get("name"), role);
    }

    /**
     * 検索条件クラス
     */
    public static class UserSearchCriteria {
        private String search;
        private String status;
        private String role;
        private Boolean emailVerified;
        private Boolean phoneVerified;
        private java.time.LocalDateTime createdAfter;
        private java.time.LocalDateTime createdBefore;

        // Getters and Setters
        public String getSearch() { return search; }
        public void setSearch(String search) { this.search = search; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public Boolean getEmailVerified() { return emailVerified; }
        public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

        public Boolean getPhoneVerified() { return phoneVerified; }
        public void setPhoneVerified(Boolean phoneVerified) { this.phoneVerified = phoneVerified; }

        public java.time.LocalDateTime getCreatedAfter() { return createdAfter; }
        public void setCreatedAfter(java.time.LocalDateTime createdAfter) { this.createdAfter = createdAfter; }

        public java.time.LocalDateTime getCreatedBefore() { return createdBefore; }
        public void setCreatedBefore(java.time.LocalDateTime createdBefore) { this.createdBefore = createdBefore; }

        @Override
        public String toString() {
            return "UserSearchCriteria{" +
                    "search='" + search + '\'' +
                    ", status='" + status + '\'' +
                    ", role='" + role + '\'' +
                    ", emailVerified=" + emailVerified +
                    ", phoneVerified=" + phoneVerified +
                    ", createdAfter=" + createdAfter +
                    ", createdBefore=" + createdBefore +
                    '}';
        }
    }
}
