package com.slm.tcc;

import com.slm.tcc.entity.TccOrder;
import com.slm.tcc.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class MockShopping {

    private int userNum = 5;
    @Autowired
    private OrderService orderService;
    private ExecutorService executorService = Executors.newFixedThreadPool(userNum);
    private Object lock = new Object();
    private CountDownLatch countDownLatch = new CountDownLatch(userNum);

    @Test
    public void purchasing() {
        TccOrder order = orderService.createOrder(1L, 1L);
        log.info("创建订单{}", order.getId());
    }

    /**
     * 模拟多人下单
     *
     * @throws InterruptedException
     */
    @Test
    public void MultiPartyPurchasing() throws InterruptedException {
        for (int i = 0; i < userNum; i++) {
            executorService.submit(() -> {
                try {
                    synchronized (lock) {
                        countDownLatch.countDown();
                        lock.wait();
                    }
                    orderService.createOrder(1L, 1L);
                } catch (InterruptedException e) {
                    log.error("创建订单异常{}", e.getMessage());
                }
            });
        }
        countDownLatch.await();
        synchronized (lock) {
            lock.notifyAll();
        }
        while (true) {}
    }

    @Test
    public void mockAutoclose() {
        orderService.autoclose();
    }

}
