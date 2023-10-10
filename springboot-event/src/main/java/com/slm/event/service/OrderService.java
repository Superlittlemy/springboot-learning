package com.slm.event.service;

import com.slm.event.event.EmailEvent;
import com.slm.event.event.PriceCheckEvent;
import com.slm.event.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationContext applicationContext;

    public void buy(Order order) {
        long start = System.currentTimeMillis();
        // 1. 检查价格
        applicationContext.publishEvent(new PriceCheckEvent(this, order.getPrice()));
        // 2. 创建订单
        // 3. 发送邮件
        applicationContext.publishEvent(new EmailEvent("采购了新鲜蔬菜100公斤"));
        long end = System.currentTimeMillis();
        log.info("下单完成，总耗时：" + (end - start));
    }

}
