package com.slm.caffeine.controller;

import com.slm.caffeine.entity.Dic;
import com.slm.caffeine.entity.DicItem;
import com.slm.caffeine.model.ApiResponse;
import com.slm.caffeine.model.DicCreate;
import com.slm.caffeine.model.DicItemCreate;
import com.slm.caffeine.service.DicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dic")
@RequiredArgsConstructor
public class DicController {

    private final DicService dicService;

    @GetMapping("list")
    public ApiResponse<Page<Dic>> list(@RequestParam(required = false) Integer status,
                                       Pageable pageable) {
        return ApiResponse.ok(dicService.list(status, pageable));
    }

    @GetMapping("{id}/item")
    public ApiResponse<List<DicItem>> items(@PathVariable String id) {
        return ApiResponse.ok(dicService.items(id));
    }

    @PostMapping()
    public ApiResponse<?> addDic(@RequestBody DicCreate dicCreate) {
        dicService.addDic(dicCreate);
        return ApiResponse.ok();
    }

    @PostMapping("{id}/item")
    public ApiResponse<?> addDicItem(@PathVariable String id, @RequestBody DicItemCreate dicItemCreate) {
        dicService.addDicItem(id, dicItemCreate);
        return ApiResponse.ok();
    }

}
