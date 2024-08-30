package com.slm.mybatis.entity;

import com.slm.mybatis.model.AppConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class App implements Serializable {

    private int id;
    private String name;
    private User owner; // 管理员
    private List<User> users; // 用户
    private AppConfig config; // 配置

}
