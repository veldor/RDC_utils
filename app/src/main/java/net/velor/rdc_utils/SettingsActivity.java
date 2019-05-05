package net.velor.rdc_utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.velor.rdc_utils.handlers.ForemanHandler;

import java.util.List;

import utils.EditPin;
import utils.Fingerprint;
import utils.FingerprintUtils;
import utils.LoginActivity;
import utils.Security;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String ONCOSCREENING_UNLOCK = "oncoscreening_unlock";
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            // проверю, если настройка изменяет время проверки смены, обновлю событие
            if(preference.getKey().equals(MainActivity.FIELD_SCHEDULE_CHECK_TIME)){
                ForemanHandler.startPlanner();
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!Security.isLogged(getApplicationContext())){
            // перенаправляю на страницу входа
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SalaryPreferenceFragment.class.getName().equals(fragmentName)
                || SchedulePreferenceFragment.class.getName().equals(fragmentName)
                || SecurityPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows salary preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class SchedulePreferenceFragment extends PreferenceFragment {

        private TimePreference mCheckTime;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // тут добавлю настройку
            mCheckTime = new TimePreference(getActivity(), null);
            PreferenceScreen rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(rootScreen);
            mCheckTime.setKey(MainActivity.FIELD_SCHEDULE_CHECK_TIME);
            mCheckTime.setTitle(R.string.schedule_setting_title);
            setHasOptionsMenu(true);
            rootScreen.addPreference(mCheckTime);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(MainActivity.FIELD_SCHEDULE_CHECK_TIME));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows schedule preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */

    // ========================================== Настройки зарплаты ===========================================
    public static class SalaryPreferenceFragment extends PreferenceFragment {
        private SharedPreferences mPreferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {

            mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            super.onCreate(savedInstanceState);

            PreferenceScreen rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(rootScreen);

            EditTextPreference upLimit = new EditTextPreference(getActivity());
            upLimit.setKey(SalaryActivity.FIELD_UP_LIMIT);
            upLimit.setTitle(R.string.bounty_up_limit);
            EditText upLimitView = upLimit.getEditText();
            upLimitView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            upLimitView.setMaxEms(10);
            upLimitView.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
            rootScreen.addPreference(upLimit);

            EditTextPreference payForHour = new EditTextPreference(getActivity());
            payForHour.setKey(SalaryActivity.FIELD_PAY_FOR_HOUR);
            payForHour.setTitle(R.string.pay_for_hour);
            EditText payForHourView = payForHour.getEditText();
            payForHourView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            rootScreen.addPreference(payForHour);

            EditTextPreference payForContrast = new EditTextPreference(getActivity());
            payForContrast.setKey(SalaryActivity.FIELD_PAY_FOR_CONTRAST);
            payForContrast.setTitle(R.string.pay_for_contrast);
            EditText payForContrastView = payForContrast.getEditText();
            payForContrastView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            rootScreen.addPreference(payForContrast);

            EditTextPreference payForDynamicContrast = new EditTextPreference(getActivity());
            payForDynamicContrast.setKey(SalaryActivity.FIELD_PAY_FOR_DYNAMIC_CONTRAST);
            payForDynamicContrast.setTitle(R.string.pay_for_dynamic_contrast);
            EditText payForDynamicContrastView = payForDynamicContrast.getEditText();
            payForDynamicContrastView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            rootScreen.addPreference(payForDynamicContrast);

            EditTextPreference normalPercent = new EditTextPreference(getActivity());
            normalPercent.setKey(SalaryActivity.FIELD_NORMAL_BOUNTY_PERCENT);
            normalPercent.setTitle(R.string.normal_percent);
            EditText normalPercentView = normalPercent.getEditText();
            normalPercentView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            rootScreen.addPreference(normalPercent);

            EditTextPreference highPercent = new EditTextPreference(getActivity());
            highPercent.setKey(SalaryActivity.FIELD_HIGH_BOUNTY_PERCENT);
            highPercent.setTitle(R.string.high_percent);
            EditText highPercentView = highPercent.getEditText();
            highPercentView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            rootScreen.addPreference(highPercent);

            if(mPreferences.getBoolean(SettingsActivity.ONCOSCREENING_UNLOCK, false)){
                SwitchPreference isOncoscreenings = new SwitchPreference(getActivity());
                isOncoscreenings.setKey(SalaryActivity.FIELD_SHOW_ONCOSCREENINGS);
                isOncoscreenings.setSwitchTextOn(R.string.os_show);
                isOncoscreenings.setSwitchTextOff(R.string.os_hidden);
                isOncoscreenings.setTitle(R.string.show_os_field);
                rootScreen.addPreference(isOncoscreenings);



                EditTextPreference payForOs = new EditTextPreference(getActivity());
                payForOs.setKey(SalaryActivity.FIELD_PAY_FOR_ONCOSCREENINGS);
                payForOs.setTitle(R.string.pay_for_os);
                EditText payForOsView = payForOs.getEditText();
                payForOsView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                rootScreen.addPreference(payForOs);
                bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_PAY_FOR_ONCOSCREENINGS));

                isOncoscreenings.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if(!(boolean)newValue){
                            mPreferences.edit().putBoolean(ONCOSCREENING_UNLOCK, false).apply();
                            Toast.makeText(getActivity(), "Настройка будет скрыта", Toast.LENGTH_LONG).show();
                        }
                        return true;
                    }
                });
            }


            ListPreference defaultCenter = new ListPreference(getActivity());
            defaultCenter.setTitle(R.string.default_selected_center);
            defaultCenter.setEntries(SalaryDayActivity.CENTERS_LIST);
            defaultCenter.setEntryValues(SalaryDayActivity.CENTERS_LIST);
            defaultCenter.setKey(SalaryActivity.FIELD_DEFAULT_CENTER);
            rootScreen.addPreference(defaultCenter);

            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_UP_LIMIT));
            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_PAY_FOR_HOUR));
            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_PAY_FOR_CONTRAST));
            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_PAY_FOR_DYNAMIC_CONTRAST));
            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_NORMAL_BOUNTY_PERCENT));
            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_HIGH_BOUNTY_PERCENT));
            bindPreferenceSummaryToValue(findPreference(SalaryActivity.FIELD_DEFAULT_CENTER));

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows security preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class SecurityPreferenceFragment extends PreferenceFragment {
        private SwitchPreference mUseFingerprint;
        private SharedPreferences mPreferences;
        private SwitchPreference mUsePin;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            PreferenceScreen rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(rootScreen);

            mUsePin = new SwitchPreference(getActivity());
            mUsePin.setKey(EditPin.FIELD_USE_PIN);
            mUsePin.setTitle(R.string.use_pin);
            rootScreen.addPreference(mUsePin);

            if(FingerprintUtils.checkFingerprintCompatibility(getActivity())){
                mUseFingerprint = new SwitchPreference(getActivity());
                mUseFingerprint.setKey(Fingerprint.FIELD_USE_FINGERPRINT);
                mUseFingerprint.setTitle(R.string.use_fingerprint);
                rootScreen.addPreference(mUseFingerprint);
                mUseFingerprint.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if((Boolean)newValue){
                            return FingerprintUtils.enableToUse(getActivity());
                        }
                        else{
                            return FingerprintUtils.disableToUse(getActivity());
                        }
                    }
                });
            }

            // обработаю попытку изменения переключателя пин-кода
            mUsePin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                        // включаю вход по пин-коду
                        startActivity(new Intent(getActivity(), EditPin.class));
                    } else {
                        // выключаю вход по пин-коду
                        startActivityForResult(new Intent(getActivity(), LoginActivity.class), EditPin.DISABLE_PIN);
                    }
                    return false;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            mUsePin.setChecked(mPreferences.getBoolean(EditPin.FIELD_USE_PIN, false));
            if(FingerprintUtils.checkFingerprintCompatibility(getActivity())) {
                mUseFingerprint.setDependency(SalaryActivity.FIELD_USE_PIN);
                mUseFingerprint.setChecked(mPreferences.getBoolean(Fingerprint.FIELD_USE_FINGERPRINT, false));
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if(requestCode == EditPin.DISABLE_PIN && resultCode == Activity.RESULT_OK){
                mPreferences.edit().putBoolean(EditPin.FIELD_USE_PIN, false).apply();
                mPreferences.edit().putBoolean(Fingerprint.FIELD_USE_FINGERPRINT, false).apply();
            }
        }
    }
}
