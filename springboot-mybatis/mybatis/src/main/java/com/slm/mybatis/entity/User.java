package com.slm.mybatis.entity;

import com.slm.mybatis.model.Type;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class User implements Serializable {

    private Long id;
    private String name;
    private LocalDate birthday;
    private Type type;

}