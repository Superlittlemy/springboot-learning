package com.slm.tcc.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
public class TccOrder {

    private Long id;
    private Long productId;
    private String productName;
    private Long productNum;
    private BigDecimal productPrice;
    private BigDecimal orderAmount;
    private int orderStatus; // 订单状态：0.待确认、1.正常、9.关闭
    private LocalDateTime orderTime;

}
