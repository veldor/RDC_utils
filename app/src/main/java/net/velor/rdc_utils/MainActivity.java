package net.velor.rdc_utils;

import android.Manifest;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
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

import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.adapters.WorkersAdapter;
import net.velor.rdc_utils.dialogs.DayShiftDialog;
import net.velor.rdc_utils.handlers.ScheduleHandler;
import net.velor.rdc_utils.handlers.SharedPreferencesHandler;
import net.velor.rdc_utils.handlers.ShiftsHandler;
import net.velor.rdc_utils.handlers.XMLHandler;
import net.velor.rdc_utils.subclasses.WorkingPerson;
import net.velor.rdc_utils.view_models.ScheduleViewModel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import utils.App;
import utils.LoginActivity;
import utils.Security;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, DayShiftDialog.AnswerDialogListener, NavigationView.OnNavigationItemSelectedListener {
    public static final String FIELD_REMIND_TOMORROW = "remind_tomorrow";
    public static final String FIELD_REMIND_SALARY = "remind_salary";
    public static final String FIELD_WORK_IN_CC = "callcenter";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // определю корневой элемент
        mRootView = findViewById(R.id.rootView);

        // Если не заполнены месяц и год- заполню их текущими значениями
        if (sYear == 0) {
            sYear = sCalendar.get(Calendar.YEAR);
        }
        if (sMonth == 0) {
            sMonth = sCalendar.get(Calendar.MONTH) + 1;
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

        // зарегистрирую модель
        mMyViewModel = ViewModelProviders.of(this).get(ScheduleViewModel.class);

        /*// проверю обновления
        // проверю обновления
        final LiveData<Boolean> version = mMyViewModel.startCheckUpdate();
        version.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if (aBoolean != null && aBoolean) {
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar();
                }
            }
        });*/
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

        return super.onCreateOptionsMenu(menu);
    }

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
        }
        return super.onOptionsItemSelected(item);
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
        ScheduleHandler.handlePersons(sSheet.getSheetAt(sSheet.getSheetIndex(rowName)));
    }

    private void selectPerson(int which) {
        // выберу нужный лист
        mTable = sSheet.getSheetAt(which);
        // попробую получить сохранённое имя
        String name = SharedPreferencesHandler.getPerson();
        if (mTable != null) {
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
                        // если имя совпадает- автоматически подставляю его
                        if (!name.isEmpty() && cell.getStringCellValue().equals(name)) {
                            makeSchedule(row);
                            return;
                        }
                        mPersonList.add(cell.getStringCellValue());
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

    private void fillPersonName(int which) {
        // получу имя, попытаюсь протестировать его валидность
        XSSFRow row = mTable.getRow(which);
        if (row != null) {
            XSSFCell cell = row.getCell(0);
            if (cell.getCellTypeEnum().equals(CellType.STRING)) {
                String personName = cell.getStringCellValue();
                showPersonConfirmDialog(personName, row);
            } else {
                Toast.makeText(this, this.getString(R.string.cant_found_person_message), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, this.getString(R.string.cant_found_person_message), Toast.LENGTH_LONG).show();
        }
    }

    private void makeSchedule(XSSFRow row) {
        // получу расписание
        Iterator<Cell> cells = row.cellIterator();
        // пропущу строку с именем
        cells.next();
        // получу количество дней в выбранном месяце
        int daysCounter = sCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        // составлю массив расписания
        ArrayList<String> scheduleList = new ArrayList<>();
        HashSet<String> shiftsList = new HashSet<>();
        Cell cell;
        CellType cellType;
        String value;
        while (cells.hasNext() && daysCounter > 0) {
            cell = cells.next();
            cellType = cell.getCellTypeEnum();
            if (cellType.equals(CellType.NUMERIC)) {
                value = String.valueOf(cell.getNumericCellValue());
            } else if (cellType.equals(CellType.STRING)) {
                value = cell.getStringCellValue().trim();
            } else {
                value = "";
            }
            scheduleList.add(value);
            if (!TextUtils.isEmpty(value)) {
                shiftsList.add(value);
            }
            --daysCounter;
        }
        // отправлю список смен на обработку, надо зарегистрировать те, что ещё не зарегистрированы
        mMyViewModel.checkShifts(shiftsList);
        loadShifts();
        mMyViewModel.fillMonth(scheduleList);
        drawCalendar();
    }

    private void showPersonConfirmDialog(final String personName, final XSSFRow row) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.confirm_person_dialog_title)
                .setMessage(String.format(this.getString(R.string.confirm_name_message), personName))
                .setPositiveButton(getString(R.string.name_accept_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMyViewModel.savePerson(personName);
                        Toast.makeText(MainActivity.this, getString(R.string.person_saved_message), Toast.LENGTH_LONG).show();
                        makeSchedule(row);
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

    private void showPersonChooseDialog(ArrayList<String> personList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, personList);
        // создам диалоговое окно
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.choose_person_dialog_title)
                .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        fillPersonName(which);
                    }
                });
        dialogBuilder.create().show();
    }


    private void hideSheetLoadingDialog() {
        if (mSheetLoadingDialog != null) {
            mSheetLoadingDialog.hide();
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
                if(workingPeople != null && workingPeople.size() > 0){
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
