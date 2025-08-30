package com.acme.api.asynctaskqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    /**
     * The executor for regular jobs. All initial jobs are handled by this executor.
     */
    @Bean
    public ThreadPoolExecutor normalJobExecutor() {
        int poolSize = 5;
        int queueCapacity = 10;
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);

        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                new ThreadPoolExecutor.AbortPolicy() // throws RejectedExecutionException if full
        );
    }

    /**
     * Exclusive executor to handle compensation jobs. It contains a smaller number of worker threads
     * since jobs are not expected to fail often.
     */
    @Bean
    public ThreadPoolExecutor compensationJobExecutor() {
        int poolSize = 2;          // fewer threads for compensation
        int queueCapacity = 5;     // smaller queue
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);

        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }


    /**
     * Retry executor is single-threaded. This was a decision based solely for simplicity
     * of demonstration of retry logic in a FIFO manner; which made it easier to see them
     * sequentially in the logs. For example:
     * <pre>
     * 18:06:59.864: ⚠️ Queue full. Retrying Email attempt 3 after 8601ms (simulated)...
     * 18:06:59.932: POST http://localhost:8080/v1/jobs	429	16 ms
     * 18:07:00.012: ⚠️ Queue full. Retrying Email attempt 4 after 16600ms (simulated)...
     * 18:07:00.035: POST http://localhost:8080/v1/jobs	429	15 ms
     * 18:07:00.044: ⚠️ Queue full. Retrying Email attempt 5 after 32596ms (simulated)...
     * 18:07:00.086: POST http://localhost:8080/v1/jobs	429	8 ms
     * 18:07:00.097: ⚠️ Queue full. Retrying Email attempt 6 after 64378ms (simulated)...
     * </pre>
     */
    @Bean
    public ScheduledExecutorService retryScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
