package com.slm.easypoi.utils;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ExcelUtil {

    public static <T> void export(OutputStream os, Class<T> rowType, String sheetName, List<T> data) throws IOException {
        ExportParams exportParams = new ExportParams();
        exportParams.setSheetName(sheetName);
        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, rowType, data);
        workbook.write(os);
        os.close();
    }

    public static <T> List<T> read(InputStream is, Class<T> rowType) throws Exception {
        return ExcelImportUtil.importExcel(is, rowType, new ImportParams());
    }

}
