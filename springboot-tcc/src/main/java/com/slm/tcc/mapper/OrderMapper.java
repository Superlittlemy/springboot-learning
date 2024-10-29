package com.slm.tcc.mapper;

import com.slm.tcc.entity.TccOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    Long create(TccOrder order);

    int confirm(Long id);

    void cancel(Long id);

    List<TccOrder> getAutocloseOrder();

}
