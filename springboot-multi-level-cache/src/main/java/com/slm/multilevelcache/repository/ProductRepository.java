package com.slm.multilevelcache.repository;

import com.slm.multilevelcache.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * 商品仓储
 *
 * <p>本模块用内存实现代替真实的数据库，目的是让整套缓存链路可以独立运行与测试，
 * 不依赖外部存储。接入 MyBatis 时只需替换实现类，缓存层代码无需改动。</p>
 */
public interface ProductRepository {

    Optional<Product> findById(Long id);

    List<Product> findByNameContaining(String keyword);

    Product save(Product product);

    boolean deleteById(Long id);

}