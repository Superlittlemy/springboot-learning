package com.slm.multilevelcache.controller;

import com.slm.multilevelcache.cache.CacheLevel;
import com.slm.multilevelcache.cache.MultiLevelCache;
import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.consistency.CacheLoader;
import com.slm.multilevelcache.entity.Product;
import com.slm.multilevelcache.enums.ResultStatus;
import com.slm.multilevelcache.model.ApiResponse;
import com.slm.multilevelcache.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品接口
 *
 * <p>只提供 GET，可安全地被 Nginx proxy_cache 缓存。
 * 写接口一律走 POST/PUT/DELETE，Nginx 默认不会缓存这些方法，
 * 这也是接入层缓存必须遵守的边界。</p>
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    /**
     * 暴露命中层级的响应头
     */
    private static final String CACHE_LEVEL_HEADER = "X-Cache-Level";

    @Autowired
    private ProductService productService;

    @Autowired
    private MultiLevelCacheManager cacheManager;

    /**
     * 查询商品详情
     *
     * <p>响应头 {@code X-Cache-Level} 会标明本次命中的是 L0/L1/L2 还是回源了数据库，
     * 是验证多级缓存是否真正生效最直接的方式。</p>
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Long id) {
        CacheLoader.CacheResult<Product> result = productService.getById(id);
        if (!result.isPresent()) {
            ApiResponse<Product> body = ApiResponse.failure(ResultStatus.PRODUCT_NOT_FOUND, null);
            return ResponseEntity.status(ResultStatus.PRODUCT_NOT_FOUND.getHttpStatus())
                    .header(CACHE_LEVEL_HEADER, result.getLevel().getHeader())
                    .body(body);
        }
        return ResponseEntity.ok()
                .header(CACHE_LEVEL_HEADER, result.getLevel().getHeader())
                .body(ApiResponse.success(result.getValue()));
    }

    /**
     * 按名称模糊查询
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Product>>> list(@RequestParam(required = false) String name) {
        List<Product> products = productService.listByName(name);
        return ResponseEntity.ok().body(ApiResponse.success(products));
    }

    /**
     * 更新商品
     */
    @PutMapping
    public ApiResponse<Product> update(@Valid @RequestBody Product product) {
        return ApiResponse.success(productService.update(product));
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        return productService.delete(id) ? ApiResponse.success() : ApiResponse.failure(ResultStatus.PRODUCT_NOT_FOUND);
    }

    /**
     * 仅供演示与排查：查看本节点每个缓存的 L1 条目数
     *
     * <p>刷新页面观察这个数字在多次请求间保持不变，即可确认 L1 已经生效。</p>
     */
    @GetMapping("/cache-stats")
    public ApiResponse<Map<String, Long>> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (MultiLevelCache cache : cacheManager.getCaches()) {
            stats.put(cache.getName(), cache.localSize());
        }
        return ApiResponse.success(stats);
    }

}