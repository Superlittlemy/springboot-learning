package com.slm.seata.account.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用明细
 */
@Data
public class ExpenseDetail {

    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private LocalDateTime createdTime;

}
