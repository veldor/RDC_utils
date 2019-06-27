package net.velor.rdc_utils.workers;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import utils.MakeLog;

public class LogWorker extends Worker {
    private static final String BACKUP_DIR_NAME = "rdc_log";
    private static final String LOG_FILE_NAME = "schedules.log";

    public LogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String text = data.getString(MakeLog.LOG_MESSAGE);
        Calendar cal = Calendar.getInstance();
        String time = cal.get(Calendar.HOUR_OF_DAY) + "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND);
        text = time + " " + text;
        File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR_NAME);
        if (!backupDir.exists()) {
            boolean result = backupDir.mkdirs();
            if (result) {
                Log.d("surprise", "ReserveWorker doWork: dir created");
            }
        }
        File log = new File(backupDir, LOG_FILE_NAME);
        // если файла нет- создаю его и записываю туда
        if(!log.exists()){
            try
            {
                boolean result = log.createNewFile();
                if(result){
                    Log.d("surprise", "doWork: файл создан");
                }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(log, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Result.success();
    }
}
