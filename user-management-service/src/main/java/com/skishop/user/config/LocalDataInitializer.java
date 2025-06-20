package com.skishop.user.config;

import com.skishop.user.entity.Role;
import com.skishop.user.entity.User;
import com.skishop.user.repository.RoleRepository;
import com.skishop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ローカル開発環境用のテストデータ初期化
 */
@Component
@ConditionalOnProperty(name = "init.test.data", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class LocalDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Initializing test data for local development...");
            initializeTestData();
            log.info("Test data initialization completed");
        } else {
            log.info("Test data already exists, skipping initialization");
        }
    }

    private void initializeTestData() {
        try {
            // デフォルトロールの作成
            Role userRole = createDefaultRole();
            
            // テストユーザーの作成
            createTestUsers(userRole);
            
        } catch (Exception e) {
            log.error("Failed to initialize test data", e);
        }
    }

    private Role createDefaultRole() {
        Role role = Role.builder()
                .name("USER")
                .description("Default user role")
                .build();
        return roleRepository.save(role);
    }

    private void createTestUsers(Role userRole) {
        // アクティブユーザー
        User activeUser = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("090-1234-5678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(User.Gender.OTHER)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .role(userRole)
                .build();
        
        // 認証待ちユーザー
        User pendingUser = User.builder()
                .email("pending@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Pending")
                .lastName("User")
                .phoneNumber("090-8765-4321")
                .birthDate(LocalDate.of(1985, 5, 15))
                .gender(User.Gender.MALE)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .role(userRole)
                .build();
        
        // 管理者ユーザー
        User adminUser = User.builder()
                .email("admin@skishop.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .phoneNumber("090-0000-1111")
                .birthDate(LocalDate.of(1980, 12, 31))
                .gender(User.Gender.FEMALE)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .role(userRole)
                .build();

        userRepository.save(activeUser);
        userRepository.save(pendingUser);
        userRepository.save(adminUser);

        log.info("Created test users:");
        log.info("  - Active User: test@example.com / password123");
        log.info("  - Pending User: pending@example.com / password123");
        log.info("  - Admin User: admin@skishop.com / admin123");
    }
}
