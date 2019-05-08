package net.velor.rdc_utils;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.skydoves.colorpickerpreference.ColorEnvelope;
import com.skydoves.colorpickerpreference.ColorListener;
import com.skydoves.colorpickerpreference.ColorPickerDialog;
import com.skydoves.colorpickerpreference.FlagView;

import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.dialogs.DeleteConfirmDialog;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import utils.App;
import utils.LoginActivity;
import utils.Security;

public class ShiftEditActivity extends AppCompatActivity implements DeleteConfirmDialog.AnswerDialogListener {
    public final static String MODE_TYPE = "type";
    public final static String MODE_CREATE = "create";
    final static String MODE_UPDATE = "update";
    private static final String ALARM_ON = "1";
    private static final int MENU_SAVE_ID = 1;
    private static final int MENU_CANCEL_ID = 2;
    private static final int MENU_DELETE_ID = 3;

    private Button mAlarmBtn;
    // хранилища текстовых данных
    private TextView mShiftStartView, mShiftFinishView, mAlarmView;
    private EditText mColorView;
    private Switch mAlarmSwitcher;
    private DbWork mDb;
    private boolean mReady = false;

    // хранилища для значений
    private String mFullName = null;
    private String mShortName = null;
    private String mStartName = null;
    private String mFinishName = null;
    private String mColorName = null;
    private String mAlarmName = null;
    private boolean mAlarm = false;

    // режим
    private String mMode;
    private TextInputEditText mFullNameView, mShortNameView;
    private long mId;
    private View mRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_edit);

        mRoot = findViewById(R.id.rootView);

        final Intent i = getIntent();
        // назначу режим выполнения
        mMode = i.getStringExtra(MODE_TYPE);

        // создам подключение к базе данных
        mDb = App.getInstance().getDatabaseProvider();

        // назначу переменные
        mFullNameView = findViewById(R.id.shift_full_name);
        mShortNameView = findViewById(R.id.shift_short_name);
        mShiftStartView = findViewById(R.id.shift_start_time_view);
        mShiftFinishView = findViewById(R.id.shift_finish_time_view);
        mColorView = findViewById(R.id.edit_shift_color_view);
        mAlarmView = findViewById(R.id.shift_alarm_time_view);

        // если выбран режим обновления- загружу данные смены

        if (mMode.equals(MODE_UPDATE)) {
            mId = i.getLongExtra(ShiftCursorAdapter.COL_ID, 0);
            Map<String, String> data = mDb.getShift(mId);
            String fullNameValue = data.get(ShiftCursorAdapter.COL_NAME_FULL);
            mFullNameView.setText(fullNameValue);
            mFullName = fullNameValue;
            String shortNameValue = data.get(ShiftCursorAdapter.COL_NAME_SHORT);
            mShortNameView.setText(shortNameValue);
            mShortName = shortNameValue;
            String color = data.get(ShiftCursorAdapter.COL_SHIFT_COLOR);
            if (color != null) {
                // назначен цвет
                mColorView.setText(color);
                mColorName = color;
            }
            String start = data.get(ShiftCursorAdapter.COL_SHIFT_START);
            if (start != null) {
                mShiftStartView.setText(start);
                mStartName = start;
            }
            String finish = data.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
            if (finish != null) {
                mShiftFinishView.setText(finish);
                mFinishName = finish;
            }
            String alarm = data.get(ShiftCursorAdapter.COL_ALARM);
            assert alarm != null;
            if (alarm.equals(ALARM_ON)) {
                mAlarmSwitcher = findViewById(R.id.alarmSwitcher);
                mAlarmBtn = findViewById(R.id.set_alarm_time_btn);
                mAlarmSwitcher.setChecked(true);
                mAlarmBtn.setEnabled(true);
                String alarmTime = data.get(ShiftCursorAdapter.COL_ALARM_TIME);
                if (alarmTime != null) {
                    mAlarmView.setText(alarmTime);
                    mAlarmName = alarmTime;
                }
            }
        }


        // Попробую реализовать ручной выбор цвета
        mColorView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 7) {
                    String color = s.toString();
                    Pattern ptn = Pattern.compile("#[\\da-f]{6}", Pattern.CASE_INSENSITIVE);
                    if (ptn.matcher(color).matches()) {
                        // допустимый цвет, крашу кружок
                        paintRound(color);
                        mColorName = color;

                    }
                }
            }
        });


        // обрабатываю изменения информации
        final TextInputLayout fullNameLayout = findViewById(R.id.fullTextLayout);
        mFullNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                // проверю базу данных на наличие дубликатов

                if (s.length() > 0) {
                    checkDuplicates(s.toString(), ShiftCursorAdapter.COL_NAME_FULL, fullNameLayout);
                    mFullName = s.toString();
                } else {
                    mFullName = null;
                    setReady();
                }
            }
        });
        // обрабатываю изменения информации
        final TextInputLayout shortNameLayout = findViewById(R.id.shortTextLayout);
        mShortNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    checkDuplicates(s.toString(), ShiftCursorAdapter.COL_NAME_SHORT, shortNameLayout);
                    mShortName = s.toString();
                } else {
                    mShortName = null;
                    setReady();
                }
            }
        });


        // Диалог выбора цвета значка

        final ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(this);
        builder.setTitle(getString(R.string.color_select));
        builder.setPreferenceName("MyColorPickerDialog");
        builder.setFlagView(new CustomFlag(this, R.layout.color_flag));
        builder.setPositiveButton(getString(android.R.string.ok), new ColorListener() {
            @Override
            public void onColorSelected(ColorEnvelope colorEnvelope) {
                mColorView.setText(String.format("#%06X", (0xFFFFFF & colorEnvelope.getColor())));
                paintRound("#" + colorEnvelope.getColorHtml());
                mColorName = "#" + colorEnvelope.getColorHtml();
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        // покажу диалог при клике на кнопку выбора цвета
        Button colorSelectBtn = findViewById(R.id.edit_shift_color_btn);
        colorSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });

        // Обработаю кнопки настройки времени
        // хранилища кнопок
        mAlarmBtn = findViewById(R.id.set_alarm_time_btn);

        // переключатель будильника
        mAlarmSwitcher = findViewById(R.id.alarmSwitcher);
        mAlarmSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAlarmBtn.setEnabled(isChecked);
                mAlarm = isChecked;
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!Security.isLogged(getApplicationContext())) {
            // перенаправляю на страницу входа
            startActivityForResult(new Intent(this, LoginActivity.class), Security.LOGIN_REQUIRED);
        }
    }

    private boolean setReady() {
        invalidateOptionsMenu();
        return mReady && mFullName != null && mShortName != null;
    }

    private void timePicker(final View callerView) {
        // Get Current Time
        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String txt = String.format(Locale.ENGLISH, "%d:%02d", hourOfDay, minute);
                        switch (callerView.getId()) {
                            case R.id.edit_shift_start_time_bnt:
                                mShiftStartView.setText(txt);
                                mStartName = txt;
                                break;
                            case R.id.edit_shift_finish_time_bnt:
                                mShiftFinishView.setText(txt);
                                mFinishName = txt;
                                break;
                            case R.id.set_alarm_time_btn:
                                mAlarmView.setText(txt);
                                mAlarmName = txt;
                                break;
                        }
                    }
                }, 8, 0, true);
        timePickerDialog.setTitle("Название");
        Log.d("surprise", "name dialog");
        timePickerDialog.show();
        timePickerDialog.setTitle("Название");
    }

    public void setTime(View view) {
        timePicker(view);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // удалю смену
        delete();
    }

    public void confirmSave(View view) {
        // поочерёдно проверю все необходимые данные, если всё заполнено- сохраню тип смены
        if (TextUtils.isEmpty(mFullName)) {
            activateInput(mFullNameView, "Необходимо заполнить название");
        } else if (TextUtils.isEmpty(mShortName)) {
            activateInput(mShortNameView, "Необходимо заполнить короткое название");
        } else {
            save();
        }
    }

    private void activateInput(EditText target, String info) {
        target.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        Snackbar.make(mRoot, info, Snackbar.LENGTH_SHORT).show();
    }

    public class CustomFlag extends FlagView {

        private TextView textView;
        private View view;

        public CustomFlag(Context context, int layout) {
            super(context, layout);
            textView = findViewById(R.id.flag_color_code);
            view = findViewById(R.id.flag_color_layout);
        }

        @Override
        public void onRefresh(ColorEnvelope colorEnvelope) {
            textView.setText(String.format("#%06X", (0xFFFFFF & colorEnvelope.getColor())));
            view.setBackgroundColor(colorEnvelope.getColor());
        }
    }

    // РАСКРАШИВАЮ КРУЖОК
    private void paintRound(String color) {
        for (Drawable drawable : mColorView.getCompoundDrawables()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN));
            }
        }
    }

    private void checkDuplicates(final String value, final String col, final TextInputLayout ll) {
                int id = mDb.checkName(col, value);
                // если нашёлся повтор
                if (id != 0 && !(mMode.equals(MODE_UPDATE) && id == mId)) {
                    // дубликат
                    ll.setErrorEnabled(true);
                    ll.setError("Значение уже используется.");
                    mReady = false;
                } else {
                    ll.setErrorEnabled(false);
                    mReady = true;
                }
                setReady();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (setReady()) {
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
        if (mId > 0)
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

    private void delete() {
        mDb.deleteShift(mId);
        Toast.makeText(getApplicationContext(), getString(R.string.shift_delete), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void save() {
        final ContentValues cv = new ContentValues();
        cv.put(ShiftCursorAdapter.COL_NAME_FULL, mFullName);
        cv.put(ShiftCursorAdapter.COL_NAME_SHORT, mShortName);
        cv.put(ShiftCursorAdapter.COL_SHIFT_START, mStartName);
        cv.put(ShiftCursorAdapter.COL_SHIFT_FINISH, mFinishName);
        cv.put(ShiftCursorAdapter.COL_SHIFT_COLOR, mColorName);
        cv.put(ShiftCursorAdapter.COL_ALARM, mAlarm);
        cv.put(ShiftCursorAdapter.COL_ALARM_TIME, mAlarmName);
        if (mMode.equals(MODE_CREATE)) {
            mDb.insertShift(cv);
            Toast.makeText(getApplicationContext(), getString(R.string.shift_added), Toast.LENGTH_SHORT).show();
        } else if (mMode.equals(MODE_UPDATE)) {
            mDb.updateShift(cv, mId);
            Toast.makeText(getApplicationContext(), getString(R.string.shift_update), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
