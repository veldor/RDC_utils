package net.velor.rdc_utils.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.handlers.SharedPreferencesHandler;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CheckPlannerWorker extends Worker {
    private static final String MY_TAG = "tomorrow_check_planner";

    public CheckPlannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Calendar cal = Calendar.getInstance();
        // установлю время проверки завтрашней смены
        String time = SharedPreferencesHandler.getScheduleCheckTime();
        assert time != null;
        String[] time_array = time.split(":");
        int hour = Integer.valueOf(time_array[0]);
        int minutes = Integer.valueOf(time_array[1]);

        // Проверю, если позже времени установки проверяльщика- установлю его на следующий день
        int now_hour = cal.get(Calendar.HOUR_OF_DAY);
        if (now_hour > hour || (now_hour == hour && cal.get(Calendar.MINUTE) >= minutes)) {
            cal.add(Calendar.DATE, 1);
            Log.d("surprise", "CheckPlannerWorker doWork: check planned on tomorrow");
        }
        else{
            Log.d("surprise", "CheckPlannerWorker doWork: check planned on today");
        }

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);

        // определю, через какое время назначить проверку
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long plannedTime = cal.getTimeInMillis();
        long difference = plannedTime - currentTime;
        Log.d("surprise", "CheckPlannerWorker doWork: diff is " + difference);

        // назначу запуск рабочего, который проверит расписание на завтра
        OneTimeWorkRequest checkTomorrow = new OneTimeWorkRequest.Builder(CheckTomorrowWorker.class).addTag(MY_TAG).setInitialDelay(difference, TimeUnit.MILLISECONDS).build();
        //OneTimeWorkRequest checkTomorrow = new OneTimeWorkRequest.Builder(CheckTomorrowWorker.class).addTag(MY_TAG).setInitialDelay(5, TimeUnit.SECONDS).build();
        WorkManager.getInstance().cancelAllWorkByTag(MY_TAG);
        WorkManager.getInstance().enqueue(checkTomorrow);
        return Worker.Result.success();
    }
}
