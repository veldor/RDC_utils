package net.velor.rdc_utils.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import net.velor.rdc_utils.R;
import net.velor.rdc_utils.SalaryActivity;
import net.velor.rdc_utils.database.DbWork;

import java.util.Locale;

import static utils.CashHandler.countPercent;
import static utils.CashHandler.roundTo;

public class SalaryShiftsAdapter extends CursorAdapter {

    private final LayoutInflater mInflater;
    private final boolean mOverLimit;
    private final String mForHour;
    private final String mUpBounty;
    private final String mContrastCost;
    private final String mNormalBounty;
    private final String mDContrastCost;
    private final String mOncoscreeningCost;
    private final SharedPreferences mPrefsManager;

    public SalaryShiftsAdapter(Context context, Cursor c, boolean overLimit, int flags) {
        super(context, c, flags);
        mOverLimit = overLimit;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // получу данные для расчёта зарплаты

        mPrefsManager = PreferenceManager.getDefaultSharedPreferences(context);
        mForHour = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_HOUR, "0");
        mNormalBounty = mPrefsManager.getString(SalaryActivity.FIELD_NORMAL_BOUNTY_PERCENT, "0");
        mUpBounty = mPrefsManager.getString(SalaryActivity.FIELD_HIGH_BOUNTY_PERCENT, "0");
        mContrastCost = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_CONTRAST, "0");
        mDContrastCost = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_DYNAMIC_CONTRAST, "0");
        mOncoscreeningCost = mPrefsManager.getString(SalaryActivity.FIELD_PAY_FOR_ONCOSCREENINGS, "0");
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.salary_day_layout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView date = view.findViewById(R.id.dayDate);
        date.setText(cursor.getString(cursor.getColumnIndex(DbWork.SD_COL_DAY)));
        TextView revenue = view.findViewById(R.id.revenueSumm);
        String revenueSumm = cursor.getString(cursor.getColumnIndex(DbWork.SD_COL_REVENUE));
        revenue.setText(String.format(Locale.ENGLISH, "Выручка: %s рублей", revenueSumm));
        TextView center = view.findViewById(R.id.centerName);
        center.setText(cursor.getString(cursor.getColumnIndex(DbWork.SD_COL_CENTER)));
        TextView contrasts = view.findViewById(R.id.contrastsSumm);
        String contrastsSumm = cursor.getString(cursor.getColumnIndex(DbWork.SD_COL_CONTRASTS));
        contrasts.setText(contrastsSumm);
        TextView dContrasts = view.findViewById(R.id.dynamicContrastsSumm);
        String dContrastsSumm = cursor.getString(cursor.getColumnIndex(DbWork.SD_COL_DCONTRASTS));
        dContrasts.setText(dContrastsSumm);
        String screeningsSumm = cursor.getString(cursor.getColumnIndex(DbWork.SD_COL_SCREENINGS));
        if(mPrefsManager.getBoolean(SalaryActivity.FIELD_SHOW_ONCOSCREENINGS, false)){
            TextView screenings = view.findViewById(R.id.oncoscreeningsSumm);
            screenings.setVisibility(View.VISIBLE);
            screenings.setText(screeningsSumm);
        }

        float hours = cursor.getFloat(cursor.getColumnIndex(DbWork.SD_COL_DURATION));
        // получу сумму, заработанную за день
        float summForHours = hours * Float.parseFloat(mForHour);
        float summForContrasts = Integer.parseInt(contrastsSumm) * Float.parseFloat(mContrastCost);
        float summForDContrasts = Integer.parseInt(dContrastsSumm) * Float.parseFloat(mDContrastCost);
        float summForScreenings = Integer.parseInt(screeningsSumm) * Float.parseFloat(mOncoscreeningCost);
        float ndfl = countPercent(summForHours, "13.");
        float totalSumm;
        if(mOverLimit){
            float summBounty = countPercent(Float.valueOf(revenueSumm), mUpBounty);
            totalSumm = summBounty + summForContrasts + summForDContrasts + summForScreenings - ndfl;
        }
        else{
            float summBounty = countPercent(Float.valueOf(revenueSumm), mNormalBounty);
            totalSumm = summBounty + summForContrasts + summForDContrasts + summForScreenings + summForHours - ndfl;
        }
        TextView salarySummView = view.findViewById(R.id.salarySumm);
        salarySummView.setText(String.format(Locale.ENGLISH, "Заработано %s рублей", roundTo(totalSumm)));
    }

}
