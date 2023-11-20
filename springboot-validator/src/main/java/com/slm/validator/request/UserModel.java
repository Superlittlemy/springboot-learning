package com.slm.validator.request;

import com.slm.validator.request.customer.MobileCheck;
import com.slm.validator.request.group.UserCreate;
import com.slm.validator.request.group.UserModify;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;

@Data
public class UserModel {

    @NotBlank(message = "用户id不能为空", groups = { UserModify.class })
    private String id;
    @NotBlank(message = "姓名不能为空", groups = { UserCreate.class, UserModify.class })
    private String name;
    @Length(min = 18, message = "二代身份证必须为18位", groups = { UserCreate.class, UserModify.class })
    private String idNo;
    @MobileCheck(groups = { UserCreate.class, UserModify.class })
    private String mobile;

    private String address;

}
