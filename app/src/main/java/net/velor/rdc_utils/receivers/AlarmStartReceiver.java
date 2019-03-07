package net.velor.rdc_utils.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.velor.rdc_utils.R;

import utils.Notificator;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.EXTRA_HOUR;
import static android.provider.AlarmClock.EXTRA_SKIP_UI;


public class AlarmStartReceiver extends BroadcastReceiver {


    public static final String EXTRA_HOURS = "hours";
    public static final String EXTRA_MINUTES = "minutes";

    @Override
    public void onReceive(Context context, Intent intent) {
        // закрою меню уведомлений
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeIntent);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notificator.NEXT_DAY_SHIFT_NOTIFICATION);
        Intent alarmIntent = new Intent(ACTION_SET_ALARM);
        alarmIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        int hours = intent.getIntExtra(EXTRA_HOURS, -1);
        if(hours != -1){
            int minutes = intent.getIntExtra(EXTRA_MINUTES, 0);
            alarmIntent.putExtra(EXTRA_HOUR, hours);
            alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minutes);
            alarmIntent.putExtra(EXTRA_SKIP_UI, true);
        }
        context.startActivity(alarmIntent);
        Toast.makeText(context, context.getString(R.string.alarm_set_toast), Toast.LENGTH_LONG).show();
    }
}
