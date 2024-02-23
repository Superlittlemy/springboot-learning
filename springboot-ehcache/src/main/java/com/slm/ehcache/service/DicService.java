package com.slm.ehcache.service;

import com.slm.ehcache.entity.Dic;
import com.slm.ehcache.entity.DicItem;
import com.slm.ehcache.exception.BizException;
import com.slm.ehcache.model.ConvertDTO;
import com.slm.ehcache.model.DicCreate;
import com.slm.ehcache.model.DicItemCreate;
import com.slm.ehcache.repository.DicItemRepository;
import com.slm.ehcache.repository.DicRepository;
import lombok.RequiredArgsConstructor;
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

    public void deleteDic(String id) {
        dicRepository.deleteById(id);
    }

    public Dic get(String id) {
        return dicRepository.findById(id).orElse(null);
    }

    public void addDicItem(String dicId, DicItemCreate dicItemCreate) {
        dicItemRepository.save(DicItem.builder()
                .dicId(dicId)
                .code(dicItemCreate.getCode())
                .name(dicItemCreate.getName())
                .build());
    }

    public void deleteDicItem(String id) {
        dicItemRepository.deleteById(id);
    }

    public String convert(String dicCode, String itemCOde) {
        Dic dic = dicRepository.getByCode(dicCode).orElseThrow(() -> new BizException("字典不存在"));
        return dicItemRepository.getByDicIdAndCode(dic.getId(), itemCOde).map(DicItem::getName).orElse(null);
    }

    public List<ConvertDTO> convertAll(List<ConvertDTO> convertDTOS) {
        convertDTOS.forEach(dto -> {
            dicRepository.getByCode(dto.getDicCode())
                    .flatMap(dic -> dicItemRepository.getByDicIdAndCode(dic.getId(), dto.getItemCode()))
                    .ifPresent(dicItem -> {
                        dto.setConvertName(dicItem.getName());
                    });
        });
        return convertDTOS;
    }

}
