package net.velor.rdc_utils.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import net.velor.rdc_utils.R;
import net.velor.rdc_utils.ShiftEditActivity;
import net.velor.rdc_utils.ShiftSettingsActivity;

import java.util.ArrayList;

import utils.App;

public class DayShiftDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private AnswerDialogListener mListener;

    public interface AnswerDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, int choice);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        assert getArguments() != null;
        ArrayList<String> al = getArguments().getStringArrayList("values");
        assert al != null;
        String[] stringArray = new String[al.size()];
        stringArray = al.toArray(stringArray);
        assert getArguments() != null;
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.select_day_type))
                .setItems(stringArray, this)
                .setNegativeButton(App.getInstance().getString(R.string.edit_shfits_variant_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(App.getInstance(), ShiftSettingsActivity.class);
                        startActivity(i);
                    }
                })
                .setPositiveButton(App.getInstance().getString(R.string.add_shfit_variant_message), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(App.getInstance(), ShiftEditActivity.class);
                        i.putExtra(ShiftEditActivity.MODE_TYPE, ShiftEditActivity.MODE_CREATE);
                        startActivity(i);
                    }
                });
        return adb.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (AnswerDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
            mListener.onDialogPositiveClick(DayShiftDialog.this, which);
    }
}

