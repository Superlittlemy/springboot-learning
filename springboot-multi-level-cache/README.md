# springboot-multi-level-cache

> 基于 Nginx proxy_cache + Caffeine + Redis 的多级缓存架构，并把**缓存一致性单独抽成一个包**处理。

## 一、架构

```
        ┌─────────────────────────────────────────────┐
        │  L0  Nginx proxy_cache（接入层缓存）          │
        │      命中时请求根本不进入应用，零应用开销     │
        └────────────────────┬────────────────────────┘
                             │ 未命中
        ┌────────────────────▼────────────────────────┐
        │  L1  Caffeine（本地缓存，进程内）             │
        │      ~100ns 读取，但每个节点私有              │
        └────────────────────┬────────────────────────┘
                             │ 未命中（并回填 L1）
        ┌────────────────────▼────────────────────────┐
        │  L2  Redis（分布式缓存，跨节点共享）           │
        │      ~1ms 读取                              │
        └────────────────────┬────────────────────────┘
                             │ 未命中（并回填 L2）
        ┌────────────────────▼────────────────────────┐
        │  DB   数据源（唯一真相源）                    │
        └─────────────────────────────────────────────┘
```

每一层都砍掉它下面那层的流量，所以**上面的命中率必须远高于下面**，否则缓存就是负优化。
接口会通过 `X-Cache-Level` 响应头暴露实际命中的层级（L0-Nginx / L1-Caffeine / L2-Redis / DB），
`X-Cache-Status` 则来自 Nginx（`HIT` / `MISS` / `EXPIRED` / `STALE`）。

### 为什么三层要这么组合

| 层级 | 解决的问题 | 代价 |
|---|---|---|
| Nginx | 海量重复请求的重复计算，成本最低 | 一致性最差，只能靠 TTL 和 `purge` |
| Caffeine | 挡住 Redis 的网络往返与连接竞争 | 内存占用随实例数放大；各节点数据独立 |
| Redis | 跨节点共享一份，减少回源数据库 | 网络往返；成为新的单点 |

### 本地缓存与分布式缓存的矛盾

这是多级缓存的核心难点：**L1 越快，一致性代价越大**。

Caffeine 的读取是纳秒级，但它只存在于当前 JVM 里。Redis 的删除命令**删不掉别的进程内存里的数据**。
所以 L1 一旦缓存了旧值，即使 L2 已经被删除、数据库已经更新，这个节点仍会一直返回旧值，直到本地 TTL 到期。

本模块的解法是：**Redis Pub/Sub 广播失效**，并用「L1 TTL 短于 L2」作为兜底自愈手段。

---

## 二、缓存一致性方案（单独处理）

一致性逻辑集中在 `com.slm.multilevelcache.consistency` 包，业务代码里**不出现任何 Redis 调用**，
写路径统一收敛成「更新数据库 → 删除缓存」。

### 四层保障机制

| # | 机制 | 解决的问题 | 实现位置 |
|---|---|---|---|
| 1 | 删除缓存而非更新缓存 | 并发读会把中间态写进缓存；删除是幂等的 | `CacheConsistencyService#evict` |
| 2 | Redis Pub/Sub 广播失效 | L1 私有，Redis 删不到 | `CacheConsistencyService#publish` + `CacheInvalidateListener` |
| 3 | 延迟双删 | 并发读在删缓存后把旧值回填 | `CacheConsistencyService#scheduleDelayedEvict` |
| 4 | TTL 自愈 + 定时对账 | 广播消息丢失（Pub/Sub 不持久化） | `MultiLevelCache#reconcileLocalAgainstRemote` |

另外还单独处理了三类经典故障：

| 故障 | 方案 | 实现位置 |
|---|---|---|
| 缓存穿透 | 缓存空值占位符 + 短 TTL | `CacheNullValue` / `CacheLoader` |
| 缓存击穿 | 分布式锁互斥重建 | `RedisDistributedLock` |
| 缓存雪崩 | L2 TTL 抖动 | `CacheProperties#remoteTtlJitter` |

### 1. 写路径：先更新数据库，再删除缓存

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Product update(Product product) {
    productRepository.save(product);          // ① 先写库
    cacheConsistencyService.evict(CACHE_PRODUCT, product.getId());   // ② 再删缓存
    cacheConsistencyService.evictAll(CACHE_PRODUCT_LIST);
    return product;
}
```

顺序**不能颠倒**。如果先删缓存再写库，并发读线程会在数据库尚未提交时回填旧值，
一致性窗口从「毫秒级」放大到「整个事务时长」。

更进一步，`CacheConsistencyService` 检测到当前处于事务中时，会把失效动作
**推迟到 `afterCommit` 执行**：

```java
private void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
        return;
    }
    action.run();
}
```

原因：若在事务提交前删缓存，并发读线程会查到**尚未提交**的新数据并写回缓存，
提交后缓存里就留下了「读到了未提交数据」的脏值。
推迟到提交后再删，可以彻底消除这个窗口 —— 比单纯依靠延迟双删更彻底。

对应的验证用例是 `CacheConsistencyServiceTest#evictShouldDeferUntilAfterCommitInTransaction`。

### 2. 延迟双删解决的具体竞态

```
时间轴
  T0  写线程：更新数据库（新数据已提交）
  T1  写线程：删除缓存  ──────────────┐
  T2  读线程：缓存未命中 → 查数据库      │  这里读线程拿到的是旧数据吗？
  T3  写线程：数据库已提交，读线程若在提交前查就会拿到旧值    │
  T4  读线程：把旧值写回缓存 ──────────┘  ← 脏数据重新进入缓存
  T5  写线程：延迟 1 秒后二次删除 → 清除 T4 写入的旧值
```

二次删除延迟几百毫秒即可覆盖这个窗口。二次删除同样会广播，
否则其它节点的 L1 里仍可能残留被回填的旧值。

### 3. 广播失效

```java
// 发起方节点：删完 L2 后广播
redisTemplate.convertAndSend("multi-level-cache:invalidate", message);

// 订阅端：每个节点收到后只清自己的 L1
public void onInvalidateMessage(CacheInvalidateMessage message) {
    if (message.getType() == PATTERN) {
        cacheManager.clearLocal(message.getCacheName());
    } else {
        cacheManager.evictLocal(message.getCacheName(), message.getKey());
    }
}
```

两个容易踩的点，代码里都已处理：

- **Pub/Sub 会把消息回环给发布者自己。** 本节点在消息到达前可能正好把旧值回填进 L1，
  所以订阅端收到消息后仍然要再清一次本节点 L1。
- **订阅端只清 L1，不碰 L2。** L2 已经由发起方删除，重复删除没有意义，
  还会和其它节点的写入竞争。

### 4. TTL 自愈与对账（最后一道防线）

Redis Pub/Sub **不持久化**：节点重连期间的消息会永久丢失。
所以本方案保证的是**最终一致**，而不是强一致。

两道兜底：

- **L1 TTL（5 分钟）短于 L2 TTL（30 分钟）**：广播一旦丢失，本节点最多脏 5 分钟。
  反过来配置会导致 L1 长时间无法自愈。
- **定时对账**（默认 60 秒）：遍历 L1 中的 key，凡是 L2 已不存在的就清掉，
  把不一致的收敛时间从 5 分钟压缩到 1 分钟。

```java
// MultiLevelCache#reconcileLocalAgainstRemote
for (Object key : localCache.asMap().keySet()) {
    if (Boolean.FALSE.equals(redisTemplate.hasKey(remoteKey(key)))) {
        localCache.invalidate(key);   // L2 没有 = 陈旧数据
    }
}
```

### 一致性的边界（重要）

这套方案**不适用于**强一致要求的场景：

- 涉及**资金扣减、库存扣减**的业务，必须在数据库层用唯一约束或乐观锁兜底，不能只依赖缓存。
- 上述机制依赖**单 Redis 主库、不发生网络分区**。
- Redis 故障切换（主从切换）期间 Pub/Sub 频道会断开，期间的消息丢失，由 TTL 与对账兜底。

---

## 三、防穿透 / 防击穿 / 防雪崩

### 防穿透：缓存空值占位符

Redis 不能存 `null`，所以查不到数据时写入一个 `CacheNullValue.INSTANCE` 占位对象，
TTL 设得远短于正常缓存（本模块 1 分钟 vs 30 分钟）。

风险：数据被创建后，在占位符过期前会「查不到」。所以 TTL 必须短。
`CacheLoader#tryRead` 负责把占位符还原成真正的 `null`，业务层感知不到。

### 防击穿：分布式锁互斥重建

热点 key 过期瞬间可能有成百上千请求同时回源数据库。抢到锁的线程负责回源并回填，
抢不到的线程短暂等待后再读缓存；**等待后仍未回填，则兜底自己回源**：

```java
if (distributedLock.tryLock(lockKey, token)) {
    // 抢到锁：二次检查，避免等锁期间别人已回填，白跑一趟数据库
    CacheResult<T> second = tryRead(cache, key);
    if (second.isHit()) return second;
    return doLoad(cache, key, fallback);
}
// 没抢到：等 50ms 重读，仍为空则自己回源
Thread.sleep(50L);
if (tryRead(cache, key).isHit()) return ...;
return doLoad(cache, key, fallback);
```

**为什么等待后仍要兜底回源**：此时若直接返回空，会把「数据存在」误判成「数据不存在」，
缓存就改变了查询结果。**语义正确优先于保护数据库** —— 多一次回源只是多一次数据库压力。
代价是防击穿不是绝对的：高并发下仍可能有多个线程同时回源，
真正要压到「只回源一次」需要引入 stale-while-revalidate（读旧值 + 后台异步刷新），
本模块没有实现，属于已知取舍。

加锁用 `SET key token NX EX ttl` 一步完成，避免 `SETNX` 与 `EXPIRE` 分两步时进程崩溃留下永不过期的锁。
解锁用 Lua 脚本原子校验 token，防止「A 的锁超时释放 → B 抢到 → A 恢复后误删 B 的锁」。

### 防雪崩：TTL 抖动

L2 写入时在基准 TTL 的 ±10% 内随机偏移，打散大批 key 的到期时刻。

L1 侧无法做逐 key 抖动 —— Caffeine 2.x 的 `expireAfterWrite` 是「每缓存」级别而非「每条目」级别
（逐条目 `Expiry` 是 Caffeine 3.x 的 API），因此 L1 依赖「短 TTL + 重建互斥锁」抗雪崩。

---

## 四、代码结构

```
com.slm.multilevelcache
├── cache/                         L1 + L2 读写
│   ├── MultiLevelCache            实现 Spring Cache，串起两层
│   ├── MultiLevelCacheManager     缓存实例管理 + L1 广播失效接收端
│   ├── CacheLevel                 层级枚举，用于响应头埋点
│   └── CacheNullValue             空值占位符
├── consistency/                   ★ 缓存一致性，单独成包
│   ├── CacheConsistencyService    写路径失效、广播、延迟双删、对账
│   ├── CacheInvalidateMessage     广播消息体
│   ├── CacheInvalidateListener    订阅端
│   ├── DistributedLock            分布式锁接口
│   ├── RedisDistributedLock       SET NX EX + Lua 解锁
│   └── CacheLoader                三级读路径 + 防穿透/防击穿
├── config/                        RedisTemplate、CacheManager、订阅容器、配置项
├── controller/service/entity/     演示用的商品业务
└── repository/                    内存实现，扮演数据库角色
```

**分层约束**：业务代码不得直接使用 `RedisTemplate`，
一律通过 `CacheManager` / `CacheLoader` / `CacheConsistencyService` 访问缓存。

---

## 五、运行

### 本地运行

```bash
# 1. 启动 Redis
docker run -d --name cache-redis -p 6379:6379 redis:7-alpine

# 2. 启动应用
mvn spring-boot:run
```

验证各级缓存是否生效：

```bash
# 第一次：回源数据库
curl -i http://localhost:8080/cache/product/1001 | grep -i x-cache-level
# X-Cache-Level: DB

# 第二次：命中本地缓存
curl -i http://localhost:8080/cache/product/1001 | grep -i x-cache-level
# X-Cache-Level: L1-Caffeine

# 看 L1 条目数
curl http://localhost:8080/cache/product/cache-stats
```

### 完整链路（Nginx + Redis + 两个应用节点）

```bash
docker compose up -d

# 经过 L0
curl -i http://localhost/cache/product/1001 | grep -i x-cache-status
# X-Cache-Status: MISS   第一次
# X-Cache-Status: HIT    第二次
```

编排里起了**两个应用节点**，这是必须的 ——
只有多节点才能验证广播失效：在 1 号节点更新数据后，2 号节点的 L1 必须靠广播被清理。

### 跨节点一致性验证

```bash
# 直连 1 号节点，读几次让它的 L1 有数据
curl -H "Host: cache-app-1" http://localhost:8080/cache/product/1001
curl -H "Host: cache-app-1" http://localhost:8080/cache/product/1001

# 通过 Nginx 更新（落到 1 号节点），广播失效会清掉 2 号节点的 L1
curl -X PUT http://localhost/cache/product -H "Content-Type: application/json" \
  -d '{"id":1001,"name":"机械键盘","price":599.00,"stock":50}'
```

---

## 六、测试

```bash
mvn test
```

73 个用例，全部为纯单元测试，**不依赖 Redis 与 Docker**——
`FakeRedis` 用一个 `Map` 支撑 `RedisTemplate` 的常用操作，既能验证跨层读写行为，
也能在测试中直接检查 L2 里到底存了什么。

| 测试类 | 覆盖内容 |
|---|---|
| `MultiLevelCacheTest` | L1/L2 读写顺序、L2 命中回填 L1、双层删除、空值短 TTL、Redis 故障降级、TTL 过期、对账清理陈旧数据 |
| `MultiLevelCacheManagerTest` | 缓存实例唯一性、广播失效落点、按前缀隔离、对账统计 |
| `CacheConsistencyServiceTest` | 失效删除 L1+L2 并广播、延迟双删复现并解决竞态、PATTERN 广播、广播失败不影响写操作 |
| `CacheLoaderTest` | 三级读取、防穿透空值、防击穿互斥重建（拿锁后二次检查、抢锁失败后等待重读与兜底回源）、回源异常降级 |
| `RedisDistributedLockTest` | SET NX EX 加锁、Lua 原子解锁、Redis 故障时视为抢锁失败 |
| `CacheInvalidateListenerTest` | 广播消息序列化往返、监听器清理 L1、非法消息不导致订阅线程崩溃 |
| `ProductServiceImplTest` | 读写路径、**先落库再删缓存的顺序契约**、更新/删除的失效范围 |
| `MultiLevelCacheApplicationTests` | Spring 上下文装配，验证各层组件能被正确注入 |

其中 `shouldScheduleSecondEvictWhenDelayedDoubleDeleteEnabled` 是最有价值的一个用例：
它真实复现了「删缓存后并发读把旧值写回」的竞态，并验证延迟双删确实清掉了它。

---

## 七、接入真实数据库

本模块用 `InMemoryProductRepository` 扮演数据库，目的是让缓存链路能独立运行与测试。
接入 MyBatis 时只需：

1. 实现 `ProductRepository` 接口换成 Mapper 版本
2. 缓存层代码**无需任何改动** —— 这正是把一致性逻辑单独抽包的目的