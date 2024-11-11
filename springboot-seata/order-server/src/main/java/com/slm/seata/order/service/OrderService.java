package com.slm.seata.order.service;

import com.slm.seata.order.entity.Order;
import com.slm.seata.order.model.request.CreateOrder;

/**
 * 订单服务
 */
public interface OrderService {

    Order createOrder(CreateOrder createOrder);

}
