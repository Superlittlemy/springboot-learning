package com.slm.caffeine.service;

import com.slm.caffeine.entity.Dic;
import com.slm.caffeine.entity.DicItem;
import com.slm.caffeine.model.DicCreate;
import com.slm.caffeine.model.DicItemCreate;
import com.slm.caffeine.repository.DicItemRepository;
import com.slm.caffeine.repository.DicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DicService {

    private final DicRepository dicRepository;
    private final DicItemRepository dicItemRepository;

    public Page<Dic> list(Integer status, Pageable pageable) {
        return dicRepository.findAll((root, query, cb) -> {
            //用于添加所有查询条件
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        }, pageable);
    }

    @Cacheable(value = "dicItems", key = "#id")
    public List<DicItem> items(String id) {
        return dicItemRepository.findAllByDicId(id);
    }

    public void addDic(DicCreate dicCreate) {
        dicRepository.save(Dic.builder()
                .code(dicCreate.getCode())
                .name(dicCreate.getName())
                .status(1)
                .build());
    }

    @CacheEvict(value = "dicItems", key = "#dicId")
    public void addDicItem(String dicId, DicItemCreate dicItemCreate) {
        dicItemRepository.save(DicItem.builder()
                .dicId(dicId)
                .code(dicItemCreate.getCode())
                .name(dicItemCreate.getName())
                .build());
    }

}
