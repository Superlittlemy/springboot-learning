package com.slm.event.controller;

import com.slm.event.model.Order;
import com.slm.event.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public void buy() {
        log.info("进入下单流程");
        orderService.buy(Order.builder().price(BigDecimal.ONE).build());
    }

}
