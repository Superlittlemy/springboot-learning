package com.slm.tcc.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品消费明细
 */
@Data
public class ProductConsumptionDetail {

    private Long id;
    private Long productId; // 商品id
    private Long num; // 消费数量
    private Long orderId; // 关联订单id
    private int status; // 状态：0.待确认、1.正常、2.撤销
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

}
