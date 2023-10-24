package com.slm.jpa.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserUpdateRequest {

    @NotNull(message = "用户id不能为空")
    private Long id;
    @NotBlank(message = "用户姓名不能为空")
    private String name;

}
