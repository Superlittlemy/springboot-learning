package com.slm.caffeine.repository;

import com.slm.caffeine.entity.DicItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DicItemRepository extends JpaRepository<DicItem, String> {

    List<DicItem> findAllByDicId(String dicId);

}
