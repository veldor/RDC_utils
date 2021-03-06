package net.velor.rdc_utils.view_models;

import android.Manifest;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.pm.PackageManager;
import android.util.Log;

import net.velor.rdc_utils.workers.ReserveWorker;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import utils.App;

import static net.velor.rdc_utils.workers.ReserveWorker.OPERATION_NAME;
import static net.velor.rdc_utils.workers.ReserveWorker.OPERATION_START_BACKUP;
import static net.velor.rdc_utils.workers.ReserveWorker.OPERATION_START_RESTORE;


public class ReserveViewModel extends ViewModel {
    private MutableLiveData<Boolean> haveRights = new MutableLiveData<>();

    public LiveData<Boolean> checkRights() {
        // проверю права доступа
        App context = App.getInstance();
        int writeResult;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            writeResult = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readResult = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        haveRights.setValue(writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED);
    }
        return haveRights;
    }

    public LiveData<WorkInfo> doBackup() {
        if(checkRights().getValue() != null){
            Log.d("surprise", "ReserveViewModel doBackup: start backup");
            Data inputData = new Data.Builder()
                    .putInt(OPERATION_NAME, OPERATION_START_BACKUP)
                    .build();
            OneTimeWorkRequest loadCountersWork = new OneTimeWorkRequest.Builder(ReserveWorker.class).setInputData(inputData).build();
            WorkManager.getInstance(App.getInstance()).enqueue(loadCountersWork);
            // отслежу выполнение задачи
            return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(loadCountersWork.getId());
        }
        return null;
    }

    public LiveData<WorkInfo> doRestore() {
        if(checkRights().getValue() != null){
            Log.d("surprise", "ReserveViewModel doRestore: start restore");
            Data inputData = new Data.Builder()
                    .putInt(OPERATION_NAME, OPERATION_START_RESTORE)
                    .build();
            OneTimeWorkRequest loadCountersWork = new OneTimeWorkRequest.Builder(ReserveWorker.class).setInputData(inputData).build();
            WorkManager.getInstance(App.getInstance()).enqueue(loadCountersWork);
            return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(loadCountersWork.getId());
        }
        return null;
    }
}
