package com.slm.mybatis.spring.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Blog {

    private Long id;
    private String title;
    private Author author;
    private LocalDateTime createTime;

}
