package net.velor.rdc_utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;

import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.adapters.WorkersAdapter;
import net.velor.rdc_utils.dialogs.DayShiftDialog;
import net.velor.rdc_utils.handlers.ExcelHandler;
import net.velor.rdc_utils.handlers.ScheduleHandler;
import net.velor.rdc_utils.handlers.SharedPreferencesHandler;
import net.velor.rdc_utils.handlers.ShiftsHandler;
import net.velor.rdc_utils.handlers.XMLHandler;
import net.velor.rdc_utils.subclasses.WorkingPerson;
import net.velor.rdc_utils.view_models.ScheduleViewModel;
import net.velor.rdc_utils.workers.UploadScheduleWorker;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

import utils.App;
import utils.LoginActivity;
import utils.Notificator;
import utils.Security;

@SuppressWarnings("NumberEquality")
public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, DayShiftDialog.AnswerDialogListener, NavigationView.OnNavigationItemSelectedListener {
    public static final String FIELD_REMIND_TOMORROW = "remind_tomorrow";
    public static final String FIELD_REMIND_SALARY = "remind_salary";
    public static final String FIELD_WORK_IN_CC = "callcenter";
    public static final String SCHEDULE_MONTH_SELECT = "month_select";
    public static final int SCHEDULE_THIS_MONTH = 1;
    public static final int SCHEDULE_NEXT_MONTH = 2;
    public static final int MENU_ITEM_LOAD_SCHEDULE = 4;
    private static final int READ_REQUEST_CODE = 42;
    private int REQUEST_WRITE_READ = 3;

    // константы имён
    public static final String FIELD_SCHEDULE_CHECK_TIME = "schedule_check_time";

    private static final int MENU_ITEM_SHIFT_SETTINGS = 1;
    private static final int MENU_ITEM_LOAD_SHIFTS = 2;
    private static final int MENU_ITEM_RESET_NAME = 3;

    private static final int PAGE_COUNT = 5;

    public static Calendar sCalendar = Calendar.getInstance();

    //текущие месяц и год
    public static int sYear, sMonth;
    public static int sDirection;
    private ViewPager mPager;
    private boolean sNeedRename;

    // хранилище доступных типов смен
    private HashMap<String, HashMap<String, String>> mShifts = new HashMap<>();
    private DayShiftDialog mDayTypeDialog;
    private ArrayList<String> mShiftValues;
    private ArrayList<Integer> mShiftIds;
    private XMLHandler mXmlHandler;
    private DrawerLayout mDrawer;
    private ScheduleViewModel mMyViewModel;
    private RelativeLayout mRootView;
    private Toolbar mToolbar;

    // переменные для работы с Экселем
    private AlertDialog mSheetLoadingDialog;
    private static XSSFWorkbook sSheet;
    private XSSFSheet mTable;
    private ArrayList<String> mPersonList;
    public static AlertDialog.Builder sShowWorkersDialogBuilder;
    private AlertDialog mSheetUploadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        // определю корневой элемент
        mRootView = findViewById(R.id.rootView);

        // зарегистрирую модель
        mMyViewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);

        // Если не заполнены месяц и год- заполню их текущими значениями
        sCalendar.set(Calendar.DATE, 15);
        if (sYear == 0) {
            sYear = sCalendar.get(Calendar.YEAR);
        }
        if (sMonth == 0) {
            sMonth = sCalendar.get(Calendar.MONTH) + 1;
        }
        // проверю, не активировано ли заполнение расписания
        Intent activatingIntent = getIntent();
        int scheduleAction = activatingIntent.getIntExtra(SCHEDULE_MONTH_SELECT, 0);
        if (scheduleAction == SCHEDULE_THIS_MONTH) {
            closeShiftNotification();
            loadShiftsFromExcel();
        } else if (scheduleAction == SCHEDULE_NEXT_MONTH) {
            sCalendar = Calendar.getInstance();
            sCalendar.set(Calendar.DATE, 15);
            sCalendar.add(Calendar.MONTH, +1);
            sMonth = sCalendar.get(Calendar.MONTH) + 1;
            closeShiftNotification();
            loadShiftsFromExcel();
        }
        // включу поддержку тулбара
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(getDate(sCalendar));
        setSupportActionBar(mToolbar);

        // включу поддержку бокового меню
        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        // зарегистрирую пункты  меню для обработки
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        // todo удалить после тестов
        Log.d("surprise", "MainActivity onCreate 200: load shifts");
        loadShiftsFromExcel();

    }

    private void closeShiftNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(Notificator.TEST_NOTIFICATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Security.isLogged(getApplicationContext())) {
            // перенаправляю на страницу входа
            startActivityForResult(new Intent(this, LoginActivity.class), Security.LOGIN_REQUIRED);
        }
        // загружу данные о сменах, создам диалог выбора типа смены, подключу менеджер страниц
        loadShifts();
    }

/*    private void makeUpdateSnackbar() {
        Snackbar updateSnackbar = Snackbar.make(mRootView, getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMyViewModel.initializeUpdate();
            }
        });
        updateSnackbar.show();
    }*/

    // ===================================== ЗАГРУЗКА ТИПОВ СМЕН ======================================================

    private void loadShifts() {
        // получаю записи типов смен из базы данных
        Cursor shifts = mMyViewModel.getAllShifts();
        // сформирую массив строк для выбора типа дня
        mShiftValues = new ArrayList<>();
        mShiftIds = new ArrayList<>();
        // добавлю строку выходного
        mShiftValues.add(getString(R.string.day_type_holidays));
        mShiftIds.add(-1);
        if (shifts.moveToFirst()) {
            do {
                HashMap<String, String> arr = new HashMap<>();
                // получу идентификатор и название смены
                String id = String.valueOf(shifts.getInt(shifts.getColumnIndex(ShiftCursorAdapter.COL_ID)));
                String name = shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_NAME_FULL));
                arr.put(ShiftCursorAdapter.COL_NAME_FULL, name);
                arr.put(ShiftCursorAdapter.COL_NAME_SHORT, shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_NAME_SHORT)));
                String start = shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_SHIFT_START));
                arr.put(ShiftCursorAdapter.COL_SHIFT_START, start);
                String finish = shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_SHIFT_FINISH));
                arr.put(ShiftCursorAdapter.COL_SHIFT_FINISH, finish);
                arr.put(ShiftCursorAdapter.COL_SHIFT_COLOR, shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_SHIFT_COLOR)));
                arr.put(ShiftCursorAdapter.COL_ALARM, shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_ALARM)));
                arr.put(ShiftCursorAdapter.COL_ALARM_TIME, shifts.getString(shifts.getColumnIndex(ShiftCursorAdapter.COL_ALARM_TIME)));
                mShifts.put(id, arr);
                StringBuilder sb = new StringBuilder();
                sb.append(name);
                if (start != null) {
                    sb.append(" с ");
                    sb.append(start);
                }
                if (finish != null) {
                    sb.append(" до ");
                    sb.append(finish);
                }
                mShiftValues.add(sb.toString());
                mShiftIds.add(Integer.valueOf(id));
            }
            while (shifts.moveToNext());
            shifts.close();
        }
        prepareDayTypeDialog();
    }

    // ===================================== ПОДГОТОВКА ДИАЛОГА ВЫБОРА ТИПА СМЕНЫ ======================================

    private void prepareDayTypeDialog() {
        // подготовлю диалог выбора типа дня
        mDayTypeDialog = new DayShiftDialog();
        Bundle args = new Bundle();
        args.putStringArrayList("values", mShiftValues);
        mDayTypeDialog.setArguments(args);
        if (mPager == null) {
            loadPager();
        } else {
            drawCalendar();
        }
    }

    // ===================================== ПОДКЛЮЧЕНИЕ МЕНЕДЖЕРА СТРАНИЦ =============================================

    private void loadPager() {
        mPager = findViewById(R.id.pager);
        MyFragmentPagerAdapter pagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                if (i == 2) {
                    drawCalendar();
                }
                sNeedRename = !sNeedRename;
                if (i < 2) {
                    sDirection = 1;
                } else {
                    sDirection = 2;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                if (i == ViewPager.SCROLL_STATE_IDLE) {
                    if (sDirection == 1) {
                        if (sMonth == 1) {
                            --sYear;
                            sMonth = 12;
                        } else {
                            --sMonth;
                        }
                        sCalendar.set(Calendar.MONTH, sMonth - 1);
                    } else {
                        if (sMonth == 12) {
                            ++sYear;
                            sMonth = 1;
                        } else {
                            ++sMonth;
                        }
                        sCalendar.set(Calendar.MONTH, sMonth - 1);
                    }
                    mPager.setCurrentItem(2, false);
                    sNeedRename = true;
                    mToolbar.setTitle(getDate(sCalendar));
                }
            }
        });
        mPager.setCurrentItem(2, false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SHIFT_SETTINGS, 0, getString(R.string.shift_settings));
        menu.add(0, MENU_ITEM_LOAD_SHIFTS, 0, getString(R.string.load_shift_settings));
        menu.add(0, MENU_ITEM_RESET_NAME, 0, getString(R.string.reset_name_settings));
        menu.add(0, MENU_ITEM_LOAD_SCHEDULE, 0, getString(R.string.load_schedule_message));

        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_SHIFT_SETTINGS:
                Intent i = new Intent(this, ShiftSettingsActivity.class);
                startActivity(i);
                break;
            case MENU_ITEM_LOAD_SHIFTS:
                loadShiftsFromExcel();
                break;
            case MENU_ITEM_RESET_NAME:
                mMyViewModel.resetName();
                Toast.makeText(this, this.getString(R.string.name_reset_message), Toast.LENGTH_LONG).show();
                break;
            case MENU_ITEM_LOAD_SCHEDULE:
                load_schedule();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void load_schedule() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        startActivityForResult(intent, READ_REQUEST_CODE);
        showSheetUploadingDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == READ_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // The document selected by the user won't be returned in the intent.
                // Instead, a URI to that document will be contained in the return intent
                // provided to this method as a parameter.
                // Pull that URI using resultData.getData().
                Uri uri;
                if (data != null) {
                    uri = data.getData();
                    final MutableLiveData<Integer> status = mMyViewModel.uploadSchedule(uri);
                    status.observe(this, new Observer<Integer>() {
                        @Override
                        public void onChanged(@Nullable Integer integer) {
                            if (integer != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    if (Objects.equals(integer, UploadScheduleWorker.STATUS_UPLOADED_SUCCESS)) {
                                        hideSheetUploadingDialog();
                                        Toast.makeText(MainActivity.this, "Расписание загружено на сервер", Toast.LENGTH_LONG).show();
                                        status.removeObservers(MainActivity.this);
                                    } else if (Objects.equals(integer, UploadScheduleWorker.STATUS_UPLOADED_FAILED)) {
                                        hideSheetUploadingDialog();
                                        Toast.makeText(MainActivity.this, "Расписание не загружено. Проверьте соединение с интернетом", Toast.LENGTH_LONG).show();
                                        status.removeObservers(MainActivity.this);
                                    } else if (Objects.equals(integer, UploadScheduleWorker.STATUS_UPLOADED_ERROR)) {
                                        hideSheetUploadingDialog();
                                        Toast.makeText(MainActivity.this, "Ошибка загрузки расписания", Toast.LENGTH_LONG).show();
                                        status.removeObservers(MainActivity.this);
                                    }
                                } else {
                                    if (integer == UploadScheduleWorker.STATUS_UPLOADED_SUCCESS) {
                                        hideSheetUploadingDialog();
                                        Toast.makeText(MainActivity.this, "Расписание загружено на сервер", Toast.LENGTH_LONG).show();
                                        status.removeObservers(MainActivity.this);
                                    } else if (integer == UploadScheduleWorker.STATUS_UPLOADED_FAILED) {
                                        hideSheetUploadingDialog();
                                        Toast.makeText(MainActivity.this, "Расписание не загружено. Проверьте соединение с интернетом", Toast.LENGTH_LONG).show();
                                        status.removeObservers(MainActivity.this);
                                    } else if (integer == UploadScheduleWorker.STATUS_UPLOADED_ERROR) {
                                        hideSheetUploadingDialog();
                                        Toast.makeText(MainActivity.this, "Ошибка загрузки расписания", Toast.LENGTH_LONG).show();
                                        status.removeObservers(MainActivity.this);
                                    }
                                }
                            }
                        }
                    });
                }
            } else {
                hideSheetUploadingDialog();
                Toast.makeText(this, "Выбор файла расписания отменён", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadShiftsFromExcel() {
        // Загружу смены из таблицы. Сначала проверю, предоставлены ли права чтения накопителя
        if (!mMyViewModel.checkFileRead()) {
            showRightsDialog();
        } else {
            // Покажу диалог загрузки таблицы и начну загружать
            if (sSheet == null) {
                showSheetLoadingDialog();
                final LiveData<Integer> loading = mMyViewModel.downloadSheet();
                loading.observe(this, new Observer<Integer>() {
                    @Override
                    public void onChanged(@Nullable Integer result) {
                        if (result != null) {
                            if (result == App.DOWNLOAD_STATUS_ERROR_DOWNLOAD) {
                                hideSheetLoadingDialog();
                                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.sheet_download_error_message), Toast.LENGTH_LONG).show();
                            } else {
                                final LiveData<XSSFWorkbook> handle = mMyViewModel.handleSheet();
                                // отслежу состояние обработки таблицы
                                handle.observe(MainActivity.this, new Observer<XSSFWorkbook>() {
                                    @Override
                                    public void onChanged(@Nullable XSSFWorkbook sheets) {
                                        if (sheets != null) {
                                            sSheet = sheets;
                                            handle.removeObservers(MainActivity.this);
                                            hideSheetLoadingDialog();
                                            selectMonth();
                                            ExcelHandler handler = new ExcelHandler(sheets);
                                        }
                                    }
                                });

                            }
                            loading.removeObservers(MainActivity.this);
                        }
                    }
                });
                showSheetLoadingDialog();
            } else {
                selectMonth();
            }
        }
    }

    private void selectMonth() {
        ArrayList<String> months = new ArrayList<>();
        // получу имя текущего месяца
        String month = DateFormat.format("LLLL", sCalendar).toString();
        Iterator<Sheet> monthSheets = sSheet.sheetIterator();
        String[] parts;
        Sheet next;
        String value;
        String possibleMonth = "";
        while (monthSheets.hasNext()) {
            next = monthSheets.next();
            value = next.getSheetName();
            months.add(value);
            // разберу имя
            parts = TextUtils.split(value, " ");
            if (parts.length == 2) {
                // проверю, не подходит ли данный месяц для автоматического заполнения
                if (parts[0].toLowerCase().trim().equals(month.toLowerCase().trim()) && parts[1].trim().equals(String.valueOf(sYear))) {
                    possibleMonth = value;
                }
            }
        }
        if (possibleMonth.isEmpty()) {
            showMonthChooseDialog(months);
        } else {
            showPossibleMonthDialog(possibleMonth, months);
        }
    }

    private void selectPerson(String rowName) {
        selectPerson(sSheet.getSheetIndex(rowName));
        //ScheduleHandler.handlePersons(sSheet.getSheetAt(sSheet.getSheetIndex(rowName)));
    }

    private void selectPerson(int which) {
        // выберу нужный лист
        mTable = sSheet.getSheetAt(which);
        // попробую получить сохранённое имя
        String name = SharedPreferencesHandler.getPerson();
        if (mTable != null) {
            Log.d("surprise", "MainActivity selectPerson from here");
            ScheduleHandler.handlePersons(sSheet.getSheetAt(sSheet.getSheetIndex(mTable.getSheetName())));
            // получу список всех работников
            mPersonList = new ArrayList<>();
            int length = mTable.getLastRowNum();
            int counter = 0;
            XSSFCell cell;
            while (counter < length) {
                XSSFRow row = mTable.getRow(counter);
                if (row != null) {
                    cell = row.getCell(0);
                    if (cell != null) {
                        String value = cell.getStringCellValue();
                        if (value != null) {
                            value = value.trim();
                            // если имя совпадает- автоматически подставляю его
                            if (!name.isEmpty() && value.equals(name)) {
                                Log.d("surprise", "MainActivity.java 529 selectPerson: autofill SCHEDULE");
                                makeSchedule(row);
                                return;
                            }
                            if (!value.isEmpty() && !mPersonList.contains(value.trim())){
                                mPersonList.add(value.trim());
                            }
                        }
                    }
                }
                counter++;
            }
            if (!mPersonList.isEmpty()) {
                showPersonChooseDialog(mPersonList);
            } else {
                Toast.makeText(this, "Cant found data", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Cant find month", Toast.LENGTH_LONG).show();
        }
    }

    private void fillPersonName(String name) {
        // получу имя, попытаюсь протестировать его валидность
        Iterator<Row> rows = mTable.rowIterator();
        while (rows.hasNext()){
            Row row = rows.next();
            if (row != null) {
                Cell cell = row.getCell(0);
                if (cell != null && cell.getCellTypeEnum().equals(CellType.STRING)) {
                    String personName = cell.getStringCellValue().trim();
                    if(personName.equals(name)){
                        showPersonConfirmDialog(name, row);
                        return;
                    }
                }
            }
        }
    }

    private void makeSchedule(Row row) {
        // получу расписание
        Iterator<Cell> cells = row.cellIterator();
        // пропущу строку с именем
        cells.next();
        // получу количество дней в выбранном месяце
        int daysCounter = sCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int doubleDaysCounter = daysCounter + 1;
        int counter = 0;
        // составлю массив расписания
        ArrayList<String> scheduleList = new ArrayList<>();
        HashMap<String, String> shiftsList = new HashMap<>();
        Cell cell;
        CellType cellType;
        String value;
        String argbColor = "00000000";
        while (++counter < doubleDaysCounter + 1) {
            cell = row.getCell(counter);
            if (cell != null) {
                cellType = cell.getCellTypeEnum();
                Log.d("surprise", "MainActivity.java 579 makeSchedule: cell type is " + cellType);
                if (cellType.equals(CellType.NUMERIC)) {
                    value = String.valueOf(cell.getNumericCellValue());
                    // получу цвет ячейки для дальнейшего использования при определении разных типов смен
                    org.apache.poi.xssf.usermodel.XSSFColor cellColor = (XSSFColor) cell.getCellStyle().getFillForegroundColorColor();
                    if (cellColor != null) {
                        argbColor = cellColor.getARGBHex();
                        Log.d("surprise", "MainActivity makeSchedule " + argbColor);
                    }
                } else if (cellType.equals(CellType.STRING)) {
                    value = cell.getStringCellValue().trim();
                    // переведу в нижний регистр
                    value = value.toLowerCase();
                    // получу цвет ячейки для дальнейшего использования при определении разных типов смен
                    org.apache.poi.xssf.usermodel.XSSFColor cellColor = (XSSFColor) cell.getCellStyle().getFillForegroundColorColor();
                    if (cellColor != null) {
                        argbColor = cellColor.getARGBHex();
                    }
                } else {
                    value = "";
                }
                scheduleList.add(value);
                Log.d("surprise", "MainActivity.java 600 makeSchedule: add value to schedule " + value);
                if (!TextUtils.isEmpty(value)) {
                    shiftsList.put(value, argbColor);
                }
            } else {
                scheduleList.add("");
                Log.d("surprise", "MainActivity.java 600 makeSchedule: add empty value to schedule ");
            }
        }

        // отправлю список смен на обработку, надо зарегистрировать те, что ещё не зарегистрированы
        mMyViewModel.checkShifts(shiftsList);
        loadShifts();
        mMyViewModel.fillMonth(scheduleList);
        drawCalendar();
    }

    private void showPersonConfirmDialog(final String personName, final Row row) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.confirm_person_dialog_title)
                .setMessage(String.format(this.getString(R.string.confirm_name_message), personName))
                .setPositiveButton(getString(R.string.name_accept_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMyViewModel.savePerson(personName);
                        Toast.makeText(MainActivity.this, getString(R.string.person_saved_message), Toast.LENGTH_LONG).show();
                        makeSchedule(row);
                        if(mSheetLoadingDialog != null){
                            mSheetLoadingDialog.dismiss();
                            mSheetLoadingDialog.hide();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.decline_name_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showPersonChooseDialog(mPersonList);
                    }
                });
        mSheetLoadingDialog = dialogBuilder.create();
        mSheetLoadingDialog.show();
    }

    private void showSheetLoadingDialog() {
        if (mSheetLoadingDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.sheet_loading_dialog_title)
                    .setView(R.layout.sheet_loading_dialog_layout)
                    .setCancelable(false);
            mSheetLoadingDialog = dialogBuilder.create();
        }
        mSheetLoadingDialog.show();
    }

    private void showSheetUploadingDialog() {
        if (mSheetUploadingDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.sheet_uploading_dialog_title)
                    .setView(R.layout.sheet_loading_dialog_layout)
                    .setCancelable(false);
            mSheetUploadingDialog = dialogBuilder.create();
        }
        mSheetUploadingDialog.show();
    }

    private void showPersonChooseDialog(ArrayList<String> personList) {
        Log.d("surprise", "MainActivity.java 673 showPersonChooseDialog: persons list is " + personList);
        mPersonList = personList;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mPersonList);
        // создам диалоговое окно
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.choose_person_dialog_title)
                .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("surprise", "MainActivity.java 673 showPersonChooseDialog:saved persons list is " + mPersonList);
                        Log.d("surprise", "MainActivity.java 680 onClick: selected " + which);
                        String name = mPersonList.get(which);
                        if(name.equals(
                                "Врачи МРТ") ||
                                name.equals("Операторы МРТ") ||
                                name.equals("Администраторы МРТ 1") ||
                                name.equals("Процедурный кабинет") ||
                                name.equals("Наркозы") ||
                                name.equals("УВТ") ||
                                name.equals("Консультативный прием") ||
                                name.equals("Консультанты") ||
                                name.equals("Врач УЗИ") ||
                                name.equals("Call-center") ||
                                name.equals("Колл-центр") ||
                                name.equals("Администраторы МРТ 2")
                        )
                        {
                            Toast.makeText(MainActivity.this, "Выберите своё имя!", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            dialog.dismiss();
                            fillPersonName(name);
                        }

                    }
                });
        dialogBuilder.create().show();
    }


    private void hideSheetLoadingDialog() {
        if (mSheetLoadingDialog != null) {
            mSheetLoadingDialog.hide();
        }
    }

    private void hideSheetUploadingDialog() {
        if (mSheetUploadingDialog != null) {
            mSheetUploadingDialog.hide();
        }
    }

    private void showPossibleMonthDialog(final String date, final ArrayList<String> months) {
        // создам диалоговое окно
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.month_found_dialog_title)
                .setMessage(String.format(getString(R.string.found_month_message), date))
                .setPositiveButton(getString(R.string.date_accept_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // загружу имена из выбранного месяца
                        selectPerson(date);
                    }
                })
                .setNegativeButton(getString(R.string.date_decline_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showMonthChooseDialog(months);
                    }
                });
        dialogBuilder.create().show();
    }

    private void showMonthChooseDialog(ArrayList<String> months) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, months);
        // создам диалоговое окно
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.choose_month_dialog_title)
                .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        selectPerson(which);
                    }
                });
        dialogBuilder.create().show();
    }

    private void showRightsDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.permissions_dialog_title)
                .setMessage("Для использования функции необходимо предоставить доступ к памяти устройства")
                .setCancelable(false)
                .setPositiveButton(R.string.permissions_dialog_positive_answer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_READ);
                    }
                })
                .setNegativeButton(R.string.permissions_dialog_negative_answer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        dialogBuilder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_READ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                loadShiftsFromExcel();
            }
        } else {
            Toast.makeText(this, getString(R.string.rights_required_message), Toast.LENGTH_LONG).show();
        }
    }

    // ===================================== ВЫБОР ТИПА ДНЯ ===================================================
    @Override
    public void onClick(View v) {
        // если нажата кнопка автозаполнения смен- автозаполню смены, иначе покажу информацию о сутках
        if (v.getId() == R.id.autoloadShiftsButton) {
            loadShiftsFromExcel();
        } else {
            showDayInfoDialog(v);
        }
    }

    private void showDayInfoDialog(View v) {
        final int dayId = v.getId();
        String dayType = mXmlHandler.getDayType(String.valueOf(dayId));
        // получу данные о смене
        HashMap<String, String> info = mShifts.get(dayType);
        ConstraintLayout view = (ConstraintLayout) getLayoutInflater().inflate(R.layout.day_info_dialog, mRootView, false);
        String month_s = String.format("%1$tB", sCalendar);
        if (dayType.equals("-1")) {
            ((TextView) view.getViewById(R.id.dayType)).setText(getString(R.string.day_info_type_holiday));
        } else {
            assert info != null;
            TextView dayTypeView = (TextView) view.getViewById(R.id.dayType);
            dayTypeView.setText(info.get(ShiftCursorAdapter.COL_NAME_FULL));
            dayTypeView.setTextColor(Color.parseColor(info.get(ShiftCursorAdapter.COL_SHIFT_COLOR)));
            String shiftBegin = info.get(ShiftCursorAdapter.COL_SHIFT_START);
            if (shiftBegin != null && !shiftBegin.isEmpty()) {
                ((TextView) view.getViewById(R.id.shiftStart)).setText(String.format(Locale.ENGLISH, this.getString(R.string.shift_begin_info), shiftBegin));
            }
            String shiftFinish = info.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
            if (shiftFinish != null && !shiftFinish.isEmpty()) {
                ((TextView) view.getViewById(R.id.shiftEnd)).setText(String.format(Locale.ENGLISH, this.getString(R.string.shift_finish_info), shiftFinish));
            }
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder
                .setTitle(String.format(Locale.ENGLISH, this.getString(R.string.day_info_date), v.getId(), month_s, sYear))
                .setView(view)
                .setNeutralButton(R.string.who_works_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScheduleHandler.discardWorkers();
                        showWorkersDialog(dayId);
                    }
                })
                .setPositiveButton(getString(R.string.register_salary_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ShiftsHandler.registerSalary(dayId);
                    }
                })
                .setNegativeButton(getString(R.string.change_type_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDayTypeDialog.show(getSupportFragmentManager(), String.valueOf(dayId));
                    }
                });

        dialogBuilder.create().show();

    }

    private void showWorkersDialog(int day) {
        // покажу диалоговое окно со списком работающих в этот день
        sShowWorkersDialogBuilder = new AlertDialog.Builder(this);
        sShowWorkersDialogBuilder
                .setTitle(day + " " + sMonth + " " + sYear);
        // загружу и покажу список работающих в этот день
        final LiveData<ArrayList<WorkingPerson>> data = mMyViewModel.showWorkers(day);
        data.observe(MainActivity.this, new Observer<ArrayList<net.velor.rdc_utils.subclasses.WorkingPerson>>() {
            @Override
            public void onChanged(@Nullable ArrayList<WorkingPerson> workingPeople) {
                if (workingPeople != null && workingPeople.size() > 0) {
                    // создам представление
                    LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_show_workers, mRootView, false);
                    RecyclerView recycler = view.findViewById(R.id.workersList);
                    WorkersAdapter adapter = new WorkersAdapter(workingPeople);
                    adapter.notifyDataSetChanged();
                    recycler.setAdapter(adapter);
                    recycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    sShowWorkersDialogBuilder.setView(view).setPositiveButton(getString(android.R.string.ok), null).create().show();
                    data.removeObservers(MainActivity.this);
                }
            }

        });
    }

    @Override
    public boolean onLongClick(View v) {
        mDayTypeDialog.show(getSupportFragmentManager(), String.valueOf(v.getId()));
        return false;
    }

    // ==================================== ТИП ДНЯ ВЫБРАН ====================================================
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int choice) {
        assert dialog.getTag() != null;
        String day = dialog.getTag();
        int id = mShiftIds.get(choice);
        String monthSchedule = mXmlHandler.setDay(day, id);
        mMyViewModel.updateSchedule(String.valueOf(sYear), String.valueOf(sMonth), monthSchedule);
        // перерисую календарь
        drawCalendar();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.salary:
                //  запускаю расписание
                startActivity(new Intent(this, SalaryActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.reserve:
                Intent startReserveIntent = new Intent(this, ReserveActivity.class);
                startReserveIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(startReserveIntent);
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        // закрою боковое меню
        mDrawer.closeDrawer(GravityCompat.START);
        return false;
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {

        SparseArray<Fragment> registeredFragments = new SparseArray<>();

        MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int i) {
            return PageFragment.newInstance(i);
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            if (position == 2) {
                drawCalendar();
            }
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (sNeedRename) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DATE, 15);
                cal.set(Calendar.MONTH, sMonth);
                cal.set(Calendar.YEAR, sYear);
                switch (position) {
                    case 0:
                    case 1:
                        cal.set(Calendar.MONTH, sMonth - 2);
                        break;
                    case 2:
                        cal.set(Calendar.MONTH, sMonth - 1);
                        break;
                }
                return getDate(cal);
            } else {
                // верну текущее имя
                return "";
            }
        }

    /*    public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }*/
    }

    public void appendMonth(final LinearLayout view) {
        LinearLayout parent = view.findViewById(R.id.parent);
        Cursor monthInfo = mMyViewModel.getSchedule(sYear, sMonth);
        mXmlHandler = new XMLHandler(monthInfo);
        CalendarInflater ci = new CalendarInflater(getApplicationContext(), this, this, monthInfo, parent, mXmlHandler, mShifts);
        ScrollView layout = ci.getLayout();
        // найду родительский элемент
        parent.removeAllViews();
        parent.addView(layout);
        monthInfo.close();
// Получу данные о месяце, если они есть
    }

    public void drawCalendar() {
        PageFragment currentPageFragment = PageFragment.registeredFragments.get(2);
        if (currentPageFragment != null) {
            LinearLayout view = (LinearLayout) currentPageFragment.getView();
            if (view != null) {
                appendMonth(view);
            } else {
                delayCalendarDraw();
            }
        } else {
            delayCalendarDraw();
        }
    }

    private void delayCalendarDraw() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                drawCalendar();
            }
        }, 100);
    }


    private String getDate(Calendar calendar) {
        return android.text.format.DateFormat.format("LLLL", calendar) + " " + sYear;
    }

}
