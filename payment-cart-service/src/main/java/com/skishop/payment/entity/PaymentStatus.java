package com.skishop.payment.entity;

/**
 * 決済ステータス
 * Java 21のsealed classを使用した改善版
 */
public sealed interface PaymentStatus 
    permits PaymentStatus.Pending, 
            PaymentStatus.RequiresAction, 
            PaymentStatus.Confirmed, 
            PaymentStatus.Completed, 
            PaymentStatus.Failed, 
            PaymentStatus.Cancelled, 
            PaymentStatus.Refunded, 
            PaymentStatus.PartiallyRefunded {

    /**
     * 表示名を取得
     */
    String getDisplayName();

    /**
     * 決済が処理中かどうか
     */
    default boolean isProcessing() {
        return this instanceof Pending || this instanceof RequiresAction;
    }

    /**
     * 決済が完了状態かどうか
     */
    default boolean isCompleted() {
        return this instanceof Completed || this instanceof Refunded || this instanceof PartiallyRefunded;
    }

    /**
     * 決済が失敗状態かどうか
     */
    default boolean isFailed() {
        return this instanceof Failed || this instanceof Cancelled;
    }

    record Pending() implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "処理中";
        }
    }

    record RequiresAction() implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "アクション必要";
        }
    }

    record Confirmed() implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "確認済み";
        }
    }

    record Completed() implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "完了";
        }
    }

    record Failed(String reason) implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "失敗";
        }
    }

    record Cancelled(String reason) implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "キャンセル";
        }
    }

    record Refunded() implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "返金済み";
        }
    }

    record PartiallyRefunded() implements PaymentStatus {
        @Override
        public String getDisplayName() {
            return "部分返金済み";
        }
    }

    /**
     * 文字列からPaymentStatusを生成
     */
    static PaymentStatus fromString(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING" -> new Pending();
            case "REQUIRES_ACTION" -> new RequiresAction();
            case "CONFIRMED" -> new Confirmed();
            case "COMPLETED" -> new Completed();
            case "FAILED" -> new Failed(null);
            case "CANCELLED" -> new Cancelled(null);
            case "REFUNDED" -> new Refunded();
            case "PARTIALLY_REFUNDED" -> new PartiallyRefunded();
            default -> throw new IllegalArgumentException("Unknown payment status: " + status);
        };
    }

    /**
     * PaymentStatusを文字列に変換
     */
    default String toStatusString() {
        return switch (this) {
            case Pending() -> "PENDING";
            case RequiresAction() -> "REQUIRES_ACTION";
            case Confirmed() -> "CONFIRMED";
            case Completed() -> "COMPLETED";
            case Failed(var reason) -> "FAILED";
            case Cancelled(var reason) -> "CANCELLED";
            case Refunded() -> "REFUNDED";
            case PartiallyRefunded() -> "PARTIALLY_REFUNDED";
        };
    }
}
