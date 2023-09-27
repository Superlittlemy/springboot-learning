package com.slm.restapi.controller;

import com.slm.restapi.model.ApiResponse;
import com.slm.restapi.model.Order;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 RESTful 接口
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    /**
     * 查询订单列表
     *
     * @return
     */
    @GetMapping("list")
    public ApiResponse<Order> queryOrderList() {
        return ApiResponse.ok(List.of(Order.builder().build()));
    }

    /**
     * 获取订单详情
     *
     * @param id 订单id
     * @return
     */
    @GetMapping("{id}")
    public ApiResponse<Order> getOrderDetails(@PathVariable Integer id) {
        return ApiResponse.ok(Order.builder().build());
    }

    /**
     * 创建订单
     *
     * @param order 订单信息
     * @return
     */
    @PostMapping
    public ApiResponse createOrder(Order order) {
        // order data persistence
        return ApiResponse.ok();
    }

    /**
     * 更新订单
     *
     * @param order 订单信息
     * @return
     */
    @PutMapping
    public ApiResponse updateOrder(Order order) {
        // order data update
        return ApiResponse.ok();
    }

    /**
     * 删除订单
     *
     * @param id 订单id
     * @return
     */
    @DeleteMapping("{id}")
    public ApiResponse deleteOrder(@PathVariable Integer id) {
        // order data delete
        return ApiResponse.ok();
    }

}
