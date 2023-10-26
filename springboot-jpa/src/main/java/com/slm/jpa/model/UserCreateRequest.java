package com.slm.jpa.model;

import com.slm.jpa.entity.Gender;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户姓名不能为空")
    private String name;
    @NotNull(message = "用户性别不能为空")
    private Gender gender;

}
