package com.slm.multilevelcache.cache;

import java.io.Serializable;

/**
 * 空值占位符
 *
 * <p>用于缓存穿透防护：数据库查不到数据时不缓存 {@code null}（Redis 也不存 null），
 * 而是缓存一个占位对象并设置很短的 TTL。这样大量针对不存在 key 的请求
 * 只会打到 L2，而不会持续回源数据库。</p>
 */
public final class CacheNullValue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单例，占位对象不携带状态
     */
    public static final CacheNullValue INSTANCE = new CacheNullValue();

    private CacheNullValue() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CacheNullValue;
    }

    @Override
    public int hashCode() {
        return CacheNullValue.class.hashCode();
    }

    @Override
    public String toString() {
        return "CacheNullValue{}";
    }

}