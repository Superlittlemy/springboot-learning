package com.slm.seata.account.mapper;

import com.slm.seata.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper
public interface AccountMapper {

    void save(Account account);

    Optional<Account> getById(Long id);

    Long updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);

    Long pay(@Param("id") Long id, @Param("amount") BigDecimal amount);
    
}
