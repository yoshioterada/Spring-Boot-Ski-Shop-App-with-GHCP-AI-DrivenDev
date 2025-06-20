package com.skishop.ai.entity;

import java.time.LocalDateTime;

/**
 * セッションステータス - Java 21のsealed interfaceを使用
 */
public sealed interface SessionStatus permits SessionStatus.Active, SessionStatus.Closed, SessionStatus.Escalated {
    record Active(LocalDateTime lastActivity) implements SessionStatus {}
    record Closed(String reason, LocalDateTime closedAt) implements SessionStatus {}
    record Escalated(String escalationReason, String assignedAgent) implements SessionStatus {}
}
