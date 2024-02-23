package com.slm.ehcache.repository;

import com.slm.ehcache.entity.Dic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DicRepository extends JpaRepository<Dic, String>, JpaSpecificationExecutor<Dic> {


    Optional<Dic> getByCode(String code);
}
