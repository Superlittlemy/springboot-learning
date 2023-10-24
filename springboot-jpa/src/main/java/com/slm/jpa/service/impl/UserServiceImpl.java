package com.slm.jpa.service.impl;

import com.slm.jpa.entity.User;
import com.slm.jpa.exception.BizException;
import com.slm.jpa.model.UserCreateRequest;
import com.slm.jpa.model.UserUpdateRequest;
import com.slm.jpa.repository.UserRepository;
import com.slm.jpa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
    }

    @Override
    public Page<User> pageQuery(String name, Boolean deleted, Pageable pageable) {
        return userRepository.findAll(new Specification<User>() {
            @Override
            public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (StringUtils.isNotBlank(name)) {
                    predicateList.add(builder.like(root.get("name"), "%" + name + "%"));
                }
                if (Objects.nonNull(deleted)) {
                    predicateList.add(builder.equal(root.get("deleted"), deleted));
                }
                return query.where(predicateList.toArray(new Predicate[predicateList.size()])).getRestriction();
            }
        }, pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(UserCreateRequest request) {
        User user = User.builder().name(request.getName()).build();
        userRepository.save(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(UserUpdateRequest request) {
        userRepository.findById(request.getId()).ifPresentOrElse(user -> {
            user.setName(request.getName());
            userRepository.save(user);
        }, () -> {
            throw new BizException("用户不存在");
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userRepository.findById(id).ifPresentOrElse(userRepository::delete, () -> {
            throw new BizException("用户不存在");
        });
    }

}
