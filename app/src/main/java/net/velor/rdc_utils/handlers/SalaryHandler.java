package net.velor.rdc_utils.handlers;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.SalaryActivity;
import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.workers.CheckPlannerWorker;
import net.velor.rdc_utils.workers.RegisterShiftWorker;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import utils.App;
import utils.CashHandler;
import utils.MakeLog;

import static utils.CashHandler.countPercent;

public class SalaryHandler {
    public static void planeRegistration() {
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
                else{
                    MakeLog.writeToLog("Не найдено время завершения смены");
                }
            } else {
                checkTomorrow();
            }
        }
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
        // запланирую проверку выручки
        long plannedTime = calendar.getTimeInMillis();
        long difference = plannedTime - currentTime;
        MakeLog.writeToLog("Запланирована регистрация зарплаты через " + TimeUnit.MILLISECONDS.toHours(difference) + " часов");
        // планирую проверку
        OneTimeWorkRequest registerSalary = new OneTimeWorkRequest.Builder(RegisterShiftWorker.class).addTag(CheckPlannerWorker.SALARY_REGISTER_TAG).setInitialDelay(difference, TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance().enqueueUniqueWork(CheckPlannerWorker.SALARY_REGISTER_TAG, ExistingWorkPolicy.REPLACE, registerSalary);
    }

    public static String countSalary(Cursor salaryDay, boolean isUpRevenue) {
        if(salaryDay.moveToFirst()){
            float hours = salaryDay.getFloat(salaryDay.getColumnIndex(DbWork.SD_COL_DURATION));
            String revenueSumm = salaryDay.getString(salaryDay.getColumnIndex(DbWork.SD_COL_REVENUE));
            String contrastsSumm = salaryDay.getString(salaryDay.getColumnIndex(DbWork.SD_COL_CONTRASTS));
            String dContrastsSumm = salaryDay.getString(salaryDay.getColumnIndex(DbWork.SD_COL_DCONTRASTS));
            String screeningsSumm = salaryDay.getString(salaryDay.getColumnIndex(DbWork.SD_COL_SCREENINGS));
            SharedPreferences prefsManager = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
            String forHour = prefsManager.getString(SalaryActivity.FIELD_PAY_FOR_HOUR, "0");
            String normalBounty = prefsManager.getString(SalaryActivity.FIELD_NORMAL_BOUNTY_PERCENT, "0");
            String upBounty = prefsManager.getString(SalaryActivity.FIELD_HIGH_BOUNTY_PERCENT, "0");
            String contrastCost = prefsManager.getString(SalaryActivity.FIELD_PAY_FOR_CONTRAST, "0");
            String dContrastCost = prefsManager.getString(SalaryActivity.FIELD_PAY_FOR_DYNAMIC_CONTRAST, "0");
            String oncoscreeningCost = prefsManager.getString(SalaryActivity.FIELD_PAY_FOR_ONCOSCREENINGS, "0");

            // получу сумму, заработанную за день
            assert forHour != null;
            float summForHours = hours * Float.valueOf(forHour);
            assert contrastCost != null;
            float summForContrasts = Integer.valueOf(contrastsSumm) * Float.valueOf(contrastCost);
            assert dContrastCost != null;
            float summForDContrasts = Integer.valueOf(dContrastsSumm) * Float.valueOf(dContrastCost);
            assert oncoscreeningCost != null;
            float summForScreenings = Integer.valueOf(screeningsSumm) * Float.valueOf(oncoscreeningCost);
            float ndfl = countPercent(summForHours, "13.");
            float totalSumm;
            if(isUpRevenue){
                float summBounty = countPercent(Float.valueOf(revenueSumm), upBounty);
                totalSumm = summBounty + summForContrasts + summForDContrasts + summForScreenings - ndfl;
            }
            else{
                float summBounty = countPercent(Float.valueOf(revenueSumm), normalBounty);
                totalSumm = summBounty + summForContrasts + summForDContrasts + summForScreenings + summForHours - ndfl;
            }
            salaryDay.close();
            return CashHandler.addRuble(totalSumm);
        }
        return "0";
    }

    public static boolean checkUpSalary(Integer year, Integer month) {
        Cursor days = App.getInstance().getDatabaseProvider().getSalaryMonth(year, month);
        float medianGain = 0;
        if(days.moveToFirst()){
            do{
                // получу сумму, заработанную за месяц
                medianGain = Float.valueOf(days.getString(days.getColumnIndex(DbWork.SM_COL_MEDIAN_GAIN)));
            }
            while (days.moveToNext());
        }
        days.close();
        SharedPreferences prefsManager = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        String limit = prefsManager.getString(SalaryActivity.FIELD_UP_LIMIT, null);
        float neededMedian = 0;
        if(limit != null && !limit.isEmpty()){
            neededMedian = Float.valueOf(limit);
        }
        return medianGain >= neededMedian;
    }
}
