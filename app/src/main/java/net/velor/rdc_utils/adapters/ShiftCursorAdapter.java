package net.velor.rdc_utils.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.velor.rdc_utils.R;

public class ShiftCursorAdapter extends CursorAdapter {


    public static final String COL_ID = "_id";
    public static final String COL_NAME_FULL = "name_full";
    public static final String COL_NAME_SHORT = "name_short";
    public static final String COL_SHIFT_START = "shift_start";
    public static final String COL_SHIFT_FINISH = "shift_end";
    public static final String COL_SHIFT_COLOR = "shift_color";
    public static final String COL_ALARM = "alarm";
    public static final String COL_ALARM_TIME = "alarm_time";
    public static final String COL_SCHEDULE_COLOR_NAME = "shift_default_color";

    private final LayoutInflater mInflater;
    private final Context mContext;

    public ShiftCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContext = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.shift_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // сделано, теперь- распределю данные
        String fullName = cursor.getString(cursor.getColumnIndex(COL_NAME_FULL));
        String shortName = cursor.getString(cursor.getColumnIndex(COL_NAME_SHORT));
        String shiftStart = cursor.getString(cursor.getColumnIndex(COL_SHIFT_START));
        String shiftFinish = cursor.getString(cursor.getColumnIndex(COL_SHIFT_FINISH));
        String shiftColor = cursor.getString(cursor.getColumnIndex(COL_SHIFT_COLOR));
        short alarm = cursor.getShort(cursor.getColumnIndex(COL_ALARM));
        String alarmTime = cursor.getString(cursor.getColumnIndex(COL_ALARM_TIME));

        // найду значения ввода
        TextView nameView = view.findViewById(R.id.shift_full_name);
        TextView sNameView = view.findViewById(R.id.shift_short_name);
        TextView sStartView = view.findViewById(R.id.shift_start_time);
        TextView sFinishView = view.findViewById(R.id.shift_finish_time);
        ImageView isAlarmView = view.findViewById(R.id.isAlarm);
        TextView alarmTimeView = view.findViewById(R.id.alarm_time);
        // поехали заполнять
        nameView.setText(fullName);
        // Если назначен цвет значка- заполняю его данным цветом, если нет- стандартным
        Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.shift_circle);
        assert drawable != null;
        drawable = DrawableCompat.wrap(drawable);
        if (shiftColor != null) {
            DrawableCompat.setTint(drawable.mutate(), Color.parseColor(shiftColor));
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        nameView.setCompoundDrawables(drawable, null, null, null);
        sNameView.setText(shortName);
        // если заполнена графа старта смены
        if (shiftStart != null) {
            sStartView.setText(shiftStart);
            sStartView.setVisibility(View.VISIBLE);
        } else {
            sStartView.setVisibility(View.INVISIBLE);
        }
        // если заполнена графа конца смены
        if (shiftFinish != null) {
            sFinishView.setText(String.format("-%s", shiftFinish));
            sFinishView.setVisibility(View.VISIBLE);
        } else {
            sFinishView.setVisibility(View.INVISIBLE);
        }

        if (alarm == 1) {
            isAlarmView.setVisibility(View.VISIBLE);
            if (alarmTime != null) {
                alarmTimeView.setText(alarmTime);
                alarmTimeView.setVisibility(View.VISIBLE);
            }
        } else {
            isAlarmView.setVisibility(View.INVISIBLE);
            alarmTimeView.setVisibility(View.INVISIBLE);
        }
    }
}
