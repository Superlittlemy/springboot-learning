package com.slm.event.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 价格检查事件
 */
public class PriceCheckEvent extends ApplicationEvent {

    private BigDecimal price;

    public PriceCheckEvent(Object source, BigDecimal price) {
        super(source);
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

}
