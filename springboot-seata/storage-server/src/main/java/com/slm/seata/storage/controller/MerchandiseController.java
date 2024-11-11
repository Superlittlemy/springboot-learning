package com.slm.seata.storage.controller;

import com.slm.seata.storage.entity.Merchandise;
import com.slm.seata.storage.model.ApiResponse;
import com.slm.seata.storage.model.MerchandiseCreate;
import com.slm.seata.storage.service.MerchandiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/merchandises")
@RequiredArgsConstructor
public class MerchandiseController {

    private final MerchandiseService merchandiseService;

    /**
     * 创建商品
     *
     * @param merchandiseCreate 商品创建信息
     * @return 商品id
     */
    @PostMapping()
    public ApiResponse<Long> create(@RequestBody MerchandiseCreate merchandiseCreate) {
        Merchandise merchandise = merchandiseService.create(merchandiseCreate);
        return ApiResponse.ok(merchandise.getId());
    }

    /**
     * 获取商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    @GetMapping("{id}")
    public ApiResponse<Merchandise> get(@PathVariable Long id) {
        return ApiResponse.ok(merchandiseService.get(id));
    }

    /**
     * 扣减库存
     *
     * @param id 商品id
     * @param quantity 扣减数量
     */
    @PutMapping("{id}/deduct")
    public ApiResponse<?> deduct(@PathVariable Long id, @RequestParam BigDecimal quantity, @RequestParam Long buyerId) {
        merchandiseService.deduct(UUID.randomUUID().toString(), id, quantity, buyerId);
        return ApiResponse.ok();
    }

}
