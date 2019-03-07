package net.velor.rdc_utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;


import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import utils.Notificator;
import androidx.annotation.Nullable;

import static utils.CashHandler.timeToInt;

public class ServiceApplication extends Service {

    private static Integer TARGET_HOUR;
    private static Integer TARGET_MINUTES;


    static final String COMMAND_TYPE = "command_type";
    static final String SHIFT_START = "shift_start";
    static final String SHIFT_END = "shift_end";
    static final int OPERATION_REMIND_TOMORROW_SHIFT = 0;
    static final int OPERATION_PLANE_SHIFT_REMINDER = 1;
    static final int OPERATION_REMIND_REGISTER_SHIFT = 2;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("my_service", "My Background Service");
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "my_service")
                .setSmallIcon(R.drawable.logo_monochrom)
                .setPriority(Notification.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE);
        Notification notification = notificationBuilder.build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int operationType = intent.getIntExtra(COMMAND_TYPE, -1);

        switch (operationType) {
            case OPERATION_REMIND_TOMORROW_SHIFT:
                checkTomorrow();
                break;
            case OPERATION_PLANE_SHIFT_REMINDER:
                planeReminder();
                break;
            case OPERATION_REMIND_REGISTER_SHIFT:
                sendSalaryRegisterReminder(intent);
                break;
            default:
                stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendSalaryRegisterReminder(final Intent intent) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {

                // получу время начала и завершения смены
                String start = intent.getStringExtra(SHIFT_START);
                String finish = intent.getStringExtra(SHIFT_END);
                Notificator notifier = new Notificator(getApplicationContext());
                notifier.sendSalaryNotification(start, finish);

                // проверю, есть ли смена завтра. Если есть- переназначу напоминание о регистрации зарплаты на завтрашний день
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
                Map<String, String> tomorrowShift = checkDay(calendar);
                if(tomorrowShift != null){
                    String shiftFinish = tomorrowShift.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                    if(!TextUtils.isEmpty(shiftFinish)){
                        String shiftStart = tomorrowShift.get(ShiftCursorAdapter.COL_SHIFT_START);
                        remindRegisterShift(shiftStart, shiftFinish);
                    }
                }
            }
        });
    }

    private void planeReminder() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(getApplicationContext(), ServiceApplication.class);
                myIntent.putExtra(COMMAND_TYPE, OPERATION_REMIND_TOMORROW_SHIFT);
                PendingIntent pIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pIntent = PendingIntent.getForegroundService(getApplicationContext(), 1, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                else{
                    pIntent = PendingIntent.getService(getApplicationContext(), 1, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                Calendar cal = Calendar.getInstance();

                // получу настройки времени выполнения проверки
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String targetTime = prefs.getString(MainActivity.FIELD_SCHEDULE_CHECK_TIME, "20:00");
                // разобью значение
                assert targetTime != null;
                String[] time = targetTime.split(":");
                TARGET_HOUR = Integer.valueOf(time[0]);
                TARGET_MINUTES = Integer.valueOf(time[1]);
                // Проверю, если позже времени установки проверяльщика- установлю его на следующий день
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if(hour > TARGET_HOUR || (hour == TARGET_HOUR && cal.get(Calendar.MINUTE) >= TARGET_MINUTES)){
                    cal.add(Calendar.DATE, 1);
                }

                cal.set(Calendar.HOUR_OF_DAY, TARGET_HOUR);
                cal.set(Calendar.MINUTE, TARGET_MINUTES);
                cal.set(Calendar.SECOND, 0);
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);
                stopSelf();
            }
        });
    }

    private void checkTomorrow() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                // получу завтрашнее число
                calendar.add(Calendar.DATE, 1);
                // получу данные о завтрашней смене
                Map<String, String> shiftInfo = checkDay(calendar);
                if(shiftInfo != null){
                    // переменная для времени будильника, если оно назначено
                    boolean alarmEnabled = false;
                    String[] alarmTimeArray = null;
                    StringBuilder sb = new StringBuilder();
                    // получу сведения о смене
                    String name = shiftInfo.get(ShiftCursorAdapter.COL_NAME_FULL);
                    sb.append("Завтра вы работаете в РДЦ.");
                    String start = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_START);
                    if (start != null) {
                        sb.append(String.format(Locale.ENGLISH, " Начало смены: в %s часов.", start));
                    }
                    String finish = shiftInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                    if (finish != null) {
                        sb.append(String.format(Locale.ENGLISH, " Завершение смены: в %s часов.", finish));
                        remindRegisterShift(start, finish);
                    }
                    String alarm = shiftInfo.get(ShiftCursorAdapter.COL_ALARM);
                    if (alarm != null && alarm.equals("1")) {
                        alarmEnabled = true;
                        String alarmTime = shiftInfo.get(ShiftCursorAdapter.COL_ALARM_TIME);
                        if (alarmTime != null) {
                            alarmTimeArray = alarmTime.split(":");
                            sb.append(String.format(Locale.ENGLISH, " Не забудьте завести будильник на %s.", alarmTime));
                        }
                    }
                    sb.append(" Удачной вам смены!");
                    Notificator notif = new Notificator(getApplicationContext());
                    notif.sendShiftNotification(name, "На работку!", sb.toString(), alarmEnabled,alarmTimeArray);
                }
                // переназначаю проверку на завтрашний день
                ServiceApplication.startMe(getApplicationContext(), OPERATION_PLANE_SHIFT_REMINDER);
                stopSelf();
            }
        });
    }

    public void remindRegisterShift(final String start, final String finish) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // тут необходимо добавить проверку- назначена ли смена на сегодня, если да- не закончена ли она. Если закончена- предложение зарегистрировать сегодняшнюю смену уже отправлялось, переназначаю смену на завтра. Иначе - ничего не делаю, в момент отправки сообщения перезапускаю сервис проверки
                Calendar calendar = Calendar.getInstance();
                Map<String, String> todayInfo = checkDay(calendar);
                if(todayInfo != null){
                    // сегодня зарегистрирована смена
                    String shiftFinish = todayInfo.get(ShiftCursorAdapter.COL_SHIFT_FINISH);
                    if(!TextUtils.isEmpty(shiftFinish)){
                        assert shiftFinish != null;
                        Integer shiftEnd = Integer.valueOf(shiftFinish.split(":")[0]);
                        if(shiftEnd > calendar.get(Calendar.HOUR_OF_DAY)){
                            // смена закончена, назначаю напоминалку на завтра
                            return;
                        }
                    }
                }
                Intent myIntent = new Intent(getApplicationContext(), ServiceApplication.class);
                myIntent.putExtra(COMMAND_TYPE, OPERATION_REMIND_REGISTER_SHIFT);
                myIntent.putExtra(SHIFT_START, start);
                myIntent.putExtra(SHIFT_END, finish);
                PendingIntent pIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pIntent = PendingIntent.getForegroundService(getApplicationContext(), 2, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                else{
                    pIntent = PendingIntent.getService(getApplicationContext(), 2, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                // назначу время на окончание смены
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE , 1);
                cal.set(Calendar.HOUR_OF_DAY, timeToInt(finish));
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);
                stopSelf();
            }
        });
    }

    public static void startMe(Context context, int command){
        Intent wakeIntent = new Intent(context, ServiceApplication.class);
        wakeIntent.putExtra(ServiceApplication.COMMAND_TYPE, command);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(wakeIntent);
        }
        else{
            context.startService(wakeIntent);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName){
        NotificationChannel nc = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        nc.setLightColor(Color.BLUE);
        nc.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(nc);
    }

    public Map<String, String> checkDay(Calendar date){
        DbWork db = new DbWork(getApplicationContext());
        db.getConnection();
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        Cursor schedule = db.getShedule(year, month + 1);
        if(schedule != null && schedule.moveToFirst()){
            // расписание найдено, получу данные о конкретном дне
            int day = date.get(Calendar.DATE);
            int dayType = XMLHandler.checkShift(schedule, day);
            if(dayType != -1){
                // если день не выходной- получу данные о смене
                Map<String, String> shiftInfo = db.getShift(dayType);
                // если сведения о смене найдены- верну их
                if(shiftInfo != null){
                    schedule.close();
                    db.closeConnection();
                    return shiftInfo;
                }
            }
        }
        db.closeConnection();
        return null;
    }

}
