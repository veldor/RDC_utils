package utils;

import android.app.Application;
import android.net.Uri;

import java.io.File;

public class App extends Application {
    private static App mAppInstance;
    public Uri updateDownloadUri;
    public boolean updateDownloadInProgress;
    public File downloadedApkFile;

    public static App getInstance(){
        return mAppInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        // добавлю файл для автосохранения настроек
        MyBackupAgent.requestBackup();
    }
}
