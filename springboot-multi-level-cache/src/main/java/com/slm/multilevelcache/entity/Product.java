package com.slm.multilevelcache.entity;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品
 *
 * <p>演示用的读多写少对象：适合被多级缓存承载。</p>
 */
@Data
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格不能为负")
    private java.math.BigDecimal price;

    private Integer stock;

    /**
     * 版本号：用于演示并发更新下的乐观锁
     */
    private Integer version;

    private LocalDateTime updatedTime;

}