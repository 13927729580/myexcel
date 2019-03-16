/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.myexcel.core;

import com.github.liaochong.myexcel.core.strategy.CellStyleStrategy;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

/**
 * excel构建器
 *
 * @author liaochong
 * @version 1.0
 */
public interface ExcelBuilder {

    /**
     * excel类型
     *
     * @param workbookType workbookType
     * @return ExcelBuilder
     */
    ExcelBuilder workbookType(WorkbookType workbookType);

    /**
     * 设置workbookType为SXSSFWorkbook的内存数据保有量
     *
     * @param rowAccessWindowSize 内存数据保有量
     * @return ExcelBuilder
     */
    ExcelBuilder rowAccessWindowSize(int rowAccessWindowSize);

    /**
     * 使用默认样式
     *
     * @return ExcelBuilder
     */
    @Deprecated
    ExcelBuilder useDefaultStyle();

    /**
     * 样式策略
     *
     * @param cellStyleStrategy 策略
     * @return ExcelFactory
     */
    ExcelBuilder cellStyleStrategy(CellStyleStrategy cellStyleStrategy);

    /**
     * 选择固定区域
     *
     * @param freezePanes 固定区域
     * @return ExcelBuilder
     */
    ExcelBuilder freezePanes(FreezePane... freezePanes);

    /**
     * 设置模板
     *
     * @param path 模板路径
     * @return ExcelBuilder
     */
    ExcelBuilder template(String path);

    /**
     * 构建
     *
     * @param renderData 渲染数据
     * @param <T>        值类型
     * @return Workbook
     */
    <T> Workbook build(Map<String, T> renderData);
}
