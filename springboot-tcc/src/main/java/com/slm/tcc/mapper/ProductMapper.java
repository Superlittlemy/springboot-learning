package com.slm.tcc.mapper;

import com.slm.tcc.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ProductMapper {

    Optional<Product> getById(Long id);

    Long deductCount(@Param("id") Long id, @Param("num") Long num);

    Long cancelDeduct(@Param("productId") Long productId, @Param("detailId") Long detailId);

}
