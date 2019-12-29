package net.velor.rdc_utils.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.handlers.FileHandler;
import net.velor.rdc_utils.handlers.SharedPreferencesHandler;
import net.velor.rdc_utils.priv.Priv;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.InputStream;

import utils.App;

public class UploadScheduleWorker extends Worker {
    public static final Integer STATUS_UPLOADED_SUCCESS = 1;
    public static final Integer STATUS_UPLOADED_FAILED = 2;
    public static final Integer STATUS_UPLOADED_ERROR = 3;

    public UploadScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(Priv.FTP_URL, 21);
            String password = SharedPreferencesHandler.getFTPPassword();
            boolean bool = ftpClient.login(Priv.FTP_LOGIN, password);
            if (bool) {
                ftpClient.changeWorkingDirectory(Priv.SCHEDULE_DIR);
                ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
                InputStream inputStream = App.getInstance().getContentResolver().openInputStream(FileHandler.file_uri);
                ftpClient.enterLocalPassiveMode();
                boolean result = ftpClient.storeFile(Priv.REMOTE_SCHEDULE_NAME, inputStream);
                if (result) {
                    App.getInstance().mSheetUpload.postValue(STATUS_UPLOADED_SUCCESS);
                }
                else{
                    App.getInstance().mSheetUpload.postValue(STATUS_UPLOADED_FAILED);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            App.getInstance().mSheetUpload.postValue(STATUS_UPLOADED_ERROR);
        }
        return Result.success();
    }
}
