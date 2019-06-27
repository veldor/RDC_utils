package net.velor.rdc_utils.handlers;

import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import net.velor.rdc_utils.workers.CheckPlannerWorker;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ForemanHandler {
    private static final String PERIODIC_CHECK_TAG = "periodic_check";

    public static void startPlanner(boolean reload){
        // запущу периодически повторяющийся проверяльщик
        PeriodicWorkRequest.Builder startPeriodicalPlanner = new PeriodicWorkRequest.Builder(CheckPlannerWorker.class, 30, TimeUnit.MINUTES).addTag(PERIODIC_CHECK_TAG);

        OneTimeWorkRequest.Builder startPlanner = new OneTimeWorkRequest.Builder(CheckPlannerWorker.class);
        if(reload){
            Data myData = new Data.Builder()
                    .putBoolean(CheckPlannerWorker.RELOAD_MARK, true)
                    .build();
            startPeriodicalPlanner.setInputData(myData);
            startPlanner.setInputData(myData);
        }
        WorkManager wm = WorkManager.getInstance();
        wm.cancelAllWorkByTag(PERIODIC_CHECK_TAG);
        wm.enqueue(startPlanner.build());
        wm.enqueue(startPeriodicalPlanner.build());
        // перепроверю регистрацию смены
        SalaryHandler.planeRegistration();
        Log.d("surprise", "ForemanHandler startPlanner: checked");
    }

    public static boolean isMyWorkerRunning(String tag) {
        List<WorkInfo> status;
        try {
            status = WorkManager.getInstance().getWorkInfosByTag(tag).get();
            for (WorkInfo workStatus : status) {
                if (workStatus.getState() == WorkInfo.State.RUNNING
                        || workStatus.getState() == WorkInfo.State.ENQUEUED) {
                    return true;
                }
            }
            return false;

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }
}
