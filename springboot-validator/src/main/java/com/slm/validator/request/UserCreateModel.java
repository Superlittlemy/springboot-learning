package com.slm.validator.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;

@Data
public class UserCreateModel {

    @NotBlank(message = "姓名不能为空")
    private String name;
    @Length(min = 18, message = "二代身份证必须为18位")
    private String idNo;

}
