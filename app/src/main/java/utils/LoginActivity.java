package utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.velor.rdc_utils.R;

import javax.crypto.Cipher;

public class LoginActivity extends AppCompatActivity {

    public static final String PIN = "pin"; // идентификатор настройки с шифрованным значением пин-кода
    public static final String FINGERPRINT_PIN = "fingerprint_pin"; // идентификатор настройки с шифрованным значением пин-кода для сканера отпечатков
    private SharedPreferences mPreferences;
    private boolean isFingerprintUsed = false;
    private FingerprintHelper mFingerprintHelper;
	private EditText mEnterPin;
	private TextView mFingerprintDialog;

	@Override
    protected void onCreate(Bundle savedInstanceState) { // инициализация вьюхи
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this); // получаю настройки приложения, где должен храниться пин приложения
        mFingerprintDialog = findViewById(R.id.fingerprintUse);
        if(mPreferences.contains(Fingerprint.FIELD_USE_FINGERPRINT) && (isFingerprintUsed = mPreferences.getBoolean(Fingerprint.FIELD_USE_FINGERPRINT, false))){
            mFingerprintDialog.setVisibility(View.VISIBLE);
        }
        else{
            mFingerprintDialog.setVisibility(View.GONE);
        }
        mEnterPin = findViewById(R.id.enterPin);
		mEnterPin.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (editable.length() == 4) {
					// проверяю соответствие введённого и сохранённого пин-кода
					// вибрация в честь срабатывания
					Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						assert v != null;
						v.vibrate(VibrationEffect.createOneShot(300,5));
					}
					else {
						assert v != null;
						v.vibrate(300);
					}
					String encoded = mPreferences.getString(PIN, null);
					String decoded = SimpleCryptoUtils.decode(encoded);
					if(editable.toString().equals(decoded)){
						Snackbar.make(findViewById(R.id.activity_login), R.string.success_pin_enter, Snackbar.LENGTH_LONG)
								.setAction("Action", null).show();
						// считаю, что пользователь вошёл
                        Security.isLogged(getApplicationContext());
						Security.setLoggedIn();
						Intent result = new Intent();
						setResult(RESULT_OK, result);
						finish();
					}
					else{

						Snackbar.make(findViewById(R.id.activity_login), R.string.error_auth_message, Snackbar.LENGTH_LONG)
								.setAction("Action", null).show();
					}
					mEnterPin.setText("");
				}
			}
		});
    }

    @Override
    protected void onResume() { // при возобновлении приложения
        super.onResume();
        if (mPreferences.contains(PIN) && isFingerprintUsed) { // если в настройках приложения сохранён пин-код
            prepareSensor();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFingerprintHelper != null) {
            mFingerprintHelper.cancel();
        }
    }

    private void prepareSensor() {
        if (FingerprintUtils.isSensorStateAt(FingerprintUtils.mSensorState.READY, this)) { // если есть возможность входа по отпечатку пальцев- срабатывает данный блок
            FingerprintManagerCompat.CryptoObject cryptoObject = CryptoUtils.getCryptoObject(); // получу криптообьект для сканера отпечатков
            if (cryptoObject != null) { // при условии, что есть возможность войти по отпечатку пальцев (включена защита и не менялся набор отпечатков
                mFingerprintHelper = new FingerprintHelper(this);
                mFingerprintHelper.startAuth(cryptoObject); // активирую ожидание прикосновения к сканеру отпечатков
            } else { // если изменилась конфигурация сканера отпечатков
                mPreferences.edit().remove(FINGERPRINT_PIN).apply(); // убираю пин-код из настроек приложения
                Toast.makeText(this, R.string.reenter_pin, Toast.LENGTH_SHORT).show(); // предупреждаю о том, что нужно ввести новый пин-код
            }

        }
    }

    public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback { // класс-помошник для поддержки использования сканера отпечатков
        private Context mContext;
        private CancellationSignal mCancellationSignal;

        FingerprintHelper(Context context) {
            mContext = context;
        }

        void startAuth(FingerprintManagerCompat.CryptoObject cryptoObject) {
            mCancellationSignal = new CancellationSignal();
            FingerprintManagerCompat manager = FingerprintManagerCompat.from(mContext);
            manager.authenticate(cryptoObject, 0, mCancellationSignal, this, null);
        }

        void cancel() {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
			mFingerprintDialog.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_fingerprint_failure, 0, 0);
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        	// Успешная аутентификация
			mFingerprintDialog.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_fingerprint_success, 0, 0);

            Cipher cipher = result.getCryptoObject().getCipher();
            String encoded = mPreferences.getString(FINGERPRINT_PIN, null);
			assert cipher != null;
			String decoded = CryptoUtils.decode(encoded, cipher);
            mEnterPin.setText(decoded);
        }

        @Override
        public void onAuthenticationFailed() {
			mFingerprintDialog.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_fingerprint_failure, 0, 0);
			Snackbar.make(findViewById(R.id.activity_login), R.string.wrong_fingerprint_action, Snackbar.LENGTH_LONG)
					.setAction("Action", null).show();
        }
    }

}
