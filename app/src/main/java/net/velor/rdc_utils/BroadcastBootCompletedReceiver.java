package net.velor.rdc_utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Objects;


public class BroadcastBootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED"))
            // установлю проверку смены на следующий день
            ServiceApplication.startMe(context, ServiceApplication.OPERATION_PLANE_SHIFT_REMINDER);
    }
}
