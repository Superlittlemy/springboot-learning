package com.slm.seata.storage.mapper;

import com.slm.seata.storage.entity.Merchandise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper
public interface MerchandiseMapper {

    void save(Merchandise merchandise);

    Optional<Merchandise> getById(Long id);

    Long deduct(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    Long cancel(String requestId);

}
