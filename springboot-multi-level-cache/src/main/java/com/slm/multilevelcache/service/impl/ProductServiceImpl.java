package com.slm.multilevelcache.service.impl;

import com.slm.multilevelcache.consistency.CacheConsistencyService;
import com.slm.multilevelcache.consistency.CacheLoader;
import com.slm.multilevelcache.entity.Product;
import com.slm.multilevelcache.enums.ResultStatus;
import com.slm.multilevelcache.exception.CacheException;
import com.slm.multilevelcache.repository.ProductRepository;
import com.slm.multilevelcache.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 商品服务实现
 *
 * <p>读路径统一走 {@link CacheLoader}，写路径统一走「更新数据库 → 删除缓存 → 广播」。
 * 业务代码里不出现任何 {@code redisTemplate} 调用，一致性策略全部收敛到
 * consistency 包里，这是本模块分层约束的一条硬性要求。</p>
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    /**
     * 商品详情缓存名
     */
    public static final String CACHE_PRODUCT = "product";

    /**
     * 商品列表缓存名
     */
    public static final String CACHE_PRODUCT_LIST = "product:list";

    @Resource
    private ProductRepository productRepository;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private CacheLoader cacheLoader;

    @Resource
    private CacheConsistencyService cacheConsistencyService;

    @Override
    public CacheLoader.CacheResult<Product> getById(Long id) {
        if (id == null) {
            throw new CacheException(ResultStatus.BAD_REQUEST, "商品 id 不能为空");
        }
        Cache cache = cacheManager.getCache(CACHE_PRODUCT);
        return cacheLoader.load(cache, id, () -> productRepository.findById(id).orElse(null));
    }

    @Override
    public List<Product> listByName(String keyword) {
        String key = keyword == null ? "" : keyword;
        Cache cache = cacheManager.getCache(CACHE_PRODUCT_LIST);
        CacheLoader.CacheResult<List<Product>> result = cacheLoader.load(cache, key,
                () -> productRepository.findByNameContaining(keyword));
        return result.getValue() != null ? result.getValue() : List.of();
    }

    /**
     * 更新商品。
     *
     * <p>严格遵循「先更新数据库，再删除缓存」：如果反过来先删缓存，
     * 并发读线程会在数据库尚未提交时回填旧值，一致性窗口会被显著放大。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product update(Product product) {
        if (product == null || product.getId() == null) {
            throw new CacheException(ResultStatus.BAD_REQUEST, "商品 id 不能为空");
        }
        Optional<Product> existing = productRepository.findById(product.getId());
        if (!existing.isPresent()) {
            throw new CacheException(ResultStatus.PRODUCT_NOT_FOUND);
        }

        Product saved = productRepository.save(product);

        // 详情缓存按 id 精确失效
        cacheConsistencyService.evict(CACHE_PRODUCT, saved.getId());
        // 列表缓存整体失效：列表是按任意关键词缓存的，改一个商品可能影响
        // 任何一条关键词结果，无法精确定位到要删哪几条。宁可多删一次，
        // 也不要留下脏数据——列表查询本身是低频且低延迟要求的。
        cacheConsistencyService.evictAll(CACHE_PRODUCT_LIST);
        log.info("商品已更新并清理缓存：id={}, version={}", saved.getId(), saved.getVersion());
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        if (id == null) {
            throw new CacheException(ResultStatus.BAD_REQUEST, "商品 id 不能为空");
        }
        boolean removed = productRepository.deleteById(id);
        if (removed) {
            cacheConsistencyService.evict(CACHE_PRODUCT, id);
            // 删除会让所有列表查询结果变化，整体失效
            cacheConsistencyService.evictAll(CACHE_PRODUCT_LIST);
        }
        return removed;
    }

}