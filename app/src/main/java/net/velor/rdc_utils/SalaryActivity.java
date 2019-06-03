package net.velor.rdc_utils;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.whiteelephant.monthpicker.MonthPickerDialog;

import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.SalaryHandler;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import utils.App;
import utils.LoginActivity;
import utils.Security;

import static utils.CashHandler.addRuble;
import static utils.CashHandler.countPercent;

public class SalaryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // переменные настроек

    public static final String FIELD_UP_LIMIT = "up_limit";
    public static final String FIELD_PAY_FOR_HOUR = "pay_for_hour";
    public static final String FIELD_NORMAL_BOUNTY_PERCENT = "normal_bounty_percent";
    public static final String FIELD_HIGH_BOUNTY_PERCENT = "high_bounty_percent";
    public static final String FIELD_PAY_FOR_CONTRAST = "pay_for_contrast";
    public static final String FIELD_PAY_FOR_DYNAMIC_CONTRAST = "pay_for_dynamic_contrast";
    public static final String FIELD_SHOW_ONCOSCREENINGS = "show_oncoscreenings";
    public static final String FIELD_PAY_FOR_ONCOSCREENINGS = "pay_for_oncoscreenings";
    public static final String FIELD_USE_PIN = "use_pin";
    public static final String FIELD_DEFAULT_CENTER = "default_center";

    public static final String CENTER_AURORA_NAME = "Аврора";
    public static final Double NDFL_RATE = 13.;
    private static final int MENU_ITEM_COUNT_SALARYS = 1;
    private static final int MENU_ITEM_SHOW_SHIFTS = 2;
    private static final String FIRST_SALARY_CASH = "first_salary_cash";
    private static final String FIRST_SALARY_CASHLESS = "first_salary_cashless";
    private static final String FIRST_SALARY_TOTAL = "first_salary_total";
    private static final String SECOND_SALARY_CASH = "second_salary_cash";
    private static final String SECOND_SALARY_CASHLESS = "second_salary_cashless";
    private static final String SECOND_SALARY_TOTAL = "second_salary_total";


    static int sYear, sMonth, sDay;
    private DbWork mDb;
    private Calendar mCal;
    private DrawerLayout mDrawer;
    private SharedPreferences mPrefsManager;
    private String mForHour;
    private boolean mLimitOver;
    private String mUpBounty;
    private String mNormalBounty;
    private String mContrastCost = "0";
    private String mDContrastCost = "0";
    private String mOncoscreeningCost = "0";
    private Button mChooseMonthBtn;
    private TextView mMedianGainSumm, mBalanceSumm, mTotalGainSumm;
    private LayoutInflater mInflater;
    private LinearLayout mDetailsLayout;
    private AlertDialog mNoSettingsDialog;
    private boolean mDetailsVisible;
    private ConstraintLayout mRootView;
    private boolean mIsCc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.salary_activity);
        mRootView = findViewById(R.id.rootView);
        // зарегистрирую модель
        //SalaryViewModel myViewModel = ViewModelProviders.of(this).get(SalaryViewModel.class);
        // проверю обновления
        /*final LiveData<Boolean> version = myViewModel.startCheckUpdate();
        version.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null && aBoolean){
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar();
                }
            }
        });*/

        mInflater = getLayoutInflater();


        // включу поддержку тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Зарплата");

        // включу поддержку бокового меню
        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        // зарегистрирую пункты  меню для обработки
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mChooseMonthBtn = findViewById(R.id.salaryDate);

        // если не назначен год и месяц- назначаю текущие
        if (sYear == 0) {
            mCal = Calendar.getInstance();
            sYear = mCal.get(Calendar.YEAR);
            sMonth = mCal.get(Calendar.MONTH);
            sDay = mCal.get(Calendar.DATE);
            mChooseMonthBtn.setText(getDate());
        }
        // получу класс для работы с базой данных
        mDb = App.getInstance().getDatabaseProvider();

        // кнопка выбора месяца
        mChooseMonthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // открою окно с диалогом выбора даты
                MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(SalaryActivity.this, new MonthPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int selectedMonth, int selectedYear) {
                        sYear = selectedYear;
                        sMonth = selectedMonth;
                        mCal.set(Calendar.YEAR, sYear);
                        mCal.set(Calendar.MONTH, sMonth);
                        mChooseMonthBtn.setText(getDate());
                        if (!mIsCc) {
                            recountData();
                        }
                        else{
                            recountDataForCc();
                        }

                    }
                }, sYear, sMonth);
                builder.setActivatedMonth(sMonth)
                        .setMinYear(2010)
                        .setMaxYear(sYear + 5)
                        .setTitle(getString(R.string.select_month))
                        .build().show();

            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SalaryDayActivity.class));
            }
        });

        // Добавлю менеджер настроек
        mPrefsManager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // статичные поля

        mMedianGainSumm = findViewById(R.id.firstSalaryCashSumm);
        mBalanceSumm = findViewById(R.id.firstSalaryCashlessSumm);
        mDetailsLayout = findViewById(R.id.detailsLayout);
        mTotalGainSumm = findViewById(R.id.firstSalarySumm);

        // кнопка отображения\скрытия деталей
        final Button showDetail = findViewById(R.id.btnDetails);
        showDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDetailsVisible) {
                    mDetailsVisible = false;
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spoiler_close);
                    Drawable[] myTextViewCompoundDrawables = showDetail.getCompoundDrawables();
                    for (Drawable drawable : myTextViewCompoundDrawables) {
                        if (drawable == null)
                            continue;
                        ObjectAnimator animator = ObjectAnimator.ofInt(drawable, "level", 10000, 0);
                        animator.start();
                    }
                    mDetailsLayout.startAnimation(anim);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mDetailsLayout.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                } else {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spoiler_open);
                    Drawable[] myTextViewCompoundDrawables = showDetail.getCompoundDrawables();
                    for (Drawable drawable : myTextViewCompoundDrawables) {
                        if (drawable == null)
                            continue;
                        ObjectAnimator animator = ObjectAnimator.ofInt(drawable, "level", 0, 10000);
                        animator.start();
                    }
                    mDetailsLayout.startAnimation(anim);
                    mDetailsLayout.setVisibility(View.VISIBLE);
                    mDetailsVisible = true;
                }
            }
        });

        // определю, не нужно ли считать зарплату колл-центру
        mIsCc = mPrefsManager.getBoolean(MainActivity.FIELD_WORK_IN_CC, false);
    }

    private void recountDataForCc() {

    }

/*    private void makeUpdateSnackbar() {
        Log.d("surprise", "SalaryActivity makeUpdateSnackbar: send foundUpdateSnackbar");
        Snackbar updateSnackbar = Snackbar.make(mRootView, getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myViewModel.initializeUpdate();
            }
        });
        updateSnackbar.show();
    }*/

    private String getDate() {
        if (mCal == null) {
            mCal = Calendar.getInstance();
            mCal.set(Calendar.YEAR, sYear);
            mCal.set(Calendar.MONTH, sMonth);
            mCal.set(Calendar.DATE, 1);
        }
        return android.text.format.DateFormat.format("LLLL", mCal) + " " + sYear + " года";
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Security.isLogged(getApplicationContext())) {
            // перенаправляю на страницу входа
            startActivityForResult(new Intent(this, LoginActivity.class), Security.LOGIN_REQUIRED);
        }
        // скрою диалог
        if (mNoSettingsDialog != null) {
            mNoSettingsDialog.cancel();
        }
        if(!mIsCc){
            recountData();
        }
        else{
            recountDataForCc();
        }
        mChooseMonthBtn.setText(getDate());

        // перепроверю регистрацию смены
        SalaryHandler.planeRegistration();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_COUNT_SALARYS, 0, getString(R.string.count_salarys));
        menu.add(0, MENU_ITEM_SHOW_SHIFTS, 0, getString(R.string.show_shifts));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_COUNT_SALARYS:
                countSalary(this);
                break;
            case MENU_ITEM_SHOW_SHIFTS:
                showShifts();
        }
        return super.onOptionsItemSelected(item);
    }


    private void countSalary(final Context context) {
                AlertDialog.Builder ad = new AlertDialog.Builder(context);
                ad.setTitle("Расчёт заработной платы");
                // проведу расчёты
                // для начала, получу все смены. Потом расчитаю первую зарплату- все смены до 16 числа посчитаю по нормальным расценкам, с вычетом НДФЛ. Это будет сумма первой зарплаты. Потом расчитаю полную стоимость месяца и вычту из него первую часть. Это будет сумма второй зарплаты
                Cursor monthInfo = mDb.getMonthInfo(sYear, sMonth);
                if (monthInfo.moveToFirst()) {
                    HashMap<String, Float> salaryData;
                    salaryData = calculateSalary(monthInfo);
                    CardView view = (CardView) mInflater.inflate(R.layout.salarys_detais_dialog, mRootView, false);
                    Float firstSalaryTotal = salaryData.get(FIRST_SALARY_TOTAL);
                    if(firstSalaryTotal != null){
                        ((TextView)view.findViewById(R.id.firstSalarySumm)).setText(addRuble(firstSalaryTotal));
                    }
                    Float firstSalaryCashSumm = salaryData.get(FIRST_SALARY_CASH);
                    if(firstSalaryCashSumm != null){
                        ((TextView)view.findViewById(R.id.firstSalaryCashSumm)).setText(addRuble(firstSalaryCashSumm));
                    }
                    Float firstSalaryCashlessSumm = salaryData.get(FIRST_SALARY_CASHLESS);
                    if(firstSalaryCashlessSumm != null){
                        ((TextView)view.findViewById(R.id.firstSalaryCashlessSumm)).setText(addRuble(firstSalaryCashlessSumm));
                    }
                    Float secondSalarySumm = salaryData.get(SECOND_SALARY_TOTAL);
                    if(secondSalarySumm != null){
                        ((TextView)view.findViewById(R.id.secondSalarySumm)).setText(addRuble(secondSalarySumm));
                    }
                    Float secondSalaryCashSumm = salaryData.get(SECOND_SALARY_CASH);
                    if(secondSalaryCashSumm != null){
                        ((TextView)view.findViewById(R.id.secondSalaryCashSumm)).setText(addRuble(secondSalaryCashSumm));
                    }
                    Float secondSalaryCashlessSumm = salaryData.get(SECOND_SALARY_CASHLESS);
                    if(secondSalaryCashlessSumm != null){
                        ((TextView)view.findViewById(R.id.secondSalaryCashlessSumm)).setText(addRuble(secondSalaryCashlessSumm));
                    }
                    ad.setView(view);
                } else {
                    String salaryData = "Кто не работает- тот не ест. Смены не расчитаны";
                    ad.setMessage(salaryData);

                }
                // создам AlertDialog с данными
                ad.show();
    }

    private HashMap<String, Float> calculateSalary(Cursor monthInfo) {
        monthInfo.moveToFirst();
        HashMap<String, Float> result = new HashMap<>();
        float firstSalaryCash = 0;
        float firstSalaryCashless = 0;
        float secondSalaryCash = 0;
        float secondSalaryCashless = 0;

        do {
            // получу дату смены
            int date = monthInfo.getInt(monthInfo.getColumnIndex(DbWork.SD_COL_DAY));

            int hours = monthInfo.getInt(monthInfo.getColumnIndex(DbWork.SD_COL_DURATION));
            int ct = monthInfo.getInt(monthInfo.getColumnIndex(DbWork.SD_COL_CONTRASTS));
            int dct = monthInfo.getInt(monthInfo.getColumnIndex(DbWork.SD_COL_DCONTRASTS));
            int os = monthInfo.getInt(monthInfo.getColumnIndex(DbWork.SD_COL_SCREENINGS));
            float revenue = monthInfo.getFloat(monthInfo.getColumnIndex(DbWork.SD_COL_REVENUE));
            float summForHours = hours * Float.valueOf(mForHour);
            float summForContrasts = ct * Float.valueOf(mContrastCost);
            float summForDContrasts = dct * Float.valueOf(mDContrastCost);
            float summForScreenings = os * Float.valueOf(mOncoscreeningCost);
            double ndfl = countPercent(summForHours, NDFL_RATE);
            if (date < 16) {
                // считаю сумму за день по обычной ставке, потом проверяю, если процен повышен- считаю по повышенной
                float summBounty = countPercent(revenue, mNormalBounty);
                firstSalaryCash += summForHours - ndfl;
                firstSalaryCashless += summBounty + summForContrasts + summForDContrasts + summForScreenings;
                secondSalaryCash = firstSalaryCash;
                if (mLimitOver) {
                    // пересчитаю сумму выручки
                    summBounty = countPercent(revenue, mUpBounty);
                    secondSalaryCashless += summBounty + summForContrasts + summForDContrasts + summForScreenings - summForHours;
                } else {
                    secondSalaryCashless += summBounty + summForContrasts + summForDContrasts + summForScreenings;
                }
            } else {
                secondSalaryCash += summForHours - ndfl;
                // считаю сумму за день по обычной ставке, потом проверяю, если процен повышен- считаю по повышенной
                float summBounty;
                if (mLimitOver) {
                    // пересчитаю сумму выручки
                    summBounty = countPercent(revenue, mUpBounty);
                    secondSalaryCashless += summBounty + summForContrasts + summForDContrasts + summForScreenings - summForHours;
                } else {
                    summBounty = countPercent(revenue, mNormalBounty);
                    secondSalaryCashless += summBounty + summForContrasts + summForDContrasts + summForScreenings;
                }
            }
        }
        while (monthInfo.moveToNext());
        monthInfo.close();

        secondSalaryCash -= firstSalaryCash;
        secondSalaryCashless -= firstSalaryCashless;

        result.put(FIRST_SALARY_CASH, firstSalaryCash);
        result.put(FIRST_SALARY_CASHLESS, firstSalaryCashless);
        result.put(FIRST_SALARY_TOTAL, firstSalaryCash + firstSalaryCashless);
        result.put(SECOND_SALARY_CASH, secondSalaryCash);
        result.put(SECOND_SALARY_CASHLESS, secondSalaryCashless);
        result.put(SECOND_SALARY_TOTAL, secondSalaryCash + secondSalaryCashless);

        return result;
    }

    // ================================================ ПРОИЗВОЖУ РАСЧЁТЫ
    private void recountData() {
        // очищу родительский элемент
        mDetailsLayout.removeAllViews();
        Cursor revenues = mDb.getSalaryMonth(sYear, sMonth);
        if (revenues.moveToFirst()) {

            // заработанная сумма
            float totalAmount = 0;
            // пока заполню статичные данные
            // средняя выручка
            float medianGain = revenues.getFloat(revenues.getColumnIndex(DbWork.SM_COL_MEDIAN_GAIN));
            String limit = mPrefsManager.getString(SalaryActivity.FIELD_UP_LIMIT, null);
            int shiftsCount = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_TOTAL_SHIFTS));
            float gainTotal = revenues.getFloat(revenues.getColumnIndex(DbWork.SM_COL_TOTAL_GAIN));

            mMedianGainSumm.setText(addRuble(medianGain));

            if (limit == null || limit.equals("")) {
                noSettingDialog(this, "Лимит повышенной премии");
                return;
            }
            Float fLimit = Float.valueOf(limit);
            float debet = gainTotal - fLimit * shiftsCount;
            if (medianGain < fLimit) {
                // обычная премия
                mLimitOver = false;
                mMedianGainSumm.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
                // посчитаю недостаток
                mBalanceSumm.setText(addRuble(debet));
                mBalanceSumm.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
                mTotalGainSumm.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
            } else {
                // повышенная премия
                mLimitOver = true;
                mMedianGainSumm.setTextColor(getResources().getColor(R.color.colorPrimary, getTheme()));
                mBalanceSumm.setText(addRuble(debet));
                mBalanceSumm.setTextColor(getResources().getColor(R.color.colorPrimary, getTheme()));
                mTotalGainSumm.setTextColor(getResources().getColor(R.color.colorPrimary, getTheme()));
            }
            // Общее количество смен

            // количество смен в Авроре
            int shiftsInAurora = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_AURORA_SHIFTS));

            // количество смен на Нижневолжской набережной
            int shiftsInNv = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_NV_SHIFTS));

            // Общее количество часов
            int hours = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_TOTAL_HOURS));

            // оплата за часы
            mForHour = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_HOUR, null);
            if (mForHour == null || mForHour.equals("")) {
                noSettingDialog(this, "Оплата за час работы");
                return;
            }
            float salaryForHours = Float.valueOf(mForHour) * hours;

            double ndfl = countPercent(salaryForHours, NDFL_RATE);

            // Общее количество часов в Авроре
            int hoursInAurora = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_AURORA_HOURS));
            // Общее количество часов
            int hoursInNv = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_NV_HOURS));

            // Общая выручка
            mNormalBounty = mPrefsManager.getString(SalaryActivity.FIELD_NORMAL_BOUNTY_PERCENT, null);
            if (mNormalBounty == null || mNormalBounty.equals("")) {
                noSettingDialog(this, "Ставка премии");
                return;
            }
            mUpBounty = mPrefsManager.getString(SalaryActivity.FIELD_HIGH_BOUNTY_PERCENT, null);
            if (mUpBounty == null || mUpBounty.equals("")) {
                noSettingDialog(this, "Ставка повышенной премии");
                return;
            }
            float bounty;
            // расчёт премии
            if (mLimitOver) {
                // расчитываю премию по повышенной ставке
                bounty = countPercent(gainTotal, mUpBounty);
                totalAmount += bounty;

            } else {
                bounty = countPercent(gainTotal, mNormalBounty);
                totalAmount += bounty + salaryForHours;
            }
            // Выручка в Авроре
            float gainAurora = revenues.getFloat(revenues.getColumnIndex(DbWork.SM_COL_AURORA_GAIN));

            // Выручка на Нижневолжской набережной
            float gainNV = revenues.getFloat(revenues.getColumnIndex(DbWork.SM_COL_NV_GAIN));

            // Сделано контрастов
            int contrastsCount = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_CONTRASTS_COUNT));

            // Сделано динамических контрастов
            int dContrastsCount = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_DYNAMIC_CONTRASTS_COUNT));

            float contrastsSumm = 0;
            float dContrastsSumm = 0;
            float osSumm = 0;


            if (contrastsCount > 0) {
                mContrastCost = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_CONTRAST, null);
                if (mContrastCost == null || mContrastCost.equals("")) {
                    noSettingDialog(this, "Оплата за контраст");
                    return;
                }
                contrastsSumm = Float.valueOf(mContrastCost) * contrastsCount;
                totalAmount += contrastsSumm;

            }
            if (dContrastsCount > 0) {
                mDContrastCost = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_DYNAMIC_CONTRAST, null);
                if (mDContrastCost == null || mDContrastCost.equals("")) {
                    noSettingDialog(this, "Оплата за динамический контраст");
                    return;
                }
                dContrastsSumm = Float.valueOf(mDContrastCost) * dContrastsCount;
                totalAmount += dContrastsSumm;
            }

            int oncoscreeningsCount = 0;

            if (mPrefsManager.getBoolean(FIELD_SHOW_ONCOSCREENINGS, false)) {
                // Сделано онкоскринингов
                oncoscreeningsCount = revenues.getInt(revenues.getColumnIndex(DbWork.SM_COL_ONCOSCREENINGS_COUNT));
                if (oncoscreeningsCount > 0) {
                    mOncoscreeningCost = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_ONCOSCREENINGS, null);
                    if (mOncoscreeningCost == null || mOncoscreeningCost.equals("")) {
                        noSettingDialog(this, "Оплата за онкоскрининг");
                        return;
                    }
                    osSumm = Float.valueOf(mOncoscreeningCost) * oncoscreeningsCount;
                    totalAmount += osSumm;
                }
            }

            appendDetail("Оплата за часы", addRuble(salaryForHours));
            appendDetail("Премия", addRuble(bounty));
            if(contrastsSumm > 0){
                appendDetail("Стоимость контрастов", addRuble(contrastsSumm));
            }
            if(dContrastsSumm > 0){
                appendDetail("Стоимость динамических контрастов", addRuble(dContrastsSumm));
            }
            if(osSumm > 0){
                appendDetail("Стоимость онкоскринингов", addRuble(osSumm));
            }
            appendDetail("Итого начислено", addRuble(totalAmount));
            appendDetail("НДФЛ", addRuble(ndfl));

            appendDetail("Отработано смен", String.valueOf(shiftsCount));
            if (shiftsInAurora > 0) {
                appendDetail("Смен в Авроре", String.valueOf(shiftsInAurora));
            }
            if (shiftsInNv > 0) {
                appendDetail("Смен на Нижневолжской набережной", String.valueOf(shiftsInNv));
            }
            appendDetail("Отработано часов", String.valueOf(hours));
            if (hoursInAurora > 0) {
                appendDetail("Часов в Авроре", String.valueOf(hoursInAurora));
            }
            if (hoursInNv > 0) {
                appendDetail("Часов на Нижневолжской набережной", String.valueOf(hoursInNv));
            }
            appendDetail("Общая выручка", addRuble(gainTotal));
            if (gainAurora > 0) {
                appendDetail("Выручка, Аврора", addRuble(gainAurora));
            }
            if (gainNV > 0) {
                appendDetail("Выручка, Нижневолжская набережная", addRuble(gainNV));
            }
            appendDetail("Проведено контрастов", String.valueOf(contrastsCount));
            appendDetail("Проведено динамических контрастов", String.valueOf(dContrastsCount));
            if(osSumm > 0){
                appendDetail("Проведено онкоскринингов", String.valueOf(oncoscreeningsCount));
            }
            totalAmount -= ndfl;
            mTotalGainSumm.setText(addRuble(totalAmount));
        }
        else{
            mTotalGainSumm.setText(R.string.zero);
            mMedianGainSumm.setText(R.string.zero);
            mBalanceSumm.setText(R.string.zero);
        }
        revenues.close();
    }

    private void noSettingDialog(Context context, String value) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setTitle("Необходима настройка");
        ad.setMessage(String.format(Locale.ENGLISH, "Отстутствует значение %s.\n Необходимо перейти в настройки приложения и назначить его.\n", value));
        ad.setPositiveButton("Перейти в настройки", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SalaryPreferenceFragment.class.getName());
                settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivity(settingsIntent);
            }
        });
        mNoSettingsDialog = ad.create();
        mNoSettingsDialog.show();
    }

    private void appendDetail(String key, String value) {
        // создам элемент списка
        View view = mInflater.inflate(R.layout.salary_detail_card, mDetailsLayout, false);
        ((TextView) view.findViewById(R.id.detailText)).setText(key);
        ((TextView) view.findViewById(R.id.detailValue)).setText(value);
        mDetailsLayout.addView(view);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.schedule:
                //  запускаю расписание
                startActivity(new Intent(this, MainActivity.class));
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
        return true;
    }

    public void showShifts() {
        Intent caller = new Intent(this, SalaryShiftsInfoActivity.class);
        caller.putExtra("year", sYear);
        caller.putExtra("month", sMonth);
        caller.putExtra("overLimit", mLimitOver);
        startActivity(caller);
    }
}