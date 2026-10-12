package com.slm.multilevelcache.repository;

import com.slm.multilevelcache.entity.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版商品仓储
 *
 * <p>扮演数据库的角色。用 {@link ConcurrentHashMap} 模拟持久化存储，
 * 写操作刻意加入一点随机延迟，好让缓存一致性的时序问题在本地也能被观察到。</p>
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(1000);

    public InMemoryProductRepository() {
        initData();
    }

    private void initData() {
        save(newProduct("机械键盘", "客制化 87 键", new BigDecimal("499.00"), 100));
        save(newProduct("人体工学椅", "网面透气", new BigDecimal("1899.00"), 20));
        save(newProduct("4K 显示器", "27 英寸", new BigDecimal("2499.00"), 35));
        save(newProduct("降噪耳机", "主动降噪", new BigDecimal("1299.00"), 60));
    }

    private Product newProduct(String name, String desc, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(desc);
        product.setPrice(price);
        product.setStock(stock);
        product.setVersion(0);
        product.setUpdatedTime(LocalDateTime.now());
        return save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.store.get(id));
    }

    @Override
    public List<Product> findByNameContaining(String keyword) {
        List<Product> result = new ArrayList<>();
        for (Product product : this.store.values()) {
            // 关键词为空视为查全部，否则 String.contains(null) 会直接 NPE
            if (keyword == null || keyword.isEmpty()
                    || (product.getName() != null && product.getName().contains(keyword))) {
                result.add(product);
            }
        }
        return result;
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(this.idGenerator.incrementAndGet());
        }
        if (product.getVersion() == null) {
            product.setVersion(0);
        }
        product.setUpdatedTime(LocalDateTime.now());
        this.store.put(product.getId(), product);
        return product;
    }

    @Override
    public boolean deleteById(Long id) {
        return id != null && this.store.remove(id) != null;
    }

    /**
     * 仅供演示与测试：清空数据
     */
    public void clear() {
        this.store.clear();
    }

    /**
     * 仅供演示与测试：当前数据量
     */
    public int count() {
        return this.store.size();
    }

}