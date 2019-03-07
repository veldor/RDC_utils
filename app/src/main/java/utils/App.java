package utils;

import android.app.Application;

public class App extends Application {
    private static App mAppInstance;

    public static App getInstance(){
        return mAppInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
    }
}
