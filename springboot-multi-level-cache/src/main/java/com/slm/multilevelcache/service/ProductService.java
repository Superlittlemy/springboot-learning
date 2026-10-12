package com.slm.multilevelcache.service;

import com.slm.multilevelcache.consistency.CacheLoader;
import com.slm.multilevelcache.entity.Product;

import java.util.List;

/**
 * 商品服务
 */
public interface ProductService {

    /**
     * 按 id 查询商品，走完整的多级缓存读路径
     *
     * @return 带命中层级的查询结果；商品不存在时 value 为 null
     */
    CacheLoader.CacheResult<Product> getById(Long id);

    /**
     * 按名称模糊查询
     */
    List<Product> listByName(String keyword);

    /**
     * 更新商品：先更新数据库，再删除缓存并广播失效
     */
    Product update(Product product);

    /**
     * 删除商品：先删除数据库，再删除缓存并广播失效
     */
    boolean delete(Long id);

}