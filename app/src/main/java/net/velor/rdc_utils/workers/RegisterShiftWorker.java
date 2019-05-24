package net.velor.rdc_utils.workers;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.adapters.ShiftCursorAdapter;
import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.SalaryHandler;
import net.velor.rdc_utils.handlers.XMLHandler;

import java.util.Calendar;
import java.util.Map;

import utils.App;
import utils.Notificator;

public class RegisterShiftWorker extends Worker {
    public RegisterShiftWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Calendar calendar = Calendar.getInstance();
        //todo удалить после теста- получаю данные о завтрашней смене вместо сегодняшней
        calendar.add(Calendar.DATE, 1);
        // получу сведения о сегодняшней смене
        DbWork databaseProvider = App.getInstance().getDatabaseProvider();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        Cursor schedule = databaseProvider.getSchedule(year, month + 1);
        if(schedule != null && schedule.moveToFirst()) {
            // расписание найдено, получу данные о конкретном дне
            int day = calendar.get(Calendar.DATE);
            int dayType = XMLHandler.checkShift(schedule, day);
            schedule.close();
            // получу информацию о смене
            if(dayType != -1){
                Map<String, String> shiftInfo = databaseProvider.getShift(dayType);
                String start = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_START);
                String finish = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                Notificator notificator = new Notificator(App.getInstance());
                notificator.sendSalaryNotification(start, finish);
                // запланирую проверку на завтрашний день
                SalaryHandler.checkTomorrow();
            }
        }
        return Worker.Result.success();
    }
}
