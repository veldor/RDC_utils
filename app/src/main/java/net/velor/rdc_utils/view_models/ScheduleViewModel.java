package net.velor.rdc_utils.view_models;

import android.Manifest;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;

import net.velor.rdc_utils.handlers.FileHandler;
import net.velor.rdc_utils.handlers.ScheduleHandler;
import net.velor.rdc_utils.handlers.SharedPreferencesHandler;
import net.velor.rdc_utils.handlers.ShiftsHandler;
import net.velor.rdc_utils.handlers.XMLHandler;
import net.velor.rdc_utils.priv.Priv;
import net.velor.rdc_utils.subclasses.WorkingPerson;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import utils.App;

public class ScheduleViewModel extends ViewModel {

    public Cursor getAllShifts() {
        return App.getInstance().getDatabaseProvider().getAllShifts();
    }

    public Cursor getSchedule(int sYear, int sMonth) {
        return App.getInstance().getDatabaseProvider().getSchedule(sYear, sMonth);
    }

    public void updateSchedule(String valueOf, String valueOf1, String monthSchedule) {
        App.getInstance().getDatabaseProvider().updateSchedule(valueOf, valueOf1, monthSchedule);
    }

    public boolean checkFileRead() {
        App context = App.getInstance();
        int writeResult;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            writeResult = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int readResult = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return (writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    public LiveData<Integer> downloadSheet() {
        return App.getInstance().downloadSheet();
    }

    public boolean sheetExists() {
        return new File(App.DOWNLOAD_FOLDER_LOCATION, Priv.SHEET_FILE_NAME).exists();
    }

    public void checkShifts(HashMap<String, String> shiftsList) {
        ShiftsHandler.checkShift(shiftsList);
    }

    public void fillMonth(ArrayList<String> scheduleList) {
        // запишу данные о месяце
        XMLHandler handler = new XMLHandler(scheduleList);
        handler.save();
    }

    public void savePerson(String personName) {
        SharedPreferencesHandler.savePerson(personName);
    }

    public void resetName() {
        SharedPreferencesHandler.resetPerson();
    }

    public LiveData<XSSFWorkbook> handleSheet() {
        return App.getInstance().handleSheet();
    }

    public LiveData<ArrayList<WorkingPerson>> showWorkers(int day) {
        return ScheduleHandler.showWorkers(day);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void load_schedule() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    }

    public MutableLiveData<Integer> uploadSchedule(Uri uri) {
        FileHandler.uploadSchedule(uri);
        return App.getInstance().mSheetUpload;
    }
}