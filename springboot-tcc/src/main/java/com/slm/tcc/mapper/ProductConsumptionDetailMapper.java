package com.slm.tcc.mapper;

import com.slm.tcc.entity.ProductConsumptionDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface ProductConsumptionDetailMapper {

    Optional<ProductConsumptionDetail> getByOrderId(Long orderId);

    Long create(ProductConsumptionDetail productConsumptionDetail);

    void confirm(Long id);

    void cancel(Long id);

}
