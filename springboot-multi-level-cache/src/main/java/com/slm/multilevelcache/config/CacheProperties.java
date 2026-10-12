package com.slm.multilevelcache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 多级缓存配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "multi-level-cache")
public class CacheProperties {

    /**
     * L1 本地缓存存活时间：刻意设得比 L2 短，本地缓存不能无限存活，
     * 否则一旦某次失效广播丢失，该节点会一直读到旧数据。
     *
     * <p>注：Caffeine 2.x 的 {@code expireAfterWrite} 是「每缓存」而非「每条目」级别，
     * 无法对不同 key 叠加随机过期，因此 L1 侧的抗雪崩依赖「短 TTL + 重建互斥锁」，
     * 抖动只作用于 L2。</p>
     */
    private Duration localTtl = Duration.ofMinutes(5);

    /**
     * L1 本地缓存最大条目数
     */
    private long localMaximumSize = 10_000;

    /**
     * L2 分布式缓存默认存活时间
     */
    private Duration remoteTtl = Duration.ofMinutes(30);

    /**
     * L2 存活时间抖动比例(0~1)：每次写入在 ±该比例内随机偏移，
     * 打散大批 key 的到期时间，避免缓存雪崩
     */
    private double remoteTtlJitter = 0.1;

    /**
     * L2 是否允许缓存空值：防止缓存穿透（缓存穿透缓存负标记）
     */
    private boolean cacheNullValue = true;

    /**
     * 空值标记在 L2 中的存活时间：远短于正常缓存，避免数据新建后长时间不可见
     */
    private Duration nullValueTtl = Duration.ofMinutes(1);

    /**
     * 缓存失效广播频道：L1 是节点私有的，写操作后需通过该频道通知所有节点清理本地缓存
     */
    private String invalidateChannel = "multi-level-cache:invalidate";

    /**
     * 重建互斥锁的锁前缀
     */
    private String lockKeyPrefix = "multi-level-cache:lock:";

    /**
     * 是否启用延迟双删：应对「读线程在删除缓存后、写缓存前把旧值写回」的并发竞态
     */
    private boolean delayedDoubleDeleteEnabled = true;

    /**
     * 延迟双删的二次删除延迟
     */
    private Duration doubleDeleteDelay = Duration.ofSeconds(1);

    /**
     * 重建锁最长持有时间：超时自动释放，避免持锁节点宕机后死锁
     */
    private Duration rebuildLockTtl = Duration.ofSeconds(5);

}