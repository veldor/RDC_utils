package net.velor.rdc_utils.handlers;

import android.graphics.Color;
import android.text.format.DateFormat;

import net.velor.rdc_utils.subclasses.ScheduleMonth;
import net.velor.rdc_utils.subclasses.ShiftType;
import net.velor.rdc_utils.subclasses.WorkingPerson;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static net.velor.rdc_utils.MainActivity.sCalendar;
import static net.velor.rdc_utils.MainActivity.sYear;

public class ExcelHandler {
    private final XSSFWorkbook mDocument;
    private XSSFSheet mTable;
    private final HashMap<Integer, String> mAvailableMonths = new HashMap<>();

    private final List<String> positions = Arrays.asList(
            "Врачи МРТ",
            "Операторы МРТ",
            "Администраторы МРТ",
            "Администраторы МРТ 2",
            "Call-center 8-20",
            "Колл-центр",
            "Процедурный кабинет",
            "УЗИ",
            "Консультативный прием",
            "УВТ",
            "Наркозы");
    public static final HashMap<String, Integer> roleColors = new HashMap<>();

    private ScheduleMonth mSelectedMonth;
    private WorkingPerson mSelectedPerson;
    private ArrayList<ShiftType> mScheduleList;
    private ArrayList<ShiftType> mShiftsList;

    public ExcelHandler(XSSFWorkbook sheets) {
        mDocument = sheets;
        // создам список найденных месяцев
        fillMonthList();

        roleColors.put("Врачи МРТ", Color.parseColor("#FF0000"));
        roleColors.put("Операторы МРТ", Color.parseColor("#0026FF"));
        roleColors.put("Администраторы МРТ", Color.parseColor("#4CFF00"));
        roleColors.put("Администраторы МРТ 2", Color.parseColor("#00FF21"));
        roleColors.put("Процедурный кабинет", Color.parseColor("#000000"));
        roleColors.put("УЗИ", Color.parseColor("#B200FF"));
        roleColors.put("Консультативный прием", Color.parseColor("#4800FF"));
        roleColors.put("УВТ", Color.parseColor("#7FC9FF"));
        roleColors.put("Наркозы", Color.parseColor("#00FFFF"));
    }

    private void fillMonthList() {
        Iterator<Sheet> monthSheets = mDocument.sheetIterator();
        Sheet sheetItem;
        int counter = 0;
        String sheetName;
        while (monthSheets.hasNext()) {
            sheetItem = monthSheets.next();
            sheetName = sheetItem.getSheetName();
            // проверю, не скрыта ли таблица
            if (!mDocument.isSheetHidden(counter) && !sheetName.startsWith("Лист")) {
                mAvailableMonths.put(counter, sheetName);
            }
            counter++;
        }
    }

    public ScheduleMonth getPossibleMonth() {
        // отображу текущий месяц
        String currentMonth = DateFormat.format("LLLL", sCalendar).toString();
        String searchValue = currentMonth + " " + sYear;
        if (mAvailableMonths.containsValue(searchValue)) {
            for (int key :
                    mAvailableMonths.keySet()) {
                String value = mAvailableMonths.get(key);
                if (value != null && value.toLowerCase().equals(searchValue.toLowerCase())) {
                    return new ScheduleMonth(key, value);
                }
            }
        }
        return null;
    }

    public String[] getMonths() {
        return mAvailableMonths.values().toArray(new String[]{});
    }

    public void selectMonth(String month) {
        if (mAvailableMonths.containsValue(month)) {
            for (int key :
                    mAvailableMonths.keySet()) {
                String value = mAvailableMonths.get(key);
                if (value != null && value.toLowerCase().equals(month)) {
                    mSelectedMonth = new ScheduleMonth(key, value);
                }
            }
        }
    }

    public ScheduleMonth getSelectedMonth() {
        return mSelectedMonth;
    }

    public ArrayList<WorkingPerson> getPersons() {
        ArrayList<WorkingPerson> answer = new ArrayList<>();
        mTable = mDocument.getSheetAt(mSelectedMonth.index);
        // количество строк в таблице
        int length = mTable.getLastRowNum();

        if (length > 0) {
            String role = "";
            int counter = 0;
            XSSFCell cell;
            String value;
            while (counter < length) {
                // получаю строку
                XSSFRow row = mTable.getRow(counter);
                if (row != null) {
                    // обрабатываю строку
                    // получаю ячейку с именем
                    cell = row.getCell(0);
                    if (cell != null) {
                        // продолжаю обработку
                        value = cell.getStringCellValue();
                        if (value != null && !value.isEmpty()) {
                            if (positions.contains(value.trim())) {
                                role = value;
                            } else {
                                answer.add(new WorkingPerson(value, role, counter));
                            }
                        }
                    }
                }
                counter++;
            }
        }
        return answer;
    }

    public void setPerson(WorkingPerson workingPerson) {
        mSelectedPerson = workingPerson;
    }

    public WorkingPerson getSelectedPerson() {
        return mSelectedPerson;
    }

    public void makeSchedule() {
        if (mSelectedPerson != null) {
            XSSFRow row = mTable.getRow(mSelectedPerson.rowId);
            if (row != null) {
                Iterator<Cell> cells = row.cellIterator();
                // пропущу строку с именем
                cells.next();
                // получу количество дней в выбранном месяце
                int daysCounter = sCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                int doubleDaysCounter = daysCounter + 1;
                int counter = 0;
                // составлю массив расписания
                mScheduleList = new ArrayList<>();
                mShiftsList = new ArrayList<>();
                Cell cell;
                CellType cellType;
                String value;
                ShiftType dayShiftType;
                while (++counter < doubleDaysCounter + 1) {
                    cell = row.getCell(counter);
                    if (cell != null) {
                        cellType = cell.getCellTypeEnum();
                        if (cellType.equals(CellType.NUMERIC)) {
                            value = String.valueOf(cell.getNumericCellValue());
                            registerWorkdayType(value, cell);
                            dayShiftType = new ShiftType(value, getCellColor(cell));
                        } else if (cellType.equals(CellType.STRING)) {
                            value = cell.getStringCellValue().trim();
                            // переведу в нижний регистр
                            value = value.toLowerCase();
                            // зарегистрирую тип смены
                            registerWorkdayType(value, cell);
                            dayShiftType = new ShiftType(value, getCellColor(cell));
                        } else {
                            dayShiftType = null;
                        }
                        mScheduleList.add(dayShiftType);
                    } else {
                        mScheduleList.add(null);
                    }
                }
            }
        }
    }

    private String getCellColor(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style != null) {
            org.apache.poi.ss.usermodel.Color rawColor = cell.getCellStyle().getFillForegroundColorColor();
            if (rawColor != null) {
                XSSFColor trueColor = (XSSFColor) rawColor;
                return trueColor.getARGBHex();
            }
        }
        return "ffffffff";
    }

    private void registerWorkdayType(String value, Cell cell) {
        CellStyle style = cell.getCellStyle();
        String colorValue = "FFFFFFFF";
        if (style != null) {
            org.apache.poi.ss.usermodel.Color rawColor = cell.getCellStyle().getFillForegroundColorColor();
            if (rawColor != null) {
                XSSFColor trueColor = (XSSFColor) rawColor;
                colorValue = trueColor.getARGBHex();
            }
        }
        ShiftType newShiftType = new ShiftType();
        newShiftType.scheduleName = value;
        newShiftType.scheduleColor = colorValue;
        // проверю, нет ли данного типа смены в списке
        if (mShiftsList.size() == 0) {
            mShiftsList.add(newShiftType);
        } else {
            boolean foundDuplicate = false;
            for (ShiftType st :
                    mShiftsList) {
                if (st.scheduleColor.equals(newShiftType.scheduleColor) && st.scheduleName.equals(newShiftType.scheduleName)) {
                    foundDuplicate = true;
                    break;
                }
            }
            if (!foundDuplicate) {
                mShiftsList.add(newShiftType);
            }
        }
    }

    public ArrayList<ShiftType> getShiftsList() {
        return mShiftsList;
    }

    public ArrayList<ShiftType> getScheduleList() {
        return mScheduleList;
    }

    public boolean isPersonExists(String person) {
        ArrayList<WorkingPerson> persons = getPersons();
        String[] personInfo = person.split(";");
        if (personInfo.length == 2) {
            for (WorkingPerson wp :
                    persons) {
                if (wp.name.equals(personInfo[1]) && wp.role.equals(personInfo[0])) {
                    mSelectedPerson = wp;
                    return true;
                }
            }
        }
        return false;
    }

    public XSSFSheet getTable() {
        return mTable;
    }
}
