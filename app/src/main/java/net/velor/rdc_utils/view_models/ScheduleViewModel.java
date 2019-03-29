package net.velor.rdc_utils.view_models;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import net.velor.rdc_utils.updates.Updater;

public class ScheduleViewModel extends ViewModel {

    public LiveData<Boolean> startCheckUpdate() {
        return Updater.checkUpdate();
    }

    public void initializeUpdate() {
        Updater.update();
    }
}