package com.slm.mybatisplus.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户姓名不能为空")
    private String name;

}
