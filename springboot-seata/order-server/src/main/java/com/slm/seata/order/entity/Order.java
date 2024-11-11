package com.slm.seata.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
public class Order {

    private Long id;
    private Long merchandiseId;
    private String merchandiseName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal totalPrice;
    private Integer status; // 1.正常、9.关闭
    private LocalDateTime createdTime;

}
