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
package com.github.liaochong.html2excel.core;

import com.github.liaochong.html2excel.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 默认excel创建者
 *
 * @author liaochong
 * @version 1.0
 */
@Slf4j
public class DefaultExcelBuilder {

    private static final String DEFAULT_TEMPLATE_PATH = "/template/beetl/defaultExcelBuilderTemplate.html";

    private ExcelBuilder excelBuilder;
    /**
     * 标题
     */
    private List<String> titles;
    /**
     * sheetName
     */
    private String sheetName;
    /**
     * 字段展示顺序
     */
    private List<String> fieldDisplayOrder;

    private DefaultExcelBuilder() {
        this.excelBuilder = new BeetlExcelBuilder();
    }

    public static DefaultExcelBuilder getInstance() {
        return new DefaultExcelBuilder();
    }

    public DefaultExcelBuilder titles(List<String> titles) {
        this.titles = titles;
        return this;
    }

    public DefaultExcelBuilder sheetName(String sheetName) {
        this.sheetName = Objects.isNull(sheetName) ? "sheet" : sheetName;
        return this;
    }

    public DefaultExcelBuilder fieldDisplayOrder(List<String> fieldDisplayOrder) {
        this.fieldDisplayOrder = fieldDisplayOrder;
        return this;
    }

    public Workbook build(List<?> data) {
        if (Objects.isNull(fieldDisplayOrder) || fieldDisplayOrder.isEmpty()) {
            throw new IllegalArgumentException("FieldDisplayOrder is necessary");
        }
        this.selfAdaption();
        // 设置标题
        Map<String, Object> renderData = new HashMap<>();
        renderData.put("titles", titles);

        renderData.put("sheetName", sheetName);

        if (Objects.isNull(data) || data.isEmpty()) {
            log.info("No valid data exists");
            return excelBuilder.useDefaultStyle().build(renderData);
        }
        Optional<?> findResult = data.stream().filter(Objects::nonNull).findFirst();
        if (!findResult.isPresent()) {
            log.info("No valid data exists");
            return excelBuilder.useDefaultStyle().build(renderData);
        }
        Class<?> clazz = findResult.get().getClass();
        Method[] methods = clazz.getMethods();
        if (Objects.isNull(methods) || methods.length == 0) {
            return excelBuilder.useDefaultStyle().build(renderData);
        }
        Map<String, Method> methodMap = Arrays.stream(methods).collect(Collectors.toMap(Method::getName, method -> method));

        List<Method> sortedMethod = new ArrayList<>(fieldDisplayOrder.size());
        fieldDisplayOrder.forEach(fieldName -> sortedMethod.add(this.getMethod(methodMap, fieldName)));
        if (sortedMethod.isEmpty()) {
            log.info("The specified field mapping does not exist");
            return excelBuilder.useDefaultStyle().build(renderData);
        }
        List<List<Object>> contents = data.stream().map(d ->
                sortedMethod.stream().map(m -> getFieldValue(d, m)).collect(Collectors.toList()))
                .collect(Collectors.toList());

        renderData.put("contents", contents);
        return excelBuilder.template(DEFAULT_TEMPLATE_PATH).build(renderData);
    }

    private void selfAdaption() {
        if (Objects.isNull(titles)) {
            return;
        }
        if (fieldDisplayOrder.size() < titles.size()) {
            for (int i = 0, size = titles.size() - fieldDisplayOrder.size(); i < size; i++) {
                fieldDisplayOrder.add(null);
            }
        } else {
            for (int i = 0, size = fieldDisplayOrder.size() - titles.size(); i < size; i++) {
                titles.add(null);
            }
        }
    }

    private Method getMethod(Map<String, Method> methodMap, String fieldName) {
        if (Objects.isNull(fieldName) || fieldName.isEmpty()) {
            return null;
        }
        String formatFieldName = StringUtils.toUpperCaseFirst(fieldName);
        Method method = methodMap.get("get" + formatFieldName);
        if (Objects.isNull(method)) {
            method = methodMap.get("is" + formatFieldName);
        }
        return method;
    }

    private Object getFieldValue(Object o, Method method) {
        if (Objects.isNull(o) || Objects.isNull(method)) {
            return null;
        }
        try {
            return method.invoke(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
