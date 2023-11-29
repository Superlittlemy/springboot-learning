package com.slm.easypoi.model;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Excel(name = "姓名", width = 10)
    private String name;
    @Excel(name = "年龄", width = 8)
    private Integer age;
    @Excel(name = "性别", width = 8, replace = { "_null", "男_1", "女_0" }, suffix = "生")
    private Integer gender;
    @Excel(name = "生日", format = "yyyy-MM-dd", width = 20)
    private Date birthday;
    @Excel(name = "身高（米）", width = 15)
    private Double height;
    @Excel(name = "是否毕业", width = 15)
    private Boolean graduated;

}
