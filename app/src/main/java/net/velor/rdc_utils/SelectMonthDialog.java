package net.velor.rdc_utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.DatePicker;

import java.util.Calendar;

public class SelectMonthDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {


    private SelectMonthDialog.AnswerDialogListener mListener;

    public interface AnswerDialogListener {
        void onDialogPositiveClick(int year, int month);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mListener = (SelectMonthDialog.AnswerDialogListener) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString()
                    + " must implement AnswerDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get((Calendar.MONTH));
        return new DatePickerDialog(getActivity(), this, year, month, 1);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Log.d("surprise", "data set");
        mListener.onDialogPositiveClick(year, month);
    }
}
