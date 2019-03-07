package net.velor.rdc_utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

public class DeleteConfirmDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private AnswerDialogListener mListener;

    public interface AnswerDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_shift_delete_title))
                .setMessage(getString(R.string.dialog_shift_delete_message))
                .setPositiveButton(getString(R.string.dialog_shift_positive_btn_text), this)
                .setNegativeButton(android.R.string.no, this);

        return adb.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mListener = (AnswerDialogListener) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == Dialog.BUTTON_POSITIVE)
            mListener.onDialogPositiveClick(DeleteConfirmDialog.this);
    }
}

