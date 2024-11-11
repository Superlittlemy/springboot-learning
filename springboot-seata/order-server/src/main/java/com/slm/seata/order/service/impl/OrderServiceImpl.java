package com.slm.seata.order.service.impl;

import com.slm.seata.order.entity.Order;
import com.slm.seata.order.mapper.OrderMapper;
import com.slm.seata.order.model.client.storage.Merchandise;
import com.slm.seata.order.model.request.CreateOrder;
import com.slm.seata.order.service.OrderService;
import com.slm.seata.order.utils.ApiCenter;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ApiCenter apiCenter;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrder createOrder) {
        // 查询商品
        Merchandise merchandise = apiCenter.getMerchandiseInfo(createOrder.getMerchandiseId());
        // 构建订单
        Order order = this.generateOrder(merchandise, createOrder.getQuantity());
        // 扣减库存
        apiCenter.merchandiseDeduct(createOrder.getMerchandiseId(), createOrder.getQuantity(), createOrder.getBuyerId());
        // 扣减余额
        apiCenter.accountPay(createOrder.getBuyerId(), order.getTotalPrice());
        // 保存订单
        orderMapper.save(order);
        return order;
    }

    private Order generateOrder(Merchandise merchandise, BigDecimal quantity) {
        Order order = new Order();
        order.setMerchandiseId(merchandise.getId());
        order.setMerchandiseName(merchandise.getName());
        order.setUnitPrice(merchandise.getUnitPrice());
        order.setQuantity(quantity);
        order.setTotalPrice(quantity.multiply(merchandise.getUnitPrice()));
        order.setStatus(1);
        order.setCreatedTime(LocalDateTime.now());
        return order;
    }

}
