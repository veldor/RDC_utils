package net.velor.rdc_utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DbWork {

    private static final String TABLE_SALARY_DAY = "salary_day";
    private final DBHelper mHelper;
    private SQLiteDatabase mConnetion;

    public DbWork(Context context) {
        mHelper = new DBHelper(context);
    }

    private final int DB_VERSION = 1; // версия БД
    public static final String DB_NAME = "myDb";
    private final static String TABLE_SHIFTS = "shifts";
    private final static String TABLE_SALARY_MONTHS = "salary_months";
    private final String TABLE_SHEDULER = "sheduler";

    // salary_months_cols

    private final static String COL_ID = "_id"; // идентификатор
    final static String COL_YEAR = "c_year";
    final static String COL_MONTH = "c_month";
    public final static String SM_COL_MEDIAN_GAIN = "c_median_gain"; // средняя выручка
    public final static String SM_COL_TOTAL_SHIFTS = "c_total_shifts"; // всего смен
    final static String SM_COL_AURORA_SHIFTS = "c_aurora_shifts"; // смен в Авроре
    final static String SM_COL_NV_SHIFTS = "c_nv_shifts"; // смен на НВ
    public final static String SM_COL_TOTAL_HOURS = "c_total_hours"; // Отработано часов
    final static String SM_COL_AURORA_HOURS = "c_aurora_hours"; // Отработано часов в Авроре
    final static String SM_COL_NV_HOURS = "c_nv_hours"; // Отработано часов на НВ
    public final static String SM_COL_TOTAL_GAIN = "c_total_gain"; // выручка
    final static String SM_COL_AURORA_GAIN = "c_aurora_gain"; // выручка в Авроре
    final static String SM_COL_NV_GAIN = "c_nv_gain"; // выручка на НВ
    public final static String SM_COL_CONTRASTS_COUNT = "c_contrasts_count"; // сделано контрастов
    public final static String SM_COL_DYNAMIC_CONTRASTS_COUNT = "c_dynamic_contrasts_count"; // сделано дин.контрастов
    public final static String SM_COL_ONCOSCREENINGS_COUNT = "c_oncoscreenings_count"; // проведено онкоскринингов


    final static String SD_COL_DAY = "c_day";
    final static String SD_COL_CENTER = "c_center";
    final static String SD_COL_DURATION = "c_duration";
    final static String SD_COL_REVENUE = "c_revenue";
    final static String SD_COL_CONTRASTS = "c_contrasts";
    final static String SD_COL_DCONTRASTS = "c_d_contrasts";
    final static String SD_COL_SCREENINGS = "c_oncoscreenings";


    static final String COL_NAME_SHIFTS = "c_shedule";


    // ==================================== ПОЛУЧЕНИЕ ИНФОРМАЦИИ О МЕСЯЦЕ
    public Cursor getSalaryMonth(int year, int month) {
        String[] selectionArgs = new String[]{String.valueOf(year), String.valueOf(month)};
        return mConnetion.query(TABLE_SALARY_MONTHS, null, "c_year=? AND c_month=?", selectionArgs, null, null, null);
    }

    Cursor getAllShifts(){
        return mConnetion.query(TABLE_SHIFTS, null, null, null, null, null, null);
    }

    void updateShedule(String year, String month, String monthSchedule) {
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME_SHIFTS, monthSchedule);
        String[] selectionArgs = new String[]{year, month};
        int result = mConnetion.update(TABLE_SHEDULER,cv, "c_year=? AND c_month=?", selectionArgs);
        if(result == 0){
            Log.d("surprise", "Create absent row");
            cv.put(COL_YEAR, year);
            cv.put(COL_MONTH, month);
            mConnetion.insert(TABLE_SHEDULER, null, cv);
        }
    }
    // получу смены за выбранный месяц
    Cursor getMonthInfo(int year, int month) {
        String selection = String.format(Locale.ENGLISH, "%s = ? AND %s = ?", COL_YEAR, COL_MONTH);
        String[] args = {String.valueOf(year), String.valueOf(month)};
        return mConnetion.query(TABLE_SALARY_DAY, null, selection, args, null, null, SD_COL_DAY);
    }

    // ===================================== ДОБАВЛЮ СМЕНУ
    void insertShift(ContentValues cv) {
        mConnetion.insert(TABLE_SHIFTS, null, cv);
    }

    // ===================================== ДОБАВЛЮ ИНФОРМАЦИЮ О ВЫРУЧКЕ ЗА ДЕНЬ
    void insertRevenue(ContentValues cv) {
        mConnetion.beginTransaction();
        int year = cv.getAsInteger(COL_YEAR);
        int month = cv.getAsInteger(COL_MONTH);
        int day = cv.getAsInteger(SD_COL_DAY);
        // проверю, что данное число свободно
        if (checkDay(year, month, day)) {
            // получу данные о месяце
            Cursor monthInfo = getSalaryMonth(year, month);
            if (monthInfo.moveToFirst()) {
                // данные есть, обновлю информацию
                // данных нет, заполняю информацию
                ContentValues mcv = new ContentValues();
                // получу данные о центре
                String center = cv.getAsString(SD_COL_CENTER);
                // отработано часов
                int hours = cv.getAsInteger(SD_COL_DURATION);
                // выручка за день
                float gain = cv.getAsFloat(SD_COL_REVENUE);
                // выручка до этого
                float lastGain = monthInfo.getFloat(monthInfo.getColumnIndex(SM_COL_TOTAL_GAIN));
                float totalGain = gain + lastGain;
                // общая выручка
                mcv.put(SM_COL_TOTAL_GAIN, totalGain);
                // количество смен
                int shifts = monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_TOTAL_SHIFTS)) + 1;
                mcv.put(SM_COL_TOTAL_SHIFTS, shifts);
                // считаю общую выручку с округлением до 2 знаков после запятой
                float medianGain = (gain + lastGain) / shifts;
                mcv.put(SM_COL_MEDIAN_GAIN, medianGain);
                // прошлое значение отработанных часов
                int lastHours = monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_TOTAL_HOURS));
                mcv.put(SM_COL_TOTAL_HOURS, hours + lastHours);
                if (center.equals(SalaryActivity.CENTER_AURORA_NAME)) {
                    mcv.put(SM_COL_AURORA_SHIFTS, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_AURORA_SHIFTS)) + 1);
                    mcv.put(SM_COL_AURORA_HOURS, hours + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_AURORA_HOURS)));
                    // округляю
                    float aGain = gain + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_AURORA_GAIN));
                    mcv.put(SM_COL_AURORA_GAIN, aGain);
                } else {
                    mcv.put(SM_COL_NV_SHIFTS, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_NV_SHIFTS)) + 1);
                    mcv.put(SM_COL_NV_HOURS, hours + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_NV_HOURS)));
                    float nvGain = gain + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_NV_GAIN));
                    mcv.put(SM_COL_NV_GAIN, nvGain);
                }
                mcv.put(SM_COL_CONTRASTS_COUNT, cv.getAsInteger(SD_COL_CONTRASTS) + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_CONTRASTS_COUNT)));
                mcv.put(SM_COL_DYNAMIC_CONTRASTS_COUNT, cv.getAsInteger(SD_COL_DCONTRASTS) + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_DYNAMIC_CONTRASTS_COUNT)));
                mcv.put(SM_COL_ONCOSCREENINGS_COUNT, cv.getAsInteger(SD_COL_SCREENINGS) + monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_ONCOSCREENINGS_COUNT)));
                // Обновлю запись
                int id = monthInfo.getInt(monthInfo.getColumnIndex(COL_ID));
                String selection = String.format(Locale.ENGLISH, "%s = ?", COL_ID);
                String[] args = {String.valueOf(id)};
                mConnetion.update(TABLE_SALARY_MONTHS, mcv, selection, args);
            } else {
                // данных нет, заполняю информацию
                ContentValues mcv = new ContentValues();
                mcv.put(COL_YEAR, year);
                mcv.put(COL_MONTH, month);
                // получу данные о центре
                String center = cv.getAsString(SD_COL_CENTER);
                // отработано часов
                int hours = cv.getAsInteger(SD_COL_DURATION);
                // выручка
                float gain = cv.getAsFloat(SD_COL_REVENUE);
                mcv.put(SM_COL_TOTAL_GAIN, gain);
                mcv.put(SM_COL_MEDIAN_GAIN, gain);
                mcv.put(SM_COL_TOTAL_HOURS, hours);
                mcv.put(SM_COL_TOTAL_SHIFTS, 1);
                if (center.equals(SalaryActivity.CENTER_AURORA_NAME)) {
                    mcv.put(SM_COL_AURORA_SHIFTS, 1);
                    mcv.put(SM_COL_AURORA_HOURS, hours);
                    mcv.put(SM_COL_AURORA_GAIN, gain);
                } else {
                    mcv.put(SM_COL_NV_SHIFTS, 1);
                    mcv.put(SM_COL_NV_HOURS, hours);
                    mcv.put(SM_COL_NV_GAIN, gain);
                }
                mcv.put(SM_COL_CONTRASTS_COUNT, cv.getAsInteger(SD_COL_CONTRASTS));
                mcv.put(SM_COL_DYNAMIC_CONTRASTS_COUNT, cv.getAsInteger(SD_COL_DCONTRASTS));
                mcv.put(SM_COL_ONCOSCREENINGS_COUNT, cv.getAsInteger(SD_COL_SCREENINGS));
                mConnetion.insert(TABLE_SALARY_MONTHS, null, mcv);
            }
            mConnetion.insert(TABLE_SALARY_DAY, null, cv);
            mConnetion.setTransactionSuccessful();
            mConnetion.endTransaction();
        }
    }

    // ===================================== ПРОВЕРКА СВОБОДНОГО ДНЯ
    boolean checkDay(int year, int month, int day) {
        String selection = String.format(Locale.ENGLISH, "%s = ? AND %s = ? AND %s = ?", COL_YEAR, COL_MONTH, SD_COL_DAY);
        String[] args = {String.valueOf(year), String.valueOf(month), String.valueOf(day)};
        String[] columns = {COL_ID};
        Cursor c = mConnetion.query(TABLE_SALARY_DAY, columns, selection, args, null, null, null);
        if (c.getCount() > 0) {
            c.close();
            return false;
        }
        c.close();
        return true;
    }


    void deleteSalaryShift(long id) {
        // сперва найду смену
        Cursor shiftData = getSalaryDay(id);
        if (shiftData.moveToFirst()) {
            // получу месяц и год, запрошу данные о месяце для редактирования
            int year = shiftData.getInt(shiftData.getColumnIndex(COL_YEAR));
            int month = shiftData.getInt(shiftData.getColumnIndex(COL_MONTH));
            Cursor monthInfo = getSalaryMonth(year, month);
            if (monthInfo.moveToFirst()) {
                mConnetion.beginTransaction();
                int monthId = monthInfo.getInt(monthInfo.getColumnIndex(COL_ID));
                // количество смен
                int shifts = monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_TOTAL_SHIFTS)) - 1;
                // если количество смен после удаления записи будет равно нулю- удалю сведения о месяце
                if(shifts == 0){
                    deleteSalaryMonth(monthId);
                }
                else{
                    ContentValues mcv = new ContentValues();
                    // получу данные о центре
                    String center = shiftData.getString(shiftData.getColumnIndex(SD_COL_CENTER));
                    // отработано часов
                    int hours = shiftData.getInt(shiftData.getColumnIndex(SD_COL_DURATION));
                    // выручка за день
                    float gain = shiftData.getFloat(shiftData.getColumnIndex(SD_COL_REVENUE));
                    // выручка до этого
                    float lastGain = monthInfo.getFloat(monthInfo.getColumnIndex(SM_COL_TOTAL_GAIN));
                    float totalGain = lastGain - gain;
                    // общая выручка
                    mcv.put(SM_COL_TOTAL_GAIN, totalGain);
                    mcv.put(SM_COL_TOTAL_SHIFTS, shifts);
                    // считаю общую выручку с округлением до 2 знаков после запятой
                    float medianGain = (totalGain) / shifts;
                    mcv.put(SM_COL_MEDIAN_GAIN, medianGain);
                    // прошлое значение отработанных часов
                    int lastHours = monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_TOTAL_HOURS));
                    mcv.put(SM_COL_TOTAL_HOURS, lastHours - hours);
                    if (center.equals(SalaryActivity.CENTER_AURORA_NAME)) {
                        mcv.put(SM_COL_AURORA_SHIFTS, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_AURORA_SHIFTS)) - 1);
                        mcv.put(SM_COL_AURORA_HOURS, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_AURORA_HOURS)) - hours);
                        // округляю
                        float aGain = monthInfo.getFloat(monthInfo.getColumnIndex(SM_COL_AURORA_GAIN)) - gain;
                        mcv.put(SM_COL_AURORA_GAIN, aGain);
                    } else {
                        mcv.put(SM_COL_NV_SHIFTS, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_NV_SHIFTS)) - 1);
                        mcv.put(SM_COL_NV_HOURS, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_NV_HOURS)) - hours);
                        float nvGain = monthInfo.getFloat(monthInfo.getColumnIndex(SM_COL_NV_GAIN)) - gain;
                        mcv.put(SM_COL_NV_GAIN, nvGain);
                    }
                    mcv.put(SM_COL_CONTRASTS_COUNT, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_CONTRASTS_COUNT)) - shiftData.getInt(shiftData.getColumnIndex(SD_COL_CONTRASTS)));
                    mcv.put(SM_COL_DYNAMIC_CONTRASTS_COUNT, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_DYNAMIC_CONTRASTS_COUNT)) - shiftData.getInt(shiftData.getColumnIndex(SD_COL_DCONTRASTS)));
                    mcv.put(SM_COL_ONCOSCREENINGS_COUNT, monthInfo.getInt(monthInfo.getColumnIndex(SM_COL_ONCOSCREENINGS_COUNT)) - shiftData.getInt(shiftData.getColumnIndex(SD_COL_SCREENINGS)));
                    // Обновлю запись
                    String selection = String.format(Locale.ENGLISH, "%s = ?", COL_ID);
                    String[] args = {String.valueOf(monthId)};
                    mConnetion.update(TABLE_SALARY_MONTHS, mcv, selection, args);
                }
                // удалю смену
                deleteSalaryDay(id);
                mConnetion.setTransactionSuccessful();
                mConnetion.endTransaction();
                shiftData.close();
                monthInfo.close();
            }
        }
    }

    private void deleteSalaryMonth(long id){
        String selection = String.format(Locale.ENGLISH, "%s = ?", COL_ID);
        String[] args = {String.valueOf(id)};
        mConnetion.delete(TABLE_SALARY_MONTHS, selection, args);
    }
    private void deleteSalaryDay(long id){
        String selection = String.format(Locale.ENGLISH, "%s = ?", COL_ID);
        String[] args = {String.valueOf(id)};
        mConnetion.delete(TABLE_SALARY_DAY, selection, args);
    }

    Cursor getSalaryDay(long id) {
        return mConnetion.query(TABLE_SALARY_DAY, null, ShiftCursorAdapter.COL_ID + " = '" + id + "'", null, null, null, null);
    }

    Cursor getShedule(int year, int month){
        String[] selectionArgs = new String[]{String.valueOf(year), String.valueOf(month)};
        return mConnetion.query(TABLE_SHEDULER, null, "c_year=? AND c_month=?", selectionArgs, null, null, null);
    }

    Map<String, String> getShift(long id) {
        Cursor c = mConnetion.query(TABLE_SHIFTS, null, ShiftCursorAdapter.COL_ID + " = '" + id + "'", null, null, null, null);
        Map <String, String> values = new HashMap<>();
        if(c.moveToFirst()){
            values.put(ShiftCursorAdapter.COL_NAME_FULL, c.getString(c.getColumnIndex(ShiftCursorAdapter.COL_NAME_FULL)));
            values.put(ShiftCursorAdapter.COL_NAME_SHORT, c.getString(c.getColumnIndex(ShiftCursorAdapter.COL_NAME_SHORT)));
            values.put(ShiftCursorAdapter.COL_SHIFT_START, c.getString(c.getColumnIndex(ShiftCursorAdapter.COL_SHIFT_START)));
            values.put(ShiftCursorAdapter.COL_SHIFT_FINISH, c.getString(c.getColumnIndex(ShiftCursorAdapter.COL_SHIFT_FINISH)));
            values.put(ShiftCursorAdapter.COL_SHIFT_COLOR, c.getString(c.getColumnIndex(ShiftCursorAdapter.COL_SHIFT_COLOR)));
            values.put(ShiftCursorAdapter.COL_ALARM, String.valueOf(c.getInt(c.getColumnIndex(ShiftCursorAdapter.COL_ALARM))));
            values.put(ShiftCursorAdapter.COL_ALARM_TIME, c.getString(c.getColumnIndex(ShiftCursorAdapter.COL_ALARM_TIME)));
        }
        c.close();
        return values;
    }
    void updateShift(ContentValues cv, long id) {
        mConnetion.update(TABLE_SHIFTS,cv, ShiftCursorAdapter.COL_ID + " = " + id, null);
    }

    int checkName(String colName, String val){
        // проверю, нет ли уже в базе строки с данным именем
        Cursor c = mConnetion.query(TABLE_SHIFTS, null, colName + " = '" + val + "'", null, null, null, null);
        if(c.moveToFirst()){
            int id = c.getInt(c.getColumnIndex(ShiftCursorAdapter.COL_ID));
            c.close();
            return id;
        }
        c.close();
        return 0;
    }

    void deleteShift(long mId) {
        // удалю смену
        mConnetion.delete(TABLE_SHIFTS, ShiftCursorAdapter.COL_ID + " = " + mId, null);
    }

    public void getConnection() {
        mConnetion = mHelper.getWritableDatabase();
    }

   public void closeConnection() {
        mConnetion.close();
    }

    private class DBHelper extends SQLiteOpenHelper {

        DBHelper(Context context) {
            // конструктор суперкласса
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d("surprise", "create table");
            // создаю таблицу зарплаты
            db.execSQL("create table " + TABLE_SALARY_MONTHS + " (" +
                    "_id integer primary key autoincrement," +
                    "c_year INT(4) NOT NULL," +
                    "c_month INT(2) NOT NULL," +
                    "c_median_gain FLOAT DEFAULT 0, " +
                    "c_total_shifts INT DEFAULT 0, " +
                    "c_aurora_shifts INT DEFAULT 0, " +
                    "c_nv_shifts INT DEFAULT 0, " +
                    "c_total_hours INT DEFAULT 0, " +
                    "c_aurora_hours INT DEFAULT 0, " +
                    "c_nv_hours INT DEFAULT 0, " +
                    "c_total_gain FLOAT DEFAULT 0, " +
                    "c_aurora_gain FLOAT DEFAULT 0, " +
                    "c_nv_gain FLOAT DEFAULT 0, " +
                    "c_contrasts_count INT DEFAULT 0, " +
                    "c_dynamic_contrasts_count INT DEFAULT 0, " +
                    "c_oncoscreenings_count INT DEFAULT 0" + ");");

            // создаю таблицу для записи данных о сменах

            db.execSQL("create table " + TABLE_SALARY_DAY + " ("
                    + COL_ID + " integer primary key autoincrement," +
                    COL_YEAR + " INT(4) NOT NULL," +
                    COL_MONTH + " INT(2) NOT NULL," +
                    SD_COL_DAY + " INT(2) NOT NULL," +
                    SD_COL_CENTER + " STRING NOT NULL," +
                    SD_COL_DURATION + " INT(2) NOT NULL," +
                    SD_COL_CONTRASTS + " INT(2) DEFAULT 0," +
                    SD_COL_DCONTRASTS + " INT(2) DEFAULT 0," +
                    SD_COL_SCREENINGS + " INT(2) DEFAULT 0," +
                    SD_COL_REVENUE + " FLOAT NOT NULL" + ");");


            // создаю таблицу для календаря
            db.execSQL("create table sheduler ("
                    + "_id integer primary key autoincrement,"
                    + "c_year CHAR(4),"
                    + "c_month CHAR(2),"
                    + "c_shedule text" + ");");
            // создаю таблицу для типов смен
            db.execSQL("create table " + TABLE_SHIFTS + " (" +
                    "_id integer primary key autoincrement," +
                    " name_full VARCHAR(50) NOT NULL," +
                    " name_short CHAR(2) NOT NULL," +
                    " shift_start CHAR(5), shift_end CHAR(5)," +
                    " shift_color CHAR(7)," +
                    " alarm BOOL DEFAULT '0'," +
                    " alarm_time CHAR(5) );");
            // добавляю первую запись типа смены- стандартную, без возможности удаления
            ContentValues cv = new ContentValues();
            cv.put("name_full", "Утро");
            cv.put("name_short", "У");
            cv.put("shift_start", "8:00");
            cv.put("shift_end", "15:00");
            cv.put("shift_color", "#FF0000");
            cv.put("alarm", 1);
            cv.put("alarm_time", "5:30");
            db.insert(TABLE_SHIFTS, null, cv);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
