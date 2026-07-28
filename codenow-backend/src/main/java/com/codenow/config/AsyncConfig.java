package com.codenow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 审计等旁路任务的异步线程池。队列满时 CallerRunsPolicy 会让请求线程同步执行以形成反压；
 * 应用关闭最多等待 30 秒，不能据此承诺所有异步任务一定落库。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 创建异步任务线程池 Bean，用于审计日志等旁路异步任务。
     * 核心线程数 2，最大线程数 8，队列容量 500，队列满时由调用线程同步执行。
     *
     * @return 配置好的线程池任务执行器
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("codenow-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
