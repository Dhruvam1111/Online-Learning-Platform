package com.olp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${app.async.core-pool-size:4}")
    private int corePoolSize = 4;

    @Value("${app.async.max-pool-size:16}")
    private int maxPoolSize = 16;

    @Value("${app.async.queue-capacity:100}")
    private int queueCapacity = 100;

    @Value("${app.async.thread-name-prefix:olp-async-}")
    private String threadNamePrefix = "olp-async-";

    @Value("${app.async.await-termination-seconds:30}")
    private int awaitTerminationSeconds = 30;

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        logger.info("Initialized bounded taskExecutor with core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("Uncaught exception in async method: {} with params: {}",
                    method.getName(), Arrays.toString(params), throwable);
        };
    }

    // Setters for testing
    public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    public void setThreadNamePrefix(String threadNamePrefix) { this.threadNamePrefix = threadNamePrefix; }
    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) { this.awaitTerminationSeconds = awaitTerminationSeconds; }

    public int getCorePoolSize() { return corePoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getQueueCapacity() { return queueCapacity; }
    public String getThreadNamePrefix() { return threadNamePrefix; }
    public int getAwaitTerminationSeconds() { return awaitTerminationSeconds; }
}
