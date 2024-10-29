package com.slm.tcc.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品
 */
@Data
public class Product {

    private Long id;
    private String productName;
    private BigDecimal price;
    private Long num;

}
