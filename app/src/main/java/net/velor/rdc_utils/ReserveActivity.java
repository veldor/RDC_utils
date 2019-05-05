package net.velor.rdc_utils;

import android.Manifest;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.velor.rdc_utils.view_models.ReserveViewModel;
import net.velor.rdc_utils.workers.ReserveWorker;

import androidx.work.Data;
import androidx.work.WorkInfo;

public class ReserveActivity extends AppCompatActivity {

    private ReserveViewModel mMyViewModel;
    private int REQUEST_WRITE_READ = 3;
    private static LiveData<WorkInfo> sBackupResult;
    private View mRootView;
    private static LiveData<WorkInfo> sRestoreResult;
    private AlertDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve);
        mRootView = findViewById(R.id.rootView);

        // покажу кнопку "назад"
        assert getSupportActionBar() != null;   //null check
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // подключу модель

        mMyViewModel = ViewModelProviders.of(this).get(ReserveViewModel.class);
        // для работы потребуется доступ к хранилищу данных, запрошу его
        LiveData<Boolean> rightsChecker = mMyViewModel.checkRights();
        if(rightsChecker != null){
            rightsChecker.observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(@Nullable Boolean aBoolean) {
                    if(aBoolean != null && !aBoolean){
                        showRightsDialog();
                    }
                }
            });
        }

        // если активити перезапущено во время работы по сохранению/восстановлению настроек
        if(sBackupResult != null){
            sBackupResult.removeObservers(this);
            showProgressDialog(getString(R.string.backup_dialog_message));
            sBackupResult.observe(this, new MakeBackupObserver());
        }
        else if (sRestoreResult != null){
            Log.d("surprise", "ReserveActivity onCreate: restore restore dialog");
            sRestoreResult.removeObservers(this);
            showProgressDialog(getString(R.string.restore_dialog_message));
            sRestoreResult.observe(this, new RestoreBackupObserver());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        return true;
    }

    private void showRightsDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.permissions_dialog_title)
                .setMessage("Для использования функции необходимо предоставить доступ к памяти устройства")
                .setCancelable(false)
                .setPositiveButton(R.string.permissions_dialog_positive_answer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(ReserveActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_READ);
                    }
                })
                .setNegativeButton(R.string.permissions_dialog_negative_answer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        dialogBuilder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_READ) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showRightsDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void doBackupAction(View view) {
        switch (view.getId()){
            case R.id.backupSettings:
                 sBackupResult = mMyViewModel.doBackup();
                showProgressDialog(getString(R.string.backup_dialog_message));
                 sBackupResult.observe(this, new MakeBackupObserver());
                break;
            case R.id.restoreSettings:
                // закрою соединение с базой данных, чтобы спокойно обновить файлы базы данных
                // todo реализовать соедниение активностей с базой данных через синглтон
                sRestoreResult = mMyViewModel.doRestore();
                showProgressDialog(getString(R.string.restore_dialog_message));
                sRestoreResult.observe(this, new RestoreBackupObserver());
        }
    }

    private void showResultSnackbar(String text) {
        Snackbar.make(mRootView, text, Snackbar.LENGTH_LONG).show();
    }

    private void showProgressDialog(String title){
        if(mProgressDialog == null){
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder
                    .setView(R.layout.progress_dialog_layout)
                    .setCancelable(false);
            mProgressDialog = dialogBuilder.create();
        }
        mProgressDialog.setTitle(title);
        mProgressDialog.show();
    }
    private void hideProgressDialog(){
        if(mProgressDialog != null){
            mProgressDialog.hide();
        }
    }

    private class MakeBackupObserver implements Observer<WorkInfo> {
        @Override
        public void onChanged(@Nullable WorkInfo workInfo) {
            assert workInfo != null;
            if(workInfo.getState() == WorkInfo.State.SUCCEEDED){
                showResultSnackbar("Резервирование данных успешно завершено");
                hideProgressDialog();
                sBackupResult.removeObservers(ReserveActivity.this);
                sBackupResult = null;
            }
            else if(workInfo.getState() == WorkInfo.State.FAILED){
                showResultSnackbar("Не удалось создать копию ¯\\_(ツ)_/¯");
                hideProgressDialog();
                sBackupResult.removeObservers(ReserveActivity.this);
                sBackupResult = null;
            }
        }
    }
    private class RestoreBackupObserver implements Observer<WorkInfo>{

        @Override
        public void onChanged(@Nullable WorkInfo workInfo) {
            assert workInfo != null;
            if(workInfo.getState() == WorkInfo.State.SUCCEEDED){
                Data data = workInfo.getOutputData();
                int result = data.getInt(ReserveWorker.OPERATION_RESULT, 0);
                if(result == ReserveWorker.RESULT_NOT_FILE){
                    showResultSnackbar("Не найдены данные для восстановления");
                }
                else if(result == ReserveWorker.RESULT_RESTORED){
                    Toast.makeText(ReserveActivity.this, getString(R.string.setting_success_restored_message), Toast.LENGTH_LONG).show();
                    Log.d("surprise", "RestoreBackupObserver onChanged: can show toast");

                    new Handler().postDelayed(new ResetApp(), 1000);
                }
                sRestoreResult.removeObservers(ReserveActivity.this);
                sRestoreResult = null;
                hideProgressDialog();
            }
            else if(workInfo.getState() == WorkInfo.State.FAILED){
                showResultSnackbar("Не удалось восстановить данные ¯\\_(ツ)_/¯");
                sRestoreResult.removeObservers(ReserveActivity.this);
                sRestoreResult = null;
                hideProgressDialog();
            }
        }
    }

    private class ResetApp implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(ReserveActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ReserveActivity.this.startActivity(intent);
            Runtime.getRuntime().exit(0);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
