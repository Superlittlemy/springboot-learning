package com.slm.seata.order.controller;

import com.slm.seata.order.entity.Order;
import com.slm.seata.order.model.ApiResponse;
import com.slm.seata.order.model.request.CreateOrder;
import com.slm.seata.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param createOrder 创建订单参数
     * @return 订单id
     */
    @PostMapping()
    public ApiResponse<Long> createOrder(@RequestBody CreateOrder createOrder) {
        Order order = orderService.createOrder(createOrder);
        return ApiResponse.ok(order.getId());
    }

}
