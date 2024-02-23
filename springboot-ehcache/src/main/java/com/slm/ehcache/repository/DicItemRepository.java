package com.slm.ehcache.repository;

import com.slm.ehcache.entity.DicItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DicItemRepository extends JpaRepository<DicItem, String> {

    List<DicItem> findAllByDicId(String dicId);

    Optional<DicItem> getByDicIdAndCode(String id, String code);

}
