package com.slm.seata.account.mapper;

import com.slm.seata.account.entity.ExpenseDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExpenseDetailMapper {

    void save(ExpenseDetail expenseDetail);

}
