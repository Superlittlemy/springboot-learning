package com.slm.multilevelcache.consistency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缓存失效广播消息
 *
 * <p>L1 是每个节点私有的进程内缓存，Redis Pub/Sub 删不到它。
 * 因此写操作方在删掉 L2 之后，必须再发一条广播，让所有节点各自清理自己的 L1。</p>
 *
 * <p>刻意使用普通 POJO 而非 record：消息需要被 Jackson 序列化后在网络上传送，
 * 保持简单可序列化比不可变更重要。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 缓存名
     */
    private String cacheName;

    /**
     * 缓存 key；PATTERN 类型时为 null，表示清空整个缓存名
     */
    private String key;

    /**
     * 失效类型
     */
    private CacheInvalidateType type;

    /**
     * 发起节点标识，仅用于排查
     */
    private String originNode;

    public static CacheInvalidateMessage of(String cacheName, Object key, String originNode) {
        return new CacheInvalidateMessage(cacheName,
                key == null ? null : String.valueOf(key),
                CacheInvalidateType.SINGLE, originNode);
    }

    public static CacheInvalidateMessage ofAll(String cacheName, String originNode) {
        return new CacheInvalidateMessage(cacheName, null, CacheInvalidateType.PATTERN, originNode);
    }

}