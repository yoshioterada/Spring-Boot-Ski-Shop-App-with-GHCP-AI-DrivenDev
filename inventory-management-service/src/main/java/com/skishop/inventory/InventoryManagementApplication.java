package com.skishop.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 在庫管理サービスのメインアプリケーションクラス
 * 
 * 機能:
 * - 商品カタログ管理
 * - 在庫状況追跡
 * - 商品属性・カテゴリ管理
 * - 価格設定・割引管理
 * - 入荷・出荷処理
 * - 商品画像・メディア管理
 */
@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.skishop.inventory.repository.jpa")
@EnableMongoRepositories(basePackages = "com.skishop.inventory.repository.mongo")
public class InventoryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryManagementApplication.class, args);
    }
}
