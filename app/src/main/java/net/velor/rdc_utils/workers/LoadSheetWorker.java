package net.velor.rdc_utils.workers;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import utils.App;

public class LoadSheetWorker extends Worker {
    public LoadSheetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        App app = App.getInstance();
        // теперь- чтение
        File file = new File(App.DOWNLOAD_FOLDER_LOCATION, App.SHEET_FILE_NAME);
        if(file.exists()){
            try {
                XSSFWorkbook excelBook;
                try{
                    OPCPackage opcPackage = OPCPackage.open(file);
                    excelBook = new XSSFWorkbook(opcPackage);
                }
                catch (Exception e){
                    excelBook = new XSSFWorkbook(new FileInputStream(file));
                }
                app.mSheetData.postValue(excelBook);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                App.getInstance().mIsSheetLoading = false;
            }
        }
        return Result.success();
    }
}
