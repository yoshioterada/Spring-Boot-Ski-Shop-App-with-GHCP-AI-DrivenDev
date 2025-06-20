package com.skishop.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 在庫管理サービス メインアプリケーション
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.skishop.inventory.repository.jpa")
@EnableMongoRepositories(basePackages = "com.skishop.inventory.repository.mongo")
@EnableScheduling
public class InventoryManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryManagementServiceApplication.class, args);
    }
}
