package com.skishop.ai.entity;

import java.time.LocalDateTime;

/**
 * チャットセッションタイプ - Java 21のsealed interfaceを使用
 */
public sealed interface SessionType permits SessionType.Support, SessionType.Recommendation, SessionType.Search {
    record Support(String category) implements SessionType {}
    record Recommendation(String intent) implements SessionType {}
    record Search(String domain) implements SessionType {}
}
