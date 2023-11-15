package com.slm.caffeine.repository;

import com.slm.caffeine.entity.Dic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DicRepository extends JpaRepository<Dic, String>, JpaSpecificationExecutor<Dic> {
}
