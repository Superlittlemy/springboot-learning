package com.slm.event.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Order {

    private BigDecimal price;

    // other attribute

}
