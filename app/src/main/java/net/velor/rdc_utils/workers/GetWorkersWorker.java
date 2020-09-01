package net.velor.rdc_utils.workers;

import android.content.Context;
import androidx.annotation.NonNull;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.handlers.ScheduleHandler;

public class GetWorkersWorker extends Worker {
    public GetWorkersWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // получу день, который нужно проверить
        Data params = getInputData();
        int day = params.getInt(ScheduleHandler.CHECKED_DAY, 1);
        ScheduleHandler.getWorkers(day);
        return Worker.Result.success();
    }
}
