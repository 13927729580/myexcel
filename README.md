# html2excel 文档
[![Build Status](https://travis-ci.org/liaochong/html2excel.svg?branch=master)](https://travis-ci.org/liaochong/html2excel)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.liaochong/html2excel/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.liaochong/html2excel)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

html2excel是一款将html中表格（table）转化成excel的工具.

版本支持 | Support Version
------------------

- All version - only support for Java 8+

优点 | Advantages
------------------

- **零学习成本**：使用html作为模板，学习成本几乎为零；
- **屏蔽POI操作**：不直接操作过重的POI；
- **可生成任意复杂表格**：本工具使用迭代单元格方式进行excel绘制，可生成任意复杂度excel；
- **支持常用背景色、边框、字体等样式设置：具体参见详情部分；
- **支持.XLS、.XLSX**：支持生成.xls、.xlsx后缀的excel；
- **支持多种模板引擎**：支持Freemarker、Groovy、Beetl等常用模板引擎；

注意 | Attention
------------------
目前只支持模板文件存放在classpath下

Maven 依赖
------------------
```xml
<dependency>
    <groupId>com.github.liaochong</groupId>
    <artifactId>html2excel</artifactId>
    <version>0.0.1</version>
</dependency>
```
可选模板 | Template
------------------
1. 以下模板引擎默认均未被引入，使用者可根据自身需要选择在pom.xml中声明引入；
2. 以下模板引擎版本为最低版本号；

```xml
<dependency>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
    <version>2.3.23</version>
</dependency>

<dependency>
    <groupId>com.ibeetl</groupId>
    <artifactId>beetl</artifactId>
    <version>2.7.23</version>
</dependency>

<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy-templates</artifactId>
    <version>2.4.13</version>
</dependency>
```
示例 | Example
------------------
1. 已存在html文件，使用这种方式时，html文件不局限于放在项目的classpath下
```java
// get html file
File htmlFile = new File("/Users/liaochong/Downloads/example.html");
// read the html file and use default excel style to create excel
Workbook workbook = HtmlToExcelFactory.readHtml(htmlFile).useDefaultStyle().build();
// this is a example,you can write the workbook to any valid outputstream
OutputStream writer = new FileOutputStream(new File("/Users/liaochong/Downloads/excel.xlsx"));
workbook.write(writer);
```
2. 使用freemarker等模板引擎，具体请参照项目中的example
```java
@RestController
public class FreemarkerExampleController {

    @GetMapping("/freemarker/build")
    public void build(HttpServletResponse response) {
        Map<String, Object> data = getData();

        ExcelBuilder excelBuilder = new FreemarkerExcelBuilder();
        Workbook workbook = excelBuilder.getTemplate("/templates/freemarker_template.ftl").useDefaultStyle().build(data);

        response.setCharacterEncoding(CharEncoding.UTF_8);
        response.addHeader("Content-Disposition", "attachment;filename=" + new String("freemarker_excel.xlsx".getBytes()));
        try {
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> getData() {
        Map<String, Object> data = new HashMap<>();
        for (int i = 1; i <= 11; i++) {
            data.put("n_"+String.valueOf(i), i);
        }
        return data;
    }
}
```
扩展 | Extend
------------------
如需使用非Freemarker、Groovy、Beetl模板引擎，可自行扩展，扩展规则如下：
1. 继承 `ExcelBuilder` 抽象类；
2. 实现抽象方法  `ExcelBuilder getTemplate(String path)`、`Workbook build(Map<String, Object> data)`；
