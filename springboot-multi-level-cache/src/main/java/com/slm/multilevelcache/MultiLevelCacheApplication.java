package com.slm.multilevelcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 多级缓存应用
 *
 * <p>{@code @EnableCaching} 让 {@code @Cacheable/@CacheEvict} 走
 * {@link com.slm.multilevelcache.cache.MultiLevelCacheManager}；
 * {@code @EnableScheduling} 驱动一致性对账任务。</p>
 */
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class MultiLevelCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiLevelCacheApplication.class, args);
    }

}