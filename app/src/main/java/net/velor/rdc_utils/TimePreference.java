package net.velor.rdc_utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Locale;

public class TimePreference extends DialogPreference {
    private String mLastHour = "20";
    private String mLastMinute ="00";
    private TimePicker mPicker =null;

    static String getHour(String time) {

        String[] pieces = time.split(":");

        return(String.format(Locale.ENGLISH,"%02d", Integer.parseInt(pieces[0])));
    }

    static String getMinute(String time) {
        String[] pieces=time.split(":");

        return(String.format(Locale.ENGLISH,"%02d",Integer.parseInt(pieces[1])));
    }

    TimePreference(Context context, AttributeSet attrs) {

        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        mPicker = new TimePicker(getContext());
        return(mPicker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mPicker.setHour(Integer.valueOf(mLastHour));
        mPicker.setMinute(Integer.valueOf(mLastMinute));
        mPicker.setIs24HourView(true);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            mLastHour = String.format(Locale.ENGLISH,"%02d", mPicker.getHour());
            mLastMinute = String.format(Locale.ENGLISH,"%02d", mPicker.getMinute());

            String time = String.format(Locale.ENGLISH,"%s:%s", mLastHour, mLastMinute);

            if (callChangeListener(time)) {
                persistString(time);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return(a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;

        if (restoreValue) {
            if (defaultValue==null) {
                time = getPersistedString("20:00");
            }
            else {
                time = getPersistedString(defaultValue.toString());
            }
        }
        else {
            time = defaultValue.toString();
        }

        mLastHour =getHour(time);
        mLastMinute =getMinute(time);
    }
}
