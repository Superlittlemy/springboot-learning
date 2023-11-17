package com.slm.ehcache.controller;

import com.slm.ehcache.entity.Dic;
import com.slm.ehcache.entity.DicItem;
import com.slm.ehcache.model.ApiResponse;
import com.slm.ehcache.model.DicCreate;
import com.slm.ehcache.model.DicItemCreate;
import com.slm.ehcache.service.DicService;
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

    @GetMapping("{id}")
    public ApiResponse<Dic> get(@PathVariable String id) {
        return ApiResponse.ok(dicService.get(id));
    }

    @PostMapping("{id}/item")
    public ApiResponse<?> addDicItem(@PathVariable String id, @RequestBody DicItemCreate dicItemCreate) {
        dicService.addDicItem(id, dicItemCreate);
        return ApiResponse.ok();
    }

}
