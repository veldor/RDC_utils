package net.velor.rdc_utils.handlers;

import android.util.Log;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Iterator;

public class ExcelHandler {
    private final XSSFWorkbook mSheets;

    public ExcelHandler(XSSFWorkbook sheets) {
        mSheets = sheets;
        getMonth();

    }

    public String getMonth(){
        Iterator<Sheet> monthSheets = mSheets.sheetIterator();
        String sheetName;
        Sheet sheetItem;
        while (monthSheets.hasNext()) {
            sheetItem = monthSheets.next();
            sheetName = sheetItem.getSheetName();
            Log.d("surprise", "getMonth:23 " + sheetName);
        }
        return null;
    }
}
