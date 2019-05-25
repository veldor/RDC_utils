package net.velor.rdc_utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.velor.rdc_utils.handlers.SalaryHandler;

import java.util.Locale;

public class AboutActivity extends AppCompatActivity {

    private int mCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        // покажу кнопку "назад"
        assert getSupportActionBar() != null;   //null check
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String version = "1.1";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TextView myNameView = findViewById(R.id.contributorName);
        TextView versionView = findViewById(R.id.appVersion);
        TextView myMailView = findViewById(R.id.contactMail);
        myNameView.setText(getString(R.string.contributor_name));
        versionView.setText(String.format(Locale.ENGLISH, getString(R.string.app_version), version));
        myMailView.setText(android.text.Html.fromHtml(getString(R.string.my_underlined_email)));

        mCounter = 0;
        versionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++ mCounter;
                if(mCounter == 20){
                    SharedPreferences prefsManager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    boolean isOsUnlocked = prefsManager.getBoolean(SettingsActivity.ONCOSCREENING_UNLOCK, false);
                    if(isOsUnlocked){
                        Toast.makeText(AboutActivity.this, getString(R.string.oncoscreening_settings_already_unlock), Toast.LENGTH_LONG).show();
                    }
                    else{
                        prefsManager.edit().putBoolean(SettingsActivity.ONCOSCREENING_UNLOCK, true).apply();
                        Toast.makeText(AboutActivity.this, getString(R.string.oncoscreening_settings_unlock), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });



        myMailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + getString(R.string.my_email)));
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_subject));
                intent.putExtra(Intent.EXTRA_TEXT, "");

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    //not_found_email_apps;
                    Toast.makeText(AboutActivity.this, "Не удаётся найти программу для отправки почты", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // перепроверю регистрацию смены
        SalaryHandler.planeRegistration();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
