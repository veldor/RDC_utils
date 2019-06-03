package net.velor.rdc_utils.handlers;

import android.database.Cursor;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.workers.CheckPlannerWorker;
import net.velor.rdc_utils.workers.RegisterShiftWorker;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import utils.App;
import utils.MakeLog;

public class SalaryHandler {
    public static boolean planeRegistration() {
        Calendar calendar = Calendar.getInstance();
        DbWork databaseProvider = App.getInstance().getDatabaseProvider();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        Cursor schedule = databaseProvider.getSchedule(year, month + 1);
        if (schedule != null && schedule.moveToFirst()) {
            int day = calendar.get(Calendar.DATE);
            int dayType = XMLHandler.checkShift(schedule, day);
            if (dayType != -1) {
                // получу информацию об окончании смены
                Map<String, String> shiftInfo = databaseProvider.getShift(dayType);
                String finish = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                if (finish != null && !finish.isEmpty()) {
                    // разложу время на часы и минуты
                    Integer[] time = TimeHandler.getTime(finish);
                    // если смена сегодня: проверю, что время ещё не прошло
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    if (hour < time[0]) {
                        // планирую смену
                        planeRemind(setTime(calendar, time));
                    } else if (hour == time[0]) {
                        // проверю минуты
                        int minutes = calendar.get(Calendar.MINUTE);
                        if (minutes < time[1]) {
                            // планирую проверку
                            planeRemind(setTime(calendar, time));
                        } else {
                            // проверю следующий день
                            checkTomorrow();
                        }
                    } else {
                        // проверю следующий день
                        checkTomorrow();
                    }

                }
            } else {
                checkTomorrow();
            }
        }
        return true;
    }

    public static void checkTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        DbWork databaseProvider = App.getInstance().getDatabaseProvider();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        Cursor schedule = databaseProvider.getSchedule(year, month + 1);
        if (schedule != null && schedule.moveToFirst()) {
            int day = calendar.get(Calendar.DATE);
            int dayType = XMLHandler.checkShift(schedule, day);
            if (dayType != -1) {
                // получу информацию об окончании смены
                Map<String, String> shiftInfo = databaseProvider.getShift(dayType);
                String finish = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                if (finish != null && !finish.isEmpty()) {
                    // разложу время на часы и минуты
                    Integer[] time = TimeHandler.getTime(finish);
                    // если смена сегодня: проверю, что время ещё не прошло
                    planeRemind(setTime(calendar, time));
                }
            }
        }
    }

    private static Calendar setTime(Calendar calendar, Integer[] time) {
        Integer hour = time[0];
        Integer minutes = time[1];
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);
        return calendar;
    }

    private static void planeRemind(Calendar calendar) {
        // получу значения текущего времени
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long plannedTime = calendar.getTimeInMillis();
        long difference = plannedTime - currentTime;
        MakeLog.writeToLog("Запланирована регистрация через " + TimeUnit.MILLISECONDS.toHours(difference) + " часов");
        // планирую проверку
        OneTimeWorkRequest registerSalary = new OneTimeWorkRequest.Builder(RegisterShiftWorker.class).addTag(CheckPlannerWorker.SALARY_REGISTER_TAG).setInitialDelay(difference, TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance().enqueueUniqueWork(CheckPlannerWorker.SALARY_REGISTER_TAG, ExistingWorkPolicy.REPLACE, registerSalary);
    }
}
