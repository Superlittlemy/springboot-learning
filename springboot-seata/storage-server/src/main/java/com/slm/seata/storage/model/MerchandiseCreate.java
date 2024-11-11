package com.slm.seata.storage.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchandiseCreate {

    private String name;
    private BigDecimal unitPrice;
    private BigDecimal quantity;

}
