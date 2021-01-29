package net.velor.rdc_utils.workers;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.velor.rdc_utils.handlers.SharedPreferencesHandler;
import net.velor.rdc_utils.priv.Priv;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import utils.App;
import utils.MakeLog;
import utils.Notificator;

public class CheckScheduleChangeWorker extends Worker {


    public CheckScheduleChangeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "doWork: i start");
        OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(Priv.MD5_SUMM_LINK)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful()){
                    int code = response.code();
                    if(code == 200){
                        ResponseBody body = response.body();
                        if(body != null){
                            String result = body.string();
                            if(result.length() == 32){
                                MakeLog.writeToLog("Проверено расписание, хеш- " + result);
                                // сравню значение с записанным ранее
                                String oldValue = SharedPreferencesHandler.getScheduleHash();
                                if(!result.equals(oldValue)){
                                    // расписание сменилось- запишу новое значение и выдам нотификацию
                                    SharedPreferencesHandler.setScheduleHash(result);
                                    Notificator notificator = new Notificator(App.getInstance());
                                    notificator.sendScheduleChangedNotification();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.d("surprise", "doWork: has error " + e.getMessage());
                e.printStackTrace();
            }
        return Result.success();
    }
}
