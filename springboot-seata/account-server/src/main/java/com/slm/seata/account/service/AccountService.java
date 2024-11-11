package com.slm.seata.account.service;

import com.slm.seata.account.entity.Account;
import com.slm.seata.account.model.AccountCreate;

import java.math.BigDecimal;

/**
 * 账户服务
 */
public interface AccountService {

    /**
     * 创建账户
     *
     * @param accountCreate 账户创建信息
     * @return 完整账户信息
     */
    Account create(AccountCreate accountCreate);

    /**
     * 获取账户信息
     *
     * @param id 账户id
     * @return 账户信息
     */
    Account get(Long id);

    /**
     * 账户充值
     *
     * @param id 账户id
     * @param amount 重置金额
     */
    void recharge(Long id, BigDecimal amount);

    /**
     * 账户支付
     *
     * @param id 账户id
     * @param amount 支付金额
     */
    void pay(Long id, BigDecimal amount);

}
