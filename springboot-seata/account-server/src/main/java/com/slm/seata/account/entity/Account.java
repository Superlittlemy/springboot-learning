package com.slm.seata.account.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户
 */
@Data
public class Account {

    private Long id;
    private String username;
    private BigDecimal balance;

}
