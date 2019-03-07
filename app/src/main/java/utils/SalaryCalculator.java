package utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import net.velor.rdc_utils.DbWork;
import net.velor.rdc_utils.SalaryActivity;

import java.nio.DoubleBuffer;
import java.util.Calendar;

public class SalaryCalculator {
    public static Float[] calculateSalary(){
        Context context = App.getInstance();
        // получу данные о текущем месяце
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        DbWork db = new DbWork(context);
        db.getConnection();
        Cursor info = db.getSalaryMonth(year, month);
        if(info != null && info.moveToFirst()){
            float salary = 0;
            float median = info.getFloat(info.getColumnIndex(DbWork.SM_COL_MEDIAN_GAIN));

            float total = info.getFloat(info.getColumnIndex(DbWork.SM_COL_TOTAL_GAIN));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            float tariffMedian = Float.valueOf(prefs.getString(SalaryActivity.FIELD_UP_LIMIT, "0"));

            // посчитаю НДФЛ

            int hours = info.getInt(info.getColumnIndex(DbWork.SM_COL_TOTAL_HOURS));
            Float payForHour = Float.valueOf(prefs.getString(SalaryActivity.FIELD_PAY_FOR_HOUR, "0"));
            float hoursSumm = payForHour * hours;
            double ndflRate = CashHandler.countPercent(hoursSumm, SalaryActivity.NDFL_RATE);

            int contrastsCount = info.getInt(info.getColumnIndex(DbWork.SM_COL_CONTRASTS_COUNT));
            if(contrastsCount > 0){
                Float contrastCost = Float.valueOf(prefs.getString(SalaryActivity.FIELD_PAY_FOR_CONTRAST, "0"));
                salary += contrastCost * contrastsCount;
            }
            int dContrastsCount = info.getInt(info.getColumnIndex(DbWork.SM_COL_DYNAMIC_CONTRASTS_COUNT));
            if(dContrastsCount > 0){
                Float dContrastCost = Float.valueOf(prefs.getString(SalaryActivity.FIELD_PAY_FOR_DYNAMIC_CONTRAST, "0"));
                salary += dContrastCost * dContrastsCount;
            }
            int oncoscreeningCount = info.getInt(info.getColumnIndex(DbWork.SM_COL_ONCOSCREENINGS_COUNT));
            if(oncoscreeningCount > 0){
                Float oncoscreeningCost = Float.valueOf(prefs.getString(SalaryActivity.FIELD_PAY_FOR_ONCOSCREENINGS, "0"));
                salary += oncoscreeningCost * oncoscreeningCount;
            }
            if(median >= tariffMedian){
                // расчёт по повышенному тарифу
                String bountyPercent = prefs.getString(SalaryActivity.FIELD_HIGH_BOUNTY_PERCENT, "0");
                salary += CashHandler.countPercent(total, bountyPercent);
            }
            else{
                // расчёт по обычному тарифу
                salary += hoursSumm;

                String bountyPercent = prefs.getString(SalaryActivity.FIELD_NORMAL_BOUNTY_PERCENT, "0");
                salary += CashHandler.countPercent(total, bountyPercent);
            }
            salary -= ndflRate;

            // посчитаю избыток или недостаток суммы
            int shiftsCount = info.getInt(info.getColumnIndex(DbWork.SM_COL_TOTAL_SHIFTS));
            float needed = shiftsCount * tariffMedian;

            float received = shiftsCount * median;

            float debet = received - needed;

            info.close();
            return new Float[]{salary, median, debet};
        }
        db.closeConnection();
        return null;
    }
}
