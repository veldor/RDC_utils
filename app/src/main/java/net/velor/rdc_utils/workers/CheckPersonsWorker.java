package net.velor.rdc_utils.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.MainActivity;
import net.velor.rdc_utils.handlers.ScheduleHandler;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.Calendar;
import java.util.Iterator;

public class CheckPersonsWorker extends Worker {
    public CheckPersonsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        XSSFSheet sheet = ScheduleHandler.sSheet;
        if(sheet != null){
            // получу список всех работников
            int length = sheet.getLastRowNum();
            String post = "";
            int counter = 0;
            XSSFCell cell;
            while (counter < length) {
                XSSFRow row = sheet.getRow(counter);
                if (row != null) {
                    cell = row.getCell(0);
                    if (cell != null) {
                        String person = cell.getStringCellValue();
                        if(person != null && !person.isEmpty()){
                            switch (person.trim()) {
                                case "Врачи МРТ":
                                    post = "Врач";
                                    break;
                                case "Операторы МРТ":
                                    post = "Оператор";
                                    break;
                                case "Администраторы МРТ 1":
                                    post = "Администратор";
                                    break;
                                case "Колл-центр":
                                    post = "Администратор колл-центра";
                                    break;
                                case "УЗИ":
                                    post = "Врач УЗИ";
                                    break;
                                case "Консультанты":
                                    post = "Врач-консультант";
                                    break;
                                case "УВТ":
                                    post = "Врач УВТ";
                                    break;
                                default:
                                    // проверю, занесён ли врач в базу. Если нет- занесу
                                    ScheduleHandler.addPersonToBase(post, person);
                                    // проверю смены, которые он работает в этом месяце и занесу их в базу данных
                                    int daysCounter = MainActivity.sCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                                    Iterator<Cell> cells = row.cellIterator();
                                    // пропущу строку с именем
                                    cells.next();
                                    Cell currentCell;
                                    CellType cellType;
                                    String value;
                                    int day = 0;
                                    while (cells.hasNext() && daysCounter > 0) {
                                        ++day;
                                        value = null;
                                        currentCell = cells.next();
                                        cellType = currentCell.getCellTypeEnum();
                                        if (cellType.equals(CellType.NUMERIC)) {
                                            value = String.valueOf(currentCell.getNumericCellValue());
                                        } else if (cellType.equals(CellType.STRING)) {
                                            value = currentCell.getStringCellValue().trim();
                                        }
                                        if (value != null && !TextUtils.isEmpty(value)) {
                                            // занесу данные о смене в список
                                            ScheduleHandler.addDayToSchedule(day, person, post, value);
                                        }
                                        --daysCounter;
                                    }
                            }
                        }
                    }
                }
                counter++;
            }
        }
        return Worker.Result.success();
    }
}
