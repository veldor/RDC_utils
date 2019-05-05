package net.velor.rdc_utils.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import net.velor.rdc_utils.R;

import java.util.ArrayList;

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
                .setItems(stringArray, this);
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

