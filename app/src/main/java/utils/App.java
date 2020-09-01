package utils;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.net.Uri;
import android.os.Environment;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.ForemanHandler;
import net.velor.rdc_utils.handlers.SharedPreferencesHandler;
import net.velor.rdc_utils.workers.CheckScheduleChangeWorker;
import net.velor.rdc_utils.workers.LoadSheetWorker;
import net.velor.rdc_utils.workers.ScheduleDownloadWorker;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class App extends Application {
    private static App mAppInstance;
    public Uri updateDownloadUri;
    public boolean updateDownloadInProgress;
    public File downloadedApkFile;
    private DbWork mDatabaseProvider;
    // переменная для хранения модели xlsx
    public static final File DOWNLOAD_FOLDER_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    public final MutableLiveData<XSSFWorkbook> mSheetData = new MutableLiveData<>();
    public final MutableLiveData<Integer> mSheetLoad = new MutableLiveData<>();
    public final MutableLiveData<Integer> mSheetUpload = new MutableLiveData<>();
    public boolean mIsSheetLoading = false;

    // хранилище данных статуса рабочих
    public final MutableLiveData<String> mWorkerStatus = new MutableLiveData<>();

    // статусы загрузки таблицы
    public static final int DOWNLOAD_STATUS_YET_DOWNLOADED = 1;
    public static final int DOWNLOAD_STATUS_SUCCESSFUL_DOWNLOADED = 2;
    public static final int DOWNLOAD_STATUS_ERROR_DOWNLOAD = 1;
    private SharedPreferencesHandler mSharedPreferenceshandler;


    private static final String SCHEDULE_CHECK_TAG = "schedule_check";

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

        // подключу провайдер настроек
         mSharedPreferenceshandler = new SharedPreferencesHandler(this);
         // подключу провайдер уведомлений
        Notificator notificator = new Notificator(this);

        if(!mSharedPreferenceshandler.getNotificationChannelsCreated()){
            // создам каналы
            notificator.createNotificationChannels();
        }

        // запускаю сервис приложения
        //ServiceApplication.startMe(this, ServiceApplication.OPERATION_PLANE_SHIFT_REMINDER);

        // взамен обработки событий в сервисе запущу рабочего, который проверит актуальность данных
        ForemanHandler.startPlanner(true);

        PeriodicWorkRequest myWorkRequest = new PeriodicWorkRequest.Builder(CheckScheduleChangeWorker.class, 20, TimeUnit.MINUTES)
                .addTag(SCHEDULE_CHECK_TAG)
                .build();
        WorkManager wm = WorkManager.getInstance();
        wm.cancelAllWorkByTag(SCHEDULE_CHECK_TAG);
        wm.enqueue(myWorkRequest);

        // поправлю взаимодействие с Экселем, будь он неладен
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
    }

    public DbWork getDatabaseProvider(){
        return mDatabaseProvider;
    }

    public SharedPreferencesHandler getSharedPreferenceshandler(){
        return mSharedPreferenceshandler;
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