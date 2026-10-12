package com.slm.multilevelcache.consistency;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 失效类型
 */
@Getter
@AllArgsConstructor
public enum CacheInvalidateType {

    /**
     * 清理单个 key
     */
    SINGLE("single"),

    /**
     * 清理某个 cache name 下的全部 key
     */
    PATTERN("pattern"),
    ;

    private final String type;

}