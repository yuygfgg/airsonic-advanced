package org.airsonic.player.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "BroadcastThreadPool")
    public Executor configThreadPool() {
        var threadPool = new ThreadPoolTaskExecutor();
        threadPool.setCorePoolSize(2);
        threadPool.setMaxPoolSize(5);
        threadPool.setQueueCapacity(500);
        threadPool.setDaemon(true);
        threadPool.setThreadNamePrefix("BroadcastThread-");
        threadPool.initialize();
        return threadPool;
    }
}
