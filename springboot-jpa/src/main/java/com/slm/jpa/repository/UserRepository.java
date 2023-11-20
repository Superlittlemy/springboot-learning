package com.slm.jpa.repository;

import com.slm.jpa.entity.User;
import com.slm.jpa.model.UserVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {

    Optional<UserVO> getById(Long id);

}
