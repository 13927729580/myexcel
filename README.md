# html2excel 文档
[![Build Status](https://travis-ci.org/liaochong/html2excel.svg?branch=master)](https://travis-ci.org/liaochong/html2excel)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.liaochong/html2excel/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.liaochong/html2excel)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

html2excel是一款将html中表格（table）转化成excel的工具包.

优点 | Advantages
------------------
- **零学习成本**：使用html作为模板，学习成本几乎为零；
- **可生成任意复杂表格**：本工具使用迭代单元格方式进行excel绘制，可生成任意复杂度excel；
- **屏蔽POI操作**：不直接操作过重的POI；
- **支持常用背景色、边框、字体等样式设置**：具体参见详情部分；
- **支持.XLS、.XLSX**：支持生成.xls、.xlsx后缀的excel；
- **支持多种模板引擎**：支持Freemarker、Groovy、Beetl等常用模板引擎，默认内置Beetl模板引擎（推荐引擎，[Beetl文档](http://ibeetl.com/guide/#beetl)）；
- **提供默认模板引擎，直接输出简单excel**：无需编写任何html，已内置默认模板，可直接根据POJO数据列表输出；
- **支持一次生成多sheet**：以table作为sheet单元，支持一份excel文档中多sheet；

文档 | Document
--------------
https://github.com/liaochong/html2excel/wiki

