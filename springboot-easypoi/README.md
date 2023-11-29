# springboot-easypoi

Spring Boot Excel - EasyPoi版

EasyPoi 功能如同名字 easy，主打的功能就是容易，让一个没见接触过 poi 的人员，就可以方便的写出 Excel 导出，Excel 模板导出，Excel 导入，Word 模板导出，通过简单的注解和模板语言（熟悉的表达式语法），完成以前复杂的写法。

特点：

- 基于注解的导入导出,修改注解就可以修改 Excel
- 支持常用的样式自定义
- 基于 map 可以灵活定义的表头字段
- 支持一堆多的导出导入
- 支持模板的导出，一些常见的标签,自定义标签
- 支持 HTML/Excel 转换，如果模板还不能满足用户的变态需求，请用这个功能
- 支持word的导出，支持图片,Excel

**快速开始**

1）引入依赖

```xml
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

2）定义模型

```java
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
```

注解介绍：

@Excel：核心注解，如果需求简单只使用这一个注解也是可以的，涵盖了常用的Excel需求

| 属性           | 默认值 | 功能                                    |
| -------------- | ------ | --------------------------------------- |
| name           | null   | 列表                                    |
| width          | 10     | 列宽                                    |
| replace        | {}     | 值替换，格式为：{ "newValue_oldValue" } |
| suffix         | ""     | 文字后缀                                |
| format         | ""     | 时间格式                                |
| isColumnHidden | false  | 是否隐藏列                              |

**实现基本的导入导出**

```java
public class ExcelUtil {

    // 导出
    public static <T> void export(OutputStream os, Class<T> rowType, String sheetName, List<T> data) throws IOException {
        ExportParams exportParams = new ExportParams();
        exportParams.setSheetName(sheetName);
        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, rowType, data);
        workbook.write(os);
        os.close();
    }

    // 导入
    public static <T> List<T> read(InputStream is, Class<T> rowType) throws Exception {
        return ExcelImportUtil.importExcel(is, rowType, new ImportParams());
    }

}
```

