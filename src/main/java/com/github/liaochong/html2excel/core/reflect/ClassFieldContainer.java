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
package com.github.liaochong.html2excel.core.reflect;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author liaochong
 * @version 1.0
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassFieldContainer {

    Class<?> clazz;

    List<Field> fields = new ArrayList<>();

    Map<String, Field> fieldMap = new HashMap<>();

    ClassFieldContainer parent;

    public Field getFieldByFieldName(String fieldName) {
        return this.getFieldByFieldName(fieldName, this);
    }

    private Field getFieldByFieldName(String fieldName, ClassFieldContainer container) {
        Field field = container.getFieldMap().get(fieldName);
        if (Objects.nonNull(field)) {
            return field;
        }
        ClassFieldContainer parentContainer = container.getParent();
        if (Objects.isNull(parentContainer)) {
            return null;
        }
        return getFieldByFieldName(fieldName, parentContainer);
    }

}
