package net.velor.rdc_utils.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.handlers.ForemanHandler;
import net.velor.rdc_utils.handlers.SalaryHandler;
import net.velor.rdc_utils.handlers.SharedPreferencesHandler;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import utils.App;
import utils.MakeLog;

import static java.util.Calendar.HOUR_OF_DAY;

public class CheckPlannerWorker extends Worker {
    public static final String RELOAD_MARK = "reload";
    private static final String MY_TAG = "tomorrow_check_planner";
    public static final String SALARY_REGISTER_TAG = "salary_register";

    public CheckPlannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // если задание уже запланировано- ничего не делаю
        Data params = getInputData();
        boolean reloadParam = params.getBoolean(RELOAD_MARK, false);
        if (!reloadParam && ForemanHandler.isMyWorkerRunning(MY_TAG)) {
            App.getInstance().mWorkerStatus.postValue("Проверка уже запланирована");
        } else {
            Calendar cal = Calendar.getInstance();
            // установлю время проверки завтрашней смены
            String time = SharedPreferencesHandler.getScheduleCheckTime();
            assert time != null;
            String[] time_array = time.split(":");
            int hour = Integer.valueOf(time_array[0]);
            int minutes = Integer.valueOf(time_array[1]);

            // Проверю, если позже времени установки проверяльщика- установлю его на следующий день
            int now_hour = cal.get(HOUR_OF_DAY);
            if (now_hour > hour || (now_hour == hour && cal.get(Calendar.MINUTE) >= minutes)) {
                cal.add(Calendar.DATE, 1);
            }

            cal.set(HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minutes);
            cal.set(Calendar.SECOND, 0);

            // определю, через какое время назначить проверку
            long currentTime = Calendar.getInstance().getTimeInMillis();
            long plannedTime = cal.getTimeInMillis();
            long difference = plannedTime - currentTime;
            // назначу запуск рабочего, который проверит расписание на завтра
            OneTimeWorkRequest checkTomorrow = new OneTimeWorkRequest.Builder(CheckTomorrowWorker.class).addTag(MY_TAG).setInitialDelay(difference, TimeUnit.MILLISECONDS).build();
            WorkManager.getInstance().enqueueUniqueWork(MY_TAG, ExistingWorkPolicy.REPLACE, checkTomorrow);
            App.getInstance().mWorkerStatus.postValue("Запланировал проверку");
            MakeLog.writeToLog("Запланирована проверка через " + TimeUnit.MILLISECONDS.toHours(difference) + " часов");
        }
        if (SalaryHandler.planeRegistration()) {
            Log.d("surprise", "doWork: регистрация зарплаты запланирована");
        }
        return Worker.Result.success();
    }
}
