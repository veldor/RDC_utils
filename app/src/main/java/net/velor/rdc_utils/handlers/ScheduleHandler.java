package net.velor.rdc_utils.handlers;

import android.arch.lifecycle.MutableLiveData;
import android.database.Cursor;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.subclasses.WorkingPerson;
import net.velor.rdc_utils.workers.CheckPersonsWorker;
import net.velor.rdc_utils.workers.GetWorkersWorker;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

import utils.App;

public class ScheduleHandler {
    public static final String CHECKED_DAY = "day";
    public static XSSFSheet sSheet;
    private static MutableLiveData<ArrayList<WorkingPerson>> mWorkersData = new MutableLiveData<>();

    public static void handlePersons(XSSFSheet sheet) {
        // запущу рабочего, который сделает всё как надо :)
        sSheet = sheet;
        OneTimeWorkRequest.Builder personsPlanner = new OneTimeWorkRequest.Builder(CheckPersonsWorker.class);
        WorkManager.getInstance().enqueue(personsPlanner.build());
    }

    public static void addPersonToBase(String post, String person) {
        DbWork db = App.getInstance().getDatabaseProvider();
        db.insertPerson(post, person);
    }

    public static void addDayToSchedule(int day, String person, String post, String value) {
        DbWork db = App.getInstance().getDatabaseProvider();
        db.insertDayToSchedule(post, person, day, value);
    }

    public static MutableLiveData<ArrayList<WorkingPerson>> showWorkers(int day) {
        // запущу рабочего, который сформирует список работающих в этот день и покажет в диалоге
        Data myData = new Data.Builder()
                .putInt(CHECKED_DAY, day)
                .build();
        OneTimeWorkRequest.Builder getWorkersWorker = new OneTimeWorkRequest.Builder(GetWorkersWorker.class).setInputData(myData);
        WorkManager.getInstance().enqueue(getWorkersWorker.build());
        return mWorkersData;
    }

    public static void getWorkers(int day) {
        DbWork db = App.getInstance().getDatabaseProvider();
        Cursor workers = db.getWorkers(day);
        ArrayList<WorkingPerson> persons = new ArrayList<>();
        if (workers.moveToFirst()) {
            do {
                WorkingPerson person = new WorkingPerson();
                person.name = workers.getString(workers.getColumnIndex(DbWork.COL_PERSON_NAME));
                person.role = workers.getString(workers.getColumnIndex(DbWork.COL_PERSON_POST));
                person.shift_type = workers.getString(workers.getColumnIndex(DbWork.COL_SCHEDULE_TYPE));
                persons.add(person);
            }
            while (workers.moveToNext());
        }
        mWorkersData.postValue(persons);
        workers.close();
    }

    public static void discardWorkers() {
        mWorkersData.setValue(null);
    }
}
