package net.velor.rdc_utils.handlers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.velor.rdc_utils.MainActivity;

import utils.App;

public class SharedPreferencesHandler {
    private static final String PERSON_PREFERENCE_NAME = "person_name";

    public static void savePerson(String personName){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        preferences.edit().putString(PERSON_PREFERENCE_NAME, personName).apply();
    }

    public static String getPerson() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return preferences.getString(PERSON_PREFERENCE_NAME, "");
    }

    public static void resetPerson() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        preferences.edit().remove(PERSON_PREFERENCE_NAME).apply();
    }

    public static String getScheduleCheckTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return prefs.getString(MainActivity.FIELD_SCHEDULE_CHECK_TIME, "20:00");
    }
}
