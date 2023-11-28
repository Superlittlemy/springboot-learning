package com.slm.easyexcel.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.slm.easyexcel.utils.GenderConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @ExcelProperty("姓名")
    @ColumnWidth(10)
    private String name;
    @ExcelProperty("年龄")
    @ColumnWidth(8)
    private Integer age;
    @ExcelProperty(value = "性别", converter = GenderConvert.class)
    @ColumnWidth(8)
    private Integer gender;
    @ExcelProperty("生日")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(10)
    private Date birthday;
    @ExcelProperty("身高（米）")
    @NumberFormat("##.00")
    @ColumnWidth(15)
    private Double height;
    @ExcelProperty("是否毕业")
    @ColumnWidth(15)
    private Boolean graduated;

}
