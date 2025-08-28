package com.acme.api.asynctaskqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor jobExecutor() {
        int poolSize = 5;          // fixed number of worker threads
        int queueCapacity = 10;     // bounded queue size
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);

        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                new ThreadPoolExecutor.AbortPolicy() // throws exception if queue full
        );
    }
}
