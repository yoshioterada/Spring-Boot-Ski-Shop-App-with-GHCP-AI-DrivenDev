package com.skishop.sales.config;

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Java 21のVirtual Threads設定
 * パフォーマンス向上のためにVirtual Threadsを有効化
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * Virtual ThreadsベースのTaskExecutor
     * Java 21の軽量スレッドを使用して高いスケーラビリティを実現
     */
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Virtual Threadsを使用したカスタムExecutor
     * 非同期処理専用
     */
    @Bean("virtualThreadTaskExecutor")
    public AsyncTaskExecutor virtualThreadTaskExecutor() {
        return new TaskExecutorAdapter(
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name("virtual-task-", 0)
                .factory())
        );
    }
}
