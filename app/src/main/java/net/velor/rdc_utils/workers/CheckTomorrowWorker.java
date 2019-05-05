package net.velor.rdc_utils.workers;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.ForemanHandler;
import net.velor.rdc_utils.handlers.XMLHandler;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import utils.App;
import utils.Notificator;

class CheckTomorrowWorker extends Worker {
    CheckTomorrowWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "CheckTomorrowWorker doWork: i work");
        // проверю, не нужно ли завтра на работу
        Calendar calendar = Calendar.getInstance();
        // получу завтрашнее число
        calendar.add(Calendar.DATE, 1);
        // получу данные о завтрашней смене
        DbWork databaseProvider = App.getInstance().getDatabaseProvider();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        Cursor schedule = databaseProvider.getSchedule(year, month + 1);
        if(schedule != null && schedule.moveToFirst()){
            // расписание найдено, получу данные о конкретном дне
            int day = calendar.get(Calendar.DATE);
            int dayType = XMLHandler.checkShift(schedule, day);
            schedule.close();
            if(dayType != -1){
                // если день не выходной- получу данные о смене
                Map<String, String> shiftInfo = databaseProvider.getShift(dayType);
                // переменная для времени будильника, если оно назначено
                boolean alarmEnabled = false;
                String[] alarmTimeArray = null;
                StringBuilder sb = new StringBuilder();
                // получу сведения о смене
                String name = shiftInfo.get(ShiftCursorAdapter.COL_NAME_FULL);
                sb.append("Завтра вы работаете в РДЦ.");
                String start = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_START);
                if (start != null) {
                    sb.append(String.format(Locale.ENGLISH, " Начало смены: в %s часов.", start));
                }
                String finish = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                if (finish != null) {
                    sb.append(String.format(Locale.ENGLISH, " Завершение смены: в %s часов.", finish));
                    //активирую регистрацию смены
                    String[] time_array = finish.split(":");
                    int hour = Integer.valueOf(time_array[0]);
                    int minutes = Integer.valueOf(time_array[1]);
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, 1);
                    cal.set(Calendar.HOUR, hour);
                    cal.set(Calendar.MINUTE, minutes);

                    // определю, через какое время назначить проверку
                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    long plannedTime = cal.getTimeInMillis();
                    long difference = plannedTime - currentTime;

                    // назначу запуск рабочего, который предложит сохранить сведения о смене
                    OneTimeWorkRequest registerShift = new OneTimeWorkRequest.Builder(RegisterShiftWorker.class).setInitialDelay(difference, TimeUnit.MILLISECONDS).build();
                    //OneTimeWorkRequest registerShift = new OneTimeWorkRequest.Builder(RegisterShiftWorker.class).setInitialDelay(5, TimeUnit.SECONDS).build();
                    WorkManager.getInstance().enqueue(registerShift);
                }
                String alarm = shiftInfo.get(ShiftCursorAdapter.COL_ALARM);
                if (alarm != null && alarm.equals("1")) {
                    alarmEnabled = true;
                    String alarmTime = shiftInfo.get(ShiftCursorAdapter.COL_ALARM_TIME);
                    if (alarmTime != null) {
                        alarmTimeArray = alarmTime.split(":");
                        sb.append(String.format(Locale.ENGLISH, " Не забудьте завести будильник на %s.", alarmTime));
                    }
                }
                sb.append(" Удачной вам смены!");
                Notificator notif = new Notificator(getApplicationContext());
                notif.sendShiftNotification(name, "На работку!", sb.toString(), alarmEnabled,alarmTimeArray);
                // если сведения о смене найдены- верну их
            }
            else{
                Log.d("surprise", "CheckTomorrowWorker doWork: tomorrow is holiday");
            }
        }
        // установлю проверку расписания на следующий день
        ForemanHandler.startPlanner();
        return Worker.Result.success();
    }
}
