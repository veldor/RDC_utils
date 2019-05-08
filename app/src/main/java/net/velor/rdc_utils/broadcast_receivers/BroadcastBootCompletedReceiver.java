package net.velor.rdc_utils.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.velor.rdc_utils.handlers.ForemanHandler;

import java.util.Objects;

public class BroadcastBootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {
            // установлю проверку смены на следующий день
            ForemanHandler.startPlanner(false);
        }
    }
}
