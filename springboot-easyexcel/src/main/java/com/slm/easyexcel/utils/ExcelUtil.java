package com.slm.easyexcel.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ExcelUtil {

    public static <T> void export(OutputStream os, Class<T> rowType, String SheetName, List<T> data) {
        EasyExcel.write(os).head(rowType).excelType(ExcelTypeEnum.XLSX).sheet(SheetName).doWrite(data);
    }

    public static <T> List<T> read(InputStream is, Class<T> rowType) throws IOException {
        return EasyExcel.read(is).head(rowType).sheet().doReadSync();
    }

}
