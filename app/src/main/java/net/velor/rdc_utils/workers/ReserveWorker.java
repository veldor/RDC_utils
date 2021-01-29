package net.velor.rdc_utils.workers;

import android.content.Context;
import android.os.Environment;
import androidx.annotation.NonNull;
import android.util.Log;

import net.velor.rdc_utils.database.DbWork;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReserveWorker extends Worker {

    public static final String OPERATION_NAME = "operation name";
    public static final int OPERATION_START_BACKUP = 1;
    public static final int OPERATION_START_RESTORE = 2;
    private static final String BACKUP_FILE_NAME = "backup.zip";
    private static final int BUFFER = 1024;
    private static final String PREF_BACKUP_NAME = "data1";
    private static final String DB_BACKUP_WAL_NAME = "data2";
    private static final String DB_BACKUP_SHM_NAME = "data3";
    private static final String DB_BACKUP_NAME = "data4";
    private static final String BACKUP_DIR_NAME = "RDC";
    public static final String OPERATION_RESULT = "operation result";
    public static final int RESULT_NOT_FILE = 1;
    public static final int RESULT_RESTORED = 2;
    private static final String DB_SHM_NAME = "myDb-shm";
    private static final String DB_WAL_NAME = "myDb-wal";

    public ReserveWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        Context context = getApplicationContext();
        int workType = data.getInt(OPERATION_NAME, 0);
        if (workType == OPERATION_START_BACKUP) {
            // получу доступ к файлам
            // подготовлю ZIP
            try {
                File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR_NAME);
                if (!backupDir.exists()) {
                    boolean result = backupDir.mkdirs();
                    if (result) {
                        Log.d("surprise", "ReserveWorker doWork: dir created");
                    }
                }
                File zip = new File(backupDir, BACKUP_FILE_NAME);
                FileOutputStream dest = new FileOutputStream(zip);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                byte[] dataBuffer = new byte[BUFFER];
                File sharedPrefsFile;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    sharedPrefsFile = new File(context.getDataDir() + "/shared_prefs/net.velor.rdc_utils_preferences.xml");
                } else {
                    sharedPrefsFile = new File(Environment.getDataDirectory() + "/shared_prefs/net.velor.rdc_utils_preferences.xml");
                }
                writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME);
                File walDb = context.getDatabasePath(DB_WAL_NAME);
                writeToZip(out, dataBuffer, walDb, DB_BACKUP_WAL_NAME);
                File shmDb = context.getDatabasePath(DB_SHM_NAME);
                writeToZip(out, dataBuffer, shmDb, DB_BACKUP_SHM_NAME);
                File dbFile = context.getDatabasePath(DbWork.DB_NAME);
                writeToZip(out, dataBuffer, dbFile, DB_BACKUP_NAME);
                out.close();
                return Result.success();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (workType == OPERATION_START_RESTORE) {
            // попробую найти файл с настройками
            File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR_NAME);
            File zip = new File(backupDir, BACKUP_FILE_NAME);
            if (zip.exists()) {
                // получу данные для восстановления
                try {
                    FileInputStream fin = new FileInputStream(zip);
                    ZipInputStream zin = new ZipInputStream(fin);
                    ZipEntry ze;
                    File targetFile;
                    while ((ze = zin.getNextEntry()) != null) {
                        switch (ze.getName()) {
                            case PREF_BACKUP_NAME:
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    targetFile = new File(context.getDataDir() + "/shared_prefs/net.velor.rdc_utils_preferences.xml");
                                } else {
                                    targetFile = new File(Environment.getDataDirectory() + "/shared_prefs/net.velor.rdc_utils_preferences.xml");
                                }
                                extractFromZip(zin, targetFile);
                                break;
                            case DB_BACKUP_WAL_NAME:
                                targetFile = context.getDatabasePath(DB_WAL_NAME);
                                extractFromZip(zin, targetFile);
                                break;
                            case DB_BACKUP_SHM_NAME:
                                targetFile = context.getDatabasePath(DB_SHM_NAME);
                                extractFromZip(zin, targetFile);
                                break;
                            case DB_BACKUP_NAME:
                                targetFile = context.getDatabasePath(DbWork.DB_NAME);
                                extractFromZip(zin, targetFile);
                                break;
                        }
                    }
                    zin.close();
                    Log.d("surprise", "ReserveWorker doWork: restore done!");
                    Data outputData = new Data.Builder()
                            .putInt(OPERATION_RESULT, RESULT_RESTORED)
                            .build();
                    return Result.success(outputData);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Data outputData = new Data.Builder()
                        .putInt(OPERATION_RESULT, RESULT_NOT_FILE)
                        .build();
                return Result.success(outputData);
            }
        }
        return Result.failure();
    }

    private void writeToZip(ZipOutputStream stream, byte[] dataBuffer, File oldFileName, String newFileName) {
        Log.d("surprise", "ReserveWorker writeToZip: write to zip operation by file " + oldFileName);
        if (oldFileName.exists()) {
            Log.d("surprise", "ReserveWorker writeToZip: " + oldFileName + " exists");
            FileInputStream fis;
            try {
                fis = new FileInputStream(oldFileName);
                BufferedInputStream origin = new BufferedInputStream(fis, BUFFER);
                ZipEntry entry = new ZipEntry(newFileName);
                stream.putNextEntry(entry);
                int count;

                while ((count = origin.read(dataBuffer, 0, BUFFER)) != -1) {
                    stream.write(dataBuffer, 0, count);
                }
                origin.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void extractFromZip(ZipInputStream zis, File fileName) {
        try {
            Log.d("surprise", "ReserveWorker extractFromZip: restore " + fileName);
            FileOutputStream fout = new FileOutputStream(fileName);
            for (int c = zis.read(); c != -1; c = zis.read()) {
                fout.write(c);
            }
            zis.closeEntry();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
