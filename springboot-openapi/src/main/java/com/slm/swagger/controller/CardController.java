package com.slm.swagger.controller;

import com.slm.swagger.model.ApiResponse;
import com.slm.swagger.model.Card;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/cards")
@Tag(name = "卡接口")
public class CardController {

    @GetMapping()
    @Operation(summary = "查询卡")
    public ApiResponse<List<Card>> query() {
        return ApiResponse.ok(List.of(Card.builder().id(1L).type("储值卡").amount(BigDecimal.ZERO).build()));
    }

    @GetMapping("{id}")
    @Operation(summary = "获取卡信息")
    public ApiResponse<Card> get(@PathVariable Integer id) {
        return ApiResponse.ok(Card.builder().id(1L).type("储值卡").amount(BigDecimal.ZERO).build());
    }

    @PostMapping
    @Operation(summary = "新建卡")
    public ApiResponse createCard(@RequestBody Card card) {
        return ApiResponse.ok();
    }

    @PutMapping
    @Operation(summary = "修改卡")
    public ApiResponse modifyCard(@RequestBody Card card) {
        return ApiResponse.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除卡")
    public ApiResponse deleteCard(@PathVariable Long id) {
        return ApiResponse.ok();
    }

}
