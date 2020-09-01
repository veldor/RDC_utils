package utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import net.velor.rdc_utils.R;

public class EditPin extends AppCompatActivity {

	public static final int ENABLE_PIN = 1;
	public static final int DISABLE_PIN = 0;
	public static final String FIELD_USE_PIN = "use_pin";
	private EditText mEnterPin;
	private EditText mReenterPin;
	private TextInputLayout mReenterPinParent;
	private SharedPreferences mPreferences;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
		setContentView(R.layout.activity_edit_pin);
		mEnterPin = findViewById(R.id.enterPin);
		mReenterPin = findViewById(R.id.repeatPin);

		mReenterPinParent = (TextInputLayout) mReenterPin.getParent().getParent();
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
					mReenterPinParent.setVisibility(View.VISIBLE);
					mReenterPin.requestFocus();
					validatePins();
				}
			}
		});
		mReenterPin.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (editable.length() == 4) {
					validatePins();
				}
			}
		});
	}

	private void validatePins() {
		new Handler().post(new Runnable() {
			@RequiresApi(api = Build.VERSION_CODES.M)
			@Override
			public void run() {
				// проверю, что в оба поля введено по 4 цифры и они совпадают
				Editable pinValue = mEnterPin.getText();
				Editable rPinValue = mReenterPin.getText();
				if (pinValue.length() == 4 && rPinValue.length() == 4) {
					if (pinValue.toString().equals(rPinValue.toString())) {
						// создам приватный и публичный ключ для шифрования данных
						// проверю наличие существующего ключа
						if(!SimpleCryptoUtils.isKey()){
							// Создаю новый ключ
							SimpleCryptoUtils.generateNewKey();
						}
						String enc = SimpleCryptoUtils.encode(pinValue.toString());
						// сохраню pin
						mPreferences.edit().putString(LoginActivity.PIN, enc).apply();
						mPreferences.edit().putBoolean(FIELD_USE_PIN, true).apply();
						Intent result = new Intent();
						setResult(RESULT_OK, result);
						finish();
					} else {
						mReenterPinParent.setErrorEnabled(true);
						mReenterPinParent.setError(getString(R.string.text_error_message));
					}
				}
			}
		});
	}
}
