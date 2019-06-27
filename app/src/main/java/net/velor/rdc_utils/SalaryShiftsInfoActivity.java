package net.velor.rdc_utils;

import android.arch.persistence.room.Ignore;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import net.velor.rdc_utils.adapters.SalaryShiftsAdapter;
import net.velor.rdc_utils.database.DbWork;
import net.velor.rdc_utils.handlers.SalaryHandler;
import net.velor.rdc_utils.widgets.SalaryWidget;

import java.util.Objects;

import utils.App;
import utils.LoginActivity;
import utils.Security;

public class SalaryShiftsInfoActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private DbWork mDb;
    private SalaryShiftsAdapter mAdapter;
    private static int sYear;
    private static int sMonth;

    @Override
    @Ignore
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_shifts_info);
        Intent intent = getIntent();
        sYear = intent.getIntExtra("year", 0);
        sMonth = intent.getIntExtra("month", 0);
        boolean overLimit = intent.getBooleanExtra("overLimit", false);
        mDb = App.getInstance().getDatabaseProvider();
        Cursor data = mDb.getMonthInfo(sYear, sMonth);
        Log.d("surprise", "SalaryShiftsInfoActivity onCreate: " + data.moveToFirst());
        ListView parent = findViewById(R.id.shiftsList);
        registerForContextMenu(parent);
        parent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editShift(id);
            }
        });
        mAdapter = new SalaryShiftsAdapter(getApplicationContext(), data, overLimit, 0);
        parent.setAdapter(mAdapter);
        getSupportLoaderManager().initLoader(0,null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!Security.isLogged(getApplicationContext())){
            // перенаправляю на страницу входа
            startActivityForResult(new Intent(this, LoginActivity.class), Security.LOGIN_REQUIRED);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, "Удалить");
        menu.add(0, 2, 0, "Редактировать");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case 1: //удаляю раздел, на котором вызвано меню
                AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
                deleteShift(acmi.id);
                break;
            case 2:
                acmi = (AdapterContextMenuInfo) item.getMenuInfo();
                editShift(acmi.id);
                break;

        }
        return super.onContextItemSelected(item);
    }

    private void editShift(long id) {
        Intent intent = new Intent(this, SalaryDayActivity.class);
        intent.putExtra(SalaryDayActivity.ID, id);
        startActivity(intent);
        finish();
    }
    // асинхронно удалю смену
    private void deleteShift(final long id){
                mDb.deleteSalaryShift(id);
                SalaryWidget.forceUpdateWidget();
                Toast.makeText(getApplicationContext(), "Данные удалены", Toast.LENGTH_LONG).show();
                Objects.requireNonNull(getSupportLoaderManager().getLoader(0)).forceLoad();
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return new MyCursorLoader(getApplicationContext(), mDb);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    static class MyCursorLoader extends CursorLoader {

        private DbWork mInnerDb;

        MyCursorLoader(@NonNull Context context,DbWork db) {
            super(context);
            mInnerDb = db;
        }

        @Override
        public Cursor loadInBackground() {
            return mInnerDb.getMonthInfo(sYear, sMonth);
        }
    }
}
