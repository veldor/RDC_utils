package net.velor.rdc_utils;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.SalaryHandler;
import net.velor.rdc_utils.widgets.SalaryWidget;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

import utils.App;
import utils.LoginActivity;
import utils.Notificator;
import utils.Security;

public class SalaryDayActivity extends AppCompatActivity {

    public static final String HAS_INFO = "has_info";
    public static final String DAY_FIELD = "day";
    public static final String YEAR_FIELD = "year";
    public static final String MONTH_FIELD = "month";
    public static final String DURATION = "duration";

    public static final String ID = "id";
    private static final int MENU_SAVE_ID = 1;
    private static final int MENU_CANCEL_ID = 2;
    private static final int MENU_DELETE_ID = 3;
    private static String sYearValue, sMonthValue, sDayValue, sCenterValue, sRevenueValue;
    private static int sDurationValue, sContrastsValue, sDynContrastsValue, sOsValue;
    private Button mDateBtn;
    private Button mCenterBtn;
    private Button mDurationBtn;
    private Button mContrastsBtn;
    private Button mOncoscreeningsBtn;
    private Button mDynamicContrastsBtn;
    public static final String[] CENTERS_LIST = {"Аврора", "Нижневолжская набережная"};
    private NumberPicker mNumberPicker;
    private DbWork mDb;
    private View mView;
    private SharedPreferences mPrefsManager;
    private EditText mRevenueInput;
    private static boolean sDataRewrite;
    private long mCurrentId;
    private Intent mIntent;
    private View mRoot;
    private String mMonthName;
    private Calendar mCalendar;
    private boolean mIsCc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_salary_day);
        mRoot = findViewById(R.id.rootView);
        // подключусь к базе данных
        mDb = App.getInstance().getDatabaseProvider();

        mIntent = getIntent();
        mCurrentId = mIntent.getLongExtra(ID, 0);
        mView = findViewById(android.R.id.content);
        // создам необходимые переменные
        mDateBtn = findViewById(R.id.shift_date);
        mCalendar = Calendar.getInstance();
        mCenterBtn = findViewById(R.id.shift_place);
        mDurationBtn = findViewById(R.id.shift_duration);
        mContrastsBtn = findViewById(R.id.contrasts_btn);
        mDynamicContrastsBtn = findViewById(R.id.dyn_contrasts_button);
        mRevenueInput = findViewById(R.id.day_revenue);
        mOncoscreeningsBtn = findViewById(R.id.oncoscreenings_button);
        final TextInputLayout revenueInputParent = findViewById(R.id.revenueParent);
        mRevenueInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString();
                Pattern ptn = Pattern.compile("\\d+.?\\d{0,2}");
                if (ptn.matcher(value).matches()) {
                    sRevenueValue = value;
                    revenueInputParent.setErrorEnabled(false);
                } else {
                    sRevenueValue = null;
                    revenueInputParent.setError("Неверная сумма");
                    revenueInputParent.setErrorEnabled(true);
                }
                checkReady();
                invalidateOptionsMenu();
            }
        });


        mPrefsManager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // проверю отображение строки с онкоскринингами
        boolean showOs = mPrefsManager.getBoolean(SalaryActivity.FIELD_SHOW_ONCOSCREENINGS, false);
        if (!showOs) {
            mOncoscreeningsBtn.setVisibility(View.GONE);
        }

        // определю, не нужно ли считать зарплату колл-центру
        mIsCc = mPrefsManager.getBoolean(MainActivity.FIELD_WORK_IN_CC, false);

        if(mIsCc){
            mContrastsBtn.setVisibility(View.GONE);
            mDynamicContrastsBtn.setVisibility(View.GONE);
            mCenterBtn.setVisibility(View.GONE);
            sCenterValue = CENTERS_LIST[0];
            sRevenueValue = "0";
        }

        loadInfo(mCurrentId);

        // получу shared prefs

    }

    private void delete() {
        mDb.deleteSalaryShift(mCurrentId);
        SalaryWidget.forceUpdateWidget();
        Toast.makeText(getApplicationContext(), "Смена удалена", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Security.isLogged(getApplicationContext())) {
            // перенаправляю на страницу входа
            startActivityForResult(new Intent(this, LoginActivity.class), Security.LOGIN_REQUIRED);
        }

        // перепроверю регистрацию смены
        SalaryHandler.planeRegistration();
    }

    private void loadInfo(final long currentId) {
        if (!sDataRewrite) {
            boolean outerInfo = mIntent.getBooleanExtra(HAS_INFO, false);
            if (currentId != 0) {
                Cursor info = mDb.getSalaryDay(currentId);
                if (info.moveToFirst()) {
                    // восстановление настроек
                    String year = info.getString(info.getColumnIndex(DbWork.COL_YEAR));
                    String month = info.getString(info.getColumnIndex(DbWork.COL_MONTH));
                    int correctedMonth = Integer.parseInt(month) + 1;
                    String day = info.getString(info.getColumnIndex(DbWork.SD_COL_DAY));
                    sYearValue = year;
                    sMonthValue = month;
                    sDayValue = day;
                    mCalendar.set(Calendar.YEAR, Integer.parseInt(year));
                    mCalendar.set(Calendar.MONTH, Integer.parseInt(month));
                    mCalendar.set(Calendar.DATE, Integer.parseInt(day));
                    mMonthName = String.format("%1$tB", mCalendar);
                    mDateBtn.setText(String.format(Locale.ENGLISH, "%s %d %s года", mMonthName, correctedMonth, year));
                    makeButtonReady(mDateBtn);

                    String center = info.getString(info.getColumnIndex(DbWork.SD_COL_CENTER));
                    mCenterBtn.setText(center);
                    sCenterValue = center;
                    makeButtonReady(mCenterBtn);

                    int duration = info.getInt(info.getColumnIndex(DbWork.SD_COL_DURATION));
                    mDurationBtn.setText(String.format(Locale.ENGLISH, "%d часов", duration));
                    sDurationValue = duration;
                    makeButtonReady(mDurationBtn);

                    int contrast = info.getInt(info.getColumnIndex(DbWork.SD_COL_CONTRASTS));

                    mContrastsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", contrast));
                    sContrastsValue = contrast;
                    makeButtonReady(mContrastsBtn);


                    sDynContrastsValue = info.getInt(info.getColumnIndex(DbWork.SD_COL_DCONTRASTS));
                    mDynamicContrastsBtn.setText(String.format(Locale.ENGLISH, "Динамических контрастов: %d", sDynContrastsValue));
                    makeButtonReady(mDynamicContrastsBtn);

                    sOsValue = info.getInt(info.getColumnIndex(DbWork.SD_COL_SCREENINGS));
                    mOncoscreeningsBtn.setText(String.format(Locale.ENGLISH, "Динамических контрастов: %d", sOsValue));
                    makeButtonReady(mOncoscreeningsBtn);

                    String revenue = info.getString(info.getColumnIndex(DbWork.SD_COL_REVENUE));
                    sRevenueValue = revenue;
                    mRevenueInput.setText(revenue);
                    checkReady();
                    sDataRewrite = true;
                }
            }
            // попробую получить данные из интента о перенаправленной смене
            else if (outerInfo) {
                Log.d("surprise", "заполню данные из интента");
                // проверю пришедшие данные
                int day = mIntent.getIntExtra(DAY_FIELD, -1);
                int month = mIntent.getIntExtra(MONTH_FIELD, -1);
                int year = mIntent.getIntExtra(YEAR_FIELD, -1);
                int duration = mIntent.getIntExtra(DURATION, -1);
                Log.d("surprise", "day is " + day + " month is " + month + " year is " + year + " duration is " + duration);
                if (day >= 0 && month >= 0 && year >= 0) {
                    sYearValue = String.valueOf(year);
                    sMonthValue = String.valueOf(month);
                    sDayValue = String.valueOf(day);


                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DATE, day);
                    String month_s = String.format("%1$tB", calendar);
                    mDateBtn.setText(String.format(Locale.ENGLISH, "%d %s %d года", day, month_s, year));
                    makeButtonReady(mDateBtn);
                }
                if (duration > 0) {
                    mDurationBtn.setText(String.format(Locale.ENGLISH, "%d часов", duration));
                    sDurationValue = duration;
                    makeButtonReady(mDurationBtn);
                }
                sDataRewrite = true;
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(Notificator.SALARY_FILL_NOTIFICATION);
            }
        }
        if (sCenterValue == null) {
            // получу центр по умолчанию
            String defCenter = mPrefsManager.getString(SalaryActivity.FIELD_DEFAULT_CENTER, null);
            if (defCenter != null) {
                sCenterValue = defCenter;
                mCenterBtn.setText(defCenter);
                makeButtonReady(mCenterBtn);
            }
        }
        // восстановление настроек
        if (sYearValue != null) {
            mCalendar.set(Calendar.YEAR, Integer.parseInt(sYearValue));
            mCalendar.set(Calendar.MONTH, Integer.parseInt(sMonthValue));
            mCalendar.set(Calendar.DATE, Integer.parseInt(sDayValue));
            mMonthName = String.format("%1$tB", mCalendar);
            mDateBtn.setText(String.format(Locale.ENGLISH, "%s %s %s года", sDayValue, mMonthName, sYearValue));
            makeButtonReady(mDateBtn);
        }
        if (sCenterValue != null) {
            mCenterBtn.setText(sCenterValue);
            makeButtonReady(mCenterBtn);
        }
        if (sDurationValue != 0) {
            mDurationBtn.setText(String.format(Locale.ENGLISH, "%d часов", sDurationValue));
            makeButtonReady(mDurationBtn);
        }
        if (sContrastsValue != 0) {
            mContrastsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", sContrastsValue));
            makeButtonReady(mContrastsBtn);
        }
        if (sDynContrastsValue != 0) {
            mDynamicContrastsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", sDynContrastsValue));
            makeButtonReady(mDynamicContrastsBtn);
        }
        if (sOsValue != 0) {
            mOncoscreeningsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", sOsValue));
            makeButtonReady(mOncoscreeningsBtn);
        }
        if (sRevenueValue != null) {
            mRevenueInput.setText(sRevenueValue);
            checkReady();
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (checkReady()) {
            menu.add(0, MENU_SAVE_ID, 3, R.string.menu_save)
                    .setIcon(R.drawable.ic_check_black_24dp)
                    .setTitle(R.string.menu_save)
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        menu.add(0, MENU_CANCEL_ID, 2, getString(R.string.close_action))
                .setIcon(R.drawable.ic_close_black_24dp)
                .setTitle(R.string.close_action)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        if (mCurrentId != 0)
            menu.add(0, MENU_DELETE_ID, 1, getString(R.string.delete_action))
                    .setIcon(R.drawable.ic_delete_black_24dp)
                    .setTitle(R.string.delete_action)
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_CANCEL_ID:
                finish();
                break;
            case MENU_SAVE_ID:
                save();
                break;
            case MENU_DELETE_ID:
                delete();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // ================================================= СОХРАНЮ ДАННЫЕ
    private void save() {
        // Получу все необходимые сведения
        ContentValues cv = new ContentValues();
        cv.put(DbWork.SD_COL_DAY, Integer.valueOf(sDayValue));
        cv.put(DbWork.COL_MONTH, Integer.valueOf(sMonthValue));
        cv.put(DbWork.COL_YEAR, Integer.valueOf(sYearValue));
        cv.put(DbWork.SD_COL_CENTER, sCenterValue);
        cv.put(DbWork.SD_COL_DURATION, sDurationValue);
        cv.put(DbWork.SD_COL_REVENUE, sRevenueValue);
        cv.put(DbWork.SD_COL_CONTRASTS, sContrastsValue);
        cv.put(DbWork.SD_COL_DCONTRASTS, sDynContrastsValue);
        cv.put(DbWork.SD_COL_SCREENINGS, sOsValue);
        if (mCurrentId != 0) {
            mDb.deleteSalaryShift(mCurrentId);
        }
        mDb.insertRevenue(cv);
        Toast.makeText(getApplicationContext(), R.string.day_registered_message, Toast.LENGTH_LONG).show();
        // очищаю данные
        clearData();
        // обновлю данные виджета
        SalaryWidget.forceUpdateWidget();
        finish();
    }

    // ============================================ ОЧИЩАЮ СТАТИЧЕСКИЕ ДАННЫЕ
    private void clearData() {
        sYearValue = sMonthValue = sDayValue = sCenterValue = sRevenueValue = null;
        sDurationValue = sContrastsValue = sDynContrastsValue = sOsValue = 0;
        sDataRewrite = false;
    }

    private void selectCenterDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setItems(CENTERS_LIST, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sCenterValue = CENTERS_LIST[which];
                mCenterBtn.setText(CENTERS_LIST[which]);
                makeButtonReady(mCenterBtn);
            }
        });
        ad.show();
    }


    private void selectNumber(final View view, int startValue, int maxValue) {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener dialogConfirm = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int id = view.getId();
                int val = mNumberPicker.getValue();
                switch (id) {
                    case R.id.shift_duration:
                        sDurationValue = val;
                        if (sDurationValue > 0) {
                            mDurationBtn.setText(String.format(Locale.ENGLISH, "%d часов", val));
                            makeButtonReady(mDurationBtn);
                        } else
                            mDurationBtn.setText(R.string.select_shift_duration);
                        break;
                    case R.id.contrasts_btn:
                        sContrastsValue = val;
                        if (sContrastsValue > 0) {
                            mContrastsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", val));
                            makeButtonReady(mContrastsBtn);
                        } else
                            mContrastsBtn.setText(R.string.contrasts_count);
                        break;
                    case R.id.dyn_contrasts_button:
                        sDynContrastsValue = val;
                        if (sDynContrastsValue > 0) {
                            mDynamicContrastsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", val));
                            makeButtonReady(mDynamicContrastsBtn);
                        } else
                            mDynamicContrastsBtn.setText(R.string.dynamic_contrasts_count);
                        break;
                    case R.id.oncoscreenings_button:
                        sOsValue = val;
                        if (sOsValue > 0) {
                            mOncoscreeningsBtn.setText(String.format(Locale.ENGLISH, "Контрастов: %d", val));
                            makeButtonReady(mOncoscreeningsBtn);
                        } else
                            mOncoscreeningsBtn.setText(R.string.oncoscreenings_count);
                        break;
                }
            }
        };
        ad.setPositiveButton(android.R.string.ok, dialogConfirm);
        ad.setView(R.layout.number_picker);
        AlertDialog dialog = ad.create();
        // задаю название диалога
        int id = view.getId();
        String title = "";
        switch (id) {
            case R.id.shift_duration:
                title = getString(R.string.select_shift_duration);
                break;
            case R.id.contrasts_btn:
                title = getString(R.string.contrasts_count);
                break;
            case R.id.dyn_contrasts_button:
                title = getString(R.string.dynamic_contrasts_count);
                break;
            case R.id.oncoscreenings_button:
                title = getString(R.string.oncoscreenings_count);
                break;
        }
        dialog.setTitle(title);
        dialog.show();
        mNumberPicker = dialog.findViewById(R.id.timePicker);
        if (mNumberPicker != null) {
            mNumberPicker.setMinValue(0);
            mNumberPicker.setMaxValue(maxValue);
            mNumberPicker.setValue(startValue);
        }
    }

    // добавляю дату
    private void datePicker() {
        DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                if (mDb.checkDay(year, month, dayOfMonth)) {
                    mCalendar.set(Calendar.YEAR, year);
                    mCalendar.set(Calendar.MONTH, month);
                    mCalendar.set(Calendar.DATE, dayOfMonth);
                    mMonthName = String.format("%1$tB", mCalendar);
                    mDateBtn.setText(String.format(Locale.ENGLISH, "%d %s %d года", dayOfMonth, mMonthName, year));
                    sYearValue = String.valueOf(year);
                    sMonthValue = String.valueOf(month);
                    sDayValue = String.valueOf(dayOfMonth);
                    makeButtonReady(mDateBtn);
                } else {
                    // показываю предупреждение о неверно выбранном дне
                    Snackbar snackbar = Snackbar.make(mView, "Выбранный день уже заполнен", Snackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(Color.RED);
                    snackbar.show();
                }
            }
        };
        // определю заполнение начальных данных
        int year;
        int month;
        int day;
        if (sYearValue != null) {
            year = Integer.parseInt(sYearValue);
        } else {
            year = SalaryActivity.sYear;
        }
        if (sMonthValue != null) {
            month = Integer.parseInt(sMonthValue);
        } else {
            month = SalaryActivity.sMonth;
        }
        if (sDayValue != null) {
            day = Integer.parseInt(sDayValue);
        } else {
            day = SalaryActivity.sDay;
        }
        DatePickerDialog dpd = new DatePickerDialog(this, myDateListener, year, month, day);
        dpd.setTitle("I am title");
        dpd.show();
    }

    // =========================================== ПОМЕЧАЮ КНОПКУ КАК АКТИВНУЮ ===============================================
    private void makeButtonReady(Button btn) {
        //btn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.day_layout_clicked));
        btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary, getTheme()));
        checkReady();
        invalidateOptionsMenu();
    }

    // проверю готовность к сохранению записи
    @org.jetbrains.annotations.Contract(pure = true)
    private boolean checkReady() {
        return sYearValue != null && sMonthValue != null && sDayValue != null && sCenterValue != null && sDurationValue != 0 && sRevenueValue != null;
    }

    // Добавляю дату
    public void setDate(View view) {
        datePicker();
    }

    public void setCenter(View view) {
        selectCenterDialog();
    }

    public void setShiftDuration(View view) {
        selectNumber(view, 8, 24);
    }

    public void setCounter(View view) {
        selectNumber(view, 0, 50);
    }

    public void confirmSave(View view) {
        // пошагово проверю заполненность всех необходимых полей
        if (TextUtils.isEmpty(sYearValue)) {
            activateInput(mDateBtn, "Сначала нужно выбрать дату");
        } else if (TextUtils.isEmpty(sCenterValue)) {
            activateInput(mCenterBtn, "Сначала нужно выбрать центр");
        } else if (TextUtils.isEmpty(sRevenueValue)) {
            activateInput(mRevenueInput);
        } else if (sDurationValue == 0) {
            activateInput(mDurationBtn, "Сначала нужно ввести продолжительность смены");
        } else {
            save();
        }
    }

    private void activateInput(Button target, String info) {
        target.performClick();
        Snackbar.make(mRoot, info, Snackbar.LENGTH_SHORT).show();
    }

    private void activateInput(EditText target) {
        Snackbar.make(mRoot, "Сначала нужно ввести выручку", Snackbar.LENGTH_SHORT).show();
        target.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}
