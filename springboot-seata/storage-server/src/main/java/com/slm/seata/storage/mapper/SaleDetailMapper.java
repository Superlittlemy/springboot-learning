package com.slm.seata.storage.mapper;

import com.slm.seata.storage.entity.SaleDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SaleDetailMapper {

    void save(SaleDetail saleDetail);

    Long confirm(String requestId);

    Long cancel(String requestId);

}
