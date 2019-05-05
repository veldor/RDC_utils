package net.velor.rdc_utils.handlers;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.workers.CheckPlannerWorker;

public class ForemanHandler {
    public static void startPlanner(){
        OneTimeWorkRequest startLoadSheet = new OneTimeWorkRequest.Builder(CheckPlannerWorker.class).build();
        WorkManager.getInstance().enqueue(startLoadSheet);
    }
}
