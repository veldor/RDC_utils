package net.velor.rdc_utils.handlers;

import android.net.Uri;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.workers.UploadScheduleWorker;

public class FileHandler {
    public static Uri file_uri;
    private static final int READ_REQUEST_CODE = 42;

    public static void uploadSchedule(Uri uri) {
        file_uri = uri;
        // запущу рабочего
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest myWorkRequest = new OneTimeWorkRequest.Builder(UploadScheduleWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance().enqueue(myWorkRequest);
    }


}
