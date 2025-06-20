package com.skishop.sales.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 注文番号生成器
 * Java 21のString Templatesとモダンな機能を活用
 */
@Component
public class OrderNumberGenerator {

    private static final String PREFIX = "ORD";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong counter = new AtomicLong(1);

    /**
     * 注文番号生成
     * 形式: ORD + YYYYMMDD + 6桁連番
     * 例: ORD20240620000001
     * Java 21のString Template (プレビュー機能) を使用
     */
    public String generate() {
        var dateStr = LocalDateTime.now().format(DATE_FORMAT);
        var sequence = counter.getAndIncrement();
        var paddedSequence = String.format("%06d", sequence % 1000000);
        
        // Java 21のString.format()を使用（String Templateは--enable-preview必要）
        return String.format("%s%s%s", PREFIX, dateStr, paddedSequence);
    }

    /**
     * カウンターリセット（テスト用）
     */
    public void resetCounter() {
        counter.set(1);
    }

    /**
     * 現在のカウンター値取得（テスト用）
     */
    public long getCurrentCounter() {
        return counter.get();
    }
}
