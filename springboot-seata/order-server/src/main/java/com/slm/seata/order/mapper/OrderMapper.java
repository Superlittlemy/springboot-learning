package com.slm.seata.order.mapper;

import com.slm.seata.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {

    void save(Order order);

}
