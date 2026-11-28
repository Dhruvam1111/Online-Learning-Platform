package com.olp.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    void testGetAsyncExecutor_InitializesBoundedExecutorCorrectly() {
        AsyncConfig config = new AsyncConfig();
        config.setCorePoolSize(2);
        config.setMaxPoolSize(8);
        config.setQueueCapacity(50);
        config.setThreadNamePrefix("test-async-");
        config.setAwaitTerminationSeconds(15);

        Executor executor = config.getAsyncExecutor();
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);

        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertEquals(2, pool.getCorePoolSize());
        assertEquals(8, pool.getMaxPoolSize());
        assertEquals(50, pool.getQueueCapacity());
        assertEquals("test-async-", pool.getThreadNamePrefix());

        pool.shutdown();
    }

    @Test
    void testGetAsyncUncaughtExceptionHandler_LogsWithoutThrowing() throws Exception {
        AsyncConfig config = new AsyncConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        assertNotNull(handler);

        Method testMethod = AsyncConfigTest.class.getDeclaredMethod("testGetAsyncUncaughtExceptionHandler_LogsWithoutThrowing");
        assertDoesNotThrow(() -> handler.handleUncaughtException(
                new RuntimeException("Simulated async task failure"),
                testMethod,
                "param1", 42
        ));
    }

    @Test
    void testConfigurationGettersAndSetters() {
        AsyncConfig config = new AsyncConfig();
        config.setCorePoolSize(6);
        config.setMaxPoolSize(20);
        config.setQueueCapacity(200);
        config.setThreadNamePrefix("custom-");
        config.setAwaitTerminationSeconds(45);

        assertEquals(6, config.getCorePoolSize());
        assertEquals(20, config.getMaxPoolSize());
        assertEquals(200, config.getQueueCapacity());
        assertEquals("custom-", config.getThreadNamePrefix());
        assertEquals(45, config.getAwaitTerminationSeconds());
    }
}
