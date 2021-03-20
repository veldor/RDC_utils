package utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import android.util.Log;

import net.velor.rdc_utils.MainActivity;
import net.velor.rdc_utils.R;
import net.velor.rdc_utils.SalaryActivity;
import net.velor.rdc_utils.SalaryDayActivity;
import net.velor.rdc_utils.receivers.AlarmStartReceiver;

import java.util.Calendar;

import static utils.CashHandler.timeToInt;

public class Notificator {
    private static final String SHIFTS_CHANNEL_ID = "shifts";
    private static final String SALARY_CHANNEL_ID = "salary";
    private static final String TEST_CHANNEL_ID = "test";
    public static final int NEXT_DAY_SHIFT_NOTIFICATION = 0;
    public static final int SALARY_FILL_NOTIFICATION = 1;
    public static final int TEST_NOTIFICATION = 2;
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final SharedPreferences mPreferences;

    public Notificator(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
    }

    public void sendShiftNotification(String title, String info, String text, boolean alarmEnabled, String[] alarmTimeArray) {
        // проверю, разрешено ли уведомлять о сменах
        if(mPreferences.getBoolean(MainActivity.FIELD_REMIND_TOMORROW, true)){
            // при нажатии на уведомление запушу главную активность смен
            Intent startMainIntent = new Intent(mContext, MainActivity.class);
            PendingIntent startMainPending = PendingIntent.getActivity(mContext, 0, startMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SHIFTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.logo_monochrom)
                    .setContentTitle(title)
                    .setContentText(info)
                    .setAutoCancel(true)
                    .setContentIntent(startMainPending)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));

            if (alarmEnabled) {
                Intent intent = new Intent(mContext, AlarmStartReceiver.class);
                // если назначено время будильника- подключу action с установкой времени будильника
                if (alarmTimeArray != null) {
                    intent.putExtra(AlarmStartReceiver.EXTRA_HOURS, Integer.valueOf(alarmTimeArray[0]));
                    intent.putExtra(AlarmStartReceiver.EXTRA_MINUTES, Integer.valueOf(alarmTimeArray[1]));
                }
                PendingIntent pending = PendingIntent.getBroadcast(mContext, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder.addAction(R.drawable.ic_access_alarm_black_24dp, mContext.getString(R.string.notification_set_alarm), pending);
            }

            Notification notification = notificationBuilder.build();
            mNotificationManager.notify(NEXT_DAY_SHIFT_NOTIFICATION, notification);
        }
    }

    public void sendSalaryNotification(String start, String finish) {
        if(mPreferences.getBoolean(MainActivity.FIELD_REMIND_SALARY, true)) {
            Calendar calendar = Calendar.getInstance();
            // при нажатии на кнопку- запущу регистрацию новой смены с заданными параметрами
            Intent newShift = new Intent(mContext, SalaryDayActivity.class);
            newShift.putExtra(SalaryDayActivity.HAS_INFO, true);
            newShift.putExtra(SalaryDayActivity.DAY_FIELD, calendar.get(Calendar.DATE));
            newShift.putExtra(SalaryDayActivity.MONTH_FIELD, calendar.get(Calendar.MONTH));
            newShift.putExtra(SalaryDayActivity.YEAR_FIELD, calendar.get(Calendar.YEAR));
            int intStart = timeToInt(start);
            int intFinish = timeToInt(finish);
            if (intStart > 0 && intFinish > 0) {
                newShift.putExtra(SalaryDayActivity.DURATION, intFinish - intStart);
            }
            PendingIntent registerShiftPending = PendingIntent.getActivity(mContext, 0, newShift, PendingIntent.FLAG_UPDATE_CURRENT);
            // при нажатии на уведомление запушу главную активность смен
            Intent startSalaryIntent = new Intent(mContext, SalaryActivity.class);
            PendingIntent startMainPending = PendingIntent.getActivity(mContext, 0, startSalaryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            String text = "Смена успешно закончена (судя по времени). Вы можете заполнить информацию о выручке для расчёта заработной платы";
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SALARY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.logo_monochrom)
                    .setContentTitle("Смена закончена")
                    .setContentText("Запишите выручку")
                    .setAutoCancel(true)
                    .setContentIntent(startMainPending)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .addAction(0, "Добавить сведения", registerShiftPending);
            Notification notification = notificationBuilder.build();
            mNotificationManager.notify(SALARY_FILL_NOTIFICATION, notification);
        }
    }

    void createNotificationChannels() {
        // создам каналы уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // создам канал уведомлений о сменах
            NotificationChannel nc = new NotificationChannel(SHIFTS_CHANNEL_ID, mContext.getString(R.string.shift_notifications_channel), NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription(mContext.getString(R.string.shifts_reminder));
            nc.enableLights(true);
            nc.setLightColor(Color.RED);
            nc.enableVibration(true);
            mNotificationManager.createNotificationChannel(nc);
            // создам канал уведомлений о зарплате
            nc = new NotificationChannel(SALARY_CHANNEL_ID, mContext.getString(R.string.salary_notifications_channel), NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription(mContext.getString(R.string.salary_reminder));
            nc.enableLights(true);
            nc.setLightColor(Color.BLUE);
            nc.enableVibration(true);
            mNotificationManager.createNotificationChannel(nc);
            // создам канал тестовых уведомлений
            nc = new NotificationChannel(TEST_CHANNEL_ID, mContext.getString(R.string.test_notifications_channel), NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription(mContext.getString(R.string.test_reminder));
            nc.enableLights(true);
            nc.setLightColor(Color.GREEN);
            nc.enableVibration(true);
            mNotificationManager.createNotificationChannel(nc);
        }
        App.getInstance().getSharedPreferencesHandler().notificationChannelsCreated();
        sendCustomNotification("Привет", "Я- приложение РДЦ. Буду считать вашу зарплату и отправлять вам напоминания о рабочих днях. И ещё много полезных вещей в перспективе. Надеюсь, мы сработаемся :)");
    }

    private void sendCustomNotification(String title, String test) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, TEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_monochrom)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(test));
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(TEST_NOTIFICATION, notification);
        Log.d("surprise", "Notificator sendCustomNotification: and i work");
    }
    public void sendScheduleChangedNotification() {
        Intent scheduleThisMonth = new Intent(mContext, MainActivity.class);
        scheduleThisMonth.putExtra(MainActivity.SCHEDULE_MONTH_SELECT , MainActivity.SCHEDULE_THIS_MONTH);
        PendingIntent fillThisMonthPending = PendingIntent.getActivity(mContext, 1, scheduleThisMonth, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent scheduleNextMonth = new Intent(mContext, MainActivity.class);
        scheduleNextMonth.putExtra(MainActivity.SCHEDULE_MONTH_SELECT , MainActivity.SCHEDULE_NEXT_MONTH);
        PendingIntent fillNextMonthPending = PendingIntent.getActivity(mContext, 0, scheduleNextMonth, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, TEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_monochrom)
                .setContentTitle("Обнаружено новое расписание")
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Выберите действие, чтобы загрузить расписание на данный или следующий месяц"))
                .addAction(0, "Этот месяц", fillThisMonthPending)
                .addAction(0, "Следующий месяц", fillNextMonthPending);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(TEST_NOTIFICATION, notification);
    }
}
