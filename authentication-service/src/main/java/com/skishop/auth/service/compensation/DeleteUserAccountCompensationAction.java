package com.skishop.auth.service.compensation;

import com.skishop.auth.enums.UserRegistrationStatus;
import com.skishop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ユーザーアカウント削除の補償アクション
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteUserAccountCompensationAction implements CompensationAction {

    private final UserRepository userRepository;

    @Override
    public boolean compensate(String sagaId, CompensationContext context) {
        try {
            log.info("Executing user account deletion compensation for saga: {}, user: {}", 
                sagaId, context.getUserId());

            // ユーザーアカウントを論理削除または無効化
            userRepository.findById(context.getUserId()).ifPresent(user -> {
                user.setStatus("COMPENSATED_DELETED");
                userRepository.save(user);
                
                log.info("User account compensated (deleted) for user: {} in saga: {}", 
                    context.getUserId(), sagaId);
            });

            return true;

        } catch (Exception e) {
            log.error("Failed to compensate user account deletion for saga {}: {}", 
                sagaId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isApplicable(String sagaType, String status) {
        return "USER_REGISTRATION".equals(sagaType) && 
               (UserRegistrationStatus.ACCOUNT_CREATED.name().equals(status) ||
                UserRegistrationStatus.EVENT_PUBLISHED.name().equals(status) ||
                UserRegistrationStatus.PENDING_USER_MANAGEMENT.name().equals(status));
    }

    @Override
    public int getPriority() {
        return 1; // 高優先度
    }

    @Override
    public String getActionName() {
        return "DELETE_USER_ACCOUNT";
    }
}
