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

import com.github.liaochong.html2excel.core.style.BackgroundStyle;
import com.github.liaochong.html2excel.core.style.BorderStyle;
import com.github.liaochong.html2excel.core.style.FontStyle;
import com.github.liaochong.html2excel.core.style.TdDefaultCellStyle;
import com.github.liaochong.html2excel.core.style.TextAlignStyle;
import com.github.liaochong.html2excel.core.style.ThDefaultCellStyle;
import com.github.liaochong.html2excel.exception.NoTablesException;
import com.github.liaochong.html2excel.exception.UnsupportedWorkbookTypeException;
import com.github.liaochong.html2excel.utils.StyleUtils;
import com.github.liaochong.html2excel.utils.TdUtils;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * HtmlToExcelFactory
 * <p>
 * 用于将html table解析成excel
 * </p>
 *
 * @author liaochong
 * @version 1.0
 */
public class HtmlToExcelFactory {
    /**
     * html解析后文档
     */
    private Document document;
    /**
     * excel workbook
     */
    private Workbook workbook;
    /**
     * sheet容器
     */
    private Map<Integer, Sheet> sheetMap;
    /**
     * 冻结区域
     */
    private FreezePane[] freezePanes;
    /**
     * tr容器
     */
    private List<Tr> trContainer;
    /**
     * 总列数
     */
    private int totalCols;
    /**
     * 样式容器
     */
    private Map<Tag, CellStyle> defaultCellStyleMap;
    /**
     * 单元格样式映射
     */
    private Map<Map<String, String>, CellStyle> cellStyleMap;
    /**
     * future
     */
    private CompletableFuture<Void> workbookFuture;
    /**
     * 每行的单元格最大高度map
     */
    private Map<Integer, Short> maxTdHeightMap;
    /**
     * 是否使用默认样式
     */
    private boolean useDefaultStyle;
    /**
     * 自定义颜色索引
     */
    private AtomicInteger colorIndex = new AtomicInteger(56);

    public HtmlToExcelFactory() {
    }

    /**
     * 读取html
     *
     * @param htmlFile html文件
     * @throws Exception 解析异常
     */
    public static HtmlToExcelFactory readHtml(File htmlFile) throws Exception {
        if (Objects.isNull(htmlFile) || !htmlFile.exists()) {
            throw new NoSuchFileException("html file is not exist");
        }
        HtmlToExcelFactory factory = new HtmlToExcelFactory();
        factory.document = Jsoup.parse(htmlFile, CharEncoding.UTF_8);
        return factory;
    }

    /**
     * 读取html
     *
     * @param htmlFile           html文件
     * @param htmlToExcelFactory 实例对象
     * @return HtmlToExcelFactory
     * @throws Exception 解析异常
     */
    public static HtmlToExcelFactory readHtml(File htmlFile, HtmlToExcelFactory htmlToExcelFactory) throws Exception {
        if (Objects.isNull(htmlFile) || !htmlFile.exists()) {
            throw new NoSuchFileException("Html file is not exist");
        }
        if (Objects.isNull(htmlToExcelFactory)) {
            throw new NullPointerException("HtmlToExcelFactory can not be null");
        }
        htmlToExcelFactory.document = Jsoup.parse(htmlFile, CharEncoding.UTF_8);
        return htmlToExcelFactory;
    }

    /**
     * 设置使用默认样式
     *
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory useDefaultStyle() {
        this.useDefaultStyle = true;
        return this;
    }

    /**
     * 创建固定区域
     *
     * @param freezePanes 固定区域，二维数组
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory freezePanes(FreezePane... freezePanes) {
        this.freezePanes = freezePanes;
        return this;
    }

    /**
     * 设置workbook类型
     *
     * @param workbookType 工作簿类型
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory workbookType(WorkbookType workbookType) {
        if (Objects.isNull(workbookType)) {
            throw new IllegalArgumentException("WorkbookType must be specified,or remove this method, use the default workbookType");
        }
        switch (workbookType) {
            case XLS:
                workbook = new HSSFWorkbook();
                break;
            case XLSX:
                workbook = new XSSFWorkbook();
                break;
            case SXLSX:
                throw new UnsupportedWorkbookTypeException("SXSSFWorkbook is not supported at this version");
            default:
        }
        return this;
    }

    /**
     * 开始构建
     *
     * @return Workbook
     */
    public Workbook build() {
        Elements tables = document.getElementsByTag(Tag.table.name());
        if (tables.isEmpty()) {
            throw NoTablesException.of("There is no any table exist");
        }
        // 1、创建工作簿
        this.createWorkbook(tables);

        // 2、处理解析表格
        for (int i = 0, size = tables.size(); i < size; i++) {
            // 获取所有单元格
            List<Td> tds = this.processTable(tables.get(i));
            if (Objects.isNull(tds) || tds.isEmpty()) {
                continue;
            }
            // 设置单元格样式
            this.setTdOfTable(i, tds);
            // 设置行高
            this.setRowHeight(i);
        }
        return workbook;
    }

    /**
     * 设置行高，最小12
     *
     * @param i 表格索引
     */
    private void setRowHeight(int i) {
        Sheet sheet = sheetMap.get(i);
        for (int j = 0, size = trContainer.size(); j < size; j++) {
            Row row = sheet.getRow(j);
            if (Objects.isNull(maxTdHeightMap) || Objects.isNull(maxTdHeightMap.get(row.getRowNum()))) {
                row.setHeightInPoints(row.getHeightInPoints() + 5);
            } else {
                row.setHeightInPoints((short) (maxTdHeightMap.get(row.getRowNum()) + 5));
            }
        }
    }

    /**
     * 创建workbook，因为创建workbook比较耗时，异步处理
     *
     * @param tables 表格集合
     */
    private void createWorkbook(Elements tables) {
        workbookFuture = CompletableFuture.runAsync(() -> {
            if (Objects.isNull(workbook)) {
                workbook = new XSSFWorkbook();
            }
            if (useDefaultStyle) {
                defaultCellStyleMap = new EnumMap<>(Tag.class);
                defaultCellStyleMap.put(Tag.th, new ThDefaultCellStyle().supply(workbook));
                defaultCellStyleMap.put(Tag.td, new TdDefaultCellStyle().supply(workbook));
            }
            sheetMap = new ConcurrentHashMap<>(tables.size());
            for (int i = 0; i < tables.size(); i++) {
                Element table = tables.get(i);
                Elements captions = table.getElementsByTag(Tag.caption.name());
                String sheetName = captions.isEmpty() ? "sheet" + (i + 1) : captions.first().text();
                Sheet sheet = workbook.createSheet(sheetName);
                sheetMap.put(i, sheet);

                Elements trs = table.getElementsByTag(Tag.tr.name());
                int cloNum = trs.parallelStream().mapToInt(tr -> {
                    Elements tds = tr.children();
                    return tds.stream().mapToInt(td -> {
                        String colSpan = td.attr(Tag.colspan.name());
                        if (!TdUtils.isSpanValid(colSpan)) {
                            return 1;
                        }
                        int colSpanVal = Integer.parseInt(colSpan);
                        return colSpanVal > 0 ? colSpanVal : 1;
                    }).sum();
                }).max().orElse(0);

                for (int j = 0; j < trs.size(); j++) {
                    Row row = sheet.createRow(j);
                    for (int k = 0; k <= cloNum; k++) {
                        row.createCell(k);
                    }
                }
                if (Objects.nonNull(freezePanes) && freezePanes.length > i) {
                    FreezePane freezePane = freezePanes[i];
                    if (Objects.isNull(freezePane)) {
                        throw new IllegalStateException("FreezePane is null");
                    }
                    sheet.createFreezePane(freezePane.getColSplit(), freezePane.getRowSplit());
                }
            }
        });
    }

    /**
     * 解析每一个table
     *
     * @param table 表格
     */
    private List<Td> processTable(Element table) {
        this.initialize();
        // 表样式
        Map<String, String> tableStyle = StyleUtils.parseStyle(table);
        Elements trs = table.getElementsByTag(Tag.tr.name());
        if (Objects.isNull(trs) || trs.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0, size = trs.size(); i < size; i++) {
            Tr tr = new Tr(i);
            tr.setStyle(StyleUtils.mixStyle(StyleUtils.parseStyle(trs.get(i)), tableStyle));
            trContainer.add(tr);
            this.processTr(trs.get(i), tr);
        }
        this.countTotalCols();
        return this.adjustTdPosition();
    }

    /**
     * 初始化，每解析一个表格需要重新初始化
     */
    private void initialize() {
        totalCols = 0;
        trContainer = new ArrayList<>();
        cellStyleMap = new HashMap<>();
    }

    /**
     * 计算总列数
     */
    private void countTotalCols() {
        ToIntFunction<Tr> function = tr -> tr.getTds().stream().mapToInt(td -> TdUtils.get(td::getColSpan, td::getCol))
                .max().orElse(0);
        totalCols = trContainer.parallelStream().mapToInt(function).max()
                .orElseThrow(() -> new NoTablesException("不存在任何单元格"));
    }

    /**
     * 创建
     *
     * @param tableIndex 表格索引
     */
    private void setTdOfTable(int tableIndex, List<Td> allTds) {
        workbookFuture.join();
        Sheet sheet = sheetMap.get(tableIndex);
        allTds.forEach(td -> this.setCell(td, sheet));
        // 自适应列宽
        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 设置单元格
     *
     * @param td    单元格
     * @param sheet 单元格所在的sheet
     */
    private void setCell(Td td, Sheet sheet) {
        Cell cell = sheet.getRow(td.getRow()).getCell(td.getCol());
        cell.setCellValue(td.getContent());

        // 设置单元格样式
        int boundCol = TdUtils.get(td::getColSpan, td::getCol);
        int boundRow = TdUtils.get(td::getRowSpan, td::getRow);

        cellStyleMap = new HashMap<>();
        for (int i = td.getRow(); i <= boundRow; i++) {
            for (int j = td.getCol(); j <= boundCol; j++) {
                Row row = sheet.getRow(i);
                cell = row.getCell(j);
                this.setCellStyle(row, cell, td);
            }
        }
        if (td.getColSpan() > 0 || td.getRowSpan() > 0) {
            sheet.addMergedRegion(new CellRangeAddress(td.getRow(), boundRow, td.getCol(), boundCol));
        }
    }

    /**
     * 设置单元格样式
     *
     * @param cell 单元格
     * @param td   td单元格
     */
    private void setCellStyle(Row row, Cell cell, Td td) {
        if (useDefaultStyle) {
            if (td.isTh()) {
                cell.setCellStyle(defaultCellStyleMap.get(Tag.th));
            } else {
                cell.setCellStyle(defaultCellStyleMap.get(Tag.td));
            }
        } else {
            if (cellStyleMap.containsKey(td.getStyle())) {
                cell.setCellStyle(cellStyleMap.get(td.getStyle()));
                return;
            }
            if (Objects.isNull(maxTdHeightMap)) {
                maxTdHeightMap = new ConcurrentHashMap<>();
            }
            CellStyle cellStyle = workbook.createCellStyle();
            // background-color
            BackgroundStyle.setBackgroundColor(workbook, cellStyle, td.getStyle(), colorIndex);
            // text-align
            TextAlignStyle.setTextAlign(cellStyle, td.getStyle());
            // border
            BorderStyle.setBorder(cellStyle, td.getStyle());
            // font
            FontStyle.setFont(workbook, row, cellStyle, td.getStyle(), maxTdHeightMap);
            cell.setCellStyle(cellStyle);
            cellStyleMap.put(td.getStyle(), cellStyle);
        }
    }

    /**
     * 处理行元素
     *
     * @param trElement tr
     * @param tr        tr容器
     */
    private void processTr(Element trElement, Tr tr) {
        Elements childrenElements = trElement.children();
        this.processTd(childrenElements, tr);
    }

    /**
     * 处理行内元素
     *
     * @param tdElements 元素：th、td
     * @param tr         元素容器
     */
    private void processTd(Elements tdElements, Tr tr) {
        if (tdElements.isEmpty()) {
            return;
        }
        for (int i = 0, size = tdElements.size(); i < size; i++) {
            Element tdElement = tdElements.get(i);
            Td td = new Td();
            td.setTh(Objects.equals(Tag.th.name(), tdElement.tagName()));
            td.setRow(tr.getIndex());
            td.setStyle(StyleUtils.mixStyle(StyleUtils.parseStyle(tdElement), tr.getStyle()));
            // 除每行第一个单元格外，修正含跨列的单元格位置
            if (i > 0) {
                int shift = tr.getTds().stream().filter(t -> t.getColSpan() > 0)
                        .mapToInt(t -> t.getColSpan() - 1).sum();
                td.setCol(i + shift);
            } else {
                td.setCol(i);
            }

            String colSpan = tdElement.attr(Tag.colspan.name());
            td.setColSpan(TdUtils.getSpan(colSpan));

            String rowSpan = tdElement.attr(Tag.rowspan.name());
            td.setRowSpan(TdUtils.getSpan(rowSpan));

            td.setContent(tdElement.text());
            tr.getTds().add(td);
        }
    }

    /**
     * 调整表格单元格位置
     *
     * @return 所有单元格
     */
    private List<Td> adjustTdPosition() {
        // 排除第一行，第一行不需要进行调整
        for (int i = 1; i < trContainer.size(); i++) {
            Tr tr = trContainer.get(i);
            tr.getTds().forEach(td -> this.adjustTdPosition(td, tr.getIndex()));
        }
        return trContainer.stream().flatMap(tr -> tr.getTds().stream()).collect(Collectors.toList());
    }

    /**
     * 调整表格单元格位置
     *
     * @param td      单元格
     * @param trIndex 单元格所在行索引
     */
    private void adjustTdPosition(Td td, int trIndex) {
        Predicate<Tr> predicate = tr -> tr.getIndex() < trIndex;
        List<Td> tds = trContainer.stream().filter(predicate).flatMap(tr -> tr.getTds().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tds)) {
            return;
        }
        for (int i = 0; i < totalCols; i++) {
            Td td1 = tds.stream().filter(prevTd -> prevTd.getCol() <= td.getCol()
                    && TdUtils.get(prevTd::getRowSpan, prevTd::getRow) >= td.getRow()).findFirst().orElse(null);
            if (Objects.isNull(td1)) {
                return;
            }
            int prevTdColSpan = td1.getColSpan();
            int realCol = prevTdColSpan > 0 ? td.getCol() + prevTdColSpan : td.getCol() + 1;
            td.setCol(realCol);
            tds.remove(td1);
        }
    }

    private enum Tag {
        /**
         * table
         */
        table,
        /**
         * caption
         */
        caption,
        /**
         * thead
         */
        thead,
        /**
         * tbody
         */
        tbody,
        /**
         * tr
         */
        tr,
        /**
         * th
         */
        th,
        /**
         * td
         */
        td,
        /**
         * colspan
         */
        colspan,
        /**
         * rowspan
         */
        rowspan;
    }
}
