package utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.workers.LogWorker;
import net.velor.rdc_utils.workers.ReserveWorker;


public class MakeLog {
    public static final String LOG_MESSAGE = "log_message";

    public static void writeToLog(String msg){
        App context = App.getInstance();
        int writeResult = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readResult = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED){
            Data inputData = new Data.Builder()
                    .putString(LOG_MESSAGE, msg)
                    .build();
            OneTimeWorkRequest writeLogWork = new OneTimeWorkRequest.Builder(LogWorker.class).setInputData(inputData).build();
            WorkManager.getInstance().enqueue(writeLogWork);
            Log.d("surprise", "writeToLog: write");
        }
        else{
            Log.d("surprise", "writeToLog: no permissions");
        }
    }
}
