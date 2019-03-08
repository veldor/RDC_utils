package net.velor.rdc_utils;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import net.velor.rdc_utils.view_models.ScheduleViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import utils.LoginActivity;
import utils.Security;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DayShiftDialog.AnswerDialogListener, NavigationView.OnNavigationItemSelectedListener {

    // константы имён
    static final String FIELD_SCHEDULE_CHECK_TIME = "schedule_check_time";

    private static final int MENU_ITEM_SHIFT_SETTINGS = 1;

    private static final int PAGE_COUNT = 5;

    private static GregorianCalendar sCalendar = new GregorianCalendar();

    //текущие месяц и год
    public static int sYear, sMonth;
    public static int sDirection;
    private ViewPager mPager;
    private boolean sNeedRename;
    private DbWork mDb;

    // хранилище доступных типов смен
    private HashMap<String, HashMap<String, String>> mShifts = new HashMap<>();
    private View.OnClickListener mActivityLink;
    private DayShiftDialog mDayTypeDialog;
    private ArrayList<String> mShiftValues;
    private ArrayList<Integer> mShiftIds;
    private XMLHandler mXmlHandler;
    private DrawerLayout mDrawer;
    private ScheduleViewModel mMyViewModel;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mRootView = findViewById(R.id.rootView);

        mActivityLink = this;
        // создам подключение к базе данных
        mDb = new DbWork(this);
        mDb.getConnection();

        // включу поддержку тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Расписание");
        setSupportActionBar(toolbar);

        // включу поддержку бокового меню
        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        // зарегистрирую пункты  меню для обработки
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Если не заполнены месяц и год- заполню их текущими значениями
        if (sYear == 0) {
            sYear = sCalendar.get(Calendar.YEAR);
        }
        if (sMonth == 0) {
            sMonth = sCalendar.get(Calendar.MONTH) + 1;
        }
        // запускаю сервис приложения
        ServiceApplication.startMe(this, ServiceApplication.OPERATION_PLANE_SHIFT_REMINDER);

        // зарегистрирую модель
        mMyViewModel = ViewModelProviders.of(this).get(ScheduleViewModel.class);

        // проверю обновления
        // проверю обновления
        final LiveData<Boolean> version = mMyViewModel.startCheckUpdate();
        version.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                Log.d("surprise", "MainActivity onChanged: here " + aBoolean);
                if(aBoolean != null && !aBoolean){
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar();
                }
            }
        });
    }

    private void makeUpdateSnackbar() {
        Log.d("surprise", "MainActivity makeUpdateSnackbar: showing update snackbar");
            Snackbar updateSnackbar = Snackbar.make(mRootView, getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
            updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMyViewModel.initializeUpdate();
                }
            });
            updateSnackbar.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!Security.isLogged(getApplicationContext())){
            // перенаправляю на страницу входа
            startActivityForResult(new Intent(this, LoginActivity.class), Security.LOGIN_REQUIRED);
        }
        // загружу данные о сменах, создам диалог выбора типа смены, подключу менеджер страниц
        loadShifts();
    }

    // ===================================== ЗАГРУЗКА ТИПОВ СМЕН ======================================================

    private void loadShifts() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // получаю записи типов смен из базы данных
                Cursor shifts = mDb.getAllShifts();

                // сформирую массив строк для выбора типа дня
                mShiftValues = new ArrayList<>();
                mShiftIds = new ArrayList<>();

                // добавлю строку выходного
                mShiftValues.add(getString(R.string.day_type_hoiday));
                mShiftIds.add(-1);

                shifts.moveToFirst();
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
                prepareDayTypeDialog();
            }
        });
    }

    // ===================================== ПОДГОТОВКА ДИАЛОГА ВЫБОРА ТИПА СМЕНЫ ======================================

    private void prepareDayTypeDialog() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // подготовлю диалог выбора типа дня
                mDayTypeDialog = new DayShiftDialog();
                Bundle args = new Bundle();
                args.putStringArrayList("values", mShiftValues);
                mDayTypeDialog.setArguments(args);
                if (mPager == null) {
                    loadPager();
                }
                else{
                    drawCalendar();
                }
            }
        });
    }

    // ===================================== ПОДКЛЮЧЕНИЕ МЕНЕДЖЕРА СТРАНИЦ =============================================

    private void loadPager() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
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
                            } else {
                                if (sMonth == 12) {
                                    ++sYear;
                                    sMonth = 1;
                                } else {
                                    ++sMonth;
                                }
                            }
                            mPager.setCurrentItem(2, false);
                            sNeedRename = true;
                        }
                    }
                });
                mPager.setCurrentItem(2, false);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SHIFT_SETTINGS, 0, getString(R.string.shift_settings));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_SHIFT_SETTINGS:
                Intent i = new Intent(this, ShiftSettingsActivity.class);
                startActivity(i);
            break;
        }
        return super.onOptionsItemSelected(item);
    }


    // ===================================== ВЫБОР ТИПА ДНЯ ===================================================
    @Override
    public void onClick(View v) {
        mDayTypeDialog.show(getSupportFragmentManager(), String.valueOf(v.getId()));
    }

    // ==================================== ТИП ДНЯ ВЫБРАН ====================================================
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int selected) {
        assert dialog.getTag() != null;
        String day = dialog.getTag();
        int id = mShiftIds.get(selected);
        String monthSchedule = mXmlHandler.setDay(day, id);
        mDb.updateShedule(String.valueOf(sYear), String.valueOf(sMonth), monthSchedule);
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
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        // закрою боковое меню
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
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
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        drawCalendar();
                    }
                });
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
                String title;
                switch (position) {
                    case 0:
                    case 1:
                        if (sMonth == 1) {
                            title = (sYear - 1) + "-" + 12;
                        } else {
                            title = sYear + "-" + (sMonth - 1);
                        }
                        break;
                    case 2:
                        title = sYear + "-" + sMonth;
                        break;
                    case 3:
                        if (sMonth == 12) {
                            title = (sYear + 1) + "-" + 1;
                        } else {
                            title = sYear + "-" + (sMonth + 1);
                        }
                        break;
                    default:
                        title = "loading...";
                }
                return title;
            } else {
                // верну текущее имя
                return "";
            }
        }

    /*    public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }*/
    }

    public void appendMonth(final LinearLayout view, final View.OnClickListener activity) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                LinearLayout parent = view.findViewById(R.id.parent);
                Cursor monthInfo = mDb.getShedule(sYear, sMonth);
                mXmlHandler = new XMLHandler(monthInfo);
                CalendarInflater ci = new CalendarInflater(getApplicationContext(), activity, monthInfo, parent, mXmlHandler, mShifts);
                ScrollView layout = ci.getLayout();
                // найду родительский элемент
                parent.removeAllViews();
                parent.addView(layout);
                monthInfo.close();
            }
// Получу данные о месяце, если они есть

        });
    }

    public void drawCalendar() {
        PageFragment currentPageFragment = PageFragment.registeredFragments.get(2);
        if (currentPageFragment != null) {
            LinearLayout view = (LinearLayout) currentPageFragment.getView();
            if (view != null) {
                appendMonth(view, mActivityLink);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.closeConnection();
    }
}
