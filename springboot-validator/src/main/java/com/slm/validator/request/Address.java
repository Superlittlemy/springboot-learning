package com.slm.validator.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class Address {

    @NotBlank(message = "地址不能为空")
    private String value;

}
