# springboot-easyexcel

Spring Boot Excel - EasyExcel版

**应对 Excel 导入导出场景**

EasyExcel是一个基于Java的、快速、简洁、解决大文件内存溢出的Excel处理工具。他能让你在不用考虑性能、内存的等因素的情况下，快速完成Excel的读、写等功能。

> 官网地址：https://easyexcel.opensource.alibaba.com

**快速开始**

引入依赖

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.2</version>
</dependency>
```

创建导出 Excel 行实体

```java
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
```

以上代码中的注解解释

- @ExcelProperty：核心注解，value 属性用来设置表头名称，converter 属性用来设置类型转换器
- @ColumnWidth：用于设置表格列的宽度
- @DateTimeFormat：用于设置日期转换格式
- @NumberFormat：用于设置数字转换格式

自定义类型转换器

```java
/**
 * 性别类型转换器
 */
public class GenderConvert implements Converter<Integer> {


    @Override
    public Class<?> supportJavaTypeKey() {
        return Converter.super.supportJavaTypeKey();
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return Converter.super.supportExcelTypeKey();
    }

    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        return StringUtils.isEmpty(cellData.getStringValue()) ? null : ("男".equals(cellData.getStringValue()) ? 1 : 0);
    }

    @Override
    public WriteCellData<?> convertToExcelData(Integer value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        return new WriteCellData<>(Objects.isNull(value) ? "" : (value == 1 ? "男" : "女"));
    }

}
```

EasyExcel 工具类集成

```java
public class ExcelUtil {

    public static <T> void export(OutputStream os, Class<T> rowType, String SheetName, List<T> data) {
        EasyExcel.write(os).head(rowType).excelType(ExcelTypeEnum.XLSX).sheet(SheetName).doWrite(data);
    }

    public static <T> List<T> read(MultipartFile file, Class<T> rowType) throws IOException {
        return EasyExcel.read(file.getInputStream()).head(rowType).sheet().doReadSync();
    }

}
```

通过控制层接收接口，服务层预处理数据，进入 Excel 工具类执行实际操作

示例：

**导出**

![](D:\workspace\practice-master\springboot-master\code\springboot-easyexcel\Snipaste_2023-11-28_10-56-29.png)

**导入**

![](D:\workspace\practice-master\springboot-master\code\springboot-easyexcel\Snipaste_2023-11-28_10-57-02.png)

简单的导入导出功能就实现啦！