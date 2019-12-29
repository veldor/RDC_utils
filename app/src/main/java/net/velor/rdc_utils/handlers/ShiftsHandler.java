package net.velor.rdc_utils.handlers;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;

import net.velor.rdc_utils.MainActivity;
import net.velor.rdc_utils.SalaryDayActivity;
import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.database.DbWork;

import org.apache.xmlbeans.impl.jam.mutable.MAnnotatedElement;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import utils.App;

import static utils.CashHandler.timeToInt;

public class ShiftsHandler {

    private static void checkShift(String shiftName, String shiftColor) {
        // проверю, есть ли смены в базе данных, если нет- внесу
        DbWork databaseProvider = App.getInstance().getDatabaseProvider();
        Cursor existentShift = databaseProvider.getShiftByShiftName(shiftName, shiftColor);
        if (!existentShift.moveToFirst()) {
            // создам и зарегистрирую новую смену
            final ContentValues cv = new ContentValues();
            cv.put(ShiftCursorAdapter.COL_NAME_FULL, shiftName);
            cv.put(ShiftCursorAdapter.COL_NAME_SHORT, shiftName);
            cv.put(ShiftCursorAdapter.COL_SCHEDULE_COLOR_NAME, shiftColor);
            int requiredColor = Color.parseColor("#" + shiftColor);
            String hexColor = String.format("#%06X", (0xFFFFFF & requiredColor));
            Log.d("surprise", "ShiftsHandler checkShift set color " + hexColor);
            cv.put(ShiftCursorAdapter.COL_SHIFT_COLOR, hexColor);
            cv.put(ShiftCursorAdapter.COL_ALARM, false);
            databaseProvider.insertShift(cv);
        }
        existentShift.close();
    }

    public static void checkShift(HashMap<String, String> shiftsList) {
        for(Map.Entry<String, String> entry : shiftsList.entrySet()) {
            String name = entry.getKey();
            String color = entry.getValue();
            checkShift(name, color);
            // do what you have to do here
            // In your case, another loop.
        }
    }

    static HashMap<String, String> getShiftsWithNames() {
        Cursor existentShifts = App.getInstance().getDatabaseProvider().getAllShifts();
        HashMap<String, String> result = new HashMap<>();
        if (existentShifts.moveToFirst()) {
            int id;
            String name;
            do {
                // получу идентификатор и название смены
                id = existentShifts.getInt(existentShifts.getColumnIndex(ShiftCursorAdapter.COL_ID));
                name = existentShifts.getString(existentShifts.getColumnIndex(ShiftCursorAdapter.COL_NAME_SHORT));
                result.put(name, String.valueOf(id));
            }
            while (existentShifts.moveToNext());
        }
        existentShifts.close();
        return result;
    }

    private static Map<String, String> getShiftInfo(Calendar calendar) {
        DbWork databaseProvider = App.getInstance().getDatabaseProvider();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        Cursor schedule = databaseProvider.getSchedule(year, month);
        if (schedule.moveToFirst()) {
            // обработаю информацию
            int dayType = XMLHandler.checkShift(schedule, day);
            if (dayType != -1) {
                return databaseProvider.getShift(dayType);
            }
        }
        return null;
    }

    public static void registerSalary(int dayId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, MainActivity.sYear);
        calendar.set(Calendar.MONTH, MainActivity.sMonth);
        Log.d("surprise", "ShiftsHandler registerSalary " + MainActivity.sMonth);
        calendar.set(Calendar.DATE, dayId);
        // получу информацию про день
        // проверю, не зарегистрирована ли уже смена
        Map<String, String> shiftInfo = ShiftsHandler.getShiftInfo(calendar);
        int duration = 0;
        if (shiftInfo != null) {
            String start = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_START);
            String finish = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
            if (start != null && !start.isEmpty() && finish != null && !finish.isEmpty()) {
                int intStart = timeToInt(start);
                int intFinish = timeToInt(finish);
                duration = intFinish - intStart;
            }
        }
        // при нажатии на кнопку- запущу регистрацию новой смены с заданными параметрами
        Intent newShift = new Intent(App.getInstance(), SalaryDayActivity.class);
        newShift.putExtra(SalaryDayActivity.HAS_INFO, true);
        newShift.putExtra(SalaryDayActivity.DAY_FIELD, calendar.get(Calendar.DATE));
        newShift.putExtra(SalaryDayActivity.MONTH_FIELD, MainActivity.sMonth);
        newShift.putExtra(SalaryDayActivity.YEAR_FIELD, MainActivity.sYear);
        newShift.putExtra(SalaryDayActivity.DURATION, duration);
        newShift.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Cursor registered = App.getInstance().getDatabaseProvider().getSalaryDayByDate(MainActivity.sYear, MainActivity.sMonth - 1, dayId);
        if (registered.moveToFirst()) {
            // смена зарегистрирована
            newShift.putExtra(SalaryDayActivity.ID, registered.getLong(registered.getColumnIndex(DbWork.COL_ID)));
        }
        App.getInstance().startActivity(newShift);
    }
}
