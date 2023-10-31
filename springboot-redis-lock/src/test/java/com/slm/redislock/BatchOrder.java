package com.slm.redislock;

import com.slm.redislock.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@Slf4j
@SpringBootTest
public class BatchOrder {

    @Autowired
    private OrderService orderService;

    @Test
    void batchOrder() {
        new Thread(() -> {
            while (true) {
                try {
                    orderService.createOrder(new BigDecimal("10"), 100);
                    Thread.sleep(300);
                } catch (Exception e) {
                    log.error("想要10个商品，余额不足");
                    break;
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                try {
                    orderService.createOrder(new BigDecimal("20"), 100);
                    Thread.sleep(300);
                } catch (Exception e) {
                    log.error("想要20个商品，余额不足");
                    break;
                }
            }
        }).start();
        while (true) {}
    }

}
