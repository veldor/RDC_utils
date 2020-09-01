package net.velor.rdc_utils.handlers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.velor.rdc_utils.MainActivity;

import utils.App;

public class SharedPreferencesHandler {

    private static final String PREFERENCE_NC_CREATED = "notificatioin_channels_created";
    private static final String SCHEDULE_HASH_PREFERENCE_NAME = "schedule_hash";
    public static final String FTP_PASSWORD = "ftp_pass";
    private final SharedPreferences mPreferences;

    public SharedPreferencesHandler(Context context){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getFTPPassword() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return preferences.getString(FTP_PASSWORD, "");
    }

    public Boolean getNotificationChannelsCreated(){
        return mPreferences.getBoolean(PREFERENCE_NC_CREATED, false);
    }

    private static final String PERSON_PREFERENCE_NAME = "person_name";

    public static void savePerson(String personName){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        preferences.edit().putString(PERSON_PREFERENCE_NAME, personName).apply();
    }

    public static String getPerson() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return preferences.getString(PERSON_PREFERENCE_NAME, "");
    }
    public static String getScheduleHash() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return preferences.getString(SCHEDULE_HASH_PREFERENCE_NAME, "");
    }
    public static void setScheduleHash(String hash) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        preferences.edit().putString(SCHEDULE_HASH_PREFERENCE_NAME, hash).apply();
    }

    public static void resetPerson() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        preferences.edit().remove(PERSON_PREFERENCE_NAME).apply();
    }

    public static String getScheduleCheckTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return prefs.getString(MainActivity.FIELD_SCHEDULE_CHECK_TIME, "20:00");
    }

    public void notificationChannelsCreated() {
        mPreferences.edit().putBoolean(PREFERENCE_NC_CREATED, true).apply();
    }
}
