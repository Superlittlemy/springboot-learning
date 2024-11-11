package com.slm.seata.storage.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售明细
 */
@Data
public class SaleDetail {

    private Long id;
    private Long merchandiseId;
    private String merchandiseName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal totalPrice;
    private Long accountId; // 下单客户
    private Integer status; // 状态：0.待确认、1.正常、9.撤销
    private String requestId;
    private LocalDateTime createdTime;

}
