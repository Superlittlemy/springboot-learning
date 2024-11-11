package com.slm.seata.storage.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品
 */
@Data
public class Merchandise {

    private Long id;
    private String name;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private Integer status; // 状态：0.未上架、1.正常销售、9.售罄

}
