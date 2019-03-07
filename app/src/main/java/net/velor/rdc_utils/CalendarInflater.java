package net.velor.rdc_utils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;

class CalendarInflater {
    private static final String MODE_NEW = "new";
    private static final String MODE_FILLED = "filled";
    private static final String HOLIDAY_VALUE = "-1";
    private final Cursor mMonthInfo;
    private final View.OnClickListener mHandler;
    private final LinearLayout mParent;
    private final int mYear;
    private final int mMonth;
    private final XMLHandler mXmlHandler;
    private final HashMap<String, HashMap<String, String>> mShiftTypes;
    private Context mContext;
    private int mThisDay;

    CalendarInflater(Context context, View.OnClickListener handler, Cursor monthInfo, LinearLayout parent, XMLHandler xmlHandler, HashMap<String, HashMap<String, String>> shiftTypes) {
        mContext = context;
        mMonthInfo = monthInfo;
        mHandler = handler;
        mParent = parent;
        mXmlHandler = xmlHandler;
        mShiftTypes = shiftTypes;

        // захвачу данные о месяце на момент создания
        mYear = MainActivity.sYear;
        mMonth = MainActivity.sMonth;
    }

    ScrollView getLayout() {
        // если есть информация о месяце- разбираю её, если нет- месяц не заполнен, все дни- выходные
        String mode;
        if(mMonthInfo.moveToFirst()){
            mode = MODE_FILLED;
        }
        else{
            mode = MODE_NEW;
        }
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><month year='");
        xml.append(MainActivity.sYear);
        xml.append("' month='");
        xml.append(MainActivity.sMonth);
        xml.append("'>");
        Calendar mycal = new GregorianCalendar(mYear, mMonth - 1, 1);

        // если идёт текущий месяц- найду сегодняшнее число
        Calendar now = new GregorianCalendar();
        if(now.get(Calendar.YEAR) == mYear && (now.get(Calendar.MONTH) + 1) == mMonth ){
            // найду сегодняшнее число
            mThisDay = now.get(Calendar.DATE);
        }
        // узнаю, сколько дней нужно прорисовать
        int daysInMonth = mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
        // С какого дня недели начинается месяц
        int mStartDay = mycal.get(Calendar.DAY_OF_WEEK);
        int offset;
        switch (mStartDay){
            case 1:
                offset = 6;
                break;
            default:
                offset = mStartDay - 2;
        }
        LayoutInflater li = LayoutInflater.from(mContext);
        // создам элемент вывода
        ScrollView parent = (ScrollView) li.inflate(R.layout.calendar_grid_layout, mParent, false);
        GridLayout gl = parent.findViewById(R.id.calendarGridLayout);
        ((TextView)parent.findViewById(R.id.monthName)).setText(android.text.format.DateFormat.format("LLLL", mycal));
        int counter = 0;
        int shiftsCounter = 0;
        // добавлю отслеживание конкретных смен
        SparseIntArray countedShifts = new SparseIntArray();
        while (counter < daysInMonth) {
            while(offset > 0){
                li.inflate(R.layout.calendar_empty_day_layout, gl);
                --offset;
            }
            ++counter;
            View dayLayout = li.inflate(R.layout.calendar_day_layout, gl, false);
            if(mThisDay != 0){
                if(counter < mThisDay){
                    dayLayout.setBackground(mContext.getDrawable(R.drawable.day_before_now_wrapper));
                }
                else if(counter == mThisDay){
                    dayLayout.setBackground(mContext.getDrawable(R.drawable.day_now_wrapper));
                }
            }
            ((TextView)dayLayout.findViewById(R.id.dayNum)).setText(String.valueOf(counter));
            if(mode.equals(MODE_FILLED)){
                String type = mXmlHandler.getDayType(String.valueOf(counter));
                if(type.equals(HOLIDAY_VALUE)){
                    dayLayout.findViewById(R.id.shiftRound).setVisibility(View.INVISIBLE);
                }
                else {
                    // добавляю смену
                    shiftsCounter++;
                    HashMap<String, String> info = mShiftTypes.get(type);
                    if(info != null){
                        // раскрашу кружок
                        String color = info.get(ShiftCursorAdapter.COL_SHIFT_COLOR);
                        if(color != null){
                            ImageView circle = dayLayout.findViewById(R.id.shiftRound);
                            circle.getDrawable().setColorFilter(new PorterDuffColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN));
                        }
                        // плюсую смену в счётчик
                        if(countedShifts.get(Integer.valueOf(type)) > 0){
                            countedShifts.put(Integer.valueOf(type), countedShifts.get(Integer.valueOf(type)) + 1);
                        }
                        else {
                            countedShifts.put(Integer.valueOf(type), 1);
                        }
                    }
                    else{
                        Log.d("surprise", "Неизвестный тип смены, меняю на выходной");
                        dayLayout.findViewById(R.id.shiftRound).setVisibility(View.INVISIBLE);
                        String monthSchedule = mXmlHandler.setDay(String.valueOf(counter), -1);
                        Log.d("surprise", monthSchedule);
                        DbWork db = new DbWork(mContext);
                        db.getConnection();
                        db.updateShedule(String.valueOf(mYear), String.valueOf(mMonth), monthSchedule);
                    }
                }
            }
            else{
                dayLayout.findViewById(R.id.shiftRound).setVisibility(View.INVISIBLE);
            }
            dayLayout.setId(counter);
            dayLayout.setOnClickListener(mHandler);
           gl.addView(dayLayout);
        }
        // добавлю строку с общим количеством смен
        LinearLayout monthParent = parent.findViewById(R.id.monthParent);
        li.inflate(R.layout.total_shifts_value, monthParent);
        ((TextView) parent.findViewById(R.id.shiftsCount)).setText(String.valueOf(shiftsCounter));
        for (int ctr = 0; ctr < countedShifts.size(); ctr++){
            int key = countedShifts.keyAt(ctr);
            CardView detail = (CardView) li.inflate(R.layout.shift_details, monthParent, false);
            // получу название смены
            String shName = Objects.requireNonNull(mShiftTypes.get(String.valueOf(key))).get(ShiftCursorAdapter.COL_NAME_FULL);
            ((TextView)detail.findViewById(R.id.shiftName)).setText(shName);
            ((TextView)detail.findViewById(R.id.shift_count)).setText(String.valueOf(countedShifts.get(key)));
            monthParent.addView(detail);
            String color = Objects.requireNonNull(mShiftTypes.get(String.valueOf(key))).get(ShiftCursorAdapter.COL_SHIFT_COLOR);
            if(color != null){
                ((ImageView) detail.findViewById(R.id.shiftRound)).getDrawable().setColorFilter(new PorterDuffColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN));
            }
            String start = Objects.requireNonNull(mShiftTypes.get(String.valueOf(key))).get(ShiftCursorAdapter.COL_SHIFT_START);
            String finish = Objects.requireNonNull(mShiftTypes.get(String.valueOf(key))).get(ShiftCursorAdapter.COL_SHIFT_FINISH);
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            if(start != null){
                sb.append("c ");
                sb.append(start);
            }
            if(finish != null){
                sb.append(" до ");
                sb.append(finish);
            }
            if(!sb.toString().equals("(")){
                sb.append(")");
                ((TextView)detail.findViewById(R.id.shift_details)).setText(sb.toString());
            }
        }
        return parent;
    }
}
