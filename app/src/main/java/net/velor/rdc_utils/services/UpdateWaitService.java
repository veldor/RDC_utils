package net.velor.rdc_utils.services;

import android.app.DownloadManager;
import android.app.Service;
import androidx.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import net.velor.rdc_utils.updates.Updater;

import java.io.File;

import utils.App;

public class UpdateWaitService extends Service {

    private Long mDownloadId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // получу идентификатор загрузки
        MutableLiveData<Long> identifier = Updater.updateDownloadIdentification;
        if(identifier == null){
            Log.d("surprise", "UpdateWaitService onStartCommand: not found download identifier");
            stopSelf();
        }
        else{
            mDownloadId = identifier.getValue();
            // Регистрирую сервис для приёма статуса загрузки обновления
            DownloadReceiver downloadObserver = new DownloadReceiver();
            this.registerReceiver(downloadObserver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class DownloadReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            long finishedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(finishedDownloadId == mDownloadId){
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(finishedDownloadId);
                DownloadManager manager = (DownloadManager) getApplication().getSystemService(
                        Context.DOWNLOAD_SERVICE);
                Cursor cursor = manager.query(query);
                if(cursor.moveToFirst()){
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        //open the downloaded file
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Uri downloadUri = App.getInstance().updateDownloadUri;
                        Log.d("surprise", "DownloadReceiver onReceive: file is " + App.getInstance().downloadedApkFile);
                        if (Build.VERSION.SDK_INT >= 24) {
                            downloadUri = FileProvider.getUriForFile(context,
                                    "net.velor.rdc_utils.fileProvider",
                                    App.getInstance().downloadedApkFile);
                        }
                        install.setDataAndType(downloadUri,
                        manager.getMimeTypeForDownloadedFile(mDownloadId));
                        Log.d("surprise", "DownloadReceiver onReceive: trying install update");
                        context.startActivity(install);
                    }
                    else{
                        clearFile();
                    }
                }
                else{
                    clearFile();
                }
                App.getInstance().downloadedApkFile = null;
                App.getInstance().updateDownloadUri = null;
                App.getInstance().updateDownloadInProgress = false;
                context.unregisterReceiver(this);
                stopSelf();
            }
        }

        private void clearFile() {
            // удалю файл, если он создался
            File file = App.getInstance().downloadedApkFile;
            if(file != null){
                if(file.exists()){
                    boolean deleteResult = file.delete();
                    if(!deleteResult){
                        Log.d("surprise", "DownloadReceiver onReceive: не удалось удалить загруженный файл");
                    }
                }
            }
            Toast.makeText(getApplicationContext(), "update loading failed", Toast.LENGTH_LONG).show();
        }
    }
}
