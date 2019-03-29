package utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupManager;
import android.app.backup.FileBackupHelper;

class MyBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate() {
        super.onCreate();
        FileBackupHelper fileBackupHelper = new FileBackupHelper(App.getInstance(), "../databases/myDb");
        addHelper("myDb", fileBackupHelper);
    }

    public static void requestBackup() {
        BackupManager backupManager = new BackupManager(App.getInstance());
        backupManager.dataChanged();
    }
}
