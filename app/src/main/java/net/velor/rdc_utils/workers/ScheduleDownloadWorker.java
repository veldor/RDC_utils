package net.velor.rdc_utils.workers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.R;
import net.velor.rdc_utils.priv.Priv;

import java.io.File;

import utils.App;

public class ScheduleDownloadWorker extends Worker {
    private long mDownloadId;

    public ScheduleDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if(App.getInstance().mIsSheetLoading){
            App.getInstance().mSheetLoad.postValue(App.DOWNLOAD_STATUS_YET_DOWNLOADED);
        }
        else{
            String downloadedApkFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + Priv.SHEET_FILE_NAME;
            File scheduleFile = new File(downloadedApkFilePath);
            if(scheduleFile.exists()){
                // удалю файл, если он существует
                boolean deleteResult = scheduleFile.delete();
                if(!deleteResult){
                    Log.d("surprise", "ScheduleDownloadWorker doWork: error when delete previous sheet file");
                }
            }
            Uri downloadUri = Uri.parse("file://" + scheduleFile);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(Priv.LOADED_FILE_LINK));
            request.setTitle(App.getInstance().getString(R.string.download_schedule_message));
            request.setDestinationUri(downloadUri);
            DownloadManager manager = (DownloadManager) App.getInstance().getSystemService(
                    Context.DOWNLOAD_SERVICE);
            mDownloadId = manager.enqueue(request);
            DownloadReceiver downloadObserver = new DownloadReceiver();
            App.getInstance().registerReceiver(downloadObserver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        return Result.success();
    }
    // класс-обработчик события завершения загрузки
    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long finishedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(finishedDownloadId == mDownloadId){
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(finishedDownloadId);
                DownloadManager manager = (DownloadManager) App.getInstance().getSystemService(
                        Context.DOWNLOAD_SERVICE);
                Cursor cursor = manager.query(query);
                if(cursor.moveToFirst()){
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // уведомлю о завершении скачивания
                        App.getInstance().mSheetLoad.postValue(App.DOWNLOAD_STATUS_SUCCESSFUL_DOWNLOADED);
                        App.getInstance().mIsSheetLoading = true;
                    }
                    else{
                        App.getInstance().mSheetLoad.postValue(App.DOWNLOAD_STATUS_ERROR_DOWNLOAD);
                    }
                }
                else{
                    App.getInstance().mSheetLoad.postValue(App.DOWNLOAD_STATUS_ERROR_DOWNLOAD);
                }
                context.unregisterReceiver(this);
            }
        }
    }

}
