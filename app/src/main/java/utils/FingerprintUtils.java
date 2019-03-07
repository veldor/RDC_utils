package utils;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

/**
 * Created by azret.magometov on 19-Dec-16.
 */

public final class FingerprintUtils {
    private FingerprintUtils() {
    }

    public static boolean enableToUse(Context context) {
    	// проверю доступность сканера отпечатков
		if(FingerprintUtils.isSensorStateAt(mSensorState.READY, context)){
			// проверю наличие сохранённого пин-кода
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String encoded = prefs.getString(LoginActivity.PIN, null);
			if(encoded != null){
				// раскодирую строку, чтобы получить пин
				String decoded = SimpleCryptoUtils.decode(encoded);
				if(decoded != null){
					// теперь закодирую полученную строку для использования со сканером отпечатков пальцев
					String fDecoded = CryptoUtils.encode(decoded);
					// сохраню строку в конфигурацию
					if(fDecoded != null){
						prefs.edit().putString(LoginActivity.FINGERPRINT_PIN, fDecoded).apply();
						return true;
					}

				}
			}
		}
        return false;
    }

    public static boolean disableToUse(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	prefs.edit().remove(LoginActivity.FINGERPRINT_PIN).apply();
    	return true;
    }

    public enum mSensorState {
        NOT_SUPPORTED,
        NOT_BLOCKED,
        NO_FINGERPRINTS,
        READY
    }

    public static boolean checkFingerprintCompatibility(@NonNull Context context) {
        return FingerprintManagerCompat.from(context).isHardwareDetected();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static mSensorState checkSensorState(@NonNull Context context) {
        if (checkFingerprintCompatibility(context)) {

            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (!keyguardManager.isKeyguardSecure()) {
                return mSensorState.NOT_BLOCKED;
            }

            if (!FingerprintManagerCompat.from(context).hasEnrolledFingerprints()) {
                return mSensorState.NO_FINGERPRINTS;
            }

            return mSensorState.READY;

        } else {
            return mSensorState.NOT_SUPPORTED;
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static boolean isSensorStateAt(@NonNull mSensorState state, @NonNull Context context) {
        return checkSensorState(context) == state;
    }
}
