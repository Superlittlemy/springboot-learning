package com.slm.seata.account.service.impl;

import com.slm.seata.account.entity.Account;
import com.slm.seata.account.entity.ExpenseDetail;
import com.slm.seata.account.mapper.AccountMapper;
import com.slm.seata.account.mapper.ExpenseDetailMapper;
import com.slm.seata.account.model.AccountCreate;
import com.slm.seata.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final ExpenseDetailMapper expenseDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Account create(AccountCreate accountCreate) {
        Account account = new Account();
        account.setUsername(accountCreate.getUsername());
        account.setBalance(BigDecimal.ZERO);
        accountMapper.save(account);
        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accountMapper.getById(id).orElseThrow(() -> new RuntimeException("账户不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long id, BigDecimal amount) {
        Account account = this.get(id);
        accountMapper.updateBalance(id, account.getBalance().add(amount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, BigDecimal amount) {
        Account account = this.get(id);
        // 扣减余额
        Long m = accountMapper.pay(id, amount);
        if (m == 0) {
            throw new RuntimeException("账户[" + account.getUsername() + "]余额不足");
        }
        // 保存支付明细
        this.saveExpenseDetail(id, amount);
    }

    private ExpenseDetail saveExpenseDetail(Long accountId, BigDecimal amount) {
        ExpenseDetail expenseDetail = new ExpenseDetail();
        expenseDetail.setAccountId(accountId);
        expenseDetail.setAmount(amount);
        expenseDetail.setCreatedTime(LocalDateTime.now());
        expenseDetailMapper.save(expenseDetail);
        return expenseDetail;
    }

}
