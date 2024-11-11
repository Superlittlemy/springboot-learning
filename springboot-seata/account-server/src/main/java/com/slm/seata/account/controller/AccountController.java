package com.slm.seata.account.controller;

import com.slm.seata.account.entity.Account;
import com.slm.seata.account.model.AccountCreate;
import com.slm.seata.account.model.ApiResponse;
import com.slm.seata.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 账户接口
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 创建账户
     *
     * @param accountCreate 创建账户信息
     * @return 账户id
     */
    @PostMapping()
    public ApiResponse<Long> create(@RequestBody AccountCreate accountCreate) {
        Account account = accountService.create(accountCreate);
        return ApiResponse.ok(account.getId());
    }

    /**
     * 查询账户信息
     *
     * @param id 账户id
     * @return 账户信息
     */
    @GetMapping("{id}")
    public ApiResponse<Account> get(@PathVariable Long id) {
        return ApiResponse.ok(accountService.get(id));
    }

    /**
     * 账户充值
     *
     * @param id 账户id
     * @param amount 充值金额
     */
    @PostMapping("{id}/recharge")
    public ApiResponse<?> recharge(@PathVariable Long id, @RequestParam BigDecimal amount) {
        accountService.recharge(id, amount);
        return ApiResponse.ok();
    }

    /**
     * 账户支付
     *
     * @param id 账户id
     * @param amount 支付金额
     */
    @PostMapping("{id}/pay")
    public ApiResponse<?> pay(@PathVariable Long id, @RequestParam BigDecimal amount) {
        accountService.pay(id, amount);
        return ApiResponse.ok();
    }

}
