package utils;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.net.Uri;
import android.os.Environment;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.ForemanHandler;
import net.velor.rdc_utils.workers.LoadSheetWorker;
import net.velor.rdc_utils.workers.ScheduleDownloadWorker;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;

public class App extends Application {
    private static App mAppInstance;
    public Uri updateDownloadUri;
    public boolean updateDownloadInProgress;
    public File downloadedApkFile;
    private DbWork mDatabaseProvider;
    // переменная для хранения модели xlsx
    public static final File DOWNLOAD_FOLDER_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    public static final String SHEET_FILE_NAME = "РДЦ_расписание.xlsx";
    public final MutableLiveData<XSSFWorkbook> mSheetData = new MutableLiveData<>();
    public final MutableLiveData<Integer> mSheetLoad = new MutableLiveData<>();
    public boolean mIsSheetLoading = false;

    // статусы загрузки таблицы
    public static final int DOWNLOAD_STATUS_YET_DOWNLOADED = 1;
    public static final int DOWNLOAD_STATUS_SUCCESSFUL_DOWNLOADED = 2;
    public static final int DOWNLOAD_STATUS_ERROR_DOWNLOAD = 1;

    public static App getInstance(){
        return mAppInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        // добавлю файл для автосохранения настроек
        MyBackupAgent.requestBackup();
        // подключу провайдер базы данных
        mDatabaseProvider = new DbWork(getApplicationContext());
        mDatabaseProvider.getConnection();

        // запускаю сервис приложения
        //ServiceApplication.startMe(this, ServiceApplication.OPERATION_PLANE_SHIFT_REMINDER);

        // взамен обработки событий в сервисе запущу рабочего, который проверит актуальность данных
        ForemanHandler.startPlanner();


        // поправлю взаимодействие с Экселем, будь он неладен
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
    }

    public DbWork getDatabaseProvider(){
        return mDatabaseProvider;
    }

    public LiveData<Integer> downloadSheet(){
        // если загрузка таблицы ещё не выполнялась в этой сессии- запущу загрузчик
        OneTimeWorkRequest downloadSchedule = new OneTimeWorkRequest.Builder(ScheduleDownloadWorker.class).build();
        WorkManager.getInstance().enqueue(downloadSchedule);
        return mSheetLoad;
    }

    public LiveData<XSSFWorkbook> handleSheet() {
        // Запущу работника
        OneTimeWorkRequest startLoadSheet = new OneTimeWorkRequest.Builder(LoadSheetWorker.class).build();
        WorkManager.getInstance().enqueue(startLoadSheet);
        // отмечу, что загружается таблица
        mIsSheetLoading = true;
        return mSheetData;
    }
}
