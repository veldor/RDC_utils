package utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public final class Security {
	public static final Integer LOGIN_REQUIRED = 1;
	private static final String LOGGING_STATUS = "logging_status";
	private static final String LOGGING_TIME = "logging_time";
	private static final String PROCESS_ID = "process_id";
	private static SharedPreferences sPreferences;

	public static Boolean isLogged(Context context) {
		sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		// проверю, включена ли защита пин-кодом
		if(sPreferences.getBoolean(EditPin.FIELD_USE_PIN, false)){
			if(sPreferences.getBoolean(LOGGING_STATUS, false)){
				// если сменился id процесса- приложение перезапущено
				int myPid = android.os.Process.myPid();
				if(myPid != sPreferences.getInt(PROCESS_ID, 0)){
					Log.d("surprise", "Другой идентификатор процесса");
					setLoggedOut();
					return false;
				}
				// если прошло больше 10 минут с момента последней активности- блокирую приложение
				Long loginTime = sPreferences.getLong(LOGGING_TIME, 0);
				Long currentTime = System.currentTimeMillis();
				if(currentTime - loginTime > 60000){
					Log.d("surprise", "Выход по таймауту");
					setLoggedOut();
					return false;
				}
				sPreferences.edit().putLong(LOGGING_TIME, System.currentTimeMillis()).apply();
				Log.d("surprise", "Вход по сохранённому значению");
				return true;
			}
			setLoggedOut();
			Log.d("surprise", "Аутентификация не пройдена");
			return false;
		}
		return true;
	}

	 static void setLoggedIn() {
		 sPreferences.edit().putBoolean(LOGGING_STATUS, true).putLong(LOGGING_TIME, System.currentTimeMillis()).putInt(PROCESS_ID, android.os.Process.myPid()).apply();
	 }
	 private static void setLoggedOut(){
		 sPreferences.edit().remove(LOGGING_STATUS).remove(LOGGING_TIME).remove(PROCESS_ID).apply();
	 }
 }
