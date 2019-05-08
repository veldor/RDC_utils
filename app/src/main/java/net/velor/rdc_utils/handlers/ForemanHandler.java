package net.velor.rdc_utils.handlers;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import net.velor.rdc_utils.workers.CheckPlannerWorker;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ForemanHandler {
    public static void startPlanner(boolean reload){
        OneTimeWorkRequest.Builder startPlanner = new OneTimeWorkRequest.Builder(CheckPlannerWorker.class);
        if(reload){
            Data myData = new Data.Builder()
                    .putBoolean(CheckPlannerWorker.RELOAD_MARK, true)
                    .build();
            startPlanner.setInputData(myData);
        }
        WorkManager.getInstance().enqueue(startPlanner.build());
    }

    public static boolean isMyWorkerRunning(String tag) {
        List<WorkInfo> status = null;
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
