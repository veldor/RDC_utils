package net.velor.rdc_utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Objects;

import utils.LoginActivity;
import utils.Security;

public class ShiftSettingsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private DbWork mDb;
    private ShiftCursorAdapter mMyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_settings);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ShiftSettingsActivity.this, ShiftEditActivity.class);
                i.putExtra(ShiftEditActivity.MODE_TYPE, ShiftEditActivity.MODE_CREATE);
                startActivity(i);

            }
        });
        // подключусь к базе данных
        mDb = new DbWork(this);
        mDb.getConnection();
        ListView mList = findViewById(R.id.shift_list);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editShift(id);
            }
        });
        mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });
        registerForContextMenu(mList);
        Cursor shifts = mDb.getAllShifts();
        mMyAdapter = new ShiftCursorAdapter(ShiftSettingsActivity.this, shifts, 0);
        mList.setAdapter(mMyAdapter);
        getSupportLoaderManager().initLoader(0,null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!Security.isLogged(getApplicationContext())){
            // перенаправляю на страницу входа
            startActivity(new Intent(this, LoginActivity.class));
        }

        Objects.requireNonNull(getSupportLoaderManager().getLoader(0)).forceLoad();
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
                AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                deleteShift(acmi.id);
                break;
            case 2:
                acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                editShift(acmi.id);
                break;

        }
        return super.onContextItemSelected(item);
    }

    private void editShift(long id) {
        Intent i = new Intent(ShiftSettingsActivity.this, ShiftEditActivity.class);
        i.putExtra(ShiftEditActivity.MODE_TYPE, ShiftEditActivity.MODE_UPDATE);
        i.putExtra(ShiftCursorAdapter.COL_ID, id);
        startActivity(i);
    }

    private void deleteShift(final long id) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if(id != 1){
                    mDb.deleteShift(id);
                    Objects.requireNonNull(getSupportLoaderManager().getLoader(0)).forceLoad();
                    Toast.makeText(getApplicationContext(), getString(R.string.shift_delete), Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Этот пункт удалить нельзя", Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDb.closeConnection();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return new MyCursorLoader(getApplicationContext(), mDb);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        mMyAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
    static class MyCursorLoader extends CursorLoader {

        private DbWork mInnerDb;

        MyCursorLoader(@NonNull Context context, DbWork db) {
            super(context);
            mInnerDb = db;
        }

        @Override
        public Cursor loadInBackground() {
            return mInnerDb.getAllShifts();
        }
    }
}
