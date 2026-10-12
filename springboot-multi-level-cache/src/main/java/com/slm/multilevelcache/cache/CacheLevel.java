package com.slm.multilevelcache.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缓存层级
 *
 * <p>命中层级会通过 {@code X-Cache-Level} 响应头暴露，便于验证多级缓存实际生效。</p>
 */
@Getter
@AllArgsConstructor
public enum CacheLevel {

    /**
     * L0：Nginx proxy_cache 接入层缓存。请求未到达应用，由 Nginx 直接响应。
     */
    L0_NGINX("L0-Nginx"),

    /**
     * L1：Caffeine 本地缓存，进程内，纳秒级读取
     */
    L1_CAFFEINE("L1-Caffeine"),

    /**
     * L2：Redis 分布式缓存，跨节点共享，毫秒级读取
     */
    L2_REDIS("L2-Redis"),

    /**
     * DB：未命中任何缓存，回源到数据库
     */
    DB("DB"),

    /**
     * MISS：缓存中不存在该 key 且未查询数据库（空结果）
     */
    NULL("NULL"),
    ;

    private final String header;

}