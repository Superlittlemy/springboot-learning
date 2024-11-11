package com.slm.seata.order.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrder {

    private Long buyerId;
    private Long merchandiseId;
    private BigDecimal quantity;

}
