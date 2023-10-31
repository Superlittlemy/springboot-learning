package com.slm.redislock.service;

import com.slm.redislock.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final static String ORDER_LOCK = "order_lock";
    private AtomicReference<BigDecimal> amount = new AtomicReference<>(new BigDecimal("200"));
    private final RedisUtil redisUtil;

    @SneakyThrows
    public void createOrder(BigDecimal a, int executeTime) {
        // 请求ID
        String requestId = UUID.randomUUID().toString();
        // 尝试获取锁
        int time = 0;
        while (!redisUtil.tryLock(ORDER_LOCK, requestId, 10) && time < 50) {
            Thread.sleep(100);
            time++;
        }
        if (time > 49) {
            log.error("订单超时");
            return;
        }
        log.info(requestId + ": get lock");
        // 业务执行
        if (a.compareTo(amount.get()) > 0) {
            // unlock
            redisUtil.unlock(ORDER_LOCK, requestId);
            throw new RuntimeException("余额不足");
        }
        amount.set(amount.get().subtract(a));
        Thread.sleep(executeTime);
        log.info(requestId + ": created order, sale " + a + " remain: " + amount.get());
        // 解锁
        redisUtil.unlock(ORDER_LOCK, requestId);
    }

}
